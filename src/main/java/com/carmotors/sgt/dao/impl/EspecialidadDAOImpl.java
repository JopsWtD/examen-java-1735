package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.dao.EspecialidadDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para la consulta de especialidades técnicas.
 */
public class EspecialidadDAOImpl implements EspecialidadDAO {

    private final Connection conexionCompartida;

    /**
     * Constructor por defecto que obtiene conexiones desde {@link ConnectionFactory}.
     */
    public EspecialidadDAOImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Constructor que recibe una conexión compartida para participar en transacciones atómicas.
     *
     * @param conexion conexión JDBC compartida
     */
    public EspecialidadDAOImpl(Connection conexion) {
        this.conexionCompartida = conexion;
    }

    @Override
    public List<Especialidad> listarTodos() {
        String sql = "SELECT id, nombre FROM especialidades ORDER BY id ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(conn, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar especialidades: " + e.getMessage(), e);
        }
    }

    @Override
    public Especialidad buscarPorId(int id) {
        String sql = "SELECT id, nombre FROM especialidades WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }
        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(conn, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar especialidad por ID " + id + ": " + e.getMessage(), e);
        }
    }

    private List<Especialidad> consultarLista(Connection conn, String sql) {
        List<Especialidad> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Especialidad(rs.getInt("id"), rs.getString("nombre")));
            }
            return lista;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar especialidades: " + e.getMessage(), e);
        }
    }

    private Especialidad consultarPorId(Connection conn, String sql, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Especialidad(rs.getInt("id"), rs.getString("nombre"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar especialidad con ID " + id + ": " + e.getMessage(), e);
        }
    }
}
