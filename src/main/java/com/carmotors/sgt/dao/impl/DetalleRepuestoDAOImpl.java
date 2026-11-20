package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.dao.DetalleRepuestoDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de {@code detalle_repuestos}.
 * Admite una {@link Connection} compartida para participar en transacciones del servicio;
 * en ese caso el DAO no cierra la conexión ni hace commit/rollback.
 */
public class DetalleRepuestoDAOImpl implements DetalleRepuestoDAO {

    private static final String COLUMNAS =
            "SELECT detalle_id, repuesto_id, cantidad_asignada, nombre_repuesto_snapshot, precio_unitario_venta "
                    + "FROM detalle_repuestos ";

    private final Connection conexionCompartida;

    public DetalleRepuestoDAOImpl() {
        this.conexionCompartida = null;
    }

    public DetalleRepuestoDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public boolean guardar(DetalleRepuesto detalleRepuesto) {
        if (detalleRepuesto == null) {
            throw new ValidacionException("La reserva del repuesto en la línea no puede ser nula.");
        }
        String sql = "INSERT INTO detalle_repuestos (detalle_id, repuesto_id, cantidad_asignada, "
                + "nombre_repuesto_snapshot, precio_unitario_venta) VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertar(conexionCompartida, sql, detalleRepuesto);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertar(connection, sql, detalleRepuesto);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la reserva del repuesto en la línea: " + e.getMessage(), e);
        }
    }

    @Override
    public DetalleRepuesto buscar(int detalleId, int repuestoId) {
        String sql = COLUMNAS + "WHERE detalle_id = ? AND repuesto_id = ?";
        if (conexionCompartida != null) {
            return consultarUno(conexionCompartida, sql, detalleId, repuestoId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarUno(connection, sql, detalleId, repuestoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la reserva del repuesto en la línea: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DetalleRepuesto> listarPorDetalle(int detalleId) {
        String sql = COLUMNAS + "WHERE detalle_id = ? ORDER BY repuesto_id ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql, detalleId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql, detalleId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar los repuestos de la línea " + detalleId + ": "
                    + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int detalleId, int repuestoId) {
        String sql = "DELETE FROM detalle_repuestos WHERE detalle_id = ? AND repuesto_id = ?";
        if (conexionCompartida != null) {
            return borrar(conexionCompartida, sql, detalleId, repuestoId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return borrar(connection, sql, detalleId, repuestoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al eliminar la reserva del repuesto de la línea " + detalleId + ": "
                    + e.getMessage(), e);
        }
    }

    private boolean insertar(Connection connection, String sql, DetalleRepuesto detalleRepuesto) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, detalleRepuesto.getDetalleId());
            preparedStatement.setInt(2, detalleRepuesto.getRepuestoId());
            preparedStatement.setInt(3, detalleRepuesto.getCantidadAsignada());
            preparedStatement.setString(4, detalleRepuesto.getNombreRepuestoSnapshot());
            preparedStatement.setBigDecimal(5, detalleRepuesto.getPrecioUnitarioVenta());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la reserva del repuesto en la línea: " + e.getMessage(), e);
        }
    }

    private boolean borrar(Connection connection, String sql, int detalleId, int repuestoId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, detalleId);
            preparedStatement.setInt(2, repuestoId);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al eliminar la reserva del repuesto de la línea " + detalleId + ": "
                    + e.getMessage(), e);
        }
    }

    private DetalleRepuesto consultarUno(Connection connection, String sql, int detalleId, int repuestoId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, detalleId);
            preparedStatement.setInt(2, repuestoId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapear(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la reserva del repuesto en la línea: " + e.getMessage(), e);
        }
    }

    private List<DetalleRepuesto> consultarLista(Connection connection, String sql, int detalleId) {
        List<DetalleRepuesto> resultado = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, detalleId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    resultado.add(mapear(resultSet));
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar los repuestos de la línea " + detalleId + ": "
                    + e.getMessage(), e);
        }
    }

    private DetalleRepuesto mapear(ResultSet resultSet) throws SQLException {
        DetalleRepuesto detalleRepuesto = new DetalleRepuesto();
        detalleRepuesto.setDetalleId(resultSet.getInt("detalle_id"));
        detalleRepuesto.setRepuestoId(resultSet.getInt("repuesto_id"));
        detalleRepuesto.setCantidadAsignada(resultSet.getInt("cantidad_asignada"));
        detalleRepuesto.setNombreRepuestoSnapshot(resultSet.getString("nombre_repuesto_snapshot"));
        detalleRepuesto.setPrecioUnitarioVenta(resultSet.getBigDecimal("precio_unitario_venta"));
        return detalleRepuesto;
    }
}
