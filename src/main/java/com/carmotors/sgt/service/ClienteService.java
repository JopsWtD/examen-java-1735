package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.util.InputValidator;

import java.util.List;

/**
 * Capa de lógica de negocio para la gestión de clientes (RF-17, RN-13, RN-17).
 */
public class ClienteService {

    private static final int MAX_LONGITUD_NOMBRE = 100;
    private static final int MAX_LONGITUD_DOCUMENTO = 30;
    private static final int MAX_LONGITUD_TELEFONO = 20;
    private static final int MAX_LONGITUD_EMAIL = 100;

    private final ClienteDAO clienteDAO;

    public ClienteService(ClienteDAO clienteDAO) {
        if (clienteDAO == null) {
            throw new IllegalArgumentException("El repositorio de clientes es obligatorio.");
        }
        this.clienteDAO = clienteDAO;
    }

    public List<Cliente> listarTodos() {
        return clienteDAO.listarTodos();
    }

    public List<Cliente> listarActivos() {
        return clienteDAO.listarActivos();
    }

    public Cliente buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del cliente debe ser un número positivo.");
        }
        Cliente cliente = clienteDAO.buscarPorId(id);
        if (cliente == null) {
            throw new EntidadNoEncontradaException("No se encontró un cliente con el ID: " + id);
        }
        return cliente;
    }

    public Cliente buscarPorDocumento(String documento) {
        String docNormalizado = InputValidator.normalizarTexto(documento);
        if (docNormalizado.isEmpty()) {
            throw new ValidacionException("Debe ingresar un documento de identidad válido.");
        }
        Cliente cliente = clienteDAO.buscarPorDocumento(docNormalizado);
        if (cliente == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró ningún cliente con el documento: " + docNormalizado);
        }
        return cliente;
    }

    public Cliente registrarCliente(Cliente cliente) {
        validarYNormalizarDatos(cliente);

        Cliente conMismoDoc = clienteDAO.buscarPorDocumento(cliente.getDocumentoIdentidad());
        if (conMismoDoc != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un cliente registrado con el documento: " + cliente.getDocumentoIdentidad());
        }
        Cliente conMismoEmail = clienteDAO.buscarPorEmail(cliente.getEmail());
        if (conMismoEmail != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un cliente registrado con el email: " + cliente.getEmail());
        }

        cliente.setActivo(true);
        int idGenerado = clienteDAO.guardar(cliente);
        cliente.setId(idGenerado);
        return cliente;
    }

    public Cliente actualizarCliente(Cliente cliente) {
        if (cliente == null || cliente.getId() <= 0) {
            throw new ValidacionException("Debe indicar un cliente válido con ID mayor que cero.");
        }
        Cliente actual = clienteDAO.buscarPorId(cliente.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un cliente con el ID: " + cliente.getId());
        }

        validarYNormalizarDatos(cliente);

        Cliente conMismoDoc = clienteDAO.buscarPorDocumento(cliente.getDocumentoIdentidad());
        if (conMismoDoc != null && conMismoDoc.getId() != cliente.getId()) {
            throw new DatoDuplicadoException(
                    "Ya existe otro cliente registrado con el documento: " + cliente.getDocumentoIdentidad());
        }
        Cliente conMismoEmail = clienteDAO.buscarPorEmail(cliente.getEmail());
        if (conMismoEmail != null && conMismoEmail.getId() != cliente.getId()) {
            throw new DatoDuplicadoException(
                    "Ya existe otro cliente registrado con el email: " + cliente.getEmail());
        }

        // El estado de activación solo cambia mediante desactivar/reactivar (RN-17).
        cliente.setActivo(actual.isActivo());
        boolean actualizado = clienteDAO.actualizar(cliente);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el cliente con ID: " + cliente.getId());
        }
        return cliente;
    }

    /**
     * Desactiva lógicamente a un cliente para impedir nuevas operaciones sin borrar su historial (RN-17).
     */
    public Cliente desactivarCliente(int id) {
        Cliente cliente = buscarPorId(id);
        if (!cliente.isActivo()) {
            return cliente;
        }
        boolean actualizado = clienteDAO.actualizarEstadoActivo(id, false);
        if (!actualizado) {
            throw new ValidacionException("No se pudo desactivar el cliente con ID: " + id);
        }
        cliente.setActivo(false);
        return cliente;
    }

    public Cliente reactivarCliente(int id) {
        Cliente cliente = buscarPorId(id);
        if (cliente.isActivo()) {
            return cliente;
        }
        boolean actualizado = clienteDAO.actualizarEstadoActivo(id, true);
        if (!actualizado) {
            throw new ValidacionException("No se pudo reactivar el cliente con ID: " + id);
        }
        cliente.setActivo(true);
        return cliente;
    }

    private void validarYNormalizarDatos(Cliente cliente) {
        if (cliente == null) {
            throw new ValidacionException("Los datos del cliente no pueden ser nulos.");
        }

        String nombre = InputValidator.normalizarEspacios(cliente.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del cliente es obligatorio.");
        }
        if (nombre.length() > MAX_LONGITUD_NOMBRE) {
            throw new ValidacionException(
                    "El nombre del cliente no puede superar los " + MAX_LONGITUD_NOMBRE + " caracteres.");
        }
        if (!InputValidator.esNombreClienteValido(nombre)) {
            throw new ValidacionException(
                    "El nombre del cliente debe contener al menos 2 palabras con letras (sin números).");
        }
        cliente.setNombre(nombre);

        String documento = InputValidator.normalizarTexto(cliente.getDocumentoIdentidad());
        if (documento.isEmpty()) {
            throw new ValidacionException("El documento de identidad del cliente es obligatorio.");
        }
        if (documento.length() > MAX_LONGITUD_DOCUMENTO) {
            throw new ValidacionException(
                    "El documento no puede superar los " + MAX_LONGITUD_DOCUMENTO + " caracteres.");
        }
        if (!InputValidator.esDocumentoClienteValido(documento)) {
            throw new ValidacionException(
                    "El documento del cliente debe tener entre 6 y 12 dígitos o formato NIT válido (ej. 900123456-7).");
        }
        cliente.setDocumentoIdentidad(documento);

        String email = InputValidator.normalizarTexto(cliente.getEmail()).toLowerCase();
        if (email.isEmpty()) {
            throw new ValidacionException("El email del cliente es obligatorio.");
        }
        if (email.length() > MAX_LONGITUD_EMAIL) {
            throw new ValidacionException(
                    "El email no puede superar los " + MAX_LONGITUD_EMAIL + " caracteres.");
        }
        if (!InputValidator.esEmailValido(email)) {
            throw new ValidacionException("El email del cliente no tiene un formato válido: " + email);
        }
        cliente.setEmail(email);

        String telefono = InputValidator.normalizarTexto(cliente.getTelefono());
        if (telefono.length() > MAX_LONGITUD_TELEFONO) {
            throw new ValidacionException(
                    "El teléfono no puede superar los " + MAX_LONGITUD_TELEFONO + " caracteres.");
        }
        if (!telefono.isEmpty() && !InputValidator.esTelefonoValido(telefono)) {
            throw new ValidacionException("El teléfono del cliente debe contener entre 7 y 15 dígitos numéricos.");
        }
        cliente.setTelefono(telefono.isEmpty() ? null : telefono);
    }
}
