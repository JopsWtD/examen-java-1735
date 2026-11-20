package com.carmotors.sgt.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.dao.EvaluacionProveedorDAO;

public class EvaluacionProveedorDAOImpl implements EvaluacionProveedorDAO {

    private final Connection conexionCompartida;

    public EvaluacionProveedorDAOImpl() {
        this(null);
    }

    public EvaluacionProveedorDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public List<EvaluacionProveedor> listarTodos() {
        String sql = "SELECT id, proveedor_id, fecha, calidad, puntualidad, comentario FROM evaluaciones_proveedor ORDER BY fecha DESC, id DESC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar evaluaciones de proveedores: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
        if (proveedorId <= 0) {
            throw new ValidacionException("El ID del proveedor debe ser un número positivo.");
        }

        String sql = "SELECT id, proveedor_id, fecha, calidad, puntualidad, comentario FROM evaluaciones_proveedor WHERE proveedor_id = ? ORDER BY fecha DESC, id DESC";
        if (conexionCompartida != null) {
            return consultarListaPorProveedor(conexionCompartida, sql, proveedorId);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarListaPorProveedor(connection, sql, proveedorId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar las evaluaciones del proveedor " + proveedorId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public EvaluacionProveedor buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID de la evaluación debe ser un número positivo.");
        }

        String sql = "SELECT id, proveedor_id, fecha, calidad, puntualidad, comentario FROM evaluaciones_proveedor WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la evaluación por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(EvaluacionProveedor evaluacion) {
        if (evaluacion == null) {
            throw new ValidacionException("La evaluación no puede ser nula.");
        }

        String sql = "INSERT INTO evaluaciones_proveedor (proveedor_id, fecha, calidad, puntualidad, comentario) VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertarEvaluacion(conexionCompartida, sql, evaluacion);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarEvaluacion(connection, sql, evaluacion);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la evaluación del proveedor: " + e.getMessage(), e);
        }
    }

    private List<EvaluacionProveedor> consultarLista(Connection connection, String sql) {
        List<EvaluacionProveedor> evaluaciones = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                evaluaciones.add(mapearEvaluacion(resultSet));
            }
            return evaluaciones;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar evaluaciones: " + e.getMessage(), e);
        }
    }

    private List<EvaluacionProveedor> consultarListaPorProveedor(Connection connection, String sql, int proveedorId) {
        List<EvaluacionProveedor> evaluaciones = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, proveedorId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    evaluaciones.add(mapearEvaluacion(resultSet));
                }
                return evaluaciones;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar evaluaciones del proveedor " + proveedorId + ": " + e.getMessage(), e);
        }
    }

    private EvaluacionProveedor consultarPorId(Connection connection, String sql, int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearEvaluacion(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la evaluación por ID " + id + ": " + e.getMessage(), e);
        }
    }

    private int insertarEvaluacion(Connection connection, String sql, EvaluacionProveedor evaluacion) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, evaluacion.getProveedorId());
            preparedStatement.setDate(2, java.sql.Date.valueOf(evaluacion.getFecha()));
            preparedStatement.setInt(3, evaluacion.getCalidad());
            preparedStatement.setInt(4, evaluacion.getPuntualidad());
            preparedStatement.setString(5, evaluacion.getComentario());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna evaluación del proveedor en la base de datos.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la evaluación del proveedor: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el ID generado al registrar la evaluación del proveedor.");
    }

    private EvaluacionProveedor mapearEvaluacion(ResultSet resultSet) throws SQLException {
        EvaluacionProveedor evaluacion = new EvaluacionProveedor();
        evaluacion.setId(resultSet.getInt("id"));
        evaluacion.setProveedorId(resultSet.getInt("proveedor_id"));
        Date fechaSql = resultSet.getDate("fecha");
        evaluacion.setFecha(fechaSql != null ? fechaSql.toLocalDate() : LocalDate.now());
        evaluacion.setCalidad(resultSet.getInt("calidad"));
        evaluacion.setPuntualidad(resultSet.getInt("puntualidad"));
        evaluacion.setComentario(resultSet.getString("comentario"));
        return evaluacion;
    }
}
