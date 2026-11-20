package com.carmotors.sgt.service;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.ConsultaFactura;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.model.FacturaDetalle;
import com.carmotors.sgt.model.FacturaSnapshot;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.ResultadoReimpresionFactura;
import com.carmotors.sgt.model.TipoConcepto;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.DetalleRepuestoDAO;
import com.carmotors.sgt.dao.FacturaDAO;
import com.carmotors.sgt.dao.FacturaDetalleDAO;
import com.carmotors.sgt.dao.FacturaSnapshotDAO;
import com.carmotors.sgt.dao.OrdenServicioDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import com.carmotors.sgt.dao.impl.ClienteDAOImpl;
import com.carmotors.sgt.dao.impl.DetalleRepuestoDAOImpl;
import com.carmotors.sgt.dao.impl.FacturaDAOImpl;
import com.carmotors.sgt.dao.impl.FacturaDetalleDAOImpl;
import com.carmotors.sgt.dao.impl.FacturaSnapshotDAOImpl;
import com.carmotors.sgt.dao.impl.OrdenServicioDAOImpl;
import com.carmotors.sgt.dao.impl.OrdenServicioDetalleDAOImpl;
import com.carmotors.sgt.dao.impl.VehiculoDAOImpl;
import com.carmotors.sgt.service.builder.FacturaTXTBuilder;
import com.carmotors.sgt.service.state.EstadoOrden;
import com.carmotors.sgt.service.state.EstadoOrdenFactory;
import com.carmotors.sgt.util.InputValidator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Reglas de negocio para generar, consultar y reimprimir facturas internas
 * (RF-22, RF-23, RF-24, RN-09, RN-11, RN-12).
 *
 * <p>La cabecera, el snapshot de cliente y vehículo, los conceptos y el cambio de la orden a
 * Facturado se guardan en una única transacción JDBC. El TXT y su marca se gestionan
 * después del commit (sección 2.7).</p>
 */
public class FacturaService {

    /** Tasa de IVA académica fija del proyecto (RN-12); se guarda en {@code facturas.tasa_iva}. */
    public static final BigDecimal TASA_IVA = new BigDecimal("0.19");

    private static final Logger LOGGER = Logger.getLogger(FacturaService.class.getName());
    private static final String PREFIJO_NUMERO = "F-";
    private static final int DIGITOS_CONSECUTIVO = 4;
    private static final int MAX_CONSECUTIVO = 9999;

    private final FacturaDAO facturaDAO;
    private final FacturaSnapshotDAO facturaSnapshotDAO;
    private final FacturaDetalleDAO facturaDetalleDAO;
    private final OrdenServicioDAO ordenServicioDAO;
    private final OrdenServicioDetalleDAO ordenServicioDetalleDAO;
    private final DetalleRepuestoDAO detalleRepuestoDAO;
    private final VehiculoDAO vehiculoDAO;
    private final ClienteDAO clienteDAO;
    private final boolean usarTransaccionJdbc;
    private final Supplier<Path> carpetaFacturas;

    /**
     * Constructor de producción con DAOs JDBC por defecto y transacción con una única {@link Connection}.
     */
    public FacturaService() {
        this(new FacturaDAOImpl(), new FacturaSnapshotDAOImpl(), new FacturaDetalleDAOImpl(),
                new OrdenServicioDAOImpl(), new OrdenServicioDetalleDAOImpl(), new DetalleRepuestoDAOImpl(),
                new VehiculoDAOImpl(), new ClienteDAOImpl(), true);
    }

    /**
     * Constructor de producción con los DAOs inyectados; usa transacción JDBC al generar la factura.
     */
    public FacturaService(FacturaDAO facturaDAO, FacturaSnapshotDAO facturaSnapshotDAO,
                          FacturaDetalleDAO facturaDetalleDAO, OrdenServicioDAO ordenServicioDAO,
                          OrdenServicioDetalleDAO ordenServicioDetalleDAO, DetalleRepuestoDAO detalleRepuestoDAO,
                          VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO) {
        this(facturaDAO, facturaSnapshotDAO, facturaDetalleDAO, ordenServicioDAO, ordenServicioDetalleDAO,
                detalleRepuestoDAO, vehiculoDAO, clienteDAO, true);
    }

