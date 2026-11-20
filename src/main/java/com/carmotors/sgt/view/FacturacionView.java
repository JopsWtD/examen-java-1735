package com.carmotors.sgt.view;

import com.carmotors.sgt.controller.FacturaController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;
import com.carmotors.sgt.model.ConsultaFactura;
import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.model.FacturaDetalle;
import com.carmotors.sgt.model.FacturaSnapshot;
import com.carmotors.sgt.model.ResultadoReimpresionFactura;
import com.carmotors.sgt.model.TipoConcepto;
import com.carmotors.sgt.util.ConsoleReader;
import com.carmotors.sgt.util.ConsoleTableFormatter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Submenú de consola de facturación (RF-22, RF-23, RF-24, RNF-03, RNF-24).
 * Solo captura datos e imprime resultados; las reglas y los cálculos residen en el servicio.
 */
public class FacturacionView implements SubmenuVista {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FacturaController facturaController;

    public FacturacionView(FacturaController facturaController) {
        if (facturaController == null) {
            throw new IllegalArgumentException("El controlador de facturación es obligatorio.");
        }
        this.facturaController = facturaController;
    }

    @Override
    public String obtenerTitulo() {
        return "Facturación";
    }

    @Override
    public void mostrarMenu() {
        boolean ejecutando = true;
        while (ejecutando) {
            System.out.println("\n==================================================");
            System.out.println("                  FACTURACIÓN                     ");
            System.out.println("==================================================");
            System.out.println("  1. Generar factura de una orden");
            System.out.println("  2. Reintentar generación del TXT");
            System.out.println("  3. Consultar factura");
            System.out.println("  4. Reimprimir factura (regenerar TXT)");
            System.out.println("  5. Listar facturas emitidas");
            System.out.println("  0. Volver al menú principal");
            System.out.println("==================================================");

            int opcion = ConsoleReader.leerEnteroEnRango("Seleccione una opción", 0, 5);
            switch (opcion) {
                case 1:
                    generarFactura();
                    break;
                case 2:
                    reintentarArchivo();
                    break;
                case 3:
                    consultarFactura();
                    break;
                case 4:
                    reimprimirFactura();
                    break;
                case 5:
                    listarFacturas();
                    break;
                case 0:
                    ejecutando = false;
                    break;
                default:
                    break;
            }
        }
    }

