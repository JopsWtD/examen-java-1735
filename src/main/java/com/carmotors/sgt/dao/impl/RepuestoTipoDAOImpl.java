package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.RepuestoTipo;
import com.carmotors.sgt.dao.RepuestoTipoDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para la consulta de tipos de repuesto.
 */
public class RepuestoTipoDAOImpl implements RepuestoTipoDAO {

    private final Connection conexionCompartida;

    /**
     * Constructor por defecto que obtiene conexiones desde {@link ConnectionFactory}.
     */
    public RepuestoTipoDAOImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Constructor que recibe una conexión compartida para participar en transacciones atómicas.
     *
     * @param conexion conexión JDBC compartida
     */
    public RepuestoTipoDAOImpl(Connection conexion) {
        this.conexionCompartida = conexion;
    }

    @Override
    public List<RepuestoTipo> listarTodos() {
        String sql = "SELECT id, nombre FROM repuesto_tipos ORDER BY id ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar tipos de repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public RepuestoTipo buscarPorId(int id) {
        String sql = "SELECT id, nombre FROM repuesto_tipos WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar tipo de repuesto por ID " + id + ": " + e.getMessage(), e);
        }
    }

    private List<RepuestoTipo> consultarLista(Connection conn, String sql) {
        List<RepuestoTipo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new RepuestoTipo(rs.getInt("id"), rs.getString("nombre")));
            }
            return lista;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar tipos de repuesto: " + e.getMessage(), e);
        }
    }

    private RepuestoTipo consultarPorId(Connection conn, String sql, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RepuestoTipo(rs.getInt("id"), rs.getString("nombre"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar tipo de repuesto con ID " + id + ": " + e.getMessage(), e);
        }
    }
}