    /**
     * Constructor completo: con {@code usarTransaccionJdbc = false} permite probar con DAOs en memoria.
     */
    public FacturaService(FacturaDAO facturaDAO, FacturaSnapshotDAO facturaSnapshotDAO,
                          FacturaDetalleDAO facturaDetalleDAO, OrdenServicioDAO ordenServicioDAO,
                          OrdenServicioDetalleDAO ordenServicioDetalleDAO, DetalleRepuestoDAO detalleRepuestoDAO,
                          VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, boolean usarTransaccionJdbc) {
        this(facturaDAO, facturaSnapshotDAO, facturaDetalleDAO, ordenServicioDAO, ordenServicioDetalleDAO,
                detalleRepuestoDAO, vehiculoDAO, clienteDAO, usarTransaccionJdbc,
                usarTransaccionJdbc ? () -> Path.of(ConnectionFactory.getInstance().getCarpetaFacturas()) : null);
    }

    public FacturaService(FacturaDAO facturaDAO, FacturaSnapshotDAO facturaSnapshotDAO,
                          FacturaDetalleDAO facturaDetalleDAO, OrdenServicioDAO ordenServicioDAO,
                          OrdenServicioDetalleDAO ordenServicioDetalleDAO, DetalleRepuestoDAO detalleRepuestoDAO,
                          VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO, boolean usarTransaccionJdbc,
                          Supplier<Path> carpetaFacturas) {
        if (facturaDAO == null) {
            throw new ValidacionException("El DAO de facturas es obligatorio.");
        }
        if (facturaSnapshotDAO == null) {
            throw new ValidacionException("El DAO de snapshots de factura es obligatorio.");
        }
        if (facturaDetalleDAO == null) {
            throw new ValidacionException("El DAO de conceptos de factura es obligatorio.");
        }
        if (ordenServicioDAO == null) {
            throw new ValidacionException("El DAO de órdenes de servicio es obligatorio.");
        }
        if (ordenServicioDetalleDAO == null) {
            throw new ValidacionException("El DAO de líneas de servicio es obligatorio.");
        }
        if (detalleRepuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos por línea es obligatorio.");
        }
        if (vehiculoDAO == null) {
            throw new ValidacionException("El DAO de vehículos es obligatorio.");
        }
        if (clienteDAO == null) {
            throw new ValidacionException("El DAO de clientes es obligatorio.");
        }
        this.facturaDAO = facturaDAO;
        this.facturaSnapshotDAO = facturaSnapshotDAO;
        this.facturaDetalleDAO = facturaDetalleDAO;
        this.ordenServicioDAO = ordenServicioDAO;
        this.ordenServicioDetalleDAO = ordenServicioDetalleDAO;
        this.detalleRepuestoDAO = detalleRepuestoDAO;
        this.vehiculoDAO = vehiculoDAO;
        this.clienteDAO = clienteDAO;
        this.usarTransaccionJdbc = usarTransaccionJdbc;
        this.carpetaFacturas = carpetaFacturas;
    }

    /**
     * Genera la factura interna de una orden Completada (RF-22, RF-23, RN-09, RN-11, RN-12).
     *
     * <p>En una única transacción JDBC: bloquea la orden, valida la transición a Facturado con el
     * patrón State y que no exista factura previa, arma los conceptos con los valores históricos de
     * la orden, calcula los totales, asigna el número consecutivo del año, guarda la cabecera, el
     * snapshot y los conceptos, comprueba que haya un snapshot y al menos un concepto, y cambia la
     * orden a Facturado. Tras confirmar, intenta escribir el TXT. Si falla el archivo,
     * devuelve la factura persistida con su marca pendiente.</p>
     *
     * @param ordenId identificador de la orden
     * @return la factura generada con su snapshot y sus conceptos
     */
    public ConsultaFactura generarFactura(int ordenId) {
        ConsultaFactura generada = persistirFactura(ordenId);
        if (carpetaFacturas != null) {
            try {
                generarArchivo(generada);
            } catch (CarMotorsException e) {
                registrarFalloArchivo(generada.getFactura().getId(), e);
            }
        }
        return generada;
    }

