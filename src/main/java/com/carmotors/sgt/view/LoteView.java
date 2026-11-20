package com.carmotors.sgt.view;

import java.util.ArrayList;
import java.util.List;

import com.carmotors.sgt.controller.LoteController;
import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.controller.RepuestoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.LotePorVencer;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.util.ConsoleReader;
import com.carmotors.sgt.util.ConsoleTableFormatter;
import com.carmotors.sgt.util.InputValidator;

public class LoteView implements SubmenuVista {

    private final LoteController loteController;
    private final ProveedorController proveedorController;
    private final RepuestoController repuestoController;

    public LoteView(LoteController loteController, ProveedorController proveedorController,
                    RepuestoController repuestoController) {
        if (loteController == null) {
            throw new IllegalArgumentException("El controlador de lotes es obligatorio.");
        }
        if (proveedorController == null) {
            throw new IllegalArgumentException("El controlador de proveedores es obligatorio.");
        }
        if (repuestoController == null) {
            throw new IllegalArgumentException("El controlador de repuestos es obligatorio.");
        }
        this.loteController = loteController;
        this.proveedorController = proveedorController;
        this.repuestoController = repuestoController;
    }

    @Override
    public String obtenerTitulo() {
        return "Gestión de lotes";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("                  GESTIÓN DE LOTES                ");
            System.out.println("==================================================");
            System.out.println("  1. Registrar lote");
            System.out.println("  2. Consultar lote por código");
            System.out.println("  3. Listar lotes por repuesto");
            System.out.println("  4. Reporte de lotes por vencer");
            System.out.println("  0. Volver al menú anterior");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 4);
            switch (opcion) {
                case 1:
                    registrarLote();
                    break;
                case 2:
                    consultarLote();
                    break;
                case 3:
                    listarPorRepuesto();
                    break;
                case 4:
                    reporteLotesPorVencer();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void registrarLote() {
        try {
            int proveedorId = ConsoleReader.leerEnteroPositivo("Ingrese el ID del proveedor");
            Proveedor proveedor = proveedorController.buscarPorId(proveedorId);
            if (!proveedor.isActivo()) {
                System.out.println("\n  [!] Error: El proveedor está inactivo y no puede registrar lotes.");
                ConsoleReader.pausar();
                return;
            }

            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el ID del repuesto");
            Repuesto repuesto = repuestoController.buscarPorId(repuestoId);
            if (!repuesto.isActivo()) {
                System.out.println("\n  [!] Error: El repuesto está inactivo y no puede recibir lotes nuevos.");
                ConsoleReader.pausar();
                return;
            }

            Lote lote = new Lote();
            lote.setCodigoLote(ConsoleReader.leerTextoObligatorio("Código del lote"));
            lote.setProveedorId(proveedorId);
            lote.setRepuestoId(repuestoId);
            lote.setFechaIngreso(ConsoleReader.leerFecha("Fecha de ingreso (yyyy-MM-dd)"));
            lote.setFechaCaducidad(ConsoleReader.leerFechaOpcional("Fecha de caducidad (opcional, yyyy-MM-dd)"));
            lote.setCantidadInicial(ConsoleReader.leerEnteroPositivo("Cantidad inicial del lote"));

            Lote guardado = loteController.registrar(lote);
            System.out.println("\n  [✓] Lote registrado correctamente con ID " + guardado.getId());
            System.out.println("  Código: " + guardado.getCodigoLote()
                    + " | Proveedor: " + proveedorId
                    + " | Repuesto: " + repuestoId
                    + " | Cantidad: " + guardado.getCantidadActual());
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void consultarLote() {
        try {
            String codigo = ConsoleReader.leerTextoObligatorio("Ingrese el código del lote");
            Lote lote = loteController.buscarPorCodigo(codigo);
            if (lote == null) {
                System.out.println("\n  [i] No existe un lote con ese código.");
            } else {
                Proveedor proveedor = proveedorController.buscarPorId(lote.getProveedorId());
                Repuesto repuesto = repuestoController.buscarPorId(lote.getRepuestoId());
                System.out.println("\n  [Lote encontrado]");
                System.out.println("  - ID: " + lote.getId());
                System.out.println("  - Código: " + lote.getCodigoLote());
                System.out.println("  - Proveedor: " + proveedor.getId() + " - " + proveedor.getNombre());
                System.out.println("  - Repuesto: " + repuesto.getId() + " - " + repuesto.getNombre());
                System.out.println("  - Fecha ingreso: " + lote.getFechaIngreso());
                System.out.println("  - Fecha caducidad: " + (lote.getFechaCaducidad() == null ? "Sin caducidad" : lote.getFechaCaducidad()));
                System.out.println("  - Cantidad inicial: " + lote.getCantidadInicial());
                System.out.println("  - Cantidad actual: " + lote.getCantidadActual());
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void listarPorRepuesto() {
        try {
            int repuestoId = ConsoleReader.leerEnteroPositivo("Ingrese el ID del repuesto");
            Repuesto repuesto = repuestoController.buscarPorId(repuestoId);
            List<Lote> lotes = loteController.listarPorRepuesto(repuestoId);

            if (lotes.isEmpty()) {
                System.out.println("\n  [i] El repuesto '" + repuesto.getNombre() + "' no tiene lotes registrados.");
            } else {
                System.out.println("\n  [Lotes del repuesto: " + repuesto.getNombre() + "]");
                List<String[]> filas = new ArrayList<>();
                for (Lote lote : lotes) {
                    Proveedor proveedor = proveedorController.buscarPorId(lote.getProveedorId());
                    String caducidad = lote.getFechaCaducidad() == null ? "Sin caducidad" : lote.getFechaCaducidad().toString();
                    filas.add(new String[]{
                            lote.getCodigoLote(),
                            proveedor.getNombre(),
                            String.valueOf(lote.getFechaIngreso()),
                            caducidad,
                            String.valueOf(lote.getCantidadInicial()),
                            String.valueOf(lote.getCantidadActual())
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"Código Lote", "Proveedor", "Ingreso", "Caducidad", "Inicial", "Restante"},
                        filas,
                        new boolean[]{false, false, false, false, true, true});
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void reporteLotesPorVencer() {
        try {
            String entrada = ConsoleReader.leerTextoOpcional("Ingrese los días de horizonte para el reporte [Enter = 30 por defecto]");
            int dias = 30;
            if (!entrada.isEmpty()) {
                if (!InputValidator.esEntero(entrada) || !InputValidator.esEnteroPositivo(InputValidator.convertirAEntero(entrada))) {
                    System.out.println("\n  [!] Error: Debe ingresar un número entero positivo mayor que cero.");
                    ConsoleReader.pausar();
                    return;
                }
                dias = InputValidator.convertirAEntero(entrada);
            }

            List<LotePorVencer> reporte = loteController.generarReporteLotesPorVencer(dias);
            if (reporte.isEmpty()) {
                System.out.println("\n  [i] No hay lotes vencidos ni próximos a vencer en los próximos " + dias + " días.");
            } else {
                System.out.println("\n  [Reporte de lotes por vencer / vencidos (horizonte: " + dias + " días a partir de hoy)]");
                List<String[]> filas = new ArrayList<>();
                for (LotePorVencer item : reporte) {
                    String diasTexto = item.getDiasRestantes() < 0
                            ? "Vencido (" + Math.abs(item.getDiasRestantes()) + "d)"
                            : (item.getDiasRestantes() == 0 ? "Vence hoy" : item.getDiasRestantes() + "d");
                    String caducidad = item.getLote().getFechaCaducidad() == null
                            ? "Sin caducidad"
                            : item.getLote().getFechaCaducidad().toString();
                    filas.add(new String[]{
                            item.getLote().getCodigoLote(),
                            item.getEstado(),
                            item.getNombreRepuesto(),
                            item.getNombreProveedor(),
                            caducidad,
                            String.valueOf(item.getLote().getCantidadActual()),
                            diasTexto
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"Código Lote", "Estado", "Repuesto", "Proveedor", "Caducidad", "Saldo", "Días"},
                        filas,
                        new boolean[]{false, false, false, false, false, true, false},
                        28);
                System.out.println("  Total de lotes listados: " + reporte.size());
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }
}
