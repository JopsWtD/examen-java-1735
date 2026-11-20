package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.ConsumoRepuesto;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ResultadoCompletarOrden;
import com.carmotors.sgt.model.StockRepuesto;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.impl.LoteDAOImpl;
import com.carmotors.sgt.dao.impl.ProveedorDAOImpl;
import com.carmotors.sgt.dao.impl.ProveedorRepuestoDAOImpl;
import com.carmotors.sgt.dao.impl.RepuestoDAOImpl;
import com.carmotors.sgt.service.observer.StockObserver;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Cálculo de stock por repuesto según la sección 2.5 del documento maestro (RF-07, RF-05).
 *
 * <p>Los lotes son la fuente de verdad: el stock físico y el operativo se obtienen con consultas
 * de agregación sobre {@code lotes}. El reservado proviene de {@code repuestos.cantidad_reservada}.</p>
 */
public class InventarioService {

    private static final String PROVEEDOR_NO_DEFINIDO = "No definido";

    private static final Logger LOGGER = Logger.getLogger(InventarioService.class.getName());

    private final RepuestoDAO repuestoDAO;
    private final LoteDAO loteDAO;
    private final ProveedorRepuestoDAO proveedorRepuestoDAO;
    private final ProveedorDAO proveedorDAO;
    private final List<StockObserver> observers = new ArrayList<>();

    public InventarioService() {
        this(new RepuestoDAOImpl(), new LoteDAOImpl(), new ProveedorRepuestoDAOImpl(), new ProveedorDAOImpl());
    }

    public InventarioService(RepuestoDAO repuestoDAO, LoteDAO loteDAO) {
        this(repuestoDAO, loteDAO, new ProveedorRepuestoDAOImpl(), new ProveedorDAOImpl());
    }

    public InventarioService(RepuestoDAO repuestoDAO, LoteDAO loteDAO,
                             ProveedorRepuestoDAO proveedorRepuestoDAO, ProveedorDAO proveedorDAO) {
        if (repuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos es obligatorio.");
        }
        if (loteDAO == null) {
            throw new ValidacionException("El DAO de lotes es obligatorio.");
        }
        if (proveedorRepuestoDAO == null) {
            throw new ValidacionException("El DAO del catálogo proveedor-repuesto es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        this.repuestoDAO = repuestoDAO;
        this.loteDAO = loteDAO;
        this.proveedorRepuestoDAO = proveedorRepuestoDAO;
        this.proveedorDAO = proveedorDAO;
    }

    /**
     * Calcula las métricas de stock del repuesto indicado.
     */
    public StockRepuesto calcularStock(int repuestoId) {
        if (repuestoId <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }

        Repuesto repuesto = repuestoDAO.buscarPorId(repuestoId);
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No se encontró un repuesto con el ID: " + repuestoId);
        }
        return calcularStock(repuesto, LocalDate.now());
    }

    /**
     * Calcula las métricas de stock de todos los repuestos activos, para reportes como stock bajo (T3.7).
     */
    public List<StockRepuesto> calcularStockActivos() {
        LocalDate hoy = LocalDate.now();
        return repuestoDAO.listarActivos().stream()
                .map(repuesto -> calcularStock(repuesto, hoy))
                .collect(Collectors.toList());
    }

    /**
     * Genera el reporte de repuestos activos cuyo disponible para nuevas reservas
     * es menor o igual a su stock mínimo (RF-05, RN-22, RNF-11).
     */
    public List<StockRepuesto> generarReporteStockBajo() {
        return calcularStockActivos().stream()
                .filter(stock -> stock.getDisponible() <= stock.getStockMinimo())
                .map(this::completarProveedorHabitual)
                .sorted(Comparator.comparingInt(StockRepuesto::getDisponible)
                        .thenComparing(stock -> stock.getRepuesto().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Registra un observador interesado en las alertas de stock (Patrón Observer, RN-22).
     */
    public void agregarObserver(StockObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Desuscribe un observador previamente registrado.
     */
    public void eliminarObserver(StockObserver observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    /**
     * Retorna una copia de la lista de observadores registrados.
     */
    public List<StockObserver> getObservers() {
        return new ArrayList<>(observers);
    }

    /**
     * Evalúa el stock del repuesto y notifica a los observadores si el repuesto está activo
     * y su disponible para nuevas reservas es menor o igual a su stock mínimo (RN-22, T5.1).
     *
     * @param repuestoId ID del repuesto a evaluar.
     * @return métricas calculadas del stock del repuesto con su proveedor habitual resuelto.
     */
    public StockRepuesto evaluarYNotificarStockBajo(int repuestoId) {
        StockRepuesto stock = calcularStock(repuestoId);
        completarProveedorHabitual(stock);

        if (stock.getRepuesto() != null && stock.getRepuesto().isActivo()
                && stock.getDisponible() <= stock.getStockMinimo()) {
            notificarStockBajo(stock);
        }

        return stock;
    }

    public void notificarStockBajoTrasCompletar(ResultadoCompletarOrden resultado) {
        for (ConsumoRepuesto consumo : resultado.getConsumos()) {
            try {
                evaluarYNotificarStockBajo(consumo.getRepuestoId());
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "La orden " + resultado.getOrden().getId() + " se completó, pero no se pudo "
                        + "evaluar la alerta de stock bajo del repuesto " + consumo.getRepuestoId() + ".", e);
            }
        }
    }

    private void notificarStockBajo(StockRepuesto stock) {
        List<StockObserver> copiaObservers = new ArrayList<>(observers);
        for (StockObserver observer : copiaObservers) {
            try {
                observer.onStockBajo(stock);
            } catch (RuntimeException e) {
                int repuestoId = 0;
                if (stock != null && stock.getRepuesto() != null) {
                    repuestoId = stock.getRepuesto().getId();
                }
                LOGGER.log(Level.WARNING, "El observador " + observer.getClass().getName()
                        + " falló al notificar el stock bajo del repuesto " + repuestoId
                        + ". Se continúa con los demás observadores.", e);
            }
        }
    }

    private StockRepuesto completarProveedorHabitual(StockRepuesto stock) {
        stock.setProveedorHabitual(resolverProveedorHabitual(stock.getRepuesto().getId()));
        return stock;
    }

    private String resolverProveedorHabitual(int repuestoId) {
        for (ProveedorRepuesto asociacion : proveedorRepuestoDAO.listarPorRepuesto(repuestoId)) {
            if (asociacion.isEsHabitual()) {
                Proveedor proveedor = proveedorDAO.buscarPorId(asociacion.getProveedorId());
                if (proveedor != null && proveedor.isActivo()) {
                    return proveedor.getNombre();
                }
            }
        }
        return PROVEEDOR_NO_DEFINIDO;
    }

    private StockRepuesto calcularStock(Repuesto repuesto, LocalDate fechaReferencia) {
        int stockFisico = loteDAO.sumarStockFisico(repuesto.getId());
        int stockOperativo = loteDAO.sumarStockOperativo(repuesto.getId(), fechaReferencia);
        int reservado = repuesto.getCantidadReservada();
        int disponible = Math.max(0, stockOperativo - reservado);
        int deficit = Math.max(0, reservado - stockOperativo);

        return new StockRepuesto(repuesto, stockFisico, stockOperativo, reservado,
                disponible, deficit, repuesto.getStockMinimo());
    }
}
