package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import com.carmotors.sgt.util.InputValidator;

import java.util.List;

/**
 * Capa de lógica de negocio para la gestión de vehículos (RF-18, RN-13, RN-17, HU-09).
 */
public class VehiculoService {

    private static final int MAX_LONGITUD_MARCA = 50;
    private static final int MAX_LONGITUD_MODELO = 50;

    private final VehiculoDAO vehiculoDAO;
    private final ClienteDAO clienteDAO;

    public VehiculoService(VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO) {
        if (vehiculoDAO == null) {
            throw new IllegalArgumentException("El repositorio de vehículos es obligatorio.");
        }
        if (clienteDAO == null) {
            throw new IllegalArgumentException("El repositorio de clientes es obligatorio.");
        }
        this.vehiculoDAO = vehiculoDAO;
        this.clienteDAO = clienteDAO;
    }

    /**
     * Registra un nuevo vehículo validando reglas de negocio (RF-18, RN-13, RN-17).
     *
     * @param vehiculo datos del vehículo a registrar
     * @return vehículo registrado con su ID generado
     */
    public Vehiculo registrarVehiculo(Vehiculo vehiculo) {
        if (vehiculo == null) {
            throw new ValidacionException("Los datos del vehículo no pueden ser nulos.");
        }

        if (vehiculo.getClienteId() <= 0) {
            throw new ValidacionException("El ID del cliente debe ser un número positivo.");
        }

        // RN-17: Verificar existencia y estado del cliente
        Cliente cliente = clienteDAO.buscarPorId(vehiculo.getClienteId());
        if (cliente == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró un cliente con el ID: " + vehiculo.getClienteId());
        }
        if (!cliente.isActivo()) {
            throw new ValidacionException("No se puede registrar un vehículo a un cliente inactivo.");
        }

        // RN-13: Normalización y validación de placa
        String placaOriginal = vehiculo.getPlaca();
        if (!InputValidator.esPlacaValida(placaOriginal)) {
            throw new ValidacionException(
                    "Formato de placa inválido. Debe tener entre 5 y 10 caracteres alfanuméricos.");
        }
        String placaNormalizada = InputValidator.normalizarPlaca(placaOriginal);
        vehiculo.setPlaca(placaNormalizada);

        // Validación de marca y modelo
        String marcaNormalizada = InputValidator.normalizarEspacios(vehiculo.getMarca());
        if (!InputValidator.esTextoNoVacio(marcaNormalizada)) {
            throw new ValidacionException("La marca del vehículo es obligatoria.");
        }
        if (marcaNormalizada.length() > MAX_LONGITUD_MARCA) {
            throw new ValidacionException(
                    "La marca no puede superar los " + MAX_LONGITUD_MARCA + " caracteres.");
        }
        if (marcaNormalizada.length() < 2
                || !InputValidator.contieneLetra(marcaNormalizada)
                || InputValidator.contieneDigito(marcaNormalizada)) {
            throw new ValidacionException("La marca del vehículo debe contener al menos 2 letras y no incluir números.");
        }
        vehiculo.setMarca(marcaNormalizada);

        String modeloNormalizado = InputValidator.normalizarEspacios(vehiculo.getModelo());
        if (!InputValidator.esTextoNoVacio(modeloNormalizado)) {
            throw new ValidacionException("El modelo del vehículo es obligatorio.");
        }
        if (modeloNormalizado.length() > MAX_LONGITUD_MODELO) {
            throw new ValidacionException(
                    "El modelo no puede superar los " + MAX_LONGITUD_MODELO + " caracteres.");
        }
        if (!InputValidator.contieneAlfanumerico(modeloNormalizado)) {
            throw new ValidacionException("El modelo del vehículo debe contener caracteres alfanuméricos válidos.");
        }
        vehiculo.setModelo(modeloNormalizado);

        // RN-13: Unicidad de placa
        Vehiculo existente = vehiculoDAO.buscarPorPlaca(placaNormalizada);
        if (existente != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un vehículo registrado con la placa: " + placaNormalizada);
        }

        int idGenerado = vehiculoDAO.guardar(vehiculo);
        vehiculo.setId(idGenerado);
        return vehiculo;
    }

    /**
     * Busca un vehículo por su identificador primario.
     *
     * @param id identificador único del vehículo
     * @return vehículo encontrado
     */
    public Vehiculo buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del vehículo debe ser un número positivo.");
        }
        Vehiculo vehiculo = vehiculoDAO.buscarPorId(id);
        if (vehiculo == null) {
            throw new EntidadNoEncontradaException("No se encontró un vehículo con el ID: " + id);
        }
        return vehiculo;
    }

    /**
     * Busca un vehículo por su placa normalizada (RN-13).
     *
     * @param placa placa del vehículo
     * @return vehículo encontrado
     */
    public Vehiculo buscarPorPlaca(String placa) {
        String placaNormalizada = InputValidator.normalizarPlaca(placa);
        if (!InputValidator.esPlacaValida(placaNormalizada)) {
            throw new ValidacionException(
                    "Formato de placa inválido. Debe tener entre 5 y 10 caracteres alfanuméricos.");
        }
        Vehiculo vehiculo = vehiculoDAO.buscarPorPlaca(placaNormalizada);
        if (vehiculo == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró ningún vehículo con la placa: " + placaNormalizada);
        }
        return vehiculo;
    }

    /**
     * Retorna los vehículos asociados a un cliente (HU-09).
     * Permite consultar incluso si el cliente se encuentra inactivo para preservar su historial.
     *
     * @param clienteId identificador del cliente
     * @return lista de vehículos del cliente
     */
    public List<Vehiculo> listarPorCliente(int clienteId) {
        if (clienteId <= 0) {
            throw new ValidacionException("El ID del cliente debe ser un número positivo.");
        }
        Cliente cliente = clienteDAO.buscarPorId(clienteId);
        if (cliente == null) {
            throw new EntidadNoEncontradaException("No se encontró un cliente con el ID: " + clienteId);
        }
        return vehiculoDAO.listarPorCliente(clienteId);
    }

    /**
     * Retorna todos los vehículos registrados en el taller.
     *
     * @return lista de vehículos
     */
    public List<Vehiculo> listarTodos() {
        return vehiculoDAO.listarTodos();
    }
}
