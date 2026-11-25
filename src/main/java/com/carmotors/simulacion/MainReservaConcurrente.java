package com.carmotors.simulacion;

import com.carmotors.sgt.dao.impl.ClienteDAOImpl;
import com.carmotors.sgt.dao.impl.EspecialidadDAOImpl;
import com.carmotors.sgt.dao.impl.ServicioTipoDAOImpl;
import com.carmotors.sgt.dao.impl.TecnicoDAOImpl;
import com.carmotors.sgt.dao.impl.VehiculoDAOImpl;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.service.InventarioService;
import com.carmotors.sgt.service.OrdenServicioService;
import com.carmotors.sgt.service.TecnicoService;
import com.carmotors.sgt.util.ConsoleReader;
import com.carmotors.sgt.util.ConsoleTableFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class MainReservaConcurrente {

    private static final OrdenServicioService ordenService = new OrdenServicioService(
            new VehiculoDAOImpl(), new ClienteDAOImpl(), new ServicioTipoDAOImpl());
    private static final TecnicoService tecnicoService =
            new TecnicoService(new TecnicoDAOImpl(), new EspecialidadDAOImpl());
    private static final InventarioService inventarioService = new InventarioService();
    private static final CountDownLatch salida = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        int repuestoId = elegirRepuesto();
        int ordenId = elegirOrden();
        int stock = ordenService.consultarDisponible(repuestoId);
        System.out.println("Disponibilidad inicial: " + stock);

        List<Thread> hilos = new ArrayList<>();
        for (OrdenServicioDetalle linea : ordenService.listarLineas(ordenId)) {
            for (DetalleTecnico asignado : ordenService.listarTecnicosDeLinea(ordenId, linea.getId())) {
                String tecnico = tecnicoService.buscarPorId(asignado.getTecnicoId()).getNombre();
                Thread hilo = new Thread(() -> reservar(tecnico, linea.getId(), repuestoId, stock));
                hilos.add(hilo);
                hilo.start();
            }
        }
        salida.countDown();
        for (Thread hilo : hilos) {
            hilo.join();
        }

        System.out.println("Disponibilidad final: " + ordenService.consultarDisponible(repuestoId));
    }

    private static int elegirRepuesto() {
        List<String[]> filas = inventarioService.calcularStockActivos().stream()
                .filter(stock -> stock.getDisponible() > 0)
                .map(stock -> new String[]{String.valueOf(stock.getRepuesto().getId()),
                        stock.getRepuesto().getNombre(), String.valueOf(stock.getDisponible())})
                .toList();
        ConsoleTableFormatter.imprimirTabla(new String[]{"ID", "Repuesto", "Disponible"}, filas);
        return ConsoleReader.leerEnteroPositivo("ID del repuesto a reservar: ");
    }

    private static int elegirOrden() {
        List<String[]> filas = Stream.of(1, 2)
                .flatMap(estadoId -> ordenService.consultarOrdenes(
                        new FiltroOrdenServicio(estadoId, null, null, null, null, null)).stream())
                .map(orden -> new String[]{String.valueOf(orden.getId()), orden.getPlaca(),
                        ordenService.obtenerNombreEstado(orden.getEstadoId()),
                        String.join(", ", orden.getTecnicos().values())})
                .toList();
        ConsoleTableFormatter.imprimirTabla(new String[]{"ID", "Placa", "Estado", "Técnicos"}, filas);
        return ConsoleReader.leerEnteroPositivo("ID de la orden: ");
    }

    private static void reservar(String tecnico, int lineaId, int repuestoId, int stock) {
        try {
            salida.await();
            ordenService.reservarRepuesto(lineaId, repuestoId, 1);
            if (stock != 1) {
                System.out.println(tecnico + " reservó una unidad.");
            } else {
                System.out.println(tecnico + " reservó la última unidad.");
            }
        } catch (Exception e) {
            System.out.println("Reserva rechazada a " + tecnico + ": " + e.getMessage());
        }
    }
}
