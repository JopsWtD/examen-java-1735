package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.Vehiculo;

import java.util.List;

public interface ConsultaOrdenServicioDAO {
    List<ResumenOrdenServicio> listar(FiltroOrdenServicio filtro);

    /**
     * Retorna las órdenes de servicio de todos los vehículos de un cliente (RF-20, HU-10),
     * de la más reciente a la más antigua.
     *
     * @param clienteId identificador del cliente (activo o inactivo, RN-17)
     * @return resúmenes de las órdenes del cliente (vacío si no tiene)
     */
    List<ResumenOrdenServicio> listarPorCliente(int clienteId);

    void cargarDatosFacturados(int ordenId, Cliente cliente, Vehiculo vehiculo);
}
