package com.carmotors.sgt.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.carmotors.sgt.controller.EvaluacionProveedorController;
import com.carmotors.sgt.controller.ProveedorController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.RankingProveedor;
import com.carmotors.sgt.util.ConsoleReader;
import com.carmotors.sgt.util.ConsoleTableFormatter;

public class EvaluacionProveedorView implements SubmenuVista {

    private final EvaluacionProveedorController evaluacionProveedorController;
    private final ProveedorController proveedorController;

    public EvaluacionProveedorView(EvaluacionProveedorController evaluacionProveedorController,
                                  ProveedorController proveedorController) {
        if (evaluacionProveedorController == null) {
            throw new IllegalArgumentException("El controlador de evaluaciones es obligatorio.");
        }
        if (proveedorController == null) {
            throw new IllegalArgumentException("El controlador de proveedores es obligatorio.");
        }
        this.evaluacionProveedorController = evaluacionProveedorController;
        this.proveedorController = proveedorController;
    }

    @Override
    public String obtenerTitulo() {
        return "Evaluaciones de proveedores";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("          EVALUACIONES DE PROVEEDORES             ");
            System.out.println("==================================================");
            System.out.println("  1. Registrar evaluación");
            System.out.println("  2. Ver historial por proveedor");
            System.out.println("  3. Ver promedio de proveedor");
            System.out.println("  4. Ranking de proveedores");
            System.out.println("  0. Volver al menú anterior");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 4);
            switch (opcion) {
                case 1:
                    registrarEvaluacion();
                    break;
                case 2:
                    verHistorial();
                    break;
                case 3:
                    verPromedio();
                    break;
                case 4:
                    verRanking();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void registrarEvaluacion() {
        try {
            int proveedorId = ConsoleReader.leerEnteroPositivo("Ingrese el ID del proveedor");
            Proveedor proveedor = proveedorController.buscarPorId(proveedorId);
            if (!proveedor.isActivo()) {
                System.out.println("\n  [!] Error: El proveedor está inactivo y no puede ser evaluado.");
                ConsoleReader.pausar();
                return;
            }

            EvaluacionProveedor evaluacion = new EvaluacionProveedor();
            evaluacion.setProveedorId(proveedorId);
            evaluacion.setCalidad(ConsoleReader.leerEnteroEnRango("Calidad del proveedor (1-5)", 1, 5));
            evaluacion.setPuntualidad(ConsoleReader.leerEnteroEnRango("Puntualidad del proveedor (1-5)", 1, 5));
            evaluacion.setComentario(ConsoleReader.leerTextoOpcional("Comentario descriptivo (opcional)"));
            evaluacion.setFecha(LocalDate.now());

            EvaluacionProveedor creada = evaluacionProveedorController.registrar(evaluacion);
            System.out.println("\n  [✓] Evaluación registrada correctamente con ID " + creada.getId());
            System.out.println("  Puntaje de evaluación: " + creada.getPuntaje() + "/5.0");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void verHistorial() {
        try {
            int proveedorId = ConsoleReader.leerEnteroPositivo("Ingrese el ID del proveedor");
            Proveedor proveedor = proveedorController.buscarPorId(proveedorId);
            List<EvaluacionProveedor> evaluaciones = evaluacionProveedorController.listarPorProveedor(proveedorId);
            if (evaluaciones.isEmpty()) {
                System.out.println("\n  [i] El proveedor '" + proveedor.getNombre() + "' aún no tiene evaluaciones.");
            } else {
                System.out.println("\n  [Historial de evaluaciones para " + proveedor.getNombre() + "]");
                List<String[]> filas = new ArrayList<>();
                for (EvaluacionProveedor evaluacion : evaluaciones) {
                    String comentario = (evaluacion.getComentario() == null || evaluacion.getComentario().isEmpty())
                            ? "Sin comentario"
                            : evaluacion.getComentario();
                    filas.add(new String[]{
                            evaluacion.getFecha() != null ? evaluacion.getFecha().toString() : "Sin fecha",
                            String.valueOf(evaluacion.getCalidad()),
                            String.valueOf(evaluacion.getPuntualidad()),
                            String.valueOf(evaluacion.getPuntaje()),
                            comentario
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"Fecha", "Calidad", "Puntualidad", "Puntaje", "Comentario"},
                        filas,
                        new boolean[]{false, true, true, true, false});
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void verPromedio() {
        try {
            int proveedorId = ConsoleReader.leerEnteroPositivo("Ingrese el ID del proveedor");
            Proveedor proveedor = proveedorController.buscarPorId(proveedorId);
            BigDecimal promedio = evaluacionProveedorController.obtenerPromedioProveedor(proveedorId);
            System.out.println("\n  [Promedio de evaluaciones] " + proveedor.getNombre());
            System.out.println("  Puntaje promedio: " + promedio + " / 5.0");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void verRanking() {
        try {
            List<RankingProveedor> ranking = evaluacionProveedorController.generarRanking();
            if (ranking.isEmpty()) {
                System.out.println("\n  [i] No hay proveedores registrados para generar el ranking.");
            } else {
                System.out.println("\n  [Ranking de proveedores]");
                List<String[]> filas = new ArrayList<>();
                int posicion = 1;
                for (RankingProveedor fila : ranking) {
                    if (fila.getPromedio() == null) {
                        filas.add(new String[]{
                                "-",
                                fila.getProveedor().getNombre(),
                                "Sin evaluar",
                                String.valueOf(fila.getCantidadEvaluaciones())
                        });
                    } else {
                        filas.add(new String[]{
                                String.valueOf(posicion),
                                fila.getProveedor().getNombre(),
                                fila.getPromedio().toString(),
                                String.valueOf(fila.getCantidadEvaluaciones())
                        });
                        posicion++;
                    }
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"#", "Proveedor", "Promedio", "Evaluaciones"},
                        filas,
                        new boolean[]{true, false, true, true});
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }
}