    private ConsultaFactura persistirFactura(int ordenId) {
        if (ordenId <= 0) {
            throw new ValidacionException("El ID de la orden debe ser un número positivo.");
        }
        LocalDateTime fechaEmision = LocalDateTime.now().withNano(0);

        if (!usarTransaccionJdbc) {
            return ejecutarGenerarFactura(ordenId, fechaEmision, facturaDAO, facturaSnapshotDAO, facturaDetalleDAO,
                    ordenServicioDAO, ordenServicioDetalleDAO, detalleRepuestoDAO, vehiculoDAO, clienteDAO);
        }

        try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                ConsultaFactura generada = ejecutarGenerarFactura(ordenId, fechaEmision,
                        new FacturaDAOImpl(connection), new FacturaSnapshotDAOImpl(connection),
                        new FacturaDetalleDAOImpl(connection), new OrdenServicioDAOImpl(connection),
                        new OrdenServicioDetalleDAOImpl(connection), new DetalleRepuestoDAOImpl(connection),
                        new VehiculoDAOImpl(connection), new ClienteDAOImpl(connection));
                connection.commit();
                return generada;
            } catch (CarMotorsException e) {
                connection.rollback();
                throw e;
            } catch (SQLException e) {
                connection.rollback();
                throw new CarMotorsException("No se pudo generar la factura: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw new CarMotorsException("Error inesperado al generar la factura: " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CarMotorsException("No se pudo completar la transacción de la factura de la orden "
                    + ordenId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Consulta una factura con su snapshot y sus conceptos (RF-24, RN-11).
     * Lee exclusivamente las tablas históricas de factura sin consultar el catálogo operativo.
     *
     * @param facturaId identificador de la factura
     * @return la factura con su snapshot y sus conceptos
     */
    public ConsultaFactura consultarFactura(int facturaId) {
        if (facturaId <= 0) {
            throw new ValidacionException("El ID de la factura debe ser un número positivo.");
        }
        Factura factura = facturaDAO.buscarPorId(facturaId);
        if (factura == null) {
            throw new EntidadNoEncontradaException("No se encontró una factura con el ID: " + facturaId);
        }
        return construirConsulta(factura);
    }

    /**
     * Consulta la factura histórica asociada a una orden de servicio (RF-24, RN-11).
     *
     * @param ordenId identificador de la orden de servicio
     * @return la factura con su snapshot y sus conceptos
     */
    public ConsultaFactura consultarFacturaPorOrden(int ordenId) {
        ConsultaFactura consulta = buscarFacturaPorOrden(ordenId);
        if (consulta == null) {
            throw new EntidadNoEncontradaException("No se encontró una factura registrada para la orden: " + ordenId);
        }
        return consulta;
    }

    /**
     * Busca la factura histórica asociada a una orden de servicio; retorna {@code null} si aún no tiene factura.
     *
     * @param ordenId identificador de la orden de servicio
     * @return la factura histórica o {@code null} si la orden no ha sido facturada
     */
    public ConsultaFactura buscarFacturaPorOrden(int ordenId) {
        if (ordenId <= 0) {
            throw new ValidacionException("El ID de la orden debe ser un número positivo.");
        }
        Factura factura = facturaDAO.buscarPorOrden(ordenId);
        if (factura == null) {
            return null;
        }
        return construirConsulta(factura);
    }

    /**
     * Consulta una factura histórica por su número único (ej. {@code F-2026-0001}) (RF-24, RN-11).
     *
     * @param numeroFactura número de factura
     * @return la factura con su snapshot y sus conceptos
     */
    public ConsultaFactura consultarFacturaPorNumero(String numeroFactura) {
        if (numeroFactura == null || numeroFactura.trim().isEmpty()) {
            throw new ValidacionException("El número de factura es obligatorio.");
        }
        String numeroNormalizado = numeroFactura.trim().toUpperCase();
        Factura factura = facturaDAO.buscarPorNumero(numeroNormalizado);
        if (factura == null) {
            throw new EntidadNoEncontradaException("No se encontró una factura con el número: " + numeroNormalizado);
        }
        return construirConsulta(factura);
    }

    /**
     * Lista las cabeceras de todas las facturas emitidas (más recientes primero).
     *
     * @return lista de facturas persistidas
     */
    public List<Factura> listarFacturas() {
        return facturaDAO.listarTodas();
    }

    /** Regenera el mismo comprobante a partir de sus datos históricos persistidos. */
    public Path reintentarArchivo(int facturaId) {
        return reimprimirFactura(facturaId).getArchivoTxt();
    }

    /**
     * Reimprime una factura existente por su ID (RF-24, RN-11).
     * Reconstruye el archivo TXT UTF-8 exclusivamente desde {@code facturas},
     * {@code factura_snapshot} y {@code factura_detalles}, sin crear otra factura ni alterar la orden.
     *
     * @param facturaId identificador de la factura
     * @return consulta histórica y ruta del archivo TXT generado
     */
    public ResultadoReimpresionFactura reimprimirFactura(int facturaId) {
        return reimprimirConsulta(consultarFactura(facturaId));
    }

    /**
     * Reimprime la factura existente asociada a una orden de servicio (RF-24, RN-11).
     *
     * @param ordenId identificador de la orden
     * @return consulta histórica y ruta del archivo TXT generado
     */
    public ResultadoReimpresionFactura reimprimirFacturaPorOrden(int ordenId) {
        return reimprimirConsulta(consultarFacturaPorOrden(ordenId));
    }

    /**
     * Reimprime una factura existente por su número único (RF-24, RN-11).
     *
     * @param numeroFactura número de factura (ej. {@code F-2026-0001})
     * @return consulta histórica y ruta del archivo TXT generado
     */
    public ResultadoReimpresionFactura reimprimirFacturaPorNumero(String numeroFactura) {
        return reimprimirConsulta(consultarFacturaPorNumero(numeroFactura));
    }

    private ConsultaFactura construirConsulta(Factura factura) {
        FacturaSnapshot snapshot = facturaSnapshotDAO.buscarPorFactura(factura.getId());
        if (snapshot == null) {
            throw new CarMotorsException("La factura " + factura.getNumeroFactura() + " no tiene snapshot de cliente "
                    + "y vehículo.");
        }
        List<FacturaDetalle> detalles = facturaDetalleDAO.listarPorFactura(factura.getId());
        return new ConsultaFactura(factura, snapshot, detalles);
    }

    private ResultadoReimpresionFactura reimprimirConsulta(ConsultaFactura consulta) {
        int facturaId = consulta.getFactura().getId();
        try {
            Path archivo = generarArchivo(consulta);
            return new ResultadoReimpresionFactura(consulta, archivo);
        } catch (CarMotorsException e) {
            registrarFalloArchivo(facturaId, e);
            throw e;
        }
    }

    private Path generarArchivo(ConsultaFactura consulta) {
        Factura factura = consulta.getFactura();
        if (carpetaFacturas == null) {
            throw new CarMotorsException("La carpeta de facturas no está configurada.");
        }
        if (factura.getNumeroFactura() == null
                || !factura.getNumeroFactura().matches("[A-Za-z0-9_-]+")
                || consulta.getDetalles().isEmpty()) {
            throw new CarMotorsException("La factura no tiene datos válidos para generar el TXT.");
        }

        String contenido = new FacturaTXTBuilder()
                .conEncabezado(factura)
                .conClienteYVehiculo(consulta.getSnapshot())
                .conConceptos(consulta.getDetalles())
                .conTotales(factura)
                .construir();
        Path destino;
        try {
            Path carpeta = Files.createDirectories(carpetaFacturas.get());
            destino = carpeta.resolve("factura_" + factura.getNumeroFactura() + ".txt");
            escribirArchivoCompleto(destino, contenido);
        } catch (IOException | InvalidPathException | SecurityException e) {
            throw new CarMotorsException("No se pudo escribir el comprobante TXT. Revise la carpeta configurada.", e);
        }

        if (!factura.isArchivoGenerado()) {
            facturaDAO.marcarArchivoGenerado(factura.getId());
            factura.setArchivoGenerado(true);
        }
        return destino;
    }

    private void escribirArchivoCompleto(Path destino, String contenido) throws IOException {
        Path temporal = Files.createTempFile(destino.getParent(), "factura_", ".tmp");
        try {
            try (BufferedWriter escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                escritor.write(contenido);
            }
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | SecurityException e) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException | SecurityException errorLimpieza) {
                e.addSuppressed(errorLimpieza);
            }
            throw e;
        }
    }

