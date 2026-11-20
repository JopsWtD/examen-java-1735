package com.carmotors.sgt.service;

import com.carmotors.sgt.config.ConnectionFactory;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.model.HistorialCliente;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.ResumenOrdenHistorial;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.ConsultaOrdenServicioDAO;
import com.carmotors.sgt.dao.FacturaDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import com.carmotors.sgt.dao.impl.ClienteDAOImpl;
import com.carmotors.sgt.dao.impl.ConsultaOrdenServicioDAOImpl;
import com.carmotors.sgt.dao.impl.FacturaDAOImpl;
import com.carmotors.sgt.dao.impl.OrdenServicioDetalleDAOImpl;
import com.carmotors.sgt.dao.impl.VehiculoDAOImpl;
import com.carmotors.sgt.service.state.EstadoOrdenFactory;
import com.carmotors.sgt.util.InputValidator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Historial de servicio de un cliente: vehículos, órdenes y total facturado (RF-20, RN-17, RNF-11, HU-10).
 *
 * <p>El historial se puede consultar por ID o documento e incluye clientes inactivos. El total
 * facturado proviene únicamente de {@code facturas.total_final}; si la orden no tiene factura
 * el total queda {@code null} (nunca se estima).</p>
 */
public class HistorialClienteService {

    private final ClienteDAO clienteDAO;
    private final VehiculoDAO vehiculoDAO;
    private final ConsultaOrdenServicioDAO consultaOrdenServicioDAO;
    private final OrdenServicioDetalleDAO ordenServicioDetalleDAO;
    private final FacturaDAO facturaDAO;
    private final boolean usarConexionJdbc;

    /**
     * Constructor de producción: abre una única conexión para las consultas de órdenes,
     * líneas y facturas.
     */
    public HistorialClienteService() {
        this(new ClienteDAOImpl(), new VehiculoDAOImpl());
    }

    /**
     * Constructor de producción con los DAOs de cliente y vehículo inyectados.
     */
    public HistorialClienteService(ClienteDAO clienteDAO, VehiculoDAO vehiculoDAO) {
        if (clienteDAO == null) {
            throw new ValidacionException("El DAO de clientes es obligatorio.");
        }
        if (vehiculoDAO == null) {
            throw new ValidacionException("El DAO de vehículos es obligatorio.");
        }
        this.clienteDAO = clienteDAO;
        this.vehiculoDAO = vehiculoDAO;
        this.consultaOrdenServicioDAO = null;
        this.ordenServicioDetalleDAO = null;
        this.facturaDAO = null;
        this.usarConexionJdbc = true;
    }

    /**
     * Constructor para pruebas con DAOs en memoria: no abre conexiones.
     */
    public HistorialClienteService(ClienteDAO clienteDAO, VehiculoDAO vehiculoDAO,
                                   ConsultaOrdenServicioDAO consultaOrdenServicioDAO,
                                   OrdenServicioDetalleDAO ordenServicioDetalleDAO,
                                   FacturaDAO facturaDAO) {
        if (clienteDAO == null) {
            throw new ValidacionException("El DAO de clientes es obligatorio.");
        }
        if (vehiculoDAO == null) {
            throw new ValidacionException("El DAO de vehículos es obligatorio.");
        }
        if (consultaOrdenServicioDAO == null) {
            throw new ValidacionException("El DAO de consulta de órdenes es obligatorio.");
        }
        if (ordenServicioDetalleDAO == null) {
            throw new ValidacionException("El DAO de líneas de servicio es obligatorio.");
        }
        if (facturaDAO == null) {
            throw new ValidacionException("El DAO de facturas es obligatorio.");
        }
        this.clienteDAO = clienteDAO;
        this.vehiculoDAO = vehiculoDAO;
        this.consultaOrdenServicioDAO = consultaOrdenServicioDAO;
        this.ordenServicioDetalleDAO = ordenServicioDetalleDAO;
        this.facturaDAO = facturaDAO;
        this.usarConexionJdbc = false;
    }

