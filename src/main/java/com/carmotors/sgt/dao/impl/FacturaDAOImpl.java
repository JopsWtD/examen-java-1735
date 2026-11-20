package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.dao.FacturaDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de la cabecera de facturas ({@code facturas}).
 * Admite una {@link Connection} compartida para participar en transacciones del servicio;
 * en ese caso el DAO no cierra la conexión ni hace commit/rollback.
 */
public class FacturaDAOImpl implements FacturaDAO {

    private static final String COLUMNAS =
            "SELECT id, numero_factura, orden_id, fecha_emision, total_repuestos, total_mano_obra, subtotal, "
                    + "tasa_iva, impuestos, total_final, archivo_generado FROM facturas ";

    private final Connection conexionCompartida;

    public FacturaDAOImpl() {
        this.conexionCompartida = null;
    }

    public FacturaDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public int guardar(Factura factura) {
        if (factura == null) {
            throw new ValidacionException("La factura no puede ser nula.");
        }
        String sql = "INSERT INTO facturas (numero_factura, orden_id, fecha_emision, total_repuestos, total_mano_obra, "
                + "subtotal, tasa_iva, impuestos, total_final, archivo_generado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        if (conexionCompartida != null) {
            return insertar(conexionCompartida, sql, factura);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertar(connection, sql, factura);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la factura: " + e.getMessage(), e);
        }
    }

    @Override
    public Factura buscarPorId(int id) {
        String sql = COLUMNAS + "WHERE id = ?";
        if (conexionCompartida != null) {
            return consultarUna(conexionCompartida, sql, id);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarUna(connection, sql, id);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la factura " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Factura buscarPorOrden(int ordenId) {
        String sql = COLUMNAS + "WHERE orden_id = ?";
        if (conexionCompartida != null) {
            return consultarUna(conexionCompartida, sql, ordenId);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarUna(connection, sql, ordenId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la factura de la orden " + ordenId + ": "
                    + e.getMessage(), e);
        }
    }

    @Override
    public Factura buscarPorNumero(String numeroFactura) {
        if (numeroFactura == null || numeroFactura.trim().isEmpty()) {
            return null;
        }
        String numeroNormalizado = numeroFactura.trim().toUpperCase();
        String sql = COLUMNAS + "WHERE UPPER(numero_factura) = UPPER(?)";
        if (conexionCompartida != null) {
            return consultarUnaPorTexto(conexionCompartida, sql, numeroNormalizado);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarUnaPorTexto(connection, sql, numeroNormalizado);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar la factura " + numeroNormalizado + ": "
                    + e.getMessage(), e);
        }
    }

    @Override
    public List<Factura> listarTodas() {
        String sql = COLUMNAS + "ORDER BY fecha_emision DESC, id DESC";
        if (conexionCompartida != null) {
            return consultarLista(conexionCompartida, sql);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarLista(connection, sql);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar las facturas: " + e.getMessage(), e);
        }
    }

    @Override
    public String obtenerUltimoNumeroDelAnio(int anio) {
        String sql = "SELECT numero_factura FROM facturas WHERE numero_factura LIKE ? "
                + "ORDER BY numero_factura DESC LIMIT 1";
        if (conexionCompartida != null) {
            return consultarUltimoNumero(conexionCompartida, sql, anio);
        }
        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarUltimoNumero(connection, sql, anio);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el último número de factura del año " + anio + ": "
                    + e.getMessage(), e);
        }
    }

    @Override
    public void marcarArchivoGenerado(int facturaId) {
        try {
            if (conexionCompartida != null) {
                actualizarMarcaArchivo(conexionCompartida, facturaId);
            } else {
                try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
                    actualizarMarcaArchivo(connection, facturaId);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo confirmar el archivo de la factura " + facturaId + ".", e);
        }
    }

    private void actualizarMarcaArchivo(Connection connection, int facturaId) throws SQLException {
        String sql = "UPDATE facturas SET archivo_generado = TRUE WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, facturaId);
            if (statement.executeUpdate() == 0) {
                Factura factura = consultarUna(connection, COLUMNAS + "WHERE id = ?", facturaId);
                if (factura == null) {
                    throw new EntidadNoEncontradaException("No se encontró una factura con el ID: " + facturaId);
                }
                if (!factura.isArchivoGenerado()) {
                    throw new CarMotorsException("No se pudo confirmar el archivo de la factura " + facturaId + ".");
                }
            }
        }
    }

    private int insertar(Connection connection, String sql, Factura factura) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, factura.getNumeroFactura());
            preparedStatement.setInt(2, factura.getOrdenId());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(factura.getFechaEmision()));
            preparedStatement.setBigDecimal(4, factura.getTotalRepuestos());
            preparedStatement.setBigDecimal(5, factura.getTotalManoObra());
            preparedStatement.setBigDecimal(6, factura.getSubtotal());
            preparedStatement.setBigDecimal(7, factura.getTasaIva());
            preparedStatement.setBigDecimal(8, factura.getImpuestos());
            preparedStatement.setBigDecimal(9, factura.getTotalFinal());
            preparedStatement.setBoolean(10, factura.isArchivoGenerado());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna fila para la factura.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar la factura: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el identificador generado para la factura.");
    }

    private Factura consultarUna(Connection connection, String sql, int valor) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, valor);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapear(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar la factura: " + e.getMessage(), e);
        }
    }

    private Factura consultarUnaPorTexto(Connection connection, String sql, String valor) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, valor);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapear(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar la factura: " + e.getMessage(), e);
        }
    }

    private List<Factura> consultarLista(Connection connection, String sql) {
        List<Factura> facturas = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                facturas.add(mapear(resultSet));
            }
            return facturas;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el listado de facturas: " + e.getMessage(), e);
        }
    }

    private String consultarUltimoNumero(Connection connection, String sql, int anio) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, "F-" + anio + "-%");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("numero_factura");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el último número de factura del año " + anio + ": "
                    + e.getMessage(), e);
        }
    }

    private Factura mapear(ResultSet resultSet) throws SQLException {
        Factura factura = new Factura();
        factura.setId(resultSet.getInt("id"));
        factura.setNumeroFactura(resultSet.getString("numero_factura"));
        factura.setOrdenId(resultSet.getInt("orden_id"));
        Timestamp fechaEmision = resultSet.getTimestamp("fecha_emision");
        factura.setFechaEmision(fechaEmision != null ? fechaEmision.toLocalDateTime() : null);
        factura.setTotalRepuestos(resultSet.getBigDecimal("total_repuestos"));
        factura.setTotalManoObra(resultSet.getBigDecimal("total_mano_obra"));
        factura.setSubtotal(resultSet.getBigDecimal("subtotal"));
        factura.setTasaIva(resultSet.getBigDecimal("tasa_iva"));
        factura.setImpuestos(resultSet.getBigDecimal("impuestos"));
        factura.setTotalFinal(resultSet.getBigDecimal("total_final"));
        factura.setArchivoGenerado(resultSet.getBoolean("archivo_generado"));
        return factura;
    }
}
