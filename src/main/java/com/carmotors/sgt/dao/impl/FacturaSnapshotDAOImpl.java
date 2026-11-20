package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.FacturaSnapshot;
import com.carmotors.sgt.dao.FacturaSnapshotDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Implementación JDBC de {@code factura_snapshot} (datos históricos de cliente y vehículo).
 * Admite una {@link Connection} compartida para participar en transacciones del servicio.
 */
public class FacturaSnapshotDAOImpl implements FacturaSnapshotDAO {

    private final Connection conexionCompartida;

    public FacturaSnapshotDAOImpl() {
        this.conexionCompartida = null;
    }

    public FacturaSnapshotDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public boolean guardar(FacturaSnapshot snapshot) {
        if (snapshot == null) {
            throw new ValidacionException("El snapshot de la factura no puede ser nulo.");
        }
        String sql = "INSERT INTO factura_snapshot (factura_id, cliente_nombre_snapshot, cliente_documento_snapshot, "
                + "cliente_telefono_snapshot, cliente_email_snapshot, vehiculo_placa_snapshot, vehiculo_marca_snapshot, "
                + "vehiculo_modelo_snapshot) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertar(conexionCompartida, sql, snapshot);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertar(connection, sql, snapshot);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el snapshot de la factura: " + e.getMessage(), e);
        }
    }

    @Override
    public FacturaSnapshot buscarPorFactura(int facturaId) {
        String sql = "SELECT factura_id, cliente_nombre_snapshot, cliente_documento_snapshot, cliente_telefono_snapshot, "
                + "cliente_email_snapshot, vehiculo_placa_snapshot, vehiculo_marca_snapshot, vehiculo_modelo_snapshot "
                + "FROM factura_snapshot WHERE factura_id = ?";
        if (conexionCompartida != null) {
            return consultar(conexionCompartida, sql, facturaId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultar(connection, sql, facturaId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar el snapshot de la factura " + facturaId + ": "
                    + e.getMessage(), e);
        }
    }

    private boolean insertar(Connection connection, String sql, FacturaSnapshot snapshot) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, snapshot.getFacturaId());
            preparedStatement.setString(2, snapshot.getClienteNombre());
            preparedStatement.setString(3, snapshot.getClienteDocumento());
            asignarTextoOpcional(preparedStatement, 4, snapshot.getClienteTelefono());
            asignarTextoOpcional(preparedStatement, 5, snapshot.getClienteEmail());
            preparedStatement.setString(6, snapshot.getVehiculoPlaca());
            preparedStatement.setString(7, snapshot.getVehiculoMarca());
            preparedStatement.setString(8, snapshot.getVehiculoModelo());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el snapshot de la factura: " + e.getMessage(), e);
        }
    }

    private void asignarTextoOpcional(PreparedStatement preparedStatement, int indice, String valor)
            throws SQLException {
        if (valor != null) {
            preparedStatement.setString(indice, valor);
        } else {
            preparedStatement.setNull(indice, Types.VARCHAR);
        }
    }

    private FacturaSnapshot consultar(Connection connection, String sql, int facturaId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, facturaId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapear(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el snapshot de la factura " + facturaId + ": "
                    + e.getMessage(), e);
        }
    }

    private FacturaSnapshot mapear(ResultSet resultSet) throws SQLException {
        FacturaSnapshot snapshot = new FacturaSnapshot();
        snapshot.setFacturaId(resultSet.getInt("factura_id"));
        snapshot.setClienteNombre(resultSet.getString("cliente_nombre_snapshot"));
        snapshot.setClienteDocumento(resultSet.getString("cliente_documento_snapshot"));
        snapshot.setClienteTelefono(resultSet.getString("cliente_telefono_snapshot"));
        snapshot.setClienteEmail(resultSet.getString("cliente_email_snapshot"));
        snapshot.setVehiculoPlaca(resultSet.getString("vehiculo_placa_snapshot"));
        snapshot.setVehiculoMarca(resultSet.getString("vehiculo_marca_snapshot"));
        snapshot.setVehiculoModelo(resultSet.getString("vehiculo_modelo_snapshot"));
        return snapshot;
    }
}
