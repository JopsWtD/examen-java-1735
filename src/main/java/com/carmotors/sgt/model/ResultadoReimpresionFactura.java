package com.carmotors.sgt.model;

import java.nio.file.Path;

/**
 * DTO de resultado para la reimpresión de una factura existente (RF-24, RN-11):
 * agrupa los datos históricos consultados y la ruta del archivo TXT regenerado.
 */
public class ResultadoReimpresionFactura {

    private final ConsultaFactura consulta;
    private final Path archivoTxt;

    public ResultadoReimpresionFactura(ConsultaFactura consulta, Path archivoTxt) {
        this.consulta = consulta;
        this.archivoTxt = archivoTxt;
    }

    public ConsultaFactura getConsulta() {
        return consulta;
    }

    public Path getArchivoTxt() {
        return archivoTxt;
    }

    @Override
    public String toString() {
        return "ResultadoReimpresionFactura{" +
                "consulta=" + consulta +
                ", archivoTxt=" + archivoTxt +
                '}';
    }
}
