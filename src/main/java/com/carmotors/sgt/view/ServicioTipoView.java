package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.ServicioTipoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.MantenimientoTipo;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.util.ConsoleReader;
import com.carmotors.sgt.util.ConsoleTableFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Submenú de consola para el catálogo de tipos de servicio (RNF-03, RNF-24).
 */
public class ServicioTipoView implements SubmenuVista {

    private final ServicioTipoController servicioTipoController;

    public ServicioTipoView(ServicioTipoController servicioTipoController) {
        if (servicioTipoController == null) {
            throw new IllegalArgumentException("El controlador de tipos de servicio es obligatorio.");
        }
        this.servicioTipoController = servicioTipoController;
    }

    @Override
    public String obtenerTitulo() {
        return "Catálogo de Tipos de Servicio";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("          CATÁLOGO DE TIPOS DE SERVICIO           ");
            System.out.println("==================================================");
            System.out.println("  1. Listar tipos de servicio");
            System.out.println("  2. Buscar tipo de servicio por ID");
            System.out.println("  3. Registrar tipo de servicio");
            System.out.println("  4. Actualizar tipo de servicio");
            System.out.println("  5. Eliminar tipo de servicio");
            System.out.println("  0. Volver al menú anterior");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 5);
            switch (opcion) {
                case 1:
                    listarServicios();
                    break;
                case 2:
                    buscarPorId();
                    break;
                case 3:
                    registrarServicioTipo();
                    break;
                case 4:
                    actualizarServicioTipo();
                    break;
                case 5:
                    eliminarServicioTipo();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void listarServicios() {
        try {
            List<ServicioTipo> lista = servicioTipoController.listarTodos();
            if (lista.isEmpty()) {
                System.out.println("\n  [i] No hay tipos de servicio registrados.");
            } else {
                System.out.println("\n--- Catálogo de Tipos de Servicio ---");
                List<String[]> filas = new ArrayList<>();
                for (ServicioTipo servicioTipo : lista) {
                    String intervalo = servicioTipo.getIntervaloMeses() == null
                            ? "N/A"
                            : servicioTipo.getIntervaloMeses() + " meses";
                    filas.add(new String[]{
                            String.valueOf(servicioTipo.getId()),
                            servicioTipo.getNombre(),
                            servicioTipoController.obtenerNombreMantenimientoTipo(servicioTipo.getMantenimientoTipoId()),
                            servicioTipoController.obtenerNombreEspecialidad(servicioTipo.getEspecialidadId()),
                            "$" + servicioTipo.getPrecioManoObraBase(),
                            intervalo
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"ID", "Nombre", "Tipo", "Especialidad", "Mano obra base", "Intervalo"},
                        filas,
                        new boolean[]{true, false, false, false, true, false});
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarPorId() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del tipo de servicio");
            ServicioTipo encontrado = servicioTipoController.buscarPorId(id);
            System.out.println("\n--- Tipo de Servicio Encontrado ---");
            imprimirDetalle(encontrado);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void registrarServicioTipo() {
        try {
            List<MantenimientoTipo> mantenimientos = servicioTipoController.listarTiposMantenimiento();
            List<Especialidad> especialidades = servicioTipoController.listarEspecialidades();
            if (mantenimientos.isEmpty() || especialidades.isEmpty()) {
                System.out.println("\n  [!] Faltan catálogos de mantenimiento o especialidades en la base de datos.");
                ConsoleReader.pausar();
                return;
            }

            ServicioTipo nuevo = capturarDatosServicio(0, mantenimientos, especialidades);
            if (nuevo == null) {
                System.out.println("\n  [i] Registro de tipo de servicio cancelado.");
                ConsoleReader.pausar();
                return;
            }
            ServicioTipo creado = servicioTipoController.registrar(nuevo);
            System.out.println("\n  [✓] Tipo de servicio registrado exitosamente con ID " + creado.getId() + ".");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al registrar tipo de servicio: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarServicioTipo() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del tipo de servicio a actualizar");
            ServicioTipo actual = servicioTipoController.buscarPorId(id);
            System.out.println("\n  Registro actual:");
            imprimirDetalle(actual);

            List<MantenimientoTipo> mantenimientos = servicioTipoController.listarTiposMantenimiento();
            List<Especialidad> especialidades = servicioTipoController.listarEspecialidades();
            if (mantenimientos.isEmpty() || especialidades.isEmpty()) {
                System.out.println("\n  [!] Faltan catálogos de mantenimiento o especialidades en la base de datos.");
                ConsoleReader.pausar();
                return;
            }

            ServicioTipo actualizado = capturarDatosServicio(actual.getId(), mantenimientos, especialidades);
            if (actualizado == null) {
                System.out.println("\n  [i] Actualización de tipo de servicio cancelada.");
                ConsoleReader.pausar();
                return;
            }
            servicioTipoController.actualizar(actualizado);
            System.out.println("\n  [✓] Tipo de servicio actualizado correctamente.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al actualizar tipo de servicio: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void eliminarServicioTipo() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del tipo de servicio a eliminar");
            ServicioTipo actual = servicioTipoController.buscarPorId(id);
            imprimirDetalle(actual);

            if (!ConsoleReader.leerConfirmacion("¿Confirma eliminar el tipo de servicio '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }

            servicioTipoController.eliminar(id);
            System.out.println("\n  [✓] Tipo de servicio eliminado correctamente.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al eliminar tipo de servicio: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private ServicioTipo capturarDatosServicio(
            int idExistente,
            List<MantenimientoTipo> mantenimientos,
            List<Especialidad> especialidades) {
        String nombre = ConsoleReader.leerTextoObligatorio("Nombre del tipo de servicio");
        MantenimientoTipo mantenimiento = seleccionarMantenimiento(mantenimientos);
        if (mantenimiento == null) {
            return null;
        }
        int especialidadId = seleccionarEspecialidad(especialidades);
        if (especialidadId == 0) {
            return null;
        }
        BigDecimal precioBase = ConsoleReader.leerBigDecimalNoNegativo("Precio base de mano de obra");

        Integer intervaloMeses = null;
        if (mantenimiento.getNombre().trim().equalsIgnoreCase("Preventivo")) {
            intervaloMeses = ConsoleReader.leerEnteroPositivo("Intervalo preventivo en meses");
        } else {
            System.out.println("  [i] Al ser Correctivo, el intervalo en meses queda definido como N/A.");
        }

        return new ServicioTipo(
                idExistente,
                nombre,
                mantenimiento.getId(),
                especialidadId,
                precioBase,
                intervaloMeses
        );
    }

    private MantenimientoTipo seleccionarMantenimiento(List<MantenimientoTipo> mantenimientos) {
        System.out.println("\n  Tipos de mantenimiento disponibles:");
        for (MantenimientoTipo tipo : mantenimientos) {
            System.out.println("    " + tipo.getId() + ". " + tipo.getNombre());
        }
        while (true) {
            int idElegido = ConsoleReader.leerEnteroEnRango(
                    "Seleccione el ID del tipo de mantenimiento (0 para cancelar)", 0, Integer.MAX_VALUE);
            if (idElegido == 0) {
                return null;
            }
            for (MantenimientoTipo tipo : mantenimientos) {
                if (tipo.getId() == idElegido) {
                    return tipo;
                }
            }
            System.out.println("  [!] Error: El ID de tipo de mantenimiento no pertenece al catálogo.");
        }
    }

    private int seleccionarEspecialidad(List<Especialidad> especialidades) {
        System.out.println("\n  Especialidades requeridas disponibles:");
        for (Especialidad esp : especialidades) {
            System.out.println("    " + esp.getId() + ". " + esp.getNombre());
        }
        while (true) {
            int idElegido = ConsoleReader.leerEnteroEnRango(
                    "Seleccione el ID de la especialidad requerida (0 para cancelar)", 0, Integer.MAX_VALUE);
            if (idElegido == 0) {
                return 0;
            }
            for (Especialidad esp : especialidades) {
                if (esp.getId() == idElegido) {
                    return idElegido;
                }
            }
            System.out.println("  [!] Error: El ID de especialidad no pertenece al catálogo.");
        }
    }

    private void imprimirDetalle(ServicioTipo servicio) {
        String nomMant = servicioTipoController.obtenerNombreMantenimientoTipo(servicio.getMantenimientoTipoId());
        String nomEsp = servicioTipoController.obtenerNombreEspecialidad(servicio.getEspecialidadId());
        String intervalo = servicio.getIntervaloMeses() == null ? "N/A" : servicio.getIntervaloMeses() + " meses";
        System.out.println("  - ID " + servicio.getId()
                + " | " + servicio.getNombre()
                + " | Tipo: " + nomMant
                + " | Especialidad: " + nomEsp
                + " | Mano obra base: $" + servicio.getPrecioManoObraBase()
                + " | Intervalo: " + intervalo);
    }
}
