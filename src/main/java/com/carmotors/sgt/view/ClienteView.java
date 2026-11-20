package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.ClienteController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.HistorialCliente;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.ResumenOrdenHistorial;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.util.ConsoleReader;
import com.carmotors.sgt.util.ConsoleTableFormatter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Submenú de consola para la gestión de clientes y vehículos (RF-17, RF-18, RF-19, RF-20, RN-13, RN-17,
 * RNF-03, RNF-24).
 */
public class ClienteView implements SubmenuVista {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClienteController clienteController;

    public ClienteView(ClienteController clienteController) {
        if (clienteController == null) {
            throw new IllegalArgumentException("El controlador de clientes es obligatorio.");
        }
        this.clienteController = clienteController;
    }

    @Override
    public String obtenerTitulo() {
        return "Clientes y Vehículos";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("         GESTIÓN DE CLIENTES Y VEHÍCULOS          ");
            System.out.println("==================================================");
            System.out.println("  -- Clientes --");
            System.out.println("  1. Listar clientes activos");
            System.out.println("  2. Listar todos los clientes (incluye inactivos)");
            System.out.println("  3. Buscar cliente por ID");
            System.out.println("  4. Buscar cliente por documento");
            System.out.println("  5. Registrar cliente");
            System.out.println("  6. Actualizar cliente");
            System.out.println("  7. Desactivar cliente");
            System.out.println("  8. Reactivar cliente");
            System.out.println("  -- Vehículos e Historial --");
            System.out.println("  9. Registrar vehículo a un cliente");
            System.out.println(" 10. Listar vehículos de un cliente");
            System.out.println(" 11. Buscar vehículo por placa");
            System.out.println(" 12. Consultar historial del cliente");
            System.out.println("  0. Volver al menú principal");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 12);
            switch (opcion) {
                case 1:
                    listarClientes(true);
                    break;
                case 2:
                    listarClientes(false);
                    break;
                case 3:
                    buscarPorId();
                    break;
                case 4:
                    buscarPorDocumento();
                    break;
                case 5:
                    registrarCliente();
                    break;
                case 6:
                    actualizarCliente();
                    break;
                case 7:
                    desactivarCliente();
                    break;
                case 8:
                    reactivarCliente();
                    break;
                case 9:
                    registrarVehiculo();
                    break;
                case 10:
                    listarVehiculosPorCliente();
                    break;
                case 11:
                    buscarVehiculoPorPlaca();
                    break;
                case 12:
                    consultarHistorialCliente();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void listarClientes(boolean soloActivos) {
        try {
            List<Cliente> clientes = soloActivos
                    ? clienteController.listarActivos()
                    : clienteController.listarTodos();
            if (clientes.isEmpty()) {
                System.out.println("\n  [i] No hay clientes registrados para el filtro seleccionado.");
            } else {
                System.out.println(soloActivos ? "\n--- Clientes Activos ---" : "\n--- Todos los Clientes ---");
                List<String[]> filas = new ArrayList<>();
                for (Cliente cliente : clientes) {
                    String tel = (cliente.getTelefono() == null || cliente.getTelefono().isEmpty())
                            ? "Sin teléfono"
                            : cliente.getTelefono();
                    String email = (cliente.getEmail() == null || cliente.getEmail().isEmpty())
                            ? "Sin email"
                            : cliente.getEmail();
                    filas.add(new String[]{
                            String.valueOf(cliente.getId()),
                            cliente.getNombre(),
                            cliente.getDocumentoIdentidad(),
                            tel,
                            email,
                            cliente.isActivo() ? "Activo" : "Inactivo"
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"ID", "Nombre", "Documento", "Teléfono", "Email", "Estado"},
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
            int id = ConsoleReader.leerEnteroPositivo("ID del cliente");
            Cliente cliente = clienteController.buscarPorId(id);
            System.out.println("\n--- Cliente Encontrado ---");
            imprimirDetalleCliente(cliente);
        } catch (EntidadNoEncontradaException e) {
            System.out.println("\n  [i] " + e.getMessage());
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarPorDocumento() {
        try {
            String documento = ConsoleReader.leerTextoObligatorio("Documento de identidad");
            Cliente cliente = clienteController.buscarPorDocumento(documento);
            System.out.println("\n--- Cliente Encontrado ---");
            imprimirDetalleCliente(cliente);
        } catch (EntidadNoEncontradaException e) {
            System.out.println("\n  [i] " + e.getMessage());
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void registrarCliente() {
        try {
            Cliente cliente = new Cliente();
            cliente.setNombre(ConsoleReader.leerNombreCliente("Nombre completo o razón social del cliente"));
            cliente.setDocumentoIdentidad(ConsoleReader.leerDocumentoCliente("Documento de identidad o NIT"));
            String telefono = ConsoleReader.leerTelefonoOpcional("Teléfono (opcional)");
            cliente.setTelefono(telefono.isEmpty() ? null : telefono);
            cliente.setEmail(ConsoleReader.leerEmail("Email"));

            Cliente creado = clienteController.registrar(cliente);
            System.out.println("\n  [✓] Cliente registrado exitosamente con ID " + creado.getId() + ".");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al registrar cliente: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void actualizarCliente() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del cliente a actualizar");
            Cliente actual = clienteController.buscarPorId(id);
            System.out.println("\n  Registro actual:");
            imprimirDetalleCliente(actual);

            Cliente actualizado = new Cliente();
            actualizado.setId(actual.getId());
            actualizado.setNombre(ConsoleReader.leerNombreCliente("Nuevo nombre completo o razón social"));
            actualizado.setDocumentoIdentidad(ConsoleReader.leerDocumentoCliente("Nuevo documento de identidad o NIT"));
            String telefono = ConsoleReader.leerTelefonoOpcional("Nuevo teléfono (Enter para conservar actual)");
            actualizado.setTelefono(telefono.isEmpty() ? actual.getTelefono() : telefono);
            actualizado.setEmail(ConsoleReader.leerEmail("Nuevo email"));
            actualizado.setActivo(actual.isActivo());

            clienteController.actualizar(actualizado);
            System.out.println("\n  [✓] Cliente actualizado correctamente.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al actualizar cliente: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void desactivarCliente() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del cliente a desactivar");
            Cliente actual = clienteController.buscarPorId(id);
            imprimirDetalleCliente(actual);

            if (!actual.isActivo()) {
                System.out.println("\n  [i] El cliente ya se encuentra inactivo.");
                ConsoleReader.pausar();
                return;
            }

            if (!ConsoleReader.leerConfirmacion("¿Confirma desactivar al cliente '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }

            clienteController.desactivar(id);
            System.out.println("\n  [✓] Cliente desactivado lógicamente (su historial permanece intacto).");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al desactivar cliente: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void reactivarCliente() {
        try {
            int id = ConsoleReader.leerEnteroPositivo("ID del cliente a reactivar");
            Cliente actual = clienteController.buscarPorId(id);
            imprimirDetalleCliente(actual);

            if (actual.isActivo()) {
                System.out.println("\n  [i] El cliente ya se encuentra activo.");
                ConsoleReader.pausar();
                return;
            }

            if (!ConsoleReader.leerConfirmacion("¿Confirma reactivar al cliente '" + actual.getNombre() + "'?")) {
                System.out.println("\n  [i] Operación cancelada.");
                ConsoleReader.pausar();
                return;
            }

            clienteController.reactivar(id);
            System.out.println("\n  [✓] Cliente reactivado correctamente.");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al reactivar cliente: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void registrarVehiculo() {
        try {
            int clienteId = ConsoleReader.leerEnteroPositivo("ID del cliente propietario");
            Cliente cliente = clienteController.buscarPorId(clienteId);
            System.out.println("  Cliente seleccionado: " + cliente.getNombre()
                    + " (" + (cliente.isActivo() ? "Activo" : "Inactivo") + ")");

            if (!cliente.isActivo()) {
                System.out.println("\n  [!] Error: No se puede registrar un vehículo a un cliente inactivo (RN-17).");
                ConsoleReader.pausar();
                return;
            }

            String placa = ConsoleReader.leerPlaca("Placa del vehículo (5-10 caracteres)");
            String marca = ConsoleReader.leerTextoObligatorio("Marca");
            String modelo = ConsoleReader.leerTextoObligatorio("Modelo");

            Vehiculo nuevo = new Vehiculo(clienteId, placa, marca, modelo);
            Vehiculo creado = clienteController.registrarVehiculo(nuevo);
            System.out.println("\n  [✓] Vehículo registrado exitosamente con ID " + creado.getId()
                    + " y placa " + creado.getPlaca() + ".");
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error al registrar vehículo: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void listarVehiculosPorCliente() {
        try {
            int clienteId = ConsoleReader.leerEnteroPositivo("ID del cliente");
            Cliente cliente = clienteController.buscarPorId(clienteId);
            System.out.println("\n--- Vehículos del Cliente: " + cliente.getNombre()
                    + " (Doc: " + cliente.getDocumentoIdentidad() + ") ---");

            List<Vehiculo> vehiculos = clienteController.listarVehiculosPorCliente(clienteId);
            if (vehiculos.isEmpty()) {
                System.out.println("  [i] El cliente no tiene vehículos registrados.");
            } else {
                List<String[]> filas = new ArrayList<>();
                for (Vehiculo v : vehiculos) {
                    filas.add(new String[]{
                            String.valueOf(v.getId()),
                            v.getPlaca(),
                            v.getMarca(),
                            v.getModelo()
                    });
                }
                ConsoleTableFormatter.imprimirTabla(
                        new String[]{"ID", "Placa", "Marca", "Modelo"},
                        filas,
                        new boolean[]{true, false, false, false});
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void buscarVehiculoPorPlaca() {
        try {
            String placa = ConsoleReader.leerPlaca("Placa del vehículo");
            Vehiculo vehiculo = clienteController.buscarVehiculoPorPlaca(placa);
            System.out.println("\n--- Vehículo Encontrado ---");
            imprimirDetalleVehiculo(vehiculo);

            try {
                Cliente propietario = clienteController.buscarPorId(vehiculo.getClienteId());
                System.out.println("  Propietario asociado:");
                imprimirDetalleCliente(propietario);
            } catch (CarMotorsException e) {
                System.out.println("  [!] Propietario ID " + vehiculo.getClienteId()
                        + " (no disponible: " + e.getMessage() + ")");
            }
        } catch (EntidadNoEncontradaException e) {
            System.out.println("\n  [i] " + e.getMessage());
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] Error: " + e.getMessage());
        }
        ConsoleReader.pausar();
    }

    private void imprimirDetalleCliente(Cliente cliente) {
        String estado = cliente.isActivo() ? "Activo" : "Inactivo";
        String tel = (cliente.getTelefono() == null || cliente.getTelefono().isEmpty())
                ? "Sin teléfono"
                : cliente.getTelefono();
        System.out.println("  - ID " + cliente.getId()
                + " | " + cliente.getNombre()
                + " | Doc: " + cliente.getDocumentoIdentidad()
                + " | Tel: " + tel
                + " | Email: " + cliente.getEmail()
                + " | Estado: " + estado);
    }

    private void consultarHistorialCliente() {
        try {
            int modo = ConsoleReader.leerEnteroEnRango("Buscar cliente por: 1) ID  2) Documento", 1, 2);
            HistorialCliente historial;
            if (modo == 1) {
                int clienteId = ConsoleReader.leerEnteroPositivo("ID del cliente");
                historial = clienteController.consultarHistorialCliente(clienteId);
            } else {
                String documento = ConsoleReader.leerTextoObligatorio("Documento del cliente");
                historial = clienteController.consultarHistorialCliente(documento);
            }
            imprimirHistorial(historial);
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void imprimirHistorial(HistorialCliente historial) {
        Cliente cliente = historial.getCliente();
        System.out.println("\n--- Historial del Cliente ---");
        System.out.println("  Cliente:    " + cliente.getNombre());
        System.out.println("  Documento:  " + cliente.getDocumentoIdentidad());
        System.out.println("  Estado:     " + (cliente.isActivo() ? "Activo" : "Inactivo"));

        List<Vehiculo> vehiculos = historial.getVehiculos();
        if (vehiculos.isEmpty()) {
            System.out.println("\n  [i] El cliente no tiene vehículos registrados.");
        } else {
            System.out.println("\n  Vehículos:");
            List<String[]> filasVehiculos = new ArrayList<>();
            for (Vehiculo vehiculo : vehiculos) {
                filasVehiculos.add(new String[]{
                        vehiculo.getPlaca(),
                        vehiculo.getMarca(),
                        vehiculo.getModelo()
                });
            }
            ConsoleTableFormatter.imprimirTabla(
                    new String[]{"Placa", "Marca", "Modelo"},
                    filasVehiculos);
        }

        List<ResumenOrdenHistorial> ordenes = historial.getOrdenes();
        if (ordenes.isEmpty()) {
            System.out.println("\n  [i] El cliente no tiene órdenes de servicio registradas.");
            return;
        }

        System.out.println("\n  Órdenes de servicio (más reciente primero):");
        List<String[]> filasOrdenes = new ArrayList<>();
        for (ResumenOrdenHistorial orden : ordenes) {
            String fecha = orden.getFechaRecepcion() == null
                    ? "Sin fecha"
                    : orden.getFechaRecepcion().format(FORMATO_FECHA_HORA);
            StringBuilder resumenLineas = new StringBuilder();
            List<OrdenServicioDetalle> lineas = orden.getLineas();
            for (int i = 0; i < lineas.size(); i++) {
                if (i > 0) {
                    resumenLineas.append("; ");
                }
                OrdenServicioDetalle linea = lineas.get(i);
                resumenLineas.append(linea.getNombreServicioSnapshot())
                        .append(" ($")
                        .append(linea.getCostoManoObra())
                        .append(')');
            }
            String facturacion = orden.isFacturada()
                    ? "$" + orden.getTotalFacturado().toPlainString() + " (" + orden.getNumeroFactura() + ")"
                    : "Sin facturar";
            filasOrdenes.add(new String[]{
                    String.valueOf(orden.getOrdenId()),
                    orden.getPlaca(),
                    orden.getEstado(),
                    fecha,
                    resumenLineas.length() == 0 ? "Sin líneas" : resumenLineas.toString(),
                    facturacion
            });
        }
        ConsoleTableFormatter.imprimirTabla(
                new String[]{"Orden", "Placa", "Estado", "Fecha recepción", "Servicios", "Facturación"},
                filasOrdenes,
                new boolean[]{true, false, false, false, false, false},
                48);
    }

    private void imprimirDetalleVehiculo(Vehiculo vehiculo) {
        System.out.println("  - ID " + vehiculo.getId()
                + " | Placa: " + vehiculo.getPlaca()
                + " | Marca: " + vehiculo.getMarca()
                + " | Modelo: " + vehiculo.getModelo()
                + " | Cliente ID: " + vehiculo.getClienteId());
    }
}
