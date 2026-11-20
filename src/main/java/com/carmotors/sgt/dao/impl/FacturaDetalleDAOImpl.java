package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.FacturaDetalle;
import com.carmotors.sgt.model.TipoConcepto;
import com.carmotors.sgt.dao.FacturaDetalleDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de los conceptos de factura ({@code factura_detalles}).
 * Admite una {@link Connection} compartida para participar en transacciones del servicio.
 */
public class FacturaDetalleDAOImpl implements FacturaDetalleDAO {

    private final Connection conexionCompartida;

    public FacturaDetalleDAOImpl() {
        this.conexionCompartida = null;
    }

    public FacturaDetalleDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public int guardar(FacturaDetalle detalle) {
        if (detalle == null) {
            throw new ValidacionException("El concepto de la factura no puede ser nulo.");
        }
        String sql = "INSERT INTO factura_detalles (factura_id, orden_detalle_id, repuesto_id, tipo_concepto, "
                + "descripcion_snapshot, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertar(conexionCompartida, sql, detalle);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertar(connection, sql, detalle);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el concepto de la factura: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FacturaDetalle> listarPorFactura(int facturaId) {
        String sql = "SELECT id, factura_id, orden_detalle_id, repuesto_id, tipo_concepto, descripcion_snapshot, "
                + "cantidad, precio_unitario, subtotal FROM factura_detalles WHERE factura_id = ? ORDER BY id ASC";
        if (conexionCompartida != null) {
            return consultar(conexionCompartida, sql, facturaId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultar(connection, sql, facturaId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar los conceptos de la factura " + facturaId + ": "
                    + e.getMessage(), e);
        }
    }

    private int insertar(Connection connection, String sql, FacturaDetalle detalle) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, detalle.getFacturaId());
            preparedStatement.setInt(2, detalle.getOrdenDetalleId());
            if (detalle.getRepuestoId() != null) {
                preparedStatement.setInt(3, detalle.getRepuestoId());
            } else {
                preparedStatement.setNull(3, Types.INTEGER);
            }
            preparedStatement.setString(4, detalle.getTipoConcepto().name());
            preparedStatement.setString(5, detalle.getDescripcionSnapshot());
            preparedStatement.setInt(6, detalle.getCantidad());
            preparedStatement.setBigDecimal(7, detalle.getPrecioUnitario());
            preparedStatement.setBigDecimal(8, detalle.getSubtotal());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna fila para el concepto de la factura.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el concepto de la factura: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el identificador generado para el concepto de la factura.");
    }

    private List<FacturaDetalle> consultar(Connection connection, String sql, int facturaId) {
        List<FacturaDetalle> detalles = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, facturaId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    detalles.add(mapear(resultSet));
                }
            }
            return detalles;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar los conceptos de la factura " + facturaId + ": "
                    + e.getMessage(), e);
        }
    }

    private FacturaDetalle mapear(ResultSet resultSet) throws SQLException {
        FacturaDetalle detalle = new FacturaDetalle();
        detalle.setId(resultSet.getInt("id"));
        detalle.setFacturaId(resultSet.getInt("factura_id"));
        detalle.setOrdenDetalleId(resultSet.getInt("orden_detalle_id"));
        int repuestoId = resultSet.getInt("repuesto_id");
        detalle.setRepuestoId(resultSet.wasNull() ? null : repuestoId);
        detalle.setTipoConcepto(TipoConcepto.valueOf(resultSet.getString("tipo_concepto")));
        detalle.setDescripcionSnapshot(resultSet.getString("descripcion_snapshot"));
        detalle.setCantidad(resultSet.getInt("cantidad"));
        detalle.setPrecioUnitario(resultSet.getBigDecimal("precio_unitario"));
        detalle.setSubtotal(resultSet.getBigDecimal("subtotal"));
        return detalle;
    }
}
