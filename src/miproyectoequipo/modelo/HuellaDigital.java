/*
 * Modelo: Huella Digital
 * Almacena el template de huella en formato Base64
 */
package miproyectoequipo.modelo;

import java.time.LocalDateTime;

/**
 * Representa una huella digital registrada.
 * El template se almacena en formato Base64 generado por el SDK ZKTeco.
 * 
 * @author Vladimir
 */
public class HuellaDigital {

    private int id;
    private String cedulaEmpleado;
    private String templateBase64;
    private LocalDateTime fechaRegistro;

    public HuellaDigital() {
        this.fechaRegistro = LocalDateTime.now();
    }

    public HuellaDigital(String cedulaEmpleado, String templateBase64) {
        this.cedulaEmpleado = cedulaEmpleado;
        this.templateBase64 = templateBase64;
        this.fechaRegistro = LocalDateTime.now();
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCedulaEmpleado() {
        return cedulaEmpleado;
    }

    public void setCedulaEmpleado(String cedulaEmpleado) {
        this.cedulaEmpleado = cedulaEmpleado;
    }

    public String getTemplateBase64() {
        return templateBase64;
    }

    public void setTemplateBase64(String templateBase64) {
        this.templateBase64 = templateBase64;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return "HuellaDigital{" + "cedulaEmpleado=" + cedulaEmpleado + ", fechaRegistro=" + fechaRegistro + '}';
    }
}
