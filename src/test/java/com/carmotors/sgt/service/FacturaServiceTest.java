package com.carmotors.sgt.service;

import com.carmotors.sgt.controller.FacturaController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.ConsultaFactura;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.DetalleTecnico;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la generación de facturas (T6.1, RF-22, RF-23, RN-09, RN-11, RN-12) con DAOs en
 * memoria y sin transacción JDBC.
 */
class FacturaServiceTest {

    private static final int PENDIENTE = 1;
    private static final int EN_PROCESO = 2;
    private static final int COMPLETADO = 3;
    private static final int FACTURADO = 4;

    private final int anio = LocalDate.now().getYear();

    private FacturaDAOMock facturaDAO;
    private FacturaSnapshotDAOMock snapshotDAO;
    private FacturaDetalleDAOMock conceptoDAO;
    private OrdenServicioDAOMock ordenDAO;
    private OrdenServicioDetalleDAOMock lineaDAO;
    private DetalleRepuestoDAOMock reservaDAO;
    private VehiculoDAOMock vehiculoDAO;
    private ClienteDAOMock clienteDAO;
    private FacturaService service;

    @BeforeEach
    void setUp() {
        facturaDAO = new FacturaDAOMock();
        snapshotDAO = new FacturaSnapshotDAOMock();
        conceptoDAO = new FacturaDetalleDAOMock();
        ordenDAO = new OrdenServicioDAOMock();
        lineaDAO = new OrdenServicioDetalleDAOMock();
        reservaDAO = new DetalleRepuestoDAOMock();
        vehiculoDAO = new VehiculoDAOMock();
        clienteDAO = new ClienteDAOMock();

        clienteDAO.agregar(new Cliente(3, "Luisa Marcela Díaz", "1093456703", "3125551103",
                "luisa.diaz@correo.com", true));
        vehiculoDAO.agregar(new Vehiculo(3, 3, "GHI789", "Mazda", "3"));

        // Facturas ya emitidas: dos del año actual y una del anterior.
        facturaDAO.agregar(factura(1, "F-" + anio + "-0001", 5));
        facturaDAO.agregar(factura(2, "F-" + anio + "-0002", 4));
        facturaDAO.agregar(factura(3, "F-" + (anio - 1) + "-0009", 11));

        // Orden 3 Completada, como en data.sql: "Cambio de batería" + "Batería 12V 45Ah".
        ordenDAO.agregar(orden(3, COMPLETADO));
        lineaDAO.agregar(linea(4, 3, "Cambio de batería", "40000.00"));
        reservaDAO.agregar(new DetalleRepuesto(4, 7, 1, "Batería 12V 45Ah", new BigDecimal("380000.00")));

        ordenDAO.agregar(orden(1, PENDIENTE));
        lineaDAO.agregar(linea(1, 1, "Cambio de aceite y filtro", "60000.00"));
        ordenDAO.agregar(orden(2, EN_PROCESO));
        lineaDAO.agregar(linea(2, 2, "Revisión y cambio de pastillas de freno", "90000.00"));
        ordenDAO.agregar(orden(5, FACTURADO));
        lineaDAO.agregar(linea(7, 5, "Cambio de aceite y filtro", "60000.00"));

        // Orden 6: una línea sin repuestos.
        ordenDAO.agregar(orden(6, COMPLETADO));
        lineaDAO.agregar(linea(60, 6, "Alineación y balanceo", "70000.00"));

        // Orden 8: mano de obra 11.50 -> IVA 2.185 -> 2.19 con HALF_UP (HALF_EVEN daría 2.18).
        ordenDAO.agregar(orden(8, COMPLETADO));
        lineaDAO.agregar(linea(80, 8, "Ajuste menor", "11.50"));

        // Orden 9: Completada pero ya facturada.
        ordenDAO.agregar(orden(9, COMPLETADO));
        lineaDAO.agregar(linea(90, 9, "Diagnóstico", "50000.00"));
        facturaDAO.agregar(factura(4, "F-" + (anio - 1) + "-0003", 9));

        // Orden 10: dos líneas; precios y nombres históricos distintos del catálogo actual.
        ordenDAO.agregar(orden(10, COMPLETADO));
        lineaDAO.agregar(linea(100, 10, "Cambio de aceite y filtro", "45000.00"));
        lineaDAO.agregar(linea(101, 10, "Alineación y balanceo", "70000.00"));
        reservaDAO.agregar(new DetalleRepuesto(100, 1, 4, "Aceite sintético 10W-40 (litro)",
                new BigDecimal("38000.00")));
        reservaDAO.agregar(new DetalleRepuesto(100, 2, 1, "Filtro de aceite", new BigDecimal("25000.00")));

        service = crearServicio();
    }

