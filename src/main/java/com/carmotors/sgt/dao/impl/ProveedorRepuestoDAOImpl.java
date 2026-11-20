package com.carmotors.sgt.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;

public class ProveedorRepuestoDAOImpl implements ProveedorRepuestoDAO {

    private static final String COLUMNAS =
            "SELECT proveedor_id, repuesto_id, costo, tiempo_entrega_dias, es_habitual FROM proveedor_repuestos ";

    private final Connection conexionCompartida;

    public ProveedorRepuestoDAOImpl() {
        this.conexionCompartida = null;
    }

    /**
     * Usa una conexión compartida para participar en una transacción abierta por el servicio.
     * En ese caso el DAO no cierra la conexión ni hace commit/rollback.
     */
    public ProveedorRepuestoDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
        String sql = COLUMNAS + "WHERE proveedor_id = ? ORDER BY repuesto_id ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql, proveedorId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql, proveedorId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar el catálogo del proveedor " + proveedorId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
        String sql = COLUMNAS + "WHERE repuesto_id = ? ORDER BY proveedor_id ASC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql, repuestoId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql, repuestoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar los proveedores del repuesto " + repuestoId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId) {
        String sql = COLUMNAS + "WHERE proveedor_id = ? AND repuesto_id = ?";
        if (conexionCompartida != null) {
            return consultarUno(conexionCompartida, sql, proveedorId, repuestoId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarUno(connection, sql, proveedorId, repuestoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean guardar(ProveedorRepuesto proveedorRepuesto) {
        if (proveedorRepuesto == null) {
            throw new ValidacionException("La asociación proveedor-repuesto no puede ser nula.");
        }
        String sql = "INSERT INTO proveedor_repuestos (proveedor_id, repuesto_id, costo, tiempo_entrega_dias, es_habitual) "
                + "VALUES (?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertar(conexionCompartida, sql, proveedorRepuesto);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertar(connection, sql, proveedorRepuesto);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto) {
        if (proveedorRepuesto == null) {
            throw new ValidacionException("La asociación proveedor-repuesto no puede ser nula.");
        }
        String sql = "UPDATE proveedor_repuestos SET costo = ?, tiempo_entrega_dias = ? "
                + "WHERE proveedor_id = ? AND repuesto_id = ?";
        if (conexionCompartida != null) {
            return ejecutarActualizarCondiciones(conexionCompartida, sql, proveedorRepuesto);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarActualizarCondiciones(connection, sql, proveedorRepuesto);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int proveedorId, int repuestoId) {
        String sql = "DELETE FROM proveedor_repuestos WHERE proveedor_id = ? AND repuesto_id = ?";
        if (conexionCompartida != null) {
            return ejecutarPorPar(conexionCompartida, sql, proveedorId, repuestoId) > 0;
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarPorPar(connection, sql, proveedorId, repuestoId) > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al eliminar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public int desmarcarHabitual(int repuestoId) {
        String sql = "UPDATE proveedor_repuestos SET es_habitual = FALSE WHERE repuesto_id = ? AND es_habitual = TRUE";
        if (conexionCompartida != null) {
            return ejecutarDesmarcar(conexionCompartida, sql, repuestoId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarDesmarcar(connection, sql, repuestoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al desmarcar el proveedor habitual: " + e.getMessage(), e);
        }
    }

    @Override
    public int desmarcarHabitualesDeProveedor(int proveedorId) {
        String sql = "UPDATE proveedor_repuestos SET es_habitual = FALSE WHERE proveedor_id = ? AND es_habitual = TRUE";
        if (conexionCompartida != null) {
            return ejecutarDesmarcar(conexionCompartida, sql, proveedorId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarDesmarcar(connection, sql, proveedorId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al desmarcar los repuestos habituales del proveedor: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public boolean marcarHabitual(int proveedorId, int repuestoId) {
        String sql = "UPDATE proveedor_repuestos SET es_habitual = TRUE WHERE proveedor_id = ? AND repuesto_id = ?";
        if (conexionCompartida != null) {
            return ejecutarPorPar(conexionCompartida, sql, proveedorId, repuestoId) > 0;
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarPorPar(connection, sql, proveedorId, repuestoId) > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al marcar el proveedor habitual: " + e.getMessage(), e);
        }
    }

    private List<ProveedorRepuesto> consultarLista(Connection connection, String sql, int id) {
        List<ProveedorRepuesto> resultado = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    resultado.add(mapear(resultSet));
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el catálogo proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    private ProveedorRepuesto consultarUno(Connection connection, String sql, int proveedorId, int repuestoId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, proveedorId);
            preparedStatement.setInt(2, repuestoId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapear(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    private boolean insertar(Connection connection, String sql, ProveedorRepuesto proveedorRepuesto) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, proveedorRepuesto.getProveedorId());
            preparedStatement.setInt(2, proveedorRepuesto.getRepuestoId());
            preparedStatement.setBigDecimal(3, proveedorRepuesto.getCosto());
            preparedStatement.setInt(4, proveedorRepuesto.getTiempoEntregaDias());
            preparedStatement.setBoolean(5, proveedorRepuesto.isEsHabitual());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    private boolean ejecutarActualizarCondiciones(Connection connection, String sql, ProveedorRepuesto proveedorRepuesto) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setBigDecimal(1, proveedorRepuesto.getCosto());
            preparedStatement.setInt(2, proveedorRepuesto.getTiempoEntregaDias());
            preparedStatement.setInt(3, proveedorRepuesto.getProveedorId());
            preparedStatement.setInt(4, proveedorRepuesto.getRepuestoId());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al actualizar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    private int ejecutarPorPar(Connection connection, String sql, int proveedorId, int repuestoId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, proveedorId);
            preparedStatement.setInt(2, repuestoId);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new CarMotorsException("Error al modificar la asociación proveedor-repuesto: " + e.getMessage(), e);
        }
    }

    private int ejecutarDesmarcar(Connection connection, String sql, int repuestoId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, repuestoId);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new CarMotorsException("Error al desmarcar el proveedor habitual: " + e.getMessage(), e);
        }
    }

    private ProveedorRepuesto mapear(ResultSet resultSet) throws SQLException {
        ProveedorRepuesto proveedorRepuesto = new ProveedorRepuesto();
        proveedorRepuesto.setProveedorId(resultSet.getInt("proveedor_id"));
        proveedorRepuesto.setRepuestoId(resultSet.getInt("repuesto_id"));
        proveedorRepuesto.setCosto(resultSet.getBigDecimal("costo"));
        proveedorRepuesto.setTiempoEntregaDias(resultSet.getInt("tiempo_entrega_dias"));
        proveedorRepuesto.setEsHabitual(resultSet.getBoolean("es_habitual"));
        return proveedorRepuesto;
    }
}
