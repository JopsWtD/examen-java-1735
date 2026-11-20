package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.VehiculoDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para la gestión de vehículos (RF-18, RN-13, RN-17).
 */
public class VehiculoDAOImpl implements VehiculoDAO {

    private final Connection conexionCompartida;

    public VehiculoDAOImpl() {
        this.conexionCompartida = null;
    }

    public VehiculoDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public int guardar(Vehiculo vehiculo) {
        String sql = "INSERT INTO vehiculos (cliente_id, placa, marca, modelo) VALUES (?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return ejecutarGuardar(conexionCompartida, sql, vehiculo);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarGuardar(conn, sql, vehiculo);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al guardar vehículo: " + e.getMessage(), e);
        }
    }

    @Override
    public Vehiculo buscarPorId(int id) {
        String sql = "SELECT id, cliente_id, placa, marca, modelo FROM vehiculos WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarPorCampo(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorCampo(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar vehículo por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Vehiculo buscarPorPlaca(String placa) {
        String sql = "SELECT id, cliente_id, placa, marca, modelo FROM vehiculos WHERE placa = ?";
        if (conexionCompartida != null) {
            return consultarPorPlaca(conexionCompartida, sql, placa);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorPlaca(conn, sql, placa);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar vehículo por placa " + placa + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Vehiculo> listarPorCliente(int clienteId) {
        String sql = "SELECT id, cliente_id, placa, marca, modelo FROM vehiculos WHERE cliente_id = ? ORDER BY id ASC";
        if (conexionCompartida != null) {
            return consultarListaPorCliente(conexionCompartida, sql, clienteId);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarListaPorCliente(conn, sql, clienteId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar vehículos del cliente " + clienteId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Vehiculo> listarTodos() {
        String sql = "SELECT id, cliente_id, placa, marca, modelo FROM vehiculos ORDER BY id ASC";
        if (conexionCompartida != null) {
            return consultarTodos(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarTodos(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar todos los vehículos: " + e.getMessage(), e);
        }
    }

    private int ejecutarGuardar(Connection conn, String sql, Vehiculo v) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getClienteId());
            ps.setString(2, v.getPlaca());
            ps.setString(3, v.getMarca());
            ps.setString(4, v.getModelo());

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                throw new CarMotorsException("No se pudo insertar el vehículo, ninguna fila fue afectada.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
                throw new CarMotorsException("No se obtuvo el ID generado para el vehículo.");
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al persistir vehículo con placa " + v.getPlaca() + ": " + e.getMessage(), e);
        }
    }

    private Vehiculo consultarPorCampo(Connection conn, String sql, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearFila(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar vehículo: " + e.getMessage(), e);
        }
    }

    private Vehiculo consultarPorPlaca(Connection conn, String sql, String placa) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, placa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearFila(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar vehículo por placa: " + e.getMessage(), e);
        }
    }

    private List<Vehiculo> consultarListaPorCliente(Connection conn, String sql, int clienteId) {
        List<Vehiculo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearFila(rs));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar lista de vehículos: " + e.getMessage(), e);
        }
    }

    private List<Vehiculo> consultarTodos(Connection conn, String sql) {
        List<Vehiculo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearFila(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar todos los vehículos: " + e.getMessage(), e);
        }
    }

    private Vehiculo mapearFila(ResultSet rs) throws SQLException {
        return new Vehiculo(
                rs.getInt("id"),
                rs.getInt("cliente_id"),
                rs.getString("placa"),
                rs.getString("marca"),
                rs.getString("modelo")
        );
    }
}