    private void generarFactura() {
        int ordenId = -1;
        try {
            ordenId = ConsoleReader.leerEnteroPositivo("ID de la orden de servicio");
            boolean confirmar = ConsoleReader.leerConfirmacion("Facturar la orden " + ordenId
                    + " la cambia a Facturado y no se puede deshacer. ¿Desea continuar?");
            if (!confirmar) {
                System.out.println("\n  [i] Operación cancelada; no se generó ninguna factura.");
                return;
            }

            ConsultaFactura generada = facturaController.generarFactura(ordenId);
            Factura factura = generada.getFactura();
            System.out.println("\n  [✓] Factura registrada en BD (ID " + factura.getId() + ").");
            if (factura.isArchivoGenerado()) {
                System.out.println("  TXT generado: factura_" + factura.getNumeroFactura() + ".txt");
            } else {
                System.out.println("  [!] El TXT quedó pendiente. Use la opción 2 o 4 con este ID para regenerarlo.");
            }
            imprimirFactura(generada);
        } catch (DatoDuplicadoException | TransicionEstadoInvalidaException e) {
            System.out.println("\n  [!] No se pudo generar la factura: " + e.getMessage());
            if (ordenId > 0) {
                ofrecerReimpresionDeOrdenFacturada(ordenId);
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudo generar la factura: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void ofrecerReimpresionDeOrdenFacturada(int ordenId) {
        try {
            ConsultaFactura existente = facturaController.buscarFacturaPorOrden(ordenId);
            if (existente == null) {
                return;
            }
            Factura factura = existente.getFactura();
            System.out.println("  [i] La orden " + ordenId + " ya tiene emitida la factura "
                    + factura.getNumeroFactura() + " (ID " + factura.getId() + ").");
            boolean reimprimir = ConsoleReader.leerConfirmacion("¿Desea consultar y reimprimir la factura existente?");
            if (reimprimir) {
                ResultadoReimpresionFactura resultado = facturaController.reimprimirFactura(factura.getId());
                System.out.println("\n  [✓] Comprobante reimpreso desde datos históricos.");
                System.out.println("  TXT generado: " + resultado.getArchivoTxt());
                imprimirFactura(resultado.getConsulta());
            }
        } catch (CarMotorsException e) {
            System.out.println("  [!] No se pudo reimprimir la factura existente: " + e.getMessage());
        }
    }

    private void reintentarArchivo() {
        try {
            int facturaId = ConsoleReader.leerEnteroPositivo("ID de la factura");
            System.out.println("\n  [✓] TXT generado: " + facturaController.reintentarArchivo(facturaId));
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudo generar el TXT: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void consultarFactura() {
        try {
            int criterio = leerCriterioBusqueda("CONSULTAR FACTURA");
            if (criterio == 0) {
                return;
            }

            ConsultaFactura consulta;
            if (criterio == 1) {
                int facturaId = ConsoleReader.leerEnteroPositivo("ID de la factura");
                consulta = facturaController.consultarFactura(facturaId);
            } else if (criterio == 2) {
                String numeroFactura = ConsoleReader.leerTextoObligatorio("Número de factura (ej. F-2026-0001)");
                consulta = facturaController.consultarFacturaPorNumero(numeroFactura);
            } else {
                int ordenId = ConsoleReader.leerEnteroPositivo("ID de la orden de servicio");
                consulta = facturaController.consultarFacturaPorOrden(ordenId);
            }

            imprimirFactura(consulta);
            boolean reimprimir = ConsoleReader.leerConfirmacion("¿Desea reimprimir/regenerar el archivo TXT de esta factura?");
            if (reimprimir) {
                ResultadoReimpresionFactura resultado = facturaController.reimprimirFactura(consulta.getFactura().getId());
                System.out.println("\n  [✓] Comprobante TXT regenerado: " + resultado.getArchivoTxt());
            }
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudo consultar la factura: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void reimprimirFactura() {
        try {
            int criterio = leerCriterioBusqueda("REIMPRIMIR FACTURA (RF-24)");
            if (criterio == 0) {
                return;
            }

            ResultadoReimpresionFactura resultado;
            if (criterio == 1) {
                int facturaId = ConsoleReader.leerEnteroPositivo("ID de la factura");
                resultado = facturaController.reimprimirFactura(facturaId);
            } else if (criterio == 2) {
                String numeroFactura = ConsoleReader.leerTextoObligatorio("Número de factura (ej. F-2026-0001)");
                resultado = facturaController.reimprimirFacturaPorNumero(numeroFactura);
            } else {
                int ordenId = ConsoleReader.leerEnteroPositivo("ID de la orden de servicio");
                resultado = facturaController.reimprimirFacturaPorOrden(ordenId);
            }

            System.out.println("\n  [✓] Comprobante reimpreso desde los datos históricos (sin modificar la orden).");
            System.out.println("  TXT generado: " + resultado.getArchivoTxt());
            imprimirFactura(resultado.getConsulta());
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudo reimprimir la factura: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private void listarFacturas() {
        try {
            List<Factura> facturas = facturaController.listarFacturas();
            if (facturas.isEmpty()) {
                System.out.println("\n  [i] No hay facturas emitidas registradas.");
                return;
            }

            System.out.println("\n  [Facturas emitidas: " + facturas.size() + "]");
            List<String[]> filas = new ArrayList<>();
            for (Factura factura : facturas) {
                String fecha = factura.getFechaEmision() != null
                        ? factura.getFechaEmision().format(FORMATO_FECHA_HORA)
                        : "N/A";
                String estadoTxt = factura.isArchivoGenerado() ? "Generado" : "Pendiente";
                filas.add(new String[]{
                        String.valueOf(factura.getId()),
                        factura.getNumeroFactura(),
                        String.valueOf(factura.getOrdenId()),
                        fecha,
                        "$" + factura.getTotalFinal().toPlainString(),
                        estadoTxt
                });
            }
            ConsoleTableFormatter.imprimirTabla(
                    new String[]{"ID", "Número", "Orden", "Fecha emisión", "Total", "TXT"},
                    filas,
                    new boolean[]{true, false, true, false, true, false});
        } catch (CarMotorsException e) {
            System.out.println("\n  [!] No se pudieron listar las facturas: " + e.getMessage());
        } finally {
            ConsoleReader.pausar();
        }
    }

    private int leerCriterioBusqueda(String titulo) {
        System.out.println("\n--- " + titulo + " ---");
        System.out.println("  1. Buscar por ID de factura");
        System.out.println("  2. Buscar por número de factura");
        System.out.println("  3. Buscar por ID de orden de servicio");
        System.out.println("  0. Cancelar");
        return ConsoleReader.leerEnteroEnRango("Seleccione criterio de búsqueda", 0, 3);
    }

    private void imprimirFactura(ConsultaFactura consulta) {
        Factura factura = consulta.getFactura();
        FacturaSnapshot snapshot = consulta.getSnapshot();
        String porcentajeIva = factura.getTasaIva() != null
                ? factura.getTasaIva().movePointRight(2).stripTrailingZeros().toPlainString()
                : "19";

        System.out.println("\n==================================================");
        System.out.println("  FACTURA " + factura.getNumeroFactura() + " (ID " + factura.getId() + ")");
        System.out.println("==================================================");
        System.out.println("  Fecha:       " + factura.getFechaEmision().format(FORMATO_FECHA_HORA));
        System.out.println("  Orden:       " + factura.getOrdenId());
        System.out.println("  Archivo TXT: " + (factura.isArchivoGenerado() ? "Generado" : "Pendiente"));
        System.out.println("  Cliente:     " + snapshot.getClienteNombre() + " | Documento: " + snapshot.getClienteDocumento());
        System.out.println("               Teléfono: " + textoOpcional(snapshot.getClienteTelefono())
                + " | Email: " + textoOpcional(snapshot.getClienteEmail()));
        System.out.println("  Vehículo:    " + snapshot.getVehiculoPlaca() + " | " + snapshot.getVehiculoMarca()
                + " " + snapshot.getVehiculoModelo());
        System.out.println("--------------------------------------------------");
        System.out.println("  Conceptos:");
        List<String[]> filasConceptos = new ArrayList<>();
        for (FacturaDetalle concepto : consulta.getDetalles()) {
            String tipo = concepto.getTipoConcepto() == TipoConcepto.MANO_OBRA ? "Mano de obra" : "Repuesto";
            filasConceptos.add(new String[]{
                    tipo,
                    concepto.getDescripcionSnapshot(),
                    String.valueOf(concepto.getCantidad()),
                    "$" + concepto.getPrecioUnitario(),
                    "$" + concepto.getSubtotal()
            });
        }
        ConsoleTableFormatter.imprimirTabla(
                new String[]{"Tipo", "Descripción", "Cantidad", "Precio unit.", "Subtotal"},
                filasConceptos,
                new boolean[]{false, false, true, true, true});
        System.out.println("--------------------------------------------------");
        System.out.println("  Total repuestos:     $" + factura.getTotalRepuestos());
        System.out.println("  Total mano de obra:  $" + factura.getTotalManoObra());
        System.out.println("  Subtotal:            $" + factura.getSubtotal());
        System.out.println("  IVA (" + porcentajeIva + "%):           $" + factura.getImpuestos());
        System.out.println("  TOTAL:               $" + factura.getTotalFinal());
        System.out.println("==================================================");
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? "N/A" : valor;
    }
}