    private FacturaService crearServicio() {
        return new FacturaService(facturaDAO, snapshotDAO, conceptoDAO, ordenDAO, lineaDAO, reservaDAO,
                vehiculoDAO, clienteDAO, false);
    }

    @Test
    void generaComprobanteUtf8YMarcaLaFactura(@TempDir Path carpeta) throws IOException {
        FacturaService conArchivo = crearServicioConArchivo(carpeta.resolve("output"));
        ConsultaFactura generada = conArchivo.generarFactura(10);
        Factura factura = generada.getFactura();
        Path archivo = carpeta.resolve("output").resolve("factura_" + factura.getNumeroFactura() + ".txt");

        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(texto.contains("Cliente: Luisa Marcela Díaz"));
        assertTrue(texto.contains("Vehículo: GHI789 | Mazda 3"));
        assertTrue(texto.contains("Línea 100 | Mano de obra | Cambio de aceite y filtro"));
        assertTrue(texto.contains("Línea 100 | Repuesto | Aceite sintético 10W-40 (litro)"));
        assertTrue(texto.contains("4 x $38000.00 = $152000.00"));
        assertTrue(texto.contains("IVA (19%): $55480.00"));
        assertTrue(texto.contains("Total (COP): $347480.00"));
        assertTrue(facturaDAO.buscarPorId(factura.getId()).isArchivoGenerado());
    }

    @Test
    void falloDeEscrituraConservaFacturaYPermiteReintentarDesdeSnapshots(@TempDir Path carpeta) throws IOException {
        Path salida = carpeta.resolve("output");
        Files.createFile(salida);
        FacturaService conArchivo = crearServicioConArchivo(salida);
        Factura factura = conArchivo.generarFactura(3).getFactura();

        assertFalse(factura.isArchivoGenerado());
        assertEquals(FACTURADO, ordenDAO.buscarPorId(3).getEstadoId());
        assertEquals(factura.getId(), facturaDAO.buscarPorOrden(3).getId());
        clienteDAO.buscarPorId(3).setNombre("Nombre actualizado");
        Files.delete(salida);

        Path archivo = conArchivo.reintentarArchivo(factura.getId());
        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(texto.contains("Luisa Marcela Díaz"));
        assertFalse(texto.contains("Nombre actualizado"));
        assertEquals(archivo, conArchivo.reintentarArchivo(factura.getId()));
        assertEquals(texto, Files.readString(archivo, StandardCharsets.UTF_8));
        assertTrue(facturaDAO.buscarPorId(factura.getId()).isArchivoGenerado());
        assertEquals(1, facturaDAO.facturas.stream().filter(item -> item.getOrdenId() == 3).count());
    }

    @Test
    void reintentaSiElTxtSeEscribioPeroFalloLaMarca(@TempDir Path carpeta) throws IOException {
        FacturaService conArchivo = crearServicioConArchivo(carpeta);
        facturaDAO.fallarMarca = true;
        Factura factura = conArchivo.generarFactura(6).getFactura();
        Path archivo = carpeta.resolve("factura_" + factura.getNumeroFactura() + ".txt");
        String original = Files.readString(archivo, StandardCharsets.UTF_8);
        assertFalse(factura.isArchivoGenerado());

        facturaDAO.fallarMarca = false;
        assertEquals(archivo, conArchivo.reintentarArchivo(factura.getId()));
        assertEquals(original, Files.readString(archivo, StandardCharsets.UTF_8));
        assertTrue(facturaDAO.buscarPorId(factura.getId()).isArchivoGenerado());
    }

    @Test
    void noEscribeArchivosSiLaOrdenNoSePuedeFacturar(@TempDir Path carpeta) throws IOException {
        FacturaService conArchivo = crearServicioConArchivo(carpeta);
        assertThrows(TransicionEstadoInvalidaException.class, () -> conArchivo.generarFactura(1));
        try (Stream<Path> archivos = Files.list(carpeta)) {
            assertEquals(0, archivos.count());
        }
    }

