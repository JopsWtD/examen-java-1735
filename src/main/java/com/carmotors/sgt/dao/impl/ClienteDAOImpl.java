package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.dao.ClienteDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para la persistencia de clientes (RF-17, RNF-07, RNF-08).
 */
public class ClienteDAOImpl implements ClienteDAO {

    private static final String COLUMNAS_SELECT =
            "SELECT id, nombre, documento_identidad, telefono, email, activo FROM clientes";

    private final Connection conexionCompartida;

    /**
     * Constructor por defecto que obtiene conexiones desde {@link ConnectionFactory}.
     */
    public ClienteDAOImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Constructor que recibe una conexión compartida para participar en transacciones atómicas.
     *
     * @param conexion conexión JDBC compartida
     */
    public ClienteDAOImpl(Connection conexion) {
        this.conexionCompartida = conexion;
    }

    @Override
    public List<Cliente> listarTodos() {
        String sql = COLUMNAS_SELECT + " ORDER BY id ASC";
        if (conexionCompartida != null) {
            return ejecutarListado(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarListado(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar clientes: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Cliente> listarActivos() {
        String sql = COLUMNAS_SELECT + " WHERE activo = TRUE ORDER BY id ASC";
        if (conexionCompartida != null) {
            return ejecutarListado(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarListado(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar clientes activos: " + e.getMessage(), e);
        }
    }

    @Override
    public Cliente buscarPorId(int id) {
        String sql = COLUMNAS_SELECT + " WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorEntero(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorEntero(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar cliente por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Cliente buscarPorDocumento(String documento) {
        String sql = COLUMNAS_SELECT + " WHERE documento_identidad = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorTexto(conexionCompartida, sql, documento);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorTexto(conn, sql, documento);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar cliente por documento: " + e.getMessage(), e);
        }
    }

    @Override
    public Cliente buscarPorEmail(String email) {
        String sql = COLUMNAS_SELECT + " WHERE email = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorTexto(conexionCompartida, sql, email);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorTexto(conn, sql, email);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar cliente por email: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo.");
        }
        String sql = "INSERT INTO clientes (nombre, documento_identidad, telefono, email, activo) "
                + "VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return ejecutarInsercion(conexionCompartida, sql, cliente);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarInsercion(conn, sql, cliente);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Cliente cliente) {
        if (cliente == null || cliente.getId() <= 0) {
            throw new IllegalArgumentException("El cliente a actualizar es inválido.");
        }
        String sql = "UPDATE clientes SET nombre = ?, documento_identidad = ?, telefono = ?, "
                + "email = ?, activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarActualizacion(conexionCompartida, sql, cliente);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarActualizacion(conn, sql, cliente);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarEstadoActivo(int id, boolean activo) {
        String sql = "UPDATE clientes SET activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarCambioEstado(conexionCompartida, sql, id, activo);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarCambioEstado(conn, sql, id, activo);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al cambiar el estado del cliente: " + e.getMessage(), e);
        }
    }

    private List<Cliente> ejecutarListado(Connection conn, String sql) {
        List<Cliente> clientes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }
            return clientes;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el listado de clientes: " + e.getMessage(), e);
        }
    }

    private Cliente ejecutarBusquedaPorEntero(Connection conn, String sql, int valor) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el cliente por identificador: " + e.getMessage(), e);
        }
    }

    private Cliente ejecutarBusquedaPorTexto(Connection conn, String sql, String valor) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el cliente: " + e.getMessage(), e);
        }
    }

    private int ejecutarInsercion(Connection conn, String sql, Cliente cliente) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDocumentoIdentidad());
            asignarTelefono(ps, 3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setBoolean(5, cliente.isActivo());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se pudo insertar el cliente en la base de datos.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new CarMotorsException("No se obtuvo el ID generado al registrar el cliente.");
        } catch (SQLException e) {
            throw new CarMotorsException("Error al insertar el cliente: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarActualizacion(Connection conn, String sql, Cliente cliente) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDocumentoIdentidad());
            asignarTelefono(ps, 3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setBoolean(5, cliente.isActivo());
            ps.setInt(6, cliente.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el cliente: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarCambioEstado(Connection conn, String sql, int id, boolean activo) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el estado del cliente: " + e.getMessage(), e);
        }
    }

    private void asignarTelefono(PreparedStatement ps, int posicion, String telefono) throws SQLException {
        if (telefono == null || telefono.isEmpty()) {
            ps.setNull(posicion, Types.VARCHAR);
        } else {
            ps.setString(posicion, telefono);
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("documento_identidad"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getBoolean("activo")
        );
    }
}
