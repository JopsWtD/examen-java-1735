package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.dao.OrdenServicioDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Implementación JDBC de la cabecera de órdenes de servicio ({@code ordenes_servicio}).
 * Admite una {@link Connection} compartida para participar en transacciones del servicio.
 */
public class OrdenServicioDAOImpl implements OrdenServicioDAO {

    private final Connection conexionCompartida;

    public OrdenServicioDAOImpl() {
        this(null);
    }

    public OrdenServicioDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public int guardar(OrdenServicio orden) {
        if (orden == null) {
            throw new ValidacionException("La orden de servicio no puede ser nula.");
        }

        String sql = "INSERT INTO ordenes_servicio (vehiculo_id, estado_id, fecha_recepcion, fecha_completado, "
                + "fecha_entrega, fecha_cancelacion, descripcion_problema) VALUES (?, ?, ?, ?, ?, ?, ?)";

        if (conexionCompartida != null) {
            return insertarOrden(conexionCompartida, sql, orden);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarOrden(connection, sql, orden);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la orden de servicio: " + e.getMessage(), e);
        }
    }

    @Override
    public OrdenServicio buscarPorId(int id) {
        String sql = "SELECT id, vehiculo_id, estado_id, fecha_recepcion, fecha_completado, fecha_entrega, "
                + "fecha_cancelacion, descripcion_problema FROM ordenes_servicio WHERE id = ?";

        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la orden de servicio " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public OrdenServicio buscarPorIdParaActualizar(int id) {
        if (conexionCompartida == null) {
            throw new CarMotorsException("La consulta con bloqueo requiere una conexión transaccional compartida.");
        }
        String sql = "SELECT id, vehiculo_id, estado_id, fecha_recepcion, fecha_completado, fecha_entrega, "
                + "fecha_cancelacion, descripcion_problema FROM ordenes_servicio WHERE id = ? FOR UPDATE";
        return consultarPorId(conexionCompartida, sql, id);
    }

    @Override
    public boolean actualizarEstado(int ordenId, int nuevoEstadoId) {
        String sql = "UPDATE ordenes_servicio SET estado_id = ? WHERE id = ?";

        if (conexionCompartida != null) {
            return actualizarEstadoOrden(conexionCompartida, sql, ordenId, nuevoEstadoId);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return actualizarEstadoOrden(connection, sql, ordenId, nuevoEstadoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el estado de la orden " + ordenId + ": "
                    + e.getMessage(), e);
        }
    }

    @Override
    public boolean marcarCompletada(int ordenId, int estadoId, LocalDateTime fechaCompletado) {
        String sql = "UPDATE ordenes_servicio SET estado_id = ?, fecha_completado = ? WHERE id = ?";

        if (conexionCompartida != null) {
            return ejecutarMarcarCompletada(conexionCompartida, sql, ordenId, estadoId, fechaCompletado);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarMarcarCompletada(connection, sql, ordenId, estadoId, fechaCompletado);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al completar la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    private boolean ejecutarMarcarCompletada(Connection connection, String sql, int ordenId, int estadoId,
                                             LocalDateTime fechaCompletado) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, estadoId);
            asignarFecha(preparedStatement, 2, fechaCompletado);
            preparedStatement.setInt(3, ordenId);
            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al completar la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean marcarCancelada(int ordenId, int estadoId, LocalDateTime fechaCancelacion) {
        String sql = "UPDATE ordenes_servicio SET estado_id = ?, fecha_cancelacion = ? WHERE id = ?";

        if (conexionCompartida != null) {
            return ejecutarMarcarCancelada(conexionCompartida, sql, ordenId, estadoId, fechaCancelacion);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarMarcarCancelada(connection, sql, ordenId, estadoId, fechaCancelacion);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al cancelar la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    private boolean ejecutarMarcarCancelada(Connection connection, String sql, int ordenId, int estadoId,
                                            LocalDateTime fechaCancelacion) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, estadoId);
            asignarFecha(preparedStatement, 2, fechaCancelacion);
            preparedStatement.setInt(3, ordenId);
            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al cancelar la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean marcarEntregada(int ordenId, int estadoId, LocalDateTime fechaEntrega) {
        String sql = "UPDATE ordenes_servicio SET estado_id = ?, fecha_entrega = ? WHERE id = ?";

        if (conexionCompartida != null) {
            return ejecutarMarcarEntregada(conexionCompartida, sql, ordenId, estadoId, fechaEntrega);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarMarcarEntregada(connection, sql, ordenId, estadoId, fechaEntrega);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al entregar la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    private boolean ejecutarMarcarEntregada(Connection connection, String sql, int ordenId, int estadoId,
                                            LocalDateTime fechaEntrega) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, estadoId);
            asignarFecha(preparedStatement, 2, fechaEntrega);
            preparedStatement.setInt(3, ordenId);
            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al entregar la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    private int insertarOrden(Connection connection, String sql, OrdenServicio orden) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, orden.getVehiculoId());
            preparedStatement.setInt(2, orden.getEstadoId());
            asignarFecha(preparedStatement, 3, orden.getFechaRecepcion());
            asignarFecha(preparedStatement, 4, orden.getFechaCompletado());
            asignarFecha(preparedStatement, 5, orden.getFechaEntrega());
            asignarFecha(preparedStatement, 6, orden.getFechaCancelacion());
            preparedStatement.setString(7, orden.getDescripcionProblema());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna fila para la orden de servicio.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la orden de servicio: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el identificador generado para la orden de servicio.");
    }

    private OrdenServicio consultarPorId(Connection connection, String sql, int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearOrden(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar la orden de servicio " + id + ": " + e.getMessage(), e);
        }
    }

    private boolean actualizarEstadoOrden(Connection connection, String sql, int ordenId, int nuevoEstadoId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, nuevoEstadoId);
            preparedStatement.setInt(2, ordenId);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el estado de la orden " + ordenId + ": "
                    + e.getMessage(), e);
        }
    }

    private void asignarFecha(PreparedStatement preparedStatement, int indice, LocalDateTime fecha)
            throws SQLException {
        if (fecha != null) {
            preparedStatement.setTimestamp(indice, Timestamp.valueOf(fecha));
        } else {
            preparedStatement.setNull(indice, Types.TIMESTAMP);
        }
    }

    private LocalDateTime leerFecha(ResultSet resultSet, String columna) throws SQLException {
        Timestamp valor = resultSet.getTimestamp(columna);
        return valor != null ? valor.toLocalDateTime() : null;
    }

    private OrdenServicio mapearOrden(ResultSet resultSet) throws SQLException {
        OrdenServicio orden = new OrdenServicio();
        orden.setId(resultSet.getInt("id"));
        orden.setVehiculoId(resultSet.getInt("vehiculo_id"));
        orden.setEstadoId(resultSet.getInt("estado_id"));
        orden.setFechaRecepcion(leerFecha(resultSet, "fecha_recepcion"));
        orden.setFechaCompletado(leerFecha(resultSet, "fecha_completado"));
        orden.setFechaEntrega(leerFecha(resultSet, "fecha_entrega"));
        orden.setFechaCancelacion(leerFecha(resultSet, "fecha_cancelacion"));
        orden.setDescripcionProblema(resultSet.getString("descripcion_problema"));
        return orden;
    }
}
