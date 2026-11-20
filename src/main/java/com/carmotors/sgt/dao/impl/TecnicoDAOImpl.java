package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.dao.TecnicoDAO;
import com.carmotors.sgt.service.state.EstadoEnProceso;
import com.carmotors.sgt.service.state.EstadoPendiente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación JDBC para la persistencia de técnicos (RF-33, RNF-07, RNF-08).
 */
public class TecnicoDAOImpl implements TecnicoDAO {

    private static final String COLUMNAS_SELECT =
            "SELECT id, nombre, documento_identidad, especialidad_id, telefono, activo FROM tecnicos";

    private final Connection conexionCompartida;

    /**
     * Constructor por defecto que obtiene conexiones desde {@link ConnectionFactory}.
     */
    public TecnicoDAOImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Constructor que recibe una conexión compartida para participar en transacciones atómicas.
     *
     * @param conexion conexión JDBC compartida
     */
    public TecnicoDAOImpl(Connection conexion) {
        this.conexionCompartida = conexion;
    }

    @Override
    public List<Tecnico> listarTodos() {
        String sql = COLUMNAS_SELECT + " ORDER BY id ASC";
        if (conexionCompartida != null) {
            return ejecutarListado(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarListado(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar técnicos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Tecnico> listarActivos() {
        String sql = COLUMNAS_SELECT + " WHERE activo = TRUE ORDER BY id ASC";
        if (conexionCompartida != null) {
            return ejecutarListado(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarListado(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar técnicos activos: " + e.getMessage(), e);
        }
    }

    @Override
    public Tecnico buscarPorId(int id) {
        String sql = COLUMNAS_SELECT + " WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorEntero(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorEntero(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar técnico por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Tecnico buscarPorIdParaActualizar(int id) {
        if (conexionCompartida == null) {
            throw new CarMotorsException("La consulta con bloqueo requiere una conexión transaccional compartida.");
        }
        return ejecutarBusquedaPorEntero(conexionCompartida, COLUMNAS_SELECT + " WHERE id = ? FOR UPDATE", id);
    }

    @Override
    public Tecnico buscarPorDocumento(String documento) {
        String sql = COLUMNAS_SELECT + " WHERE documento_identidad = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorTexto(conexionCompartida, sql, documento);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorTexto(conn, sql, documento);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar técnico por documento: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Tecnico tecnico) {
        if (tecnico == null) {
            throw new IllegalArgumentException("El técnico no puede ser nulo.");
        }
        String sql = "INSERT INTO tecnicos (nombre, documento_identidad, especialidad_id, telefono, activo) "
                + "VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return ejecutarInsercion(conexionCompartida, sql, tecnico);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarInsercion(conn, sql, tecnico);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el técnico: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Tecnico tecnico) {
        if (tecnico == null || tecnico.getId() <= 0) {
            throw new IllegalArgumentException("El técnico a actualizar es inválido.");
        }
        String sql = "UPDATE tecnicos SET nombre = ?, documento_identidad = ?, especialidad_id = ?, "
                + "telefono = ?, activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarActualizacion(conexionCompartida, sql, tecnico);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarActualizacion(conn, sql, tecnico);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el técnico: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarEstadoActivo(int id, boolean activo) {
        String sql = "UPDATE tecnicos SET activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarCambioEstado(conexionCompartida, sql, id, activo);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarCambioEstado(conn, sql, id, activo);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al cambiar el estado del técnico: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CargaLineaTecnico> listarCargaPorTecnico(int tecnicoId) {
        String sql = "SELECT os.id AS orden_id, osd.id AS detalle_id, v.placa AS placa, "
                + "osd.nombre_servicio_snapshot AS tipo_servicio, se.id AS estado_id, "
                + "se.nombre AS estado_nombre, dt.horas_trabajadas AS horas_trabajadas "
                + "FROM detalle_tecnicos dt "
                + "INNER JOIN orden_servicio_detalle osd ON osd.id = dt.detalle_id "
                + "INNER JOIN ordenes_servicio os ON os.id = osd.orden_id "
                + "INNER JOIN vehiculos v ON v.id = os.vehiculo_id "
                + "INNER JOIN servicio_estados se ON se.id = os.estado_id "
                + "WHERE dt.tecnico_id = ? AND os.estado_id IN (?, ?) "
                + "ORDER BY os.id ASC, osd.id ASC";
        if (conexionCompartida != null) {
            return ejecutarConsultaCargaPorTecnico(conexionCompartida, sql, tecnicoId);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarConsultaCargaPorTecnico(conn, sql, tecnicoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar la carga de trabajo del técnico "
                    + tecnicoId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Map<Integer, Integer> contarCargaActivaPorTecnico() {
        String sql = "SELECT dt.tecnico_id, COUNT(DISTINCT dt.detalle_id) AS carga "
                + "FROM detalle_tecnicos dt "
                + "INNER JOIN orden_servicio_detalle osd ON osd.id = dt.detalle_id "
                + "INNER JOIN ordenes_servicio os ON os.id = osd.orden_id "
                + "WHERE os.estado_id IN (?, ?) "
                + "GROUP BY dt.tecnico_id";
        if (conexionCompartida != null) {
            return ejecutarConteoCargaActiva(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarConteoCargaActiva(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al contar la carga activa de los técnicos: " + e.getMessage(), e);
        }
    }

    private List<Tecnico> ejecutarListado(Connection conn, String sql) {
        List<Tecnico> tecnicos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tecnicos.add(mapearTecnico(rs));
            }
            return tecnicos;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el listado de técnicos: " + e.getMessage(), e);
        }
    }

    private Tecnico ejecutarBusquedaPorEntero(Connection conn, String sql, int valor) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearTecnico(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el técnico por identificador: " + e.getMessage(), e);
        }
    }

    private Tecnico ejecutarBusquedaPorTexto(Connection conn, String sql, String valor) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearTecnico(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el técnico por documento: " + e.getMessage(), e);
        }
    }

    private int ejecutarInsercion(Connection conn, String sql, Tecnico tecnico) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tecnico.getNombre());
            ps.setString(2, tecnico.getDocumentoIdentidad());
            ps.setInt(3, tecnico.getEspecialidadId());
            if (tecnico.getTelefono() == null || tecnico.getTelefono().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, tecnico.getTelefono());
            }
            ps.setBoolean(5, tecnico.isActivo());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se pudo insertar el técnico en la base de datos.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new CarMotorsException("No se obtuvo el ID generado al registrar el técnico.");
        } catch (SQLException e) {
            throw new CarMotorsException("Error al insertar el técnico: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarActualizacion(Connection conn, String sql, Tecnico tecnico) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tecnico.getNombre());
            ps.setString(2, tecnico.getDocumentoIdentidad());
            ps.setInt(3, tecnico.getEspecialidadId());
            if (tecnico.getTelefono() == null || tecnico.getTelefono().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, tecnico.getTelefono());
            }
            ps.setBoolean(5, tecnico.isActivo());
            ps.setInt(6, tecnico.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el técnico: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarCambioEstado(Connection conn, String sql, int id, boolean activo) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el estado del técnico: " + e.getMessage(), e);
        }
    }

    private List<CargaLineaTecnico> ejecutarConsultaCargaPorTecnico(Connection conn, String sql, int tecnicoId) {
        List<CargaLineaTecnico> lineas = new ArrayList<>();
        int estadoPendienteId = new EstadoPendiente().getEstadoId();
        int estadoEnProcesoId = new EstadoEnProceso().getEstadoId();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tecnicoId);
            ps.setInt(2, estadoPendienteId);
            ps.setInt(3, estadoEnProcesoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lineas.add(mapearCargaLinea(rs));
                }
                return lineas;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar la carga de trabajo del técnico: " + e.getMessage(), e);
        }
    }

    private Map<Integer, Integer> ejecutarConteoCargaActiva(Connection conn, String sql) {
        Map<Integer, Integer> cargas = new HashMap<>();
        int estadoPendienteId = new EstadoPendiente().getEstadoId();
        int estadoEnProcesoId = new EstadoEnProceso().getEstadoId();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, estadoPendienteId);
            ps.setInt(2, estadoEnProcesoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cargas.put(rs.getInt("tecnico_id"), rs.getInt("carga"));
                }
                return cargas;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al contar la carga activa por técnico: " + e.getMessage(), e);
        }
    }

    private CargaLineaTecnico mapearCargaLinea(ResultSet rs) throws SQLException {
        BigDecimal horas = rs.getBigDecimal("horas_trabajadas");
        if (horas == null) {
            horas = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            horas = horas.setScale(2, RoundingMode.HALF_UP);
        }
        return new CargaLineaTecnico(
                rs.getInt("orden_id"),
                rs.getInt("detalle_id"),
                rs.getString("placa"),
                rs.getString("tipo_servicio"),
                rs.getInt("estado_id"),
                rs.getString("estado_nombre"),
                horas
        );
    }

    private Tecnico mapearTecnico(ResultSet rs) throws SQLException {
        return new Tecnico(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("documento_identidad"),
                rs.getInt("especialidad_id"),
                rs.getString("telefono"),
                rs.getBoolean("activo")
        );
    }
}
