package com.carmotors.sgt.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.dao.ProveedorDAO;

public class ProveedorDAOImpl implements ProveedorDAO {

    private final Connection conexionCompartida;

    public ProveedorDAOImpl() {
        this.conexionCompartida = null;
    }

    public ProveedorDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public List<Proveedor> listarTodos() {
        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores ORDER BY nombre ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar proveedores: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Proveedor> listarActivos() {
        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores WHERE activo = true ORDER BY nombre ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar proveedores activos: " + e.getMessage(), e);
        }
    }

    @Override
    public Proveedor buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del proveedor debe ser un número positivo.");
        }

        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarPorId(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorId(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar proveedor por ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Proveedor buscarPorEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id, nombre, telefono, email, direccion, activo FROM proveedores WHERE LOWER(email) = LOWER(?)";
        if (conexionCompartida != null) {
            return consultarPorEmail(conexionCompartida, sql, email.trim());
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorEmail(connection, sql, email.trim());
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar proveedor por email: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Proveedor proveedor) {
        if (proveedor == null) {
            throw new ValidacionException("El proveedor no puede ser nulo.");
        }

        String sql = "INSERT INTO proveedores (nombre, telefono, email, direccion, activo) VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertarProveedor(conexionCompartida, sql, proveedor);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarProveedor(connection, sql, proveedor);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Proveedor proveedor) {
        if (proveedor == null || proveedor.getId() <= 0) {
            throw new ValidacionException("El proveedor a actualizar es inválido.");
        }

        String sql = "UPDATE proveedores SET nombre = ?, telefono = ?, email = ?, direccion = ?, activo = ? WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarActualizar(conexionCompartida, sql, proveedor);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarActualizar(connection, sql, proveedor);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE proveedores SET activo = false WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarCambioEstado(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarCambioEstado(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al desactivar el proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean reactivar(int id) {
        String sql = "UPDATE proveedores SET activo = true WHERE id = ?";
        if (conexionCompartida != null) {
            return ejecutarCambioEstado(conexionCompartida, sql, id);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarCambioEstado(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al reactivar el proveedor: " + e.getMessage(), e);
        }
    }

    private List<Proveedor> consultarLista(Connection connection, String sql) {
        List<Proveedor> proveedores = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                proveedores.add(mapearProveedor(resultSet));
            }
            return proveedores;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar proveedores: " + e.getMessage(), e);
        }
    }

    private Proveedor consultarPorId(Connection connection, String sql, int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearProveedor(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar proveedor por ID " + id + ": " + e.getMessage(), e);
        }
    }

    private Proveedor consultarPorEmail(Connection connection, String sql, String email) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearProveedor(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar proveedor por email: " + e.getMessage(), e);
        }
    }

    private int insertarProveedor(Connection connection, String sql, Proveedor proveedor) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, proveedor.getNombre());
            preparedStatement.setString(2, proveedor.getTelefono());
            preparedStatement.setString(3, proveedor.getEmail());
            preparedStatement.setString(4, proveedor.getDireccion());
            preparedStatement.setBoolean(5, proveedor.isActivo());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ningún proveedor en la base de datos.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el proveedor: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el ID generado al registrar el proveedor.");
    }

    private boolean ejecutarActualizar(Connection connection, String sql, Proveedor proveedor) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, proveedor.getNombre());
            preparedStatement.setString(2, proveedor.getTelefono());
            preparedStatement.setString(3, proveedor.getEmail());
            preparedStatement.setString(4, proveedor.getDireccion());
            preparedStatement.setBoolean(5, proveedor.isActivo());
            preparedStatement.setInt(6, proveedor.getId());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar el proveedor: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarCambioEstado(Connection connection, String sql, int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al cambiar el estado del proveedor: " + e.getMessage(), e);
        }
    }

    private Proveedor mapearProveedor(ResultSet resultSet) throws SQLException {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(resultSet.getInt("id"));
        proveedor.setNombre(resultSet.getString("nombre"));
        proveedor.setTelefono(resultSet.getString("telefono"));
        proveedor.setEmail(resultSet.getString("email"));
        proveedor.setDireccion(resultSet.getString("direccion"));
        proveedor.setActivo(resultSet.getBoolean("activo"));
        return proveedor;
    }
}
