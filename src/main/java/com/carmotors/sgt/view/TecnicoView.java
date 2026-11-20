package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.TecnicoController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.util.ConsoleReader;
import com.carmotors.sgt.util.ConsoleTableFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Submenú de consola para la gestión de técnicos y especialidades (RF-33, RN-17, RNF-03, RNF-24).
 */
public class TecnicoView implements SubmenuVista {

    private final TecnicoController tecnicoController;

    public TecnicoView(TecnicoController tecnicoController) {
        if (tecnicoController == null) {
            throw new IllegalArgumentException("El controlador de técnicos es obligatorio.");
        }
        this.tecnicoController = tecnicoController;
    }

    @Override
    public String obtenerTitulo() {
        return "Técnicos y Especialidades";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("         GESTIÓN DE TÉCNICOS Y ESPECIALIDADES     ");
            System.out.println("==================================================");
            System.out.println("  1. Listar técnicos activos");
            System.out.println("  2. Listar todos los técnicos (incluye inactivos)");
            System.out.println("  3. Buscar técnico por ID");
            System.out.println("  4. Buscar técnico por documento");
            System.out.println("  5. Registrar técnico");
            System.out.println("  6. Actualizar técnico");
            System.out.println("  7. Desactivar técnico");
            System.out.println("  8. Reactivar técnico");
            System.out.println("  9. Consultar carga de trabajo de un técnico");
            System.out.println("  0. Volver al menú principal");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 9);
            switch (opcion) {
                case 1:
                    listarTecnicos(true);
                    break;
                case 2:
                    listarTecnicos(false);
                    break;
                case 3:
                    buscarPorId();
                    break;
                case 4:
                    buscarPorDocumento();
                    break;
                case 5:
                    registrarTecnico();
                    break;
                case 6:
                    actualizarTecnico();
                    break;
                case 7:
                    desactivarTecnico();
                    break;
                case 8:
                    reactivarTecnico();
                    break;
                case 9:
                    consultarCargaTrabajo();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void listarTecnicos(boolean soloActivos) {
        try {
            List<Tecnico> tecnicos = soloActivos
                    ? tecnicoController.listarActivos()
                    : tecnicoController.listarTodos();
            if (tecnicos.isEmpty()) {
                System.out.println("\n  [i] No hay técnicos registrados para el filtro seleccionado.");
            } else {
                System.out.println(soloActivos ? "\n--- Técnicos Activos ---" : "\n--- Todos los Técnicos ---");
                List<String[]> filas = new ArrayList<>();
                for (Tecnico tecnico : tecnicos) {
                    String tel = (tecnico.getTelefono() == null || tecnico.getTelefono().isEmpty())
                            ? "Sin teléfono"
                            : tecnico.getTelefono();
                    String especialidad = tecnicoController.obtenerNombreEspecialidad(tecnico.getEspecialidadId());
                    filas.add(new String[]{
                            String.valueOf(tecnico.getId()),
                            tecnico.getNombre(),
                            tecnico.getDocumentoIdentidad(),
                            especialidad,
                            tel,
                            tecnico.isActivo() ? "Activo" : "Inactivo"
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"ID", "Nombre", "Documento", "Especialidad", "Teléfono", "Estado"},
                        filas,
                        new boolean[]{true, false, false, false, false, false});
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarPorId() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del técnico");
            Tecnico tecnico = tecnicoController.buscarPorId(id);
            System.out.println("\n--- Técnico Encontrado ---");
            imprimirDetalleTecnico(tecnico);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarPorDocumento() {
        try {
            String documento = ConsoleReader.leerTextoObligatorio("Documento de identidad");
            Tecnico tecnico = tecnicoController.buscarPorDocumento(documento);
            System.out.println("\n--- Técnico Encontrado ---");
            imprimirDetalleTecnico(tecnico);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void registrarTecnico() {
        try {
            List<Especialidad> especialidades = tecnicoController.listarEspecialidades();
            if (especialidades.isEmpty()) {
                System.out.println("\n  [!] No hay especialidades registradas en el catálogo.");
                ConsoleReader.pausar();
                return;
            }

            Tecnico tecnico = new Tecnico();
            tecnico.setNombre(ConsoleReader.leerNombrePersona("Nombre completo del técnico"));
            tecnico.setDocumentoIdentidad(ConsoleReader.leerDocumentoNumerico("Documento de identidad"));
            int especialidadId = seleccionarEspecialidad(especialidades);
            if (especialidadId == 0) {
                System.out.println("\n  [i] Registro de técnico cancelado.");
                ConsoleReader.pausar();
                return;
            }
            tecnico.setEspecialidadId(especialidadId);
            String telefono = ConsoleReader.leerTelefonoOpcional("Teléfono (opcional)");
            tecnico.setTelefono(telefono.isEmpty() ? null : telefono);

            Tecnico creado = tecnicoController.registrar(tecnico);
            System.out.println("\n  [✓] Técnico registrado exitosamente con ID " + creado.getId() + ".");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al registrar técnico: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarTecnico() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del técnico a actualizar");
            Tecnico actual = tecnicoController.buscarPorId(id);
            System.out.println("\n  Registro actual:");
            imprimirDetalleTecnico(actual);

            List<Especialidad> especialidades = tecnicoController.listarEspecialidades();
            if (especialidades.isEmpty()) {
                System.out.println("\n  [!] No hay especialidades registradas en el catálogo.");
                ConsoleReader.pausar();
                return;
            }

            Tecnico actualizado = new Tecnico();
            actualizado.setId(actual.getId());
            actualizado.setNombre(ConsoleReader.leerNombrePersona("Nuevo nombre completo"));
            actualizado.setDocumentoIdentidad(ConsoleReader.leerDocumentoNumerico("Nuevo documento de identidad"));
            int especialidadId = seleccionarEspecialidad(especialidades);
            if (especialidadId == 0) {
                System.out.println("\n  [i] Actualización de técnico cancelada.");
                ConsoleReader.pausar();
                return;
            }
            actualizado.setEspecialidadId(especialidadId);
            String telefono = ConsoleReader.leerTelefonoOpcional("Nuevo teléfono (Enter para conservar actual)");
            actualizado.setTelefono(telefono.isEmpty() ? actual.getTelefono() : telefono);
            actualizado.setActivo(actual.isActivo());

            tecnicoController.actualizar(actualizado);
            System.out.println("\n  [✓] Técnico actualizado correctamente.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al actualizar técnico: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void desactivarTecnico() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del técnico a desactivar");
            Tecnico actual = tecnicoController.buscarPorId(id);
            imprimirDetalleTecnico(actual);

            if (!actual.isActivo()) {
                System.out.println("\n  [i] El técnico ya se encuentra inactivo.");
                ConsoleReader.pausar();
                return;
            }

            if (!ConsoleReader.leerConfirmacion("¿Confirma desactivar al técnico '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }

            tecnicoController.desactivar(id);
            System.out.println("\n  [✓] Técnico desactivado lógicamente (su historial permanece intacto).");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al desactivar técnico: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void reactivarTecnico() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del técnico a reactivar");
            Tecnico actual = tecnicoController.buscarPorId(id);
            imprimirDetalleTecnico(actual);

            if (actual.isActivo()) {
                System.out.println("\n  [i] El técnico ya se encuentra activo.");
                ConsoleReader.pausar();
                return;
            }

            if (!ConsoleReader.leerConfirmacion("¿Confirma reactivar al técnico '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }

            tecnicoController.reactivar(id);
            System.out.println("\n  [✓] Técnico reactivado correctamente.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al reactivar técnico: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void consultarCargaTrabajo() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del técnico");
            Tecnico tecnico = tecnicoController.buscarPorId(id);
            List<CargaLineaTecnico> lineas = tecnicoController.consultarCargaTrabajo(id);

            System.out.println("\n--- Carga de Trabajo del Técnico ---");
            imprimirDetalleTecnico(tecnico);
            System.out.println("  Total de líneas activas (Pendiente / En Proceso): " + lineas.size());

            if (lineas.isEmpty()) {
                System.out.println("\n  [i] El técnico no tiene líneas de trabajo activas asignadas.");
            } else {
                List<String[]> filas = new ArrayList<>();
                for (CargaLineaTecnico linea : lineas) {
                    filas.add(new String[]{
                            String.valueOf(linea.getOrdenId()),
                            linea.getPlaca(),
                            linea.getTipoServicio(),
                            linea.getEstadoNombre(),
                            String.valueOf(linea.getHorasTrabajadas())
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"Orden", "Placa", "Servicio", "Estado", "Horas"},
                        filas,
                        new boolean[]{true, false, false, false, true},
                        36);
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al consultar la carga de trabajo: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private int seleccionarEspecialidad(List<Especialidad> especialidades) {
        System.out.println("\n  Especialidades disponibles:");
        for (Especialidad esp : especialidades) {
            System.out.println("    " + esp.getId() + ". " + esp.getNombre());
        }
        while (true) {
            int idElegido = ConsoleReader.leerEnteroEnRango(
                    "Seleccione el ID de la especialidad (0 para cancelar)", 0, Integer.MAX_VALUE);
            if (idElegido == 0) {
                return 0;
            }
            for (Especialidad esp : especialidades) {
                if (esp.getId() == idElegido) {
                    return idElegido;
                }
            }
            System.out.println("  [!] Error: El ID de especialidad ingresado no pertenece al catálogo.");
        }
    }

    private void imprimirDetalleTecnico(Tecnico tecnico) {
        String nombreEsp = tecnicoController.obtenerNombreEspecialidad(tecnico.getEspecialidadId());
        String estado = tecnico.isActivo() ? "Activo" : "Inactivo";
        String tel = (tecnico.getTelefono() == null || tecnico.getTelefono().isEmpty())
                ? "Sin teléfono"
                : tecnico.getTelefono();
        System.out.println("  - ID " + tecnico.getId()
                + " | " + tecnico.getNombre()
                + " | Doc: " + tecnico.getDocumentoIdentidad()
                + " | Especialidad: " + nombreEsp + " (" + tecnico.getEspecialidadId() + ")"
                + " | Tel: " + tel
                + " | Estado: " + estado);
    }
}
