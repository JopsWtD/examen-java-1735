package com.carmotors.sgt.dao.impl;

import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ConsultaOrdenServicioDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConsultaOrdenServicioDAOImpl implements ConsultaOrdenServicioDAO {
    private static final String ORDEN_POR_FECHA = " ORDER BY o.fecha_recepcion DESC, o.id DESC, d.id, t.id";

    private final Connection connection;

    public ConsultaOrdenServicioDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<ResumenOrdenServicio> listar(FiltroOrdenServicio filtro) {
        StringBuilder sql = sqlBase();
        List<Object> parametros = new ArrayList<>();
        if (filtro.getEstadoId() != null) {
            sql.append(" AND o.estado_id = ?");
            parametros.add(filtro.getEstadoId());
        }
        if (filtro.getPlaca() != null && !filtro.getPlaca().isEmpty()) {
            sql.append(" AND v.placa = ?");
            parametros.add(filtro.getPlaca());
        }
        if (filtro.getDesde() != null) {
            sql.append(" AND o.fecha_recepcion >= ?");
            parametros.add(Timestamp.valueOf(filtro.getDesde().atStartOfDay()));
        }
        if (filtro.getHasta() != null) {
            sql.append(" AND o.fecha_recepcion < ?");
            parametros.add(Timestamp.valueOf(filtro.getHasta().plusDays(1).atStartOfDay()));
        }
        sql.append(ORDEN_POR_FECHA);
        return ejecutarConsulta(sql, parametros, "No se pudieron listar las órdenes de servicio.");
    }

    @Override
    public List<ResumenOrdenServicio> listarPorCliente(int clienteId) {
        StringBuilder sql = sqlBase();
        List<Object> parametros = new ArrayList<>();
        sql.append(" AND v.cliente_id = ?");
        parametros.add(clienteId);
        sql.append(ORDEN_POR_FECHA);
        return ejecutarConsulta(sql, parametros,
                "No se pudieron listar las órdenes del cliente " + clienteId + ".");
    }

    private StringBuilder sqlBase() {
        return new StringBuilder(
                "SELECT o.id, o.estado_id, o.fecha_recepcion, v.placa, d.servicio_tipo_id, "
                + "t.id AS tecnico_id, t.nombre AS tecnico_nombre "
                + "FROM ordenes_servicio o "
                + "JOIN vehiculos v ON v.id = o.vehiculo_id "
                + "LEFT JOIN orden_servicio_detalle d ON d.orden_id = o.id "
                + "LEFT JOIN detalle_tecnicos dt ON dt.detalle_id = d.id "
                + "LEFT JOIN tecnicos t ON t.id = dt.tecnico_id WHERE 1 = 1");
    }

    private List<ResumenOrdenServicio> ejecutarConsulta(StringBuilder sql, List<Object> parametros,
                                                        String mensajeError) {
        Map<Integer, ResumenOrdenServicio> ordenes = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                Object parametro = parametros.get(i);
                if (parametro instanceof Timestamp) {
                    statement.setTimestamp(i + 1, (Timestamp) parametro);
                } else {
                    statement.setObject(i + 1, parametro);
                }
            }
            try (ResultSet filas = statement.executeQuery()) {
                while (filas.next()) {
                    int id = filas.getInt("id");
                    ResumenOrdenServicio orden = ordenes.get(id);
                    if (orden == null) {
                        orden = new ResumenOrdenServicio(id, filas.getInt("estado_id"), filas.getString("placa"),
                                filas.getTimestamp("fecha_recepcion").toLocalDateTime());
                        ordenes.put(id, orden);
                    }
                    int tipoId = filas.getInt("servicio_tipo_id");
                    if (!filas.wasNull()) {
                        orden.getTiposServicio().add(tipoId);
                    }
                    int tecnicoId = filas.getInt("tecnico_id");
                    if (!filas.wasNull()) {
                        orden.getTecnicos().put(tecnicoId, filas.getString("tecnico_nombre"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException(mensajeError, e);
        }
        return new ArrayList<>(ordenes.values());
    }

    @Override
    public void cargarDatosFacturados(int ordenId, Cliente cliente, Vehiculo vehiculo) {
        String sql = "SELECT s.cliente_nombre_snapshot, s.cliente_documento_snapshot, "
                + "s.cliente_telefono_snapshot, s.cliente_email_snapshot, "
                + "s.vehiculo_placa_snapshot, s.vehiculo_marca_snapshot, s.vehiculo_modelo_snapshot "
                + "FROM facturas f JOIN factura_snapshot s ON s.factura_id = f.id WHERE f.orden_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ordenId);
            try (ResultSet fila = statement.executeQuery()) {
                if (fila.next()) {
                    cliente.setNombre(fila.getString("cliente_nombre_snapshot"));
                    cliente.setDocumentoIdentidad(fila.getString("cliente_documento_snapshot"));
                    cliente.setTelefono(fila.getString("cliente_telefono_snapshot"));
                    cliente.setEmail(fila.getString("cliente_email_snapshot"));
                    vehiculo.setPlaca(fila.getString("vehiculo_placa_snapshot"));
                    vehiculo.setMarca(fila.getString("vehiculo_marca_snapshot"));
                    vehiculo.setModelo(fila.getString("vehiculo_modelo_snapshot"));
                }
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudieron consultar los datos facturados de la orden.", e);
        }
    }
}
