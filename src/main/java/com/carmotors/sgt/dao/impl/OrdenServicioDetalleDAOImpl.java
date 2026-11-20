package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementación JDBC de las líneas de servicio ({@code orden_servicio_detalle}).
 * Admite una {@link Connection} compartida para participar en transacciones del servicio.
 */
public class OrdenServicioDetalleDAOImpl implements OrdenServicioDetalleDAO {

    private final Connection conexionCompartida;

    public OrdenServicioDetalleDAOImpl() {
        this(null);
    }

    public OrdenServicioDetalleDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public int guardar(OrdenServicioDetalle detalle) {
        if (detalle == null) {
            throw new ValidacionException("La línea de servicio no puede ser nula.");
        }

        String sql = "INSERT INTO orden_servicio_detalle (orden_id, servicio_tipo_id, nombre_servicio_snapshot, "
                + "costo_mano_obra, observaciones) VALUES (?, ?, ?, ?, ?)";

        if (conexionCompartida != null) {
            return insertarDetalle(conexionCompartida, sql, detalle);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarDetalle(connection, sql, detalle);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la línea de servicio: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrdenServicioDetalle> listarPorOrden(int ordenId) {
        String sql = "SELECT id, orden_id, servicio_tipo_id, nombre_servicio_snapshot, costo_mano_obra, observaciones "
                + "FROM orden_servicio_detalle WHERE orden_id = ? ORDER BY id ASC";

        if (conexionCompartida != null) {
            return consultarPorOrden(conexionCompartida, sql, ordenId);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorOrden(connection, sql, ordenId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar las líneas de la orden " + ordenId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId) {
        validarConexionCompartida();
        String sql = "SELECT id, orden_id, servicio_tipo_id, nombre_servicio_snapshot, costo_mano_obra, observaciones "
                + "FROM orden_servicio_detalle WHERE orden_id = ? ORDER BY id ASC FOR UPDATE";
        return consultarPorOrden(conexionCompartida, sql, ordenId);
    }

    @Override
    public void asignarTecnico(int detalleId, int tecnicoId) {
        asignarTecnico(detalleId, tecnicoId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
        validarConexionCompartida();
        String sql = "INSERT INTO detalle_tecnicos (detalle_id, tecnico_id, horas_trabajadas) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conexionCompartida.prepareStatement(sql)) {
            statement.setInt(1, detalleId);
            statement.setInt(2, tecnicoId);
            statement.setBigDecimal(3, normalizarHoras(horasTrabajadas));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CarMotorsException("Error al asignar técnico a la línea " + detalleId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existeTecnicoAsignado(int detalleId, int tecnicoId) {
        if (conexionCompartida != null) {
            String sql = "SELECT 1 FROM detalle_tecnicos WHERE detalle_id = ? AND tecnico_id = ? FOR UPDATE";
            return consultarExistenciaTecnico(conexionCompartida, sql, detalleId, tecnicoId);
        }

        String sql = "SELECT 1 FROM detalle_tecnicos WHERE detalle_id = ? AND tecnico_id = ?";
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarExistenciaTecnico(connection, sql, detalleId, tecnicoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al verificar asignación del técnico " + tecnicoId
                    + " en la línea " + detalleId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<DetalleTecnico> listarTecnicosAsignados(int detalleId) {
        String sql = "SELECT detalle_id, tecnico_id, horas_trabajadas "
                + "FROM detalle_tecnicos WHERE detalle_id = ? ORDER BY tecnico_id ASC";

        if (conexionCompartida != null) {
            return consultarTecnicosAsignados(conexionCompartida, sql, detalleId);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarTecnicosAsignados(connection, sql, detalleId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar técnicos asignados de la línea "
                    + detalleId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
        validarConexionCompartida();
        String sql = "UPDATE detalle_tecnicos SET horas_trabajadas = ? WHERE detalle_id = ? AND tecnico_id = ?";
        try (PreparedStatement statement = conexionCompartida.prepareStatement(sql)) {
            statement.setBigDecimal(1, normalizarHoras(horasTrabajadas));
            statement.setInt(2, detalleId);
            statement.setInt(3, tecnicoId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar horas trabajadas del técnico " + tecnicoId
                    + " en la línea " + detalleId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean tieneHorasTrabajadas(int detalleId) {
        validarConexionCompartida();
        String sql = "SELECT horas_trabajadas FROM detalle_tecnicos WHERE detalle_id = ? FOR UPDATE";
        try (PreparedStatement statement = conexionCompartida.prepareStatement(sql)) {
            statement.setInt(1, detalleId);
            try (ResultSet filas = statement.executeQuery()) {
                while (filas.next()) {
                    BigDecimal horas = filas.getBigDecimal("horas_trabajadas");
                    if (horas != null && horas.signum() > 0) {
                        return true;
                    }
                }
                return false;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al verificar horas trabajadas de la línea "
                    + detalleId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Map<Integer, Integer> listarReservasParaActualizar(int detalleId) {
        validarConexionCompartida();
        String sql = "SELECT repuesto_id, cantidad_asignada FROM detalle_repuestos "
                + "WHERE detalle_id = ? ORDER BY repuesto_id FOR UPDATE";
        Map<Integer, Integer> reservas = new LinkedHashMap<>();
        try (PreparedStatement statement = conexionCompartida.prepareStatement(sql)) {
            statement.setInt(1, detalleId);
            try (ResultSet filas = statement.executeQuery()) {
                while (filas.next()) {
                    reservas.put(filas.getInt("repuesto_id"), filas.getInt("cantidad_asignada"));
                }
            }
            return reservas;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar reservas de la línea "
                    + detalleId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarCostoManoObra(int ordenId, int detalleId, BigDecimal costoManoObra) {
        validarConexionCompartida();
        String sql = "UPDATE orden_servicio_detalle SET costo_mano_obra = ? WHERE orden_id = ? AND id = ?";
        try (PreparedStatement statement = conexionCompartida.prepareStatement(sql)) {
            statement.setBigDecimal(1, costoManoObra);
            statement.setInt(2, ordenId);
            statement.setInt(3, detalleId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el costo de mano de obra de la línea "
                    + detalleId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int ordenId, int detalleId) {
        validarConexionCompartida();
        // Las claves foráneas eliminan las asignaciones de esta línea.
        String sql = "DELETE FROM orden_servicio_detalle WHERE orden_id = ? AND id = ?";
        try (PreparedStatement statement = conexionCompartida.prepareStatement(sql)) {
            statement.setInt(1, ordenId);
            statement.setInt(2, detalleId);
            if (statement.executeUpdate() != 1) {
                throw new CarMotorsException("No se pudo retirar la línea de servicio.");
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al retirar la línea " + detalleId + ": " + e.getMessage(), e);
        }
    }

    private void validarConexionCompartida() {
        if (conexionCompartida == null) {
            throw new CarMotorsException("Esta operación transaccional requiere una conexión compartida.");
        }
    }

    @Override
    public OrdenServicioDetalle buscarPorId(int id) {
        String sql = "SELECT id, orden_id, servicio_tipo_id, nombre_servicio_snapshot, costo_mano_obra, observaciones "
                + "FROM orden_servicio_detalle WHERE id = ?";

        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la línea de servicio " + id + ": " + e.getMessage(), e);
        }
    }

    private OrdenServicioDetalle consultarPorId(Connection connection, String sql, int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearDetalle(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar la línea de servicio " + id + ": " + e.getMessage(), e);
        }
    }

    private int insertarDetalle(Connection connection, String sql, OrdenServicioDetalle detalle) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, detalle.getOrdenId());
            preparedStatement.setInt(2, detalle.getServicioTipoId());
            preparedStatement.setString(3, detalle.getNombreServicioSnapshot());
            preparedStatement.setBigDecimal(4, detalle.getCostoManoObra());
            if (detalle.getObservaciones() != null) {
                preparedStatement.setString(5, detalle.getObservaciones());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna fila para la línea de servicio.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la línea de servicio: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el identificador generado para la línea de servicio.");
    }

    private List<OrdenServicioDetalle> consultarPorOrden(Connection connection, String sql, int ordenId) {
        List<OrdenServicioDetalle> lineas = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, ordenId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lineas.add(mapearDetalle(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar las líneas de la orden " + ordenId + ": " + e.getMessage(), e);
        }
        return lineas;
    }

    private OrdenServicioDetalle mapearDetalle(ResultSet resultSet) throws SQLException {
        OrdenServicioDetalle detalle = new OrdenServicioDetalle();
        detalle.setId(resultSet.getInt("id"));
        detalle.setOrdenId(resultSet.getInt("orden_id"));
        detalle.setServicioTipoId(resultSet.getInt("servicio_tipo_id"));
        detalle.setNombreServicioSnapshot(resultSet.getString("nombre_servicio_snapshot"));
        detalle.setCostoManoObra(resultSet.getBigDecimal("costo_mano_obra"));
        detalle.setObservaciones(resultSet.getString("observaciones"));
        return detalle;
    }

    private boolean consultarExistenciaTecnico(Connection connection, String sql, int detalleId, int tecnicoId) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, detalleId);
            statement.setInt(2, tecnicoId);
            try (ResultSet filas = statement.executeQuery()) {
                return filas.next();
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar la asignación del técnico " + tecnicoId
                    + " en la línea " + detalleId + ": " + e.getMessage(), e);
        }
    }

    private List<DetalleTecnico> consultarTecnicosAsignados(Connection connection, String sql, int detalleId) {
        List<DetalleTecnico> asignaciones = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, detalleId);
            try (ResultSet filas = statement.executeQuery()) {
                while (filas.next()) {
                    DetalleTecnico asignacion = new DetalleTecnico();
                    asignacion.setDetalleId(filas.getInt("detalle_id"));
                    asignacion.setTecnicoId(filas.getInt("tecnico_id"));
                    asignacion.setHorasTrabajadas(normalizarHoras(filas.getBigDecimal("horas_trabajadas")));
                    asignaciones.add(asignacion);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar técnicos de la línea "
                    + detalleId + ": " + e.getMessage(), e);
        }
        return asignaciones;
    }

    private BigDecimal normalizarHoras(BigDecimal horasTrabajadas) {
        if (horasTrabajadas == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return horasTrabajadas.setScale(2, RoundingMode.HALF_UP);
    }
}
