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
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.dao.LoteDAO;

public class LoteDAOImpl implements LoteDAO {

    private final Connection conexionCompartida;

    public LoteDAOImpl() {
        this(null);
    }

    public LoteDAOImpl(Connection conexionCompartida) {
        this.conexionCompartida = conexionCompartida;
    }

    @Override
    public Lote buscarPorCodigo(String codigoLote) {
        if (codigoLote == null || codigoLote.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id, codigo_lote, proveedor_id, repuesto_id, fecha_ingreso, fecha_caducidad, cantidad_inicial, cantidad_actual "
                + "FROM lotes WHERE LOWER(codigo_lote) = LOWER(?)";

        if (conexionCompartida != null) {
            return consultarPorCodigo(conexionCompartida, sql, codigoLote.trim());
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorCodigo(connection, sql, codigoLote.trim());
        } catch (SQLException e) {
            throw new CarMotorsException("Error al buscar el lote por código: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Lote> listarPorRepuesto(int repuestoId) {
        String sql = "SELECT id, codigo_lote, proveedor_id, repuesto_id, fecha_ingreso, fecha_caducidad, cantidad_inicial, cantidad_actual "
                + "FROM lotes WHERE repuesto_id = ? "
                + "ORDER BY CASE WHEN fecha_caducidad IS NULL THEN 1 ELSE 0 END ASC, "
                + "fecha_caducidad ASC, fecha_ingreso ASC, id ASC";

        if (conexionCompartida != null) {
            return consultarPorRepuesto(conexionCompartida, sql, repuestoId);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorRepuesto(connection, sql, repuestoId);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al listar los lotes del repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Lote lote) {
        if (lote == null) {
            throw new ValidacionException("El lote no puede ser nulo.");
        }

        String sql = "INSERT INTO lotes (codigo_lote, proveedor_id, repuesto_id, fecha_ingreso, fecha_caducidad, cantidad_inicial, cantidad_actual) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        if (conexionCompartida != null) {
            return insertarLote(conexionCompartida, sql, lote);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return insertarLote(connection, sql, lote);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el lote: " + e.getMessage(), e);
        }
    }

    @Override
    public int sumarStockFisico(int repuestoId) {
        String sql = "SELECT COALESCE(SUM(cantidad_actual), 0) AS total FROM lotes WHERE repuesto_id = ?";

        if (conexionCompartida != null) {
            return consultarSuma(conexionCompartida, sql, repuestoId, null);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarSuma(connection, sql, repuestoId, null);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al calcular el stock físico del repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
        if (fechaReferencia == null) {
            throw new ValidacionException("La fecha de referencia del stock operativo es obligatoria.");
        }

        String sql = "SELECT COALESCE(SUM(cantidad_actual), 0) AS total FROM lotes "
                + "WHERE repuesto_id = ? AND (fecha_caducidad IS NULL OR fecha_caducidad >= ?)";

        if (conexionCompartida != null) {
            return consultarSuma(conexionCompartida, sql, repuestoId, fechaReferencia);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarSuma(connection, sql, repuestoId, fechaReferencia);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al calcular el stock operativo del repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Lote> listarPorVencer(LocalDate fechaLimite) {
        if (fechaLimite == null) {
            throw new ValidacionException("La fecha límite para el reporte de lotes por vencer es obligatoria.");
        }

        String sql = "SELECT id, codigo_lote, proveedor_id, repuesto_id, fecha_ingreso, fecha_caducidad, cantidad_inicial, cantidad_actual "
                + "FROM lotes "
                + "WHERE cantidad_actual > 0 AND fecha_caducidad IS NOT NULL AND fecha_caducidad <= ? "
                + "ORDER BY fecha_caducidad ASC, id ASC";

        if (conexionCompartida != null) {
            return consultarPorVencer(conexionCompartida, sql, fechaLimite);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return consultarPorVencer(connection, sql, fechaLimite);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar los lotes por vencer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean descontarUnidades(int loteId, int cantidad) {
        String sql = "UPDATE lotes SET cantidad_actual = cantidad_actual - ? WHERE id = ? AND cantidad_actual >= ?";

        if (conexionCompartida != null) {
            return ejecutarDescuento(conexionCompartida, sql, loteId, cantidad);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            return ejecutarDescuento(connection, sql, loteId, cantidad);
        } catch (SQLException e) {
            throw new CarMotorsException("Error al descontar unidades del lote " + loteId + ": " + e.getMessage(), e);
        }
    }

    private boolean ejecutarDescuento(Connection connection, String sql, int loteId, int cantidad) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, cantidad);
            preparedStatement.setInt(2, loteId);
            preparedStatement.setInt(3, cantidad);
            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new CarMotorsException("Error al descontar unidades del lote " + loteId + ": " + e.getMessage(), e);
        }
    }

    private List<Lote> consultarPorVencer(Connection connection, String sql, LocalDate fechaLimite) {
        List<Lote> lotes = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, Date.valueOf(fechaLimite));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lotes.add(mapearLote(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar los lotes por vencer: " + e.getMessage(), e);
        }
        return lotes;
    }

    private List<Lote> consultarPorRepuesto(Connection connection, String sql, int repuestoId) {
        List<Lote> lotes = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, repuestoId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lotes.add(mapearLote(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar los lotes del repuesto: " + e.getMessage(), e);
        }
        return lotes;
    }

    private int consultarSuma(Connection connection, String sql, int repuestoId, LocalDate fechaReferencia) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, repuestoId);
            if (fechaReferencia != null) {
                preparedStatement.setDate(2, Date.valueOf(fechaReferencia));
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el stock del repuesto: " + e.getMessage(), e);
        }
    }

    private Lote consultarPorCodigo(Connection connection, String sql, String codigoLote) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, codigoLote);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapearLote(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al consultar el lote por código: " + e.getMessage(), e);
        }
    }

    private int insertarLote(Connection connection, String sql, Lote lote) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, lote.getCodigoLote());
            preparedStatement.setInt(2, lote.getProveedorId());
            preparedStatement.setInt(3, lote.getRepuestoId());
            preparedStatement.setDate(4, Date.valueOf(lote.getFechaIngreso()));
            if (lote.getFechaCaducidad() != null) {
                preparedStatement.setDate(5, Date.valueOf(lote.getFechaCaducidad()));
            } else {
                preparedStatement.setNull(5, java.sql.Types.DATE);
            }
            preparedStatement.setInt(6, lote.getCantidadInicial());
            preparedStatement.setInt(7, lote.getCantidadActual());

            int filas = preparedStatement.executeUpdate();
            if (filas == 0) {
                throw new CarMotorsException("No se insertó ninguna fila para el lote indicado.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("Error al registrar el lote: " + e.getMessage(), e);
        }

        throw new CarMotorsException("No se obtuvo el identificador generado para el lote.");
    }

    private Lote mapearLote(ResultSet resultSet) throws SQLException {
        Lote lote = new Lote();
        lote.setId(resultSet.getInt("id"));
        lote.setCodigoLote(resultSet.getString("codigo_lote"));
        lote.setProveedorId(resultSet.getInt("proveedor_id"));
        lote.setRepuestoId(resultSet.getInt("repuesto_id"));
        Date fechaIngreso = resultSet.getDate("fecha_ingreso");
        lote.setFechaIngreso(fechaIngreso != null ? fechaIngreso.toLocalDate() : null);
        Date fechaCaducidad = resultSet.getDate("fecha_caducidad");
        lote.setFechaCaducidad(fechaCaducidad != null ? fechaCaducidad.toLocalDate() : null);
        lote.setCantidadInicial(resultSet.getInt("cantidad_inicial"));
        lote.setCantidadActual(resultSet.getInt("cantidad_actual"));
        return lote;
    }
}