    private void registrarFalloArchivo(int facturaId, CarMotorsException e) {
        Throwable causa = e.getCause() == null ? e : e.getCause();
        LOGGER.warning("No se pudo finalizar el TXT de la factura " + facturaId + " ("
                + causa.getClass().getSimpleName() + ").");
    }

    private ConsultaFactura ejecutarGenerarFactura(int ordenId, LocalDateTime fechaEmision,
                                                   FacturaDAO facturaDao, FacturaSnapshotDAO snapshotDao,
                                                   FacturaDetalleDAO conceptoDao, OrdenServicioDAO ordenDao,
                                                   OrdenServicioDetalleDAO lineaDao, DetalleRepuestoDAO reservaDao,
                                                   VehiculoDAO vehiculoDao, ClienteDAO clienteDao) {
        // 1. La orden existe y el patrón State permite facturarla (solo desde Completado).
        OrdenServicio orden = ordenDao.buscarPorIdParaActualizar(ordenId);
        if (orden == null) {
            throw new EntidadNoEncontradaException("No se encontró una orden de servicio con el ID: " + ordenId);
        }
        EstadoOrden nuevoEstado = EstadoOrdenFactory.desdeId(orden.getEstadoId()).facturar();

        // 2. Relación 1:1: la orden no puede tener factura previa (RN-09).
        Factura previa = facturaDao.buscarPorOrden(ordenId);
        if (previa != null) {
            throw new DatoDuplicadoException("La orden " + ordenId + " ya tiene la factura "
                    + previa.getNumeroFactura() + ".");
        }

        // 3. Conceptos con los valores históricos de la orden (sección 2.7, RN-11).
        List<FacturaDetalle> conceptos = armarConceptos(ordenId, lineaDao, reservaDao);

        // 4. Totales (RN-12).
        Factura factura = calcularTotales(conceptos);
        factura.setOrdenId(ordenId);
        factura.setFechaEmision(fechaEmision);
        factura.setArchivoGenerado(false);

        // 5. Número consecutivo del año, dentro de la misma transacción.
        factura.setNumeroFactura(siguienteNumero(facturaDao, fechaEmision.getYear()));

        // 6. Cabecera, snapshot con los datos actuales del cliente y vehículo, y conceptos.
        FacturaSnapshot snapshot = armarSnapshot(orden, vehiculoDao, clienteDao);
        int facturaId = facturaDao.guardar(factura);
        factura.setId(facturaId);
        snapshot.setFacturaId(facturaId);
        if (!snapshotDao.guardar(snapshot)) {
            throw new CarMotorsException("No se pudo registrar el snapshot de la factura " + factura.getNumeroFactura()
                    + ".");
        }
        for (FacturaDetalle concepto : conceptos) {
            concepto.setFacturaId(facturaId);
            concepto.setId(conceptoDao.guardar(concepto));
        }
        verificarFacturaCompleta(factura, snapshotDao, conceptoDao, conceptos.size());

        // 7. La orden pasa a Facturado (patrón State).
        if (!ordenDao.actualizarEstado(ordenId, nuevoEstado.getEstadoId())) {
            throw new CarMotorsException("No se pudo cambiar la orden " + ordenId + " a Facturado.");
        }
        return new ConsultaFactura(factura, snapshot, conceptos);
    }

