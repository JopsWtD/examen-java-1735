package com.carmotors.sgt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.impl.ProveedorDAOImpl;
import com.carmotors.sgt.dao.impl.ProveedorRepuestoDAOImpl;
import com.carmotors.sgt.util.InputValidator;

public class ProveedorService {

    private static final Logger LOGGER = Logger.getLogger(ProveedorService.class.getName());
    private static final int MAX_LONGITUD_NOMBRE = 100;
    private static final int MAX_LONGITUD_TELEFONO = 20;
    private static final int MAX_LONGITUD_EMAIL = 100;
    private static final int MAX_LONGITUD_DIRECCION = 150;

    private final ProveedorDAO proveedorDAO;
    private final ProveedorRepuestoDAO proveedorRepuestoDAO;
    private final boolean usarTransaccionJdbc;

    public ProveedorService() {
        this(new ProveedorDAOImpl(), new ProveedorRepuestoDAOImpl(), true);
    }

    public ProveedorService(ProveedorDAO proveedorDAO) {
        this(proveedorDAO,
                proveedorDAO instanceof ProveedorDAOImpl ? new ProveedorRepuestoDAOImpl() : null,
                proveedorDAO instanceof ProveedorDAOImpl);
    }

    public ProveedorService(
            ProveedorDAO proveedorDAO,
            ProveedorRepuestoDAO proveedorRepuestoDAO,
            boolean usarTransaccionJdbc) {
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        this.proveedorDAO = proveedorDAO;
        this.proveedorRepuestoDAO = proveedorRepuestoDAO;
        this.usarTransaccionJdbc = usarTransaccionJdbc;
    }

    public List<Proveedor> listarTodos() {
        return proveedorDAO.listarTodos();
    }

    public List<Proveedor> listarActivos() {
        return proveedorDAO.listarActivos();
    }