    /**
     * Consulta el historial de un cliente por su identificador, sin filtrar por estado (RN-17).
     *
     * @param clienteId identificador del cliente
     * @return historial con vehículos y órdenes ordenadas de la más reciente a la más antigua
     */
    public HistorialCliente consultarPorId(int clienteId) {
        if (clienteId <= 0) {
            throw new ValidacionException("El ID del cliente debe ser un número positivo.");
        }
        Cliente cliente = clienteDAO.buscarPorId(clienteId);
        if (cliente == null) {
            throw new EntidadNoEncontradaException("No se encontró un cliente con el ID: " + clienteId);
        }
        return construirHistorial(cliente);
    }

    /**
     * Consulta el historial de un cliente por su documento, sin filtrar por estado (RN-17).
     *
     * @param documento documento de identidad del cliente
     * @return historial con vehículos y órdenes ordenadas de la más reciente a la más antigua
     */
    public HistorialCliente consultarPorDocumento(String documento) {
        String documentoNormalizado = InputValidator.normalizarTexto(documento);
        if (documentoNormalizado.isEmpty()) {
            throw new ValidacionException("El documento del cliente es obligatorio.");
        }
        Cliente cliente = clienteDAO.buscarPorDocumento(documentoNormalizado);
        if (cliente == null) {
            throw new EntidadNoEncontradaException(
                    "No se encontró un cliente con el documento: " + documentoNormalizado);
        }
        return construirHistorial(cliente);
    }

    private HistorialCliente construirHistorial(Cliente cliente) {
        List<Vehiculo> vehiculos = vehiculoDAO.listarPorCliente(cliente.getId());
        List<ResumenOrdenHistorial> ordenes;
        if (!usarConexionJdbc) {
            ordenes = cargarOrdenes(cliente, consultaOrdenServicioDAO, ordenServicioDetalleDAO, facturaDAO);
        } else {
            try (Connection connection = ConnectionFactory.getInstance().getConnection()) {
                ordenes = cargarOrdenes(cliente,
                        new ConsultaOrdenServicioDAOImpl(connection),
                        new OrdenServicioDetalleDAOImpl(connection),
                        new FacturaDAOImpl(connection));
            } catch (SQLException e) {
                throw new CarMotorsException("No se pudo consultar el historial del cliente "
                        + cliente.getId() + ": " + e.getMessage(), e);
            }
        }
        return new HistorialCliente(cliente, vehiculos, ordenes);
    }

    private List<ResumenOrdenHistorial> cargarOrdenes(Cliente cliente,
                                                     ConsultaOrdenServicioDAO consultas,
                                                     OrdenServicioDetalleDAO detalles,
                                                     FacturaDAO facturas) {
        List<ResumenOrdenHistorial> historial = new ArrayList<>();
        for (ResumenOrdenServicio resumen : consultas.listarPorCliente(cliente.getId())) {
            List<OrdenServicioDetalle> lineas = detalles.listarPorOrden(resumen.getId());
            Factura factura = facturas.buscarPorOrden(resumen.getId());

            BigDecimal totalFacturado = null;
            String numeroFactura = null;
            if (factura != null) {
                totalFacturado = factura.getTotalFinal();
                numeroFactura = factura.getNumeroFactura();
            }

            historial.add(new ResumenOrdenHistorial(
                    resumen.getId(),
                    EstadoOrdenFactory.desdeId(resumen.getEstadoId()).getNombre(),
                    resumen.getFechaRecepcion(),
                    resumen.getPlaca(),
                    lineas,
                    totalFacturado,
                    numeroFactura));
        }
        return ordenarPorFechaDescendente(historial);
    }

    private List<ResumenOrdenHistorial> ordenarPorFechaDescendente(List<ResumenOrdenHistorial> ordenes) {
        Comparator<ResumenOrdenHistorial> comparador = (primera, segunda) -> {
            LocalDateTime fechaPrimera = primera.getFechaRecepcion();
            LocalDateTime fechaSegunda = segunda.getFechaRecepcion();
            if (fechaPrimera == null && fechaSegunda == null) {
                return Integer.compare(segunda.getOrdenId(), primera.getOrdenId());
            }
            if (fechaPrimera == null) {
                return 1;
            }
            if (fechaSegunda == null) {
                return -1;
            }
            int porFecha = fechaSegunda.compareTo(fechaPrimera);
            if (porFecha != 0) {
                return porFecha;
            }
            return Integer.compare(segunda.getOrdenId(), primera.getOrdenId());
        };
        return ordenes.stream().sorted(comparador).collect(Collectors.toList());
    }
}
