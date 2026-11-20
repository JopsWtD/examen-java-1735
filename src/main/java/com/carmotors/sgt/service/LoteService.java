package com.carmotors.sgt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.LotePorVencer;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.impl.LoteDAOImpl;
import com.carmotors.sgt.dao.impl.ProveedorDAOImpl;
import com.carmotors.sgt.dao.impl.ProveedorRepuestoDAOImpl;
import com.carmotors.sgt.dao.impl.RepuestoDAOImpl;
import com.carmotors.sgt.util.InputValidator;

/**
 * Reglas de negocio para el ingreso y consulta de inventario por lotes.
 */
public class LoteService {

    public static final int DIAS_VENCIMIENTO_POR_DEFECTO = 30;
    private static final int MAX_LONGITUD_CODIGO_LOTE = 40;

    private final LoteDAO loteDAO;
    private final ProveedorDAO proveedorDAO;
    private final ProveedorRepuestoDAO proveedorRepuestoDAO;
    private final RepuestoDAO repuestoDAO;
    private final boolean usarTransaccionJdbc;

    public LoteService() {
        this(new LoteDAOImpl(), new ProveedorDAOImpl(), new ProveedorRepuestoDAOImpl(), new RepuestoDAOImpl(), true);
    }

    public LoteService(LoteDAO loteDAO, ProveedorDAO proveedorDAO, ProveedorRepuestoDAO proveedorRepuestoDAO) {
        this(loteDAO, proveedorDAO, proveedorRepuestoDAO, new RepuestoDAOImpl(), true);
    }

    public LoteService(LoteDAO loteDAO, ProveedorDAO proveedorDAO, ProveedorRepuestoDAO proveedorRepuestoDAO,
                      RepuestoDAO repuestoDAO) {
        this(loteDAO, proveedorDAO, proveedorRepuestoDAO, repuestoDAO, false);
    }

    public LoteService(LoteDAO loteDAO, ProveedorDAO proveedorDAO, ProveedorRepuestoDAO proveedorRepuestoDAO,
                      RepuestoDAO repuestoDAO, boolean usarTransaccionJdbc) {
        if (loteDAO == null) {
            throw new ValidacionException("El DAO de lotes es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        if (proveedorRepuestoDAO == null) {
            throw new ValidacionException("El DAO del catálogo proveedor-repuesto es obligatorio.");
        }
        if (repuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos es obligatorio.");
        }
        this.loteDAO = loteDAO;
        this.proveedorDAO = proveedorDAO;
        this.proveedorRepuestoDAO = proveedorRepuestoDAO;
        this.repuestoDAO = repuestoDAO;
        this.usarTransaccionJdbc = usarTransaccionJdbc;
    }

    public Lote buscarPorCodigo(String codigoLote) {
        if (codigoLote == null || codigoLote.trim().isEmpty()) {
            throw new ValidacionException("El código del lote es obligatorio para la búsqueda.");
        }
        return loteDAO.buscarPorCodigo(codigoLote.trim());
    }

    public List<Lote> listarPorRepuesto(int repuestoId) {
        if (repuestoId <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }

        Repuesto repuesto = repuestoDAO.buscarPorId(repuestoId);
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No existe un repuesto con el ID: " + repuestoId);
        }

        List<Lote> lotes = loteDAO.listarPorRepuesto(repuestoId);
        List<Lote> resultado = new ArrayList<>(lotes);
        resultado.sort(Comparator
                .comparing((Lote lote) -> lote.getFechaCaducidad() == null)
                .thenComparing(Lote::getFechaCaducidad, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Lote::getFechaIngreso, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Lote::getId));
        return resultado;
    }

    /**
     * Genera el reporte de lotes que vencen dentro de los próximos 30 días o que ya están vencidos (RF-06).
     */
    public List<LotePorVencer> generarReporteLotesPorVencer() {
        return generarReporteLotesPorVencer(DIAS_VENCIMIENTO_POR_DEFECTO);
    }

    /**
     * Genera el reporte de lotes que vencen dentro de los próximos {@code dias} días o que ya están vencidos (RF-06).
     */
    public List<LotePorVencer> generarReporteLotesPorVencer(int dias) {
        return generarReporteLotesPorVencer(dias, LocalDate.now());
    }

    /**
     * Genera el reporte de lotes por vencer o vencidos tomando como referencia una fecha dada (RF-06, HU-03, RNF-11).
     *
     * @param dias horizonte de días hacia el futuro a evaluar (estrictamente mayor que cero).
     * @param fechaReferencia fecha base de cálculo (usualmente la fecha del sistema).
     * @return lista de lotes clasificados como VENCIDO o POR VENCER, ordenada por caducidad ascendente.
     */
    public List<LotePorVencer> generarReporteLotesPorVencer(int dias, LocalDate fechaReferencia) {
        if (dias <= 0) {
            throw new ValidacionException("La cantidad de días debe ser mayor que cero.");
        }
        if (fechaReferencia == null) {
            throw new ValidacionException("La fecha de referencia es obligatoria.");
        }

        LocalDate fechaLimite = fechaReferencia.plusDays(dias);
        List<Lote> lotes = loteDAO.listarPorVencer(fechaLimite);

        return lotes.stream()
                .filter(lote -> lote != null && lote.getCantidadActual() > 0 && lote.getFechaCaducidad() != null)
                .filter(lote -> !lote.getFechaCaducidad().isAfter(fechaLimite))
                .map(lote -> {
                    String estado = lote.getFechaCaducidad().isBefore(fechaReferencia) ? "VENCIDO" : "POR VENCER";
                    long diasRestantes = ChronoUnit.DAYS.between(fechaReferencia, lote.getFechaCaducidad());

                    Repuesto repuesto = repuestoDAO.buscarPorId(lote.getRepuestoId());
                    String nombreRepuesto = repuesto != null ? repuesto.getNombre() : "Repuesto #" + lote.getRepuestoId();

                    Proveedor proveedor = proveedorDAO.buscarPorId(lote.getProveedorId());
                    String nombreProveedor = proveedor != null ? proveedor.getNombre() : "Proveedor #" + lote.getProveedorId();

                    return new LotePorVencer(lote, nombreRepuesto, nombreProveedor, estado, diasRestantes);
                })
                .sorted(Comparator.comparing((LotePorVencer item) -> item.getLote().getFechaCaducidad())
                        .thenComparingInt(item -> item.getLote().getId()))
                .collect(Collectors.toList());
    }