    public Proveedor buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del proveedor debe ser un número positivo.");
        }

        Proveedor proveedor = proveedorDAO.buscarPorId(id);
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No se encontró un proveedor con el ID: " + id);
        }
        return proveedor;
    }

    public Proveedor crearProveedor(Proveedor proveedor) {
        validarProveedor(proveedor);

        String emailNormalizado = normalizarEmail(proveedor.getEmail());
        proveedor.setNombre(InputValidator.normalizarTexto(proveedor.getNombre()));
        proveedor.setEmail(emailNormalizado);
        proveedor.setTelefono(normalizarTelefono(proveedor.getTelefono()));
        proveedor.setDireccion(normalizarTextoOpcional(proveedor.getDireccion()));

        if (emailNormalizado != null) {
            Proveedor existente = proveedorDAO.buscarPorEmail(emailNormalizado);
            if (existente != null) {
                throw new DatoDuplicadoException("Ya existe un proveedor registrado con ese email.");
            }
        }

        proveedor.setActivo(true);
        int idGenerado = proveedorDAO.guardar(proveedor);
        proveedor.setId(idGenerado);
        return proveedor;
    }

    public Proveedor actualizarProveedor(Proveedor proveedor) {
        if (proveedor == null || proveedor.getId() <= 0) {
            throw new ValidacionException("Debe indicar el identificador del proveedor a actualizar.");
        }

        Proveedor actual = proveedorDAO.buscarPorId(proveedor.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + proveedor.getId());
        }

        validarProveedor(proveedor);
        String emailNormalizado = normalizarEmail(proveedor.getEmail());
        proveedor.setNombre(InputValidator.normalizarTexto(proveedor.getNombre()));
        proveedor.setEmail(emailNormalizado);
        proveedor.setTelefono(normalizarTelefono(proveedor.getTelefono()));
        proveedor.setDireccion(normalizarTextoOpcional(proveedor.getDireccion()));

        if (emailNormalizado != null) {
            Proveedor existente = proveedorDAO.buscarPorEmail(emailNormalizado);
            if (existente != null && existente.getId() != proveedor.getId()) {
                throw new DatoDuplicadoException("Ya existe otro proveedor registrado con ese email.");
            }
        }

        // RN-17: La actualización de datos nunca debe alterar el estado activo/inactivo.
        proveedor.setActivo(actual.isActivo());

        boolean actualizado = proveedorDAO.actualizar(proveedor);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el proveedor indicado.");
        }
        return proveedor;
    }

    public Proveedor desactivarProveedor(int id) {
        Proveedor proveedor = buscarPorId(id);
        if (!proveedor.isActivo()) {
            return proveedor;
        }

        if (!usarTransaccionJdbc) {
            if (proveedorRepuestoDAO != null) {
                proveedorRepuestoDAO.desmarcarHabitualesDeProveedor(id);
            }
            if (!proveedorDAO.desactivar(id)) {
                throw new ValidacionException("No se pudo desactivar el proveedor.");
            }
            proveedor.setActivo(false);
            return proveedor;
        }

        // RF-28 / RN-18 / RNF-08: Desactivar y limpiar es_habitual en una sola transacción JDBC.
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                ProveedorRepuestoDAO catalogoTransaccional = new ProveedorRepuestoDAOImpl(connection);
                ProveedorDAO proveedorTransaccional = new ProveedorDAOImpl(connection);

                catalogoTransaccional.desmarcarHabitualesDeProveedor(id);
                if (!proveedorTransaccional.desactivar(id)) {
                    throw new ValidacionException("No se pudo desactivar el proveedor.");
                }
                connection.commit();
                proveedor.setActivo(false);
                return proveedor;
            } catch (RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException errorAutoCommit) {
                    LOGGER.log(Level.WARNING, "No se pudo restaurar autoCommit al cerrar conexión.", errorAutoCommit);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo completar la desactivación transaccional del proveedor: "
                    + e.getMessage(), e);
        }
    }

    public Proveedor reactivarProveedor(int id) {
        Proveedor proveedor = buscarPorId(id);
        if (proveedor.isActivo()) {
            return proveedor;
        }

        boolean actualizado = proveedorDAO.reactivar(id);
        if (!actualizado) {
            throw new ValidacionException("No se pudo reactivar el proveedor.");
        }
        proveedor.setActivo(true);
        return proveedor;
    }

    private void validarProveedor(Proveedor proveedor) {
        if (proveedor == null) {
            throw new ValidacionException("El proveedor no puede ser nulo.");
        }

        String nombre = InputValidator.normalizarEspacios(proveedor.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del proveedor es obligatorio.");
        }
        if (nombre.length() > MAX_LONGITUD_NOMBRE) {
            throw new ValidacionException("El nombre del proveedor no puede superar los " + MAX_LONGITUD_NOMBRE + " caracteres.");
        }
        if (nombre.length() < 3 || !InputValidator.contieneLetra(nombre)) {
            throw new ValidacionException("El nombre del proveedor debe tener al menos 3 caracteres e incluir letras.");
        }
        proveedor.setNombre(nombre);

        String email = proveedor.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            String emailLimpio = InputValidator.normalizarTexto(email);
            if (emailLimpio.length() > MAX_LONGITUD_EMAIL) {
                throw new ValidacionException("El email del proveedor no puede superar los " + MAX_LONGITUD_EMAIL + " caracteres.");
            }
            if (!InputValidator.esEmailValido(emailLimpio)) {
                throw new ValidacionException("El email del proveedor no es válido.");
            }
        }

        String telefono = InputValidator.normalizarTexto(proveedor.getTelefono());
        if (!telefono.isEmpty()) {
            if (telefono.length() > MAX_LONGITUD_TELEFONO) {
                throw new ValidacionException("El teléfono del proveedor no puede superar los " + MAX_LONGITUD_TELEFONO + " caracteres.");
            }
            if (!InputValidator.esTelefonoValido(telefono)) {
                throw new ValidacionException("El teléfono del proveedor debe contener entre 7 y 15 dígitos numéricos.");
            }
        }

        String direccion = InputValidator.normalizarEspacios(proveedor.getDireccion());
        if (!direccion.isEmpty()) {
            if (direccion.length() > MAX_LONGITUD_DIRECCION) {
                throw new ValidacionException("La dirección del proveedor no puede superar los " + MAX_LONGITUD_DIRECCION + " caracteres.");
            }
            if (direccion.length() < 5 || !InputValidator.contieneLetra(direccion)) {
                throw new ValidacionException("La dirección del proveedor debe tener al menos 5 caracteres e incluir letras.");
            }
        }
    }

    private String normalizarEmail(String email) {
        String limpio = InputValidator.normalizarTexto(email);
        return limpio.isEmpty() ? null : limpio.toLowerCase();
    }

    private String normalizarTelefono(String telefono) {
        return normalizarTextoOpcional(telefono);
    }

    private String normalizarTextoOpcional(String valor) {
        String limpio = InputValidator.normalizarTexto(valor);
        return limpio.isEmpty() ? null : limpio;
    }
}