    private FacturaService crearServicioConArchivo(Path carpeta) {
        return new FacturaService(facturaDAO, snapshotDAO, conceptoDAO, ordenDAO, lineaDAO, reservaDAO,
                vehiculoDAO, clienteDAO, false, () -> carpeta);
    }

    private OrdenServicio orden(int id, int estadoId) {
        return new OrdenServicio(id, 3, estadoId, LocalDateTime.now(), "Orden " + id);
    }

    private OrdenServicioDetalle linea(int id, int ordenId, String nombre, String costoManoObra) {
        return new OrdenServicioDetalle(id, ordenId, 1, nombre, new BigDecimal(costoManoObra), null);
    }

    private Factura factura(int id, String numero, int ordenId) {
        BigDecimal cero = new BigDecimal("0.00");
        return new Factura(id, numero, ordenId, LocalDateTime.now(), cero, cero, cero,
                FacturaService.TASA_IVA, cero, cero, true);
    }

    private BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("Factura válida: conceptos, totales RN-12, número, snapshot y orden en Facturado")
    void generarFacturaValida() {
        ConsultaFactura generada = service.generarFactura(3);

        Factura factura = generada.getFactura();
        assertTrue(factura.getId() > 0);
        assertEquals("F-" + anio + "-0003", factura.getNumeroFactura());
        assertEquals(3, factura.getOrdenId());
        assertNotNull(factura.getFechaEmision());
        assertEquals(monto("380000.00"), factura.getTotalRepuestos());
        assertEquals(monto("40000.00"), factura.getTotalManoObra());
        assertEquals(monto("420000.00"), factura.getSubtotal());
        assertEquals(monto("0.19"), factura.getTasaIva());
        assertEquals(monto("79800.00"), factura.getImpuestos());
        assertEquals(monto("499800.00"), factura.getTotalFinal());
        assertFalse(factura.isArchivoGenerado());

        List<FacturaDetalle> conceptos = generada.getDetalles();
        assertEquals(2, conceptos.size());
        FacturaDetalle manoObra = conceptos.get(0);
        assertEquals(TipoConcepto.MANO_OBRA, manoObra.getTipoConcepto());
        assertNull(manoObra.getRepuestoId());
        assertEquals(4, manoObra.getOrdenDetalleId());
        assertEquals("Cambio de batería", manoObra.getDescripcionSnapshot());
        assertEquals(1, manoObra.getCantidad());
        assertEquals(monto("40000.00"), manoObra.getPrecioUnitario());
        assertEquals(monto("40000.00"), manoObra.getSubtotal());
        FacturaDetalle repuesto = conceptos.get(1);
        assertEquals(TipoConcepto.REPUESTO, repuesto.getTipoConcepto());
        assertEquals(Integer.valueOf(7), repuesto.getRepuestoId());
        assertEquals("Batería 12V 45Ah", repuesto.getDescripcionSnapshot());
        assertEquals(1, repuesto.getCantidad());
        assertEquals(monto("380000.00"), repuesto.getSubtotal());

        FacturaSnapshot snapshot = snapshotDAO.buscarPorFactura(factura.getId());
        assertNotNull(snapshot);
        assertEquals("Luisa Marcela Díaz", snapshot.getClienteNombre());
        assertEquals("1093456703", snapshot.getClienteDocumento());
        assertEquals("3125551103", snapshot.getClienteTelefono());
        assertEquals("luisa.diaz@correo.com", snapshot.getClienteEmail());
        assertEquals("GHI789", snapshot.getVehiculoPlaca());
        assertEquals("Mazda", snapshot.getVehiculoMarca());
        assertEquals("3", snapshot.getVehiculoModelo());

        assertEquals(2, conceptoDAO.listarPorFactura(factura.getId()).size());
        assertEquals(FACTURADO, ordenDAO.buscarPorId(3).getEstadoId());
    }

    @Test
    @DisplayName("Una línea sin repuestos genera solo el concepto de mano de obra")
    void lineaSinRepuestos() {
        ConsultaFactura generada = service.generarFactura(6);

        assertEquals(1, generada.getDetalles().size());
        FacturaDetalle concepto = generada.getDetalles().get(0);
        assertEquals(TipoConcepto.MANO_OBRA, concepto.getTipoConcepto());
        assertNull(concepto.getRepuestoId());
        assertEquals(monto("0.00"), generada.getFactura().getTotalRepuestos());
        assertEquals(monto("70000.00"), generada.getFactura().getTotalManoObra());
        assertEquals(monto("13300.00"), generada.getFactura().getImpuestos());
        assertEquals(monto("83300.00"), generada.getFactura().getTotalFinal());
    }

