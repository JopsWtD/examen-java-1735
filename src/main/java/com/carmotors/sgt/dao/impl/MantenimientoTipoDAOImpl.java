package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.MantenimientoTipo;
import com.carmotors.sgt.dao.MantenimientoTipoDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para la consulta de tipos de mantenimiento.
 */
public class MantenimientoTipoDAOImpl implements MantenimientoTipoDAO {

    private final Connection conexionCompartida;

    /**
     * Constructor por defecto que obtiene conexiones desde {@link ConnectionFactory}.
     */
    public MantenimientoTipoDAOImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Constructor que recibe una conexión compartida para participar en transacciones atómicas.
     *
     * @param conexion conexión JDBC compartida
     */
    public MantenimientoTipoDAOImpl(Connection conexion) {
        this.conexionCompartida = conexion;
    }

    @Override
    public List<MantenimientoTipo> listarTodos() {
        String sql = "SELECT id, nombre FROM mantenimiento_tipos ORDER BY id ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar tipos de mantenimiento: " + e.getMessage(), e);
        }
    }

    @Override
    public MantenimientoTipo buscarPorId(int id) {
        String sql = "SELECT id, nombre FROM mantenimiento_tipos WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar tipo de mantenimiento por ID " + id + ": " + e.getMessage(), e);
        }
    }

    private List<MantenimientoTipo> consultarLista(Connection conn, String sql) {
        List<MantenimientoTipo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new MantenimientoTipo(rs.getInt("id"), rs.getString("nombre")));
            }
            return lista;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar tipos de mantenimiento: " + e.getMessage(), e);
        }
    }

    private MantenimientoTipo consultarPorId(Connection conn, String sql, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new MantenimientoTipo(rs.getInt("id"), rs.getString("nombre"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar tipo de mantenimiento con ID " + id + ": " + e.getMessage(), e);
        }
    }
}