    public Lote registrarLote(Lote lote) {
        if (lote == null) {
            throw new ValidacionException("El lote no puede ser nulo.");
        }

        validarDatosBase(lote);

        if (!usarTransaccionJdbc) {
            validarRelacionProveedorRepuesto(lote, proveedorDAO, proveedorRepuestoDAO, repuestoDAO);
            validarCodigoDuplicado(lote.getCodigoLote(), loteDAO);
            lote.setCantidadActual(lote.getCantidadInicial());
            int idGenerado = loteDAO.guardar(lote);
            lote.setId(idGenerado);
            return lote;
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                LoteDAO loteDaoTransaccional = new LoteDAOImpl(connection);
                ProveedorDAO proveedorDaoTransaccional = new ProveedorDAOImpl(connection);
                ProveedorRepuestoDAO proveedorRepuestoDaoTransaccional = new ProveedorRepuestoDAOImpl(connection);
                RepuestoDAO repuestoDaoTransaccional = new RepuestoDAOImpl(connection);

                validarRelacionProveedorRepuesto(lote, proveedorDaoTransaccional, proveedorRepuestoDaoTransaccional,
                        repuestoDaoTransaccional);
                validarCodigoDuplicado(lote.getCodigoLote(), loteDaoTransaccional);

                lote.setCantidadActual(lote.getCantidadInicial());
                int idGenerado = loteDaoTransaccional.guardar(lote);
                lote.setId(idGenerado);
                connection.commit();
                return lote;
            } catch (SQLException e) {
                connection.rollback();
                throw new CarMotorsException("No se pudo registrar el lote en la base de datos: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo abrir la conexión para registrar el lote: " + e.getMessage(), e);
        }
    }

    private void validarDatosBase(Lote lote) {
        String codigoNormalizado = InputValidator.normalizarTexto(lote.getCodigoLote());
        if (codigoNormalizado.isEmpty()) {
            throw new ValidacionException("El código del lote es obligatorio.");
        }
        if (codigoNormalizado.length() > MAX_LONGITUD_CODIGO_LOTE) {
            throw new ValidacionException("El código del lote no puede superar los " + MAX_LONGITUD_CODIGO_LOTE + " caracteres.");
        }
        if (!InputValidator.esCodigoLoteValido(codigoNormalizado)) {
            throw new ValidacionException(
                    "El código del lote debe tener entre 3 y 40 caracteres alfanuméricos o guiones (sin espacios) e incluir al menos una letra.");
        }
        lote.setCodigoLote(codigoNormalizado);

        if (lote.getProveedorId() <= 0) {
            throw new ValidacionException("El ID del proveedor debe ser un número positivo.");
        }
        if (lote.getRepuestoId() <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }
        if (lote.getCantidadInicial() <= 0) {
            throw new ValidacionException("La cantidad inicial del lote debe ser mayor que cero.");
        }
        if (lote.getFechaIngreso() == null) {
            throw new ValidacionException("La fecha de ingreso del lote es obligatoria.");
        }
        if (lote.getFechaIngreso().isAfter(LocalDate.now())) {
            throw new ValidacionException("La fecha de ingreso del lote no puede ser futura.");
        }
        if (lote.getFechaCaducidad() != null && !lote.getFechaCaducidad().isAfter(lote.getFechaIngreso())) {
            throw new ValidacionException("La fecha de caducidad debe ser estrictamente posterior a la fecha de ingreso.");
        }
    }

    private void validarRelacionProveedorRepuesto(Lote lote, ProveedorDAO proveedorDao,
                                                 ProveedorRepuestoDAO catalogoDao, RepuestoDAO repuestoDao) {
        Proveedor proveedor = proveedorDao.buscarPorId(lote.getProveedorId());
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + lote.getProveedorId());
        }
        if (!proveedor.isActivo()) {
            throw new ValidacionException("El proveedor '" + proveedor.getNombre() + "' está inactivo y no puede registrar lotes.");
        }

        Repuesto repuesto = repuestoDao.buscarPorId(lote.getRepuestoId());
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No existe un repuesto con el ID: " + lote.getRepuestoId());
        }
        if (!repuesto.isActivo()) {
            throw new ValidacionException("El repuesto '" + repuesto.getNombre() + "' está inactivo y no puede recibir lotes nuevos.");
        }

        ProveedorRepuesto asociacion = catalogoDao.buscarPorProveedorYRepuesto(lote.getProveedorId(), lote.getRepuestoId());
        if (asociacion == null) {
            throw new ValidacionException("El proveedor no ofrece el repuesto indicado en el catálogo.");
        }
    }

    private void validarCodigoDuplicado(String codigoLote, LoteDAO loteDao) {
        Lote existente = loteDao.buscarPorCodigo(codigoLote);
        if (existente != null) {
            throw new DatoDuplicadoException("Ya existe un lote registrado con el código: " + codigoLote);
        }
    }
}