    @Test
    @DisplayName("El IVA se redondea a dos decimales con HALF_UP")
    void redondeoHalfUpDelIva() {
        Factura factura = service.generarFactura(8).getFactura();

        assertEquals(monto("11.50"), factura.getSubtotal());
        assertEquals(monto("2.19"), factura.getImpuestos());
        assertEquals(monto("13.69"), factura.getTotalFinal());
    }

    @Test
    @DisplayName("Usa los precios y nombres históricos de la orden, no los del catálogo actual (RN-11)")
    void usaPreciosHistoricos() {
        // El catálogo actual tendría otro precio y nombre; el servicio solo lee los snapshots de la orden.
        BigDecimal precioCatalogoActual = monto("42000.00");

        ConsultaFactura generada = service.generarFactura(10);

        List<FacturaDetalle> conceptos = generada.getDetalles();
        assertEquals(4, conceptos.size());
        assertEquals(monto("45000.00"), conceptos.get(0).getPrecioUnitario());
        FacturaDetalle aceite = conceptos.get(1);
        assertEquals("Aceite sintético 10W-40 (litro)", aceite.getDescripcionSnapshot());
        assertEquals(4, aceite.getCantidad());
        assertEquals(monto("38000.00"), aceite.getPrecioUnitario());
        assertFalse(precioCatalogoActual.equals(aceite.getPrecioUnitario()));
        assertEquals(monto("152000.00"), aceite.getSubtotal());
        assertEquals(TipoConcepto.MANO_OBRA, conceptos.get(3).getTipoConcepto());
        assertEquals(101, conceptos.get(3).getOrdenDetalleId());

        Factura factura = generada.getFactura();
        assertEquals(monto("177000.00"), factura.getTotalRepuestos());
        assertEquals(monto("115000.00"), factura.getTotalManoObra());
        assertEquals(monto("292000.00"), factura.getSubtotal());
        assertEquals(monto("55480.00"), factura.getImpuestos());
        assertEquals(monto("347480.00"), factura.getTotalFinal());
    }

    @Test
    @DisplayName("Consecutivo: sin facturas del año empieza en 0001")
    void consecutivoSinFacturasDelAnio() {
        facturaDAO = new FacturaDAOMock();
        facturaDAO.agregar(factura(3, "F-" + (anio - 1) + "-0009", 11));
        service = crearServicio();

        assertEquals("F-" + anio + "-0001", service.generarFactura(3).getFactura().getNumeroFactura());
    }

    @Test
    @DisplayName("Consecutivo: con F-<año>-0002 el siguiente es 0003, y luego 0004")
    void consecutivoSigueAlUltimoDelAnio() {
        assertEquals("F-" + anio + "-0003", service.generarFactura(6).getFactura().getNumeroFactura());
        assertEquals("F-" + anio + "-0004", service.generarFactura(8).getFactura().getNumeroFactura());
    }

    @Test
    @DisplayName("Consecutivo: rechaza superar F-<año>-9999 sin guardar nada")
    void consecutivoAgotado() {
        facturaDAO.agregar(factura(5, "F-" + anio + "-9999", 12));

        assertThrows(CarMotorsException.class, () -> service.generarFactura(3));
        assertEquals(COMPLETADO, ordenDAO.buscarPorId(3).getEstadoId());
        assertTrue(snapshotDAO.snapshots.isEmpty());
        assertTrue(conceptoDAO.conceptos.isEmpty());
    }

