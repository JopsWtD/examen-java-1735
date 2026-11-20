package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.dao.ServicioTipoDAO;

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

/**
 * Implementación JDBC para el catálogo de tipos de servicio (RNF-07, RNF-08, RNF-15).
 */
public class ServicioTipoDAOImpl implements ServicioTipoDAO {

    private static final String COLUMNAS_SELECT =
            "SELECT id, nombre, mantenimiento_tipo_id, especialidad_id, precio_mano_obra_base, intervalo_meses "
            + "FROM servicio_tipos";

    private final Connection conexionCompartida;

    /**
     * Constructor por defecto que obtiene conexiones desde {@link ConnectionFactory}.
     */
    public ServicioTipoDAOImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Constructor que recibe una conexión compartida para participar en transacciones atómicas.
     *
     * @param conexion conexión JDBC compartida
     */
    public ServicioTipoDAOImpl(Connection conexion) {
        this.conexionCompartida = conexion;
    }

    @Override
    public List<ServicioTipo> listarTodos() {
        String sql = COLUMNAS_SELECT + " ORDER BY id ASC";
        if (conexionCompartida != null) {
            return ejecutarListado(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarListado(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar tipos de servicio: " + e.getMessage(), e);
        }
    }

    @Override
    public ServicioTipo buscarPorId(int id) {
        String sql = COLUMNAS_SELECT + " WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorId(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorId(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar tipo de servicio por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ServicioTipo buscarPorNombre(String nombre) {
        String sql = COLUMNAS_SELECT + " WHERE LOWER(nombre) = LOWER(?)";
        if (conexionCompartida != null) {
            return ejecutarBusquedaPorNombre(conexionCompartida, sql, nombre);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarBusquedaPorNombre(conn, sql, nombre);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar tipo de servicio por nombre: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(ServicioTipo servicioTipo) {
        if (servicioTipo == null) {
            throw new IllegalArgumentException("El tipo de servicio no puede ser nulo.");
        }
        String sql = "INSERT INTO servicio_tipos "
                + "(nombre, mantenimiento_tipo_id, especialidad_id, precio_mano_obra_base, intervalo_meses) "
                + "VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return ejecutarInsercion(conexionCompartida, sql, servicioTipo);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarInsercion(conn, sql, servicioTipo);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el tipo de servicio: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(ServicioTipo servicioTipo) {
        if (servicioTipo == null || servicioTipo.getId() <= 0) {
            throw new IllegalArgumentException("El tipo de servicio a actualizar es inválido.");
        }
        String sql = "UPDATE servicio_tipos SET nombre = ?, mantenimiento_tipo_id = ?, especialidad_id = ?, "
                + "precio_mano_obra_base = ?, intervalo_meses = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarActualizacion(conexionCompartida, sql, servicioTipo);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarActualizacion(conn, sql, servicioTipo);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el tipo de servicio: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM servicio_tipos WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarEliminacion(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarEliminacion(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al eliminar el tipo de servicio: " + e.getMessage(), e);
        }
    }

    private List<ServicioTipo> ejecutarListado(Connection conn, String sql) {
        List<ServicioTipo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearServicioTipo(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar tipos de servicio: " + e.getMessage(), e);
        }
    }

    private ServicioTipo ejecutarBusquedaPorId(Connection conn, String sql, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearServicioTipo(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el tipo de servicio por ID: " + e.getMessage(), e);
        }
    }

    private ServicioTipo ejecutarBusquedaPorNombre(Connection conn, String sql, String nombre) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearServicioTipo(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el tipo de servicio por nombre: " + e.getMessage(), e);
        }
    }

    private int ejecutarInsercion(Connection conn, String sql, ServicioTipo servicioTipo) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            cargarParametrosComunes(ps, servicioTipo);

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se pudo insertar el tipo de servicio en la base de datos.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new CarMotorsException("No se obtuvo el ID generado al registrar el tipo de servicio.");
        } catch (SQLException e) {
            throw new CarMotorsException("Error al insertar el tipo de servicio: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarActualizacion(Connection conn, String sql, ServicioTipo servicioTipo) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            cargarParametrosComunes(ps, servicioTipo);
            ps.setInt(6, servicioTipo.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el tipo de servicio: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarEliminacion(Connection conn, String sql, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ValidacionException(
                        "No se puede eliminar el tipo de servicio porque ya está asociado a órdenes de trabajo.");
            }
            throw new CarMotorsException("Error al eliminar el tipo de servicio: " + e.getMessage(), e);
        }
    }

    private void cargarParametrosComunes(PreparedStatement ps, ServicioTipo servicioTipo) throws SQLException {
        ps.setString(1, servicioTipo.getNombre());
        ps.setInt(2, servicioTipo.getMantenimientoTipoId());
        ps.setInt(3, servicioTipo.getEspecialidadId());
        ps.setBigDecimal(4, servicioTipo.getPrecioManoObraBase().setScale(2, RoundingMode.HALF_UP));
        if (servicioTipo.getIntervaloMeses() == null) {
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(5, servicioTipo.getIntervaloMeses());
        }
    }

    private ServicioTipo mapearServicioTipo(ResultSet rs) throws SQLException {
        BigDecimal precio = rs.getBigDecimal("precio_mano_obra_base");
        if (precio != null) {
            precio = precio.setScale(2, RoundingMode.HALF_UP);
        }
        int intervaloValor = rs.getInt("intervalo_meses");
        Integer intervalo = rs.wasNull() ? null : intervaloValor;

        return new ServicioTipo(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("mantenimiento_tipo_id"),
                rs.getInt("especialidad_id"),
                precio,
                intervalo
        );
    }
}