    private List<FacturaDetalle> armarConceptos(int ordenId, OrdenServicioDetalleDAO lineaDao,
                                                DetalleRepuestoDAO reservaDao) {
        List<OrdenServicioDetalle> lineas = lineaDao.listarPorOrden(ordenId);
        if (lineas.isEmpty()) {
            throw new ValidacionException("La orden " + ordenId + " no tiene líneas de servicio para facturar.");
        }

        List<FacturaDetalle> conceptos = new ArrayList<>();
        for (OrdenServicioDetalle linea : lineas) {
            conceptos.add(crearConcepto(linea.getId(), null, TipoConcepto.MANO_OBRA,
                    linea.getNombreServicioSnapshot(), 1, linea.getCostoManoObra()));
            for (DetalleRepuesto reserva : reservaDao.listarPorDetalle(linea.getId())) {
                conceptos.add(crearConcepto(linea.getId(), reserva.getRepuestoId(), TipoConcepto.REPUESTO,
                        reserva.getNombreRepuestoSnapshot(), reserva.getCantidadAsignada(),
                        reserva.getPrecioUnitarioVenta()));
            }
        }
        return conceptos;
    }

    private FacturaDetalle crearConcepto(int ordenDetalleId, Integer repuestoId, TipoConcepto tipo,
                                         String descripcion, int cantidad, BigDecimal precioUnitario) {
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("La línea " + ordenDetalleId + " tiene un precio inválido para el concepto '"
                    + descripcion + "'; debe ser mayor o igual a cero.");
        }
        FacturaDetalle concepto = new FacturaDetalle();
        concepto.setOrdenDetalleId(ordenDetalleId);
        concepto.setRepuestoId(repuestoId);
        concepto.setTipoConcepto(tipo);
        concepto.setDescripcionSnapshot(descripcion);
        concepto.setCantidad(cantidad);
        concepto.setPrecioUnitario(redondear(precioUnitario));
        concepto.setSubtotal(redondear(concepto.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad))));
        validarConcepto(concepto);
        return concepto;
    }

    /** Los campos deben coincidir con el tipo de concepto (sección 2.7, {@code ck_fd_concepto}). */
    private void validarConcepto(FacturaDetalle concepto) {
        if (concepto.getTipoConcepto() == TipoConcepto.MANO_OBRA && concepto.getRepuestoId() != null) {
            throw new ValidacionException("Un concepto de mano de obra no puede tener repuesto asociado.");
        }
        if (concepto.getTipoConcepto() == TipoConcepto.REPUESTO && concepto.getRepuestoId() == null) {
            throw new ValidacionException("Un concepto de repuesto debe tener un repuesto asociado.");
        }
        if (concepto.getCantidad() <= 0) {
            throw new ValidacionException("La cantidad del concepto '" + concepto.getDescripcionSnapshot()
                    + "' debe ser mayor que cero.");
        }
        if (concepto.getDescripcionSnapshot() == null || concepto.getDescripcionSnapshot().trim().isEmpty()) {
            throw new ValidacionException("Todo concepto de la factura debe tener descripción.");
        }
    }

    private Factura calcularTotales(List<FacturaDetalle> conceptos) {
        BigDecimal totalRepuestos = sumarSubtotales(conceptos, TipoConcepto.REPUESTO);
        BigDecimal totalManoObra = sumarSubtotales(conceptos, TipoConcepto.MANO_OBRA);
        BigDecimal subtotal = redondear(totalRepuestos.add(totalManoObra));
        BigDecimal impuestos = redondear(subtotal.multiply(TASA_IVA));

        Factura factura = new Factura();
        factura.setTotalRepuestos(totalRepuestos);
        factura.setTotalManoObra(totalManoObra);
        factura.setSubtotal(subtotal);
        factura.setTasaIva(TASA_IVA);
        factura.setImpuestos(impuestos);
        factura.setTotalFinal(redondear(subtotal.add(impuestos)));
        return factura;
    }

    private BigDecimal sumarSubtotales(List<FacturaDetalle> conceptos, TipoConcepto tipo) {
        BigDecimal suma = conceptos.stream()
                .filter(concepto -> concepto.getTipoConcepto() == tipo)
                .map(concepto -> concepto.getSubtotal())
                .reduce(BigDecimal.ZERO, (acumulado, subtotal) -> acumulado.add(subtotal));
        return redondear(suma);
    }

    private String siguienteNumero(FacturaDAO facturaDao, int anio) {
        String ultimo = facturaDao.obtenerUltimoNumeroDelAnio(anio);
        int consecutivo = 1;
        if (ultimo != null) {
            String prefijoAnio = PREFIJO_NUMERO + anio + "-";
            if (!ultimo.startsWith(prefijoAnio)) {
                throw new CarMotorsException("El último número de factura '" + ultimo + "' no corresponde al año "
                        + anio + ".");
            }
            try {
                consecutivo = Integer.parseInt(ultimo.substring(prefijoAnio.length())) + 1;
            } catch (NumberFormatException e) {
                throw new CarMotorsException("El último número de factura '" + ultimo
                        + "' no tiene el formato F-<año>-<consecutivo>.", e);
            }
        }
        if (consecutivo > MAX_CONSECUTIVO) {
            throw new CarMotorsException("Se agotó el consecutivo de facturas del año " + anio + " (máximo "
                    + MAX_CONSECUTIVO + ").");
        }
        String consecutivoTexto = String.valueOf(consecutivo);
        while (consecutivoTexto.length() < DIGITOS_CONSECUTIVO) {
            consecutivoTexto = "0" + consecutivoTexto;
        }
        return PREFIJO_NUMERO + anio + "-" + consecutivoTexto;
    }

    private FacturaSnapshot armarSnapshot(OrdenServicio orden, VehiculoDAO vehiculoDao, ClienteDAO clienteDao) {
        Vehiculo vehiculo = vehiculoDao.buscarPorId(orden.getVehiculoId());
        if (vehiculo == null) {
            throw new EntidadNoEncontradaException("No se encontró el vehículo de la orden " + orden.getId() + ".");
        }
        Cliente cliente = clienteDao.buscarPorId(vehiculo.getClienteId());
        if (cliente == null) {
            throw new EntidadNoEncontradaException("No se encontró el cliente dueño del vehículo "
                    + vehiculo.getPlaca() + ".");
        }

        FacturaSnapshot snapshot = new FacturaSnapshot();
        snapshot.setClienteNombre(cliente.getNombre());
        snapshot.setClienteDocumento(cliente.getDocumentoIdentidad());
        snapshot.setClienteTelefono(cliente.getTelefono());
        snapshot.setClienteEmail(cliente.getEmail());
        snapshot.setVehiculoPlaca(vehiculo.getPlaca());
        snapshot.setVehiculoMarca(vehiculo.getMarca());
        snapshot.setVehiculoModelo(vehiculo.getModelo());
        return snapshot;
    }

    /** Antes del commit: exactamente un snapshot y al menos un concepto (sección 2.7). */
    private void verificarFacturaCompleta(Factura factura, FacturaSnapshotDAO snapshotDao,
                                          FacturaDetalleDAO conceptoDao, int conceptosEsperados) {
        if (snapshotDao.buscarPorFactura(factura.getId()) == null) {
            throw new CarMotorsException("La factura " + factura.getNumeroFactura() + " quedó sin snapshot.");
        }
        int conceptosGuardados = conceptoDao.listarPorFactura(factura.getId()).size();
        if (conceptosGuardados < 1 || conceptosGuardados != conceptosEsperados) {
            throw new CarMotorsException("La factura " + factura.getNumeroFactura() + " tiene " + conceptosGuardados
                    + " concepto(s) guardado(s) y se esperaban " + conceptosEsperados + ".");
        }
    }

    private BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(InputValidator.ESCALA_MONETARIA, InputValidator.REDONDEO_MONETARIO);
    }
}