    @Test
    @DisplayName("Rechaza una orden inexistente o un ID inválido")
    void rechazarOrdenInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.generarFactura(99));
        assertThrows(ValidacionException.class, () -> service.generarFactura(0));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza facturar órdenes Pendiente, En Proceso o ya Facturadas sin cambiar nada")
    void rechazarEstadosInvalidos() {
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.generarFactura(1));
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.generarFactura(2));
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.generarFactura(5));

        assertEquals(PENDIENTE, ordenDAO.buscarPorId(1).getEstadoId());
        assertEquals(EN_PROCESO, ordenDAO.buscarPorId(2).getEstadoId());
        assertEquals(FACTURADO, ordenDAO.buscarPorId(5).getEstadoId());
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza una orden que ya tiene factura (relación 1:1)")
    void rechazarOrdenConFacturaPrevia() {
        DatoDuplicadoException error = assertThrows(DatoDuplicadoException.class, () -> service.generarFactura(9));

        assertTrue(error.getMessage().contains("ya tiene la factura"), error.getMessage());
        assertEquals(COMPLETADO, ordenDAO.buscarPorId(9).getEstadoId());
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Consulta una factura con su snapshot y sus conceptos")
    void consultarFactura() {
        int facturaId = service.generarFactura(3).getFactura().getId();

        ConsultaFactura consulta = service.consultarFactura(facturaId);

        assertEquals("F-" + anio + "-0003", consulta.getFactura().getNumeroFactura());
        assertEquals("GHI789", consulta.getSnapshot().getVehiculoPlaca());
        assertEquals(2, consulta.getDetalles().size());
        assertThrows(EntidadNoEncontradaException.class, () -> service.consultarFactura(999));
    }

    @Test
    @DisplayName("T6.3 RF-24: reimpresión idéntica aunque cambien cliente, vehículo, precios y se borre el TXT")
    void reimprimirFacturaEsIdenticaTrasCambiosEnCatalogoYBorradoDelTxt(@TempDir Path carpeta) throws IOException {
        FacturaService conArchivo = crearServicioConArchivo(carpeta.resolve("output"));
        ConsultaFactura generada = conArchivo.generarFactura(10);
        Factura facturaOriginal = generada.getFactura();
        Path archivoOriginal = carpeta.resolve("output")
                .resolve("factura_" + facturaOriginal.getNumeroFactura() + ".txt");
        String contenidoOriginal = Files.readString(archivoOriginal, StandardCharsets.UTF_8);
        int cantidadFacturasInicial = facturaDAO.facturas.size();

        // Se alteran cliente, vehículo, conceptos de la orden y estado de la orden (Entregado = 5)
        // y se elimina físicamente el TXT original.
        Cliente cliente = clienteDAO.buscarPorId(3);
        cliente.setNombre("Cliente Modificado");
        cliente.setTelefono("3999999999");
        cliente.setEmail("nuevo@correo.com");

        Vehiculo vehiculo = vehiculoDAO.buscarPorId(3);
        vehiculo.setPlaca("ZZZ999");
        vehiculo.setMarca("OtraMarca");
        vehiculo.setModelo("2099");

        lineaDAO.buscarPorId(100).setNombreServicioSnapshot("Servicio cambiado");
        lineaDAO.buscarPorId(100).setCostoManoObra(monto("999999.00"));
        reservaDAO.buscar(100, 1).setNombreRepuestoSnapshot("Repuesto cambiado");
        reservaDAO.buscar(100, 1).setPrecioUnitarioVenta(monto("888888.00"));

        ordenDAO.actualizarEstado(10, 5);
        Files.delete(archivoOriginal);
        assertFalse(Files.exists(archivoOriginal));

        // Reimpresión por ID
        ResultadoReimpresionFactura reimpresaId = conArchivo.reimprimirFactura(facturaOriginal.getId());
        assertEquals(archivoOriginal, reimpresaId.getArchivoTxt());
        assertTrue(Files.exists(archivoOriginal));
        assertEquals(contenidoOriginal, Files.readString(archivoOriginal, StandardCharsets.UTF_8));
        assertEquals("Luisa Marcela Díaz", reimpresaId.getConsulta().getSnapshot().getClienteNombre());
        assertEquals("GHI789", reimpresaId.getConsulta().getSnapshot().getVehiculoPlaca());

        // Reimpresión por número (con minúsculas y espacios) y por orden
        ResultadoReimpresionFactura reimpresaNumero = conArchivo.reimprimirFacturaPorNumero(
                "  " + facturaOriginal.getNumeroFactura().toLowerCase() + "  ");
        assertEquals(contenidoOriginal, Files.readString(reimpresaNumero.getArchivoTxt(), StandardCharsets.UTF_8));

        ResultadoReimpresionFactura reimpresaOrden = conArchivo.reimprimirFacturaPorOrden(10);
        assertEquals(contenidoOriginal, Files.readString(reimpresaOrden.getArchivoTxt(), StandardCharsets.UTF_8));

        // No crea facturas nuevas ni altera el estado de la orden (sigue Entregado = 5)
        assertEquals(cantidadFacturasInicial, facturaDAO.facturas.size());
        assertEquals(5, ordenDAO.buscarPorId(10).getEstadoId());
        assertTrue(facturaDAO.buscarPorId(facturaOriginal.getId()).isArchivoGenerado());
    }

    @Test
    @DisplayName("T6.3 RF-24: consulta por número, por orden, búsqueda opcional y listado de facturas")
    void consultarPorNumeroOrdenYListar() {
        ConsultaFactura generada = service.generarFactura(3);
        String numero = generada.getFactura().getNumeroFactura();

        ConsultaFactura porNumero = service.consultarFacturaPorNumero(" " + numero.toLowerCase() + " ");
        assertEquals(generada.getFactura().getId(), porNumero.getFactura().getId());
        assertEquals("Luisa Marcela Díaz", porNumero.getSnapshot().getClienteNombre());

        ConsultaFactura porOrden = service.consultarFacturaPorOrden(3);
        assertEquals(numero, porOrden.getFactura().getNumeroFactura());
        assertEquals(2, porOrden.getDetalles().size());

        assertNotNull(service.buscarFacturaPorOrden(3));
        assertNull(service.buscarFacturaPorOrden(1));
        assertEquals(5, service.listarFacturas().size());
    }

    @Test
    @DisplayName("T6.3 RF-24: validaciones y errores en consulta y reimpresión sin crear registros")
    void validacionesConsultaYReimpresion(@TempDir Path carpeta) {
        FacturaService conArchivo = crearServicioConArchivo(carpeta);
        int cantidadInicial = facturaDAO.facturas.size();

        assertThrows(ValidacionException.class, () -> conArchivo.consultarFactura(0));
        assertThrows(ValidacionException.class, () -> conArchivo.consultarFacturaPorOrden(-1));
        assertThrows(ValidacionException.class, () -> conArchivo.buscarFacturaPorOrden(0));
        assertThrows(ValidacionException.class, () -> conArchivo.consultarFacturaPorNumero("   "));
        assertThrows(ValidacionException.class, () -> conArchivo.consultarFacturaPorNumero(null));

        assertThrows(EntidadNoEncontradaException.class, () -> conArchivo.consultarFacturaPorNumero("F-2099-0001"));
        assertThrows(EntidadNoEncontradaException.class, () -> conArchivo.consultarFacturaPorOrden(1));
        assertThrows(EntidadNoEncontradaException.class, () -> conArchivo.reimprimirFactura(999));

        // Factura 1 del setUp existe en cabecera pero sin snapshot -> error controlado
        assertThrows(CarMotorsException.class, () -> conArchivo.consultarFactura(1));
        assertEquals(cantidadInicial, facturaDAO.facturas.size());
    }

    @Test
    @DisplayName("T6.3: FacturaController delega consultas, listado y reimpresión")
    void controladorDelegaConsultaYReimpresion(@TempDir Path carpeta) throws IOException {
        FacturaService conArchivo = crearServicioConArchivo(carpeta);
        FacturaController controller = new FacturaController(conArchivo);

        ConsultaFactura generada = controller.generarFactura(6);
        Factura factura = generada.getFactura();

        assertEquals(factura.getNumeroFactura(), controller.consultarFactura(factura.getId()).getFactura().getNumeroFactura());
        assertEquals(factura.getId(), controller.consultarFacturaPorOrden(6).getFactura().getId());
        assertEquals(factura.getId(), controller.buscarFacturaPorOrden(6).getFactura().getId());
        assertEquals(factura.getId(), controller.consultarFacturaPorNumero(factura.getNumeroFactura()).getFactura().getId());
        assertFalse(controller.listarFacturas().isEmpty());

        ResultadoReimpresionFactura r1 = controller.reimprimirFactura(factura.getId());
        ResultadoReimpresionFactura r2 = controller.reimprimirFacturaPorOrden(6);
        ResultadoReimpresionFactura r3 = controller.reimprimirFacturaPorNumero(factura.getNumeroFactura());

        assertTrue(Files.exists(r1.getArchivoTxt()));
        assertEquals(r1.getArchivoTxt(), r2.getArchivoTxt());
        assertEquals(r1.getArchivoTxt(), r3.getArchivoTxt());
        assertEquals(
                Files.readString(r1.getArchivoTxt(), StandardCharsets.UTF_8),
                Files.readString(r3.getArchivoTxt(), StandardCharsets.UTF_8)
        );
    }

    private void assertNadaGuardado() {
        assertEquals(4, facturaDAO.facturas.size());
        assertTrue(snapshotDAO.snapshots.isEmpty());
        assertTrue(conceptoDAO.conceptos.isEmpty());
    }

    // ------------------------------------------------------------------
    // DAOs en memoria
    // ------------------------------------------------------------------

    private static class FacturaDAOMock implements FacturaDAO {
        private final List<Factura> facturas = new ArrayList<>();
        private int secuencia = 100;
        private boolean fallarMarca;

        void agregar(Factura factura) {
            facturas.add(factura);
        }

        @Override
        public int guardar(Factura factura) {
            secuencia++;
            facturas.add(factura);
            return secuencia;
        }

        @Override
        public Factura buscarPorId(int id) {
            for (Factura factura : facturas) {
                if (factura.getId() == id) {
                    return factura;
                }
            }
            return null;
        }

        @Override
        public Factura buscarPorOrden(int ordenId) {
            for (Factura factura : facturas) {
                if (factura.getOrdenId() == ordenId) {
                    return factura;
                }
            }
            return null;
        }

        @Override
        public Factura buscarPorNumero(String numeroFactura) {
            if (numeroFactura == null) {
                return null;
            }
            for (Factura factura : facturas) {
                if (factura.getNumeroFactura() != null
                        && factura.getNumeroFactura().equalsIgnoreCase(numeroFactura.trim())) {
                    return factura;
                }
            }
            return null;
        }

        @Override
        public List<Factura> listarTodas() {
            return new ArrayList<>(facturas);
        }

        @Override
        public void marcarArchivoGenerado(int facturaId) {
            if (fallarMarca) {
                throw new CarMotorsException("Fallo de prueba al actualizar archivo_generado.");
            }
            buscarPorId(facturaId).setArchivoGenerado(true);
        }

        @Override
        public String obtenerUltimoNumeroDelAnio(int anio) {
            String ultimo = null;
            for (Factura factura : facturas) {
                String numero = factura.getNumeroFactura();
                if (numero.startsWith("F-" + anio + "-") && (ultimo == null || numero.compareTo(ultimo) > 0)) {
                    ultimo = numero;
                }
            }
            return ultimo;
        }
    }

    private static class FacturaSnapshotDAOMock implements FacturaSnapshotDAO {
        private final Map<Integer, FacturaSnapshot> snapshots = new HashMap<>();

        @Override
        public boolean guardar(FacturaSnapshot snapshot) {
            snapshots.put(snapshot.getFacturaId(), snapshot);
            return true;
        }

        @Override
        public FacturaSnapshot buscarPorFactura(int facturaId) {
            return snapshots.get(facturaId);
        }
    }

    private static class FacturaDetalleDAOMock implements FacturaDetalleDAO {
        private final List<FacturaDetalle> conceptos = new ArrayList<>();
        private int secuencia = 0;

        @Override
        public int guardar(FacturaDetalle detalle) {
            secuencia++;
            conceptos.add(detalle);
            return secuencia;
        }

        @Override
        public List<FacturaDetalle> listarPorFactura(int facturaId) {
            List<FacturaDetalle> resultado = new ArrayList<>();
            for (FacturaDetalle concepto : conceptos) {
                if (concepto.getFacturaId() == facturaId) {
                    resultado.add(concepto);
                }
            }
            return resultado;
        }
    }

    private static class OrdenServicioDAOMock implements OrdenServicioDAO {
        private final Map<Integer, OrdenServicio> ordenes = new HashMap<>();

        void agregar(OrdenServicio orden) {
            ordenes.put(orden.getId(), orden);
        }

        @Override
        public int guardar(OrdenServicio orden) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public OrdenServicio buscarPorId(int id) {
            return ordenes.get(id);
        }

        @Override
        public OrdenServicio buscarPorIdParaActualizar(int id) {
            OrdenServicio guardada = ordenes.get(id);
            if (guardada == null) {
                return null;
            }
            return new OrdenServicio(guardada.getId(), guardada.getVehiculoId(), guardada.getEstadoId(),
                    guardada.getFechaRecepcion(), guardada.getDescripcionProblema());
        }

        @Override
        public boolean actualizarEstado(int ordenId, int nuevoEstadoId) {
            OrdenServicio orden = ordenes.get(ordenId);
            if (orden == null) {
                return false;
            }
            orden.setEstadoId(nuevoEstadoId);
            return true;
        }

        @Override
        public boolean marcarCompletada(int ordenId, int estadoId, LocalDateTime fechaCompletado) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean marcarCancelada(int ordenId, int estadoId, LocalDateTime fechaCancelacion) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean marcarEntregada(int ordenId, int estadoId, LocalDateTime fechaEntrega) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }
    }

    private static class OrdenServicioDetalleDAOMock implements OrdenServicioDetalleDAO {
        private final List<OrdenServicioDetalle> lineas = new ArrayList<>();

        void agregar(OrdenServicioDetalle linea) {
            lineas.add(linea);
        }

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrden(int ordenId) {
            List<OrdenServicioDetalle> resultado = new ArrayList<>();
            for (OrdenServicioDetalle linea : lineas) {
                if (linea.getOrdenId() == ordenId) {
                    resultado.add(linea);
                }
            }
            return resultado;
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId) {
            return listarPorOrden(ordenId);
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean existeTecnicoAsignado(int detalleId, int tecnicoId) {
            return false;
        }

        @Override
        public List<DetalleTecnico> listarTecnicosAsignados(int detalleId) {
            return new ArrayList<>();
        }

        @Override
        public boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean tieneHorasTrabajadas(int detalleId) {
            return false;
        }

        @Override
        public Map<Integer, Integer> listarReservasParaActualizar(int detalleId) {
            return new HashMap<>();
        }

        @Override
        public void eliminar(int ordenId, int detalleId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public OrdenServicioDetalle buscarPorId(int id) {
            for (OrdenServicioDetalle linea : lineas) {
                if (linea.getId() == id) {
                    return linea;
                }
            }
            return null;
        }
    }

    private static class DetalleRepuestoDAOMock implements DetalleRepuestoDAO {
        private final List<DetalleRepuesto> reservas = new ArrayList<>();

        void agregar(DetalleRepuesto reserva) {
            reservas.add(reserva);
        }

        @Override
        public boolean guardar(DetalleRepuesto detalleRepuesto) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public DetalleRepuesto buscar(int detalleId, int repuestoId) {
            for (DetalleRepuesto reserva : reservas) {
                if (reserva.getDetalleId() == detalleId && reserva.getRepuestoId() == repuestoId) {
                    return reserva;
                }
            }
            return null;
        }

        @Override
        public List<DetalleRepuesto> listarPorDetalle(int detalleId) {
            List<DetalleRepuesto> resultado = new ArrayList<>();
            for (DetalleRepuesto reserva : reservas) {
                if (reserva.getDetalleId() == detalleId) {
                    resultado.add(reserva);
                }
            }
            return resultado;
        }

        @Override
        public boolean eliminar(int detalleId, int repuestoId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }
    }

    private static class VehiculoDAOMock implements VehiculoDAO {
        private final Map<Integer, Vehiculo> vehiculos = new HashMap<>();

        void agregar(Vehiculo vehiculo) {
            vehiculos.put(vehiculo.getId(), vehiculo);
        }

        @Override
        public int guardar(Vehiculo vehiculo) {
            return 0;
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return vehiculos.get(id);
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            return null;
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            return new ArrayList<>();
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return new ArrayList<>(vehiculos.values());
        }
    }

    private static class ClienteDAOMock implements ClienteDAO {
        private final Map<Integer, Cliente> clientes = new HashMap<>();

        void agregar(Cliente cliente) {
            clientes.put(cliente.getId(), cliente);
        }

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(clientes.values());
        }

        @Override
        public List<Cliente> listarActivos() {
            return new ArrayList<>();
        }

        @Override
        public Cliente buscarPorId(int id) {
            return clientes.get(id);
        }

        @Override
        public Cliente buscarPorDocumento(String documento) {
            return null;
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            return null;
        }

        @Override
        public int guardar(Cliente cliente) {
            return 0;
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            return false;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            return false;
        }
    }
}
