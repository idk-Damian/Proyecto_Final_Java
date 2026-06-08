
package miproyectoequipo.modelo;

public class EmpleadoTiempoParcial extends Empleado {

    public static final double PAGO_POR_HORA = 5.00;
    public static final int MAX_HORAS_DIARIAS = 8;

    public EmpleadoTiempoParcial() {
        super();
        setTipoContrato(TipoContrato.TIEMPO_PARCIAL);
    }

    public EmpleadoTiempoParcial(String cedula, String nombre, String apellido, String cargo) {
        super(cedula, nombre, apellido, cargo, TipoContrato.TIEMPO_PARCIAL);
    }

    @Override
    public double calcularSueldo(double horasTrabajadas, int minutosAtraso) {
        return horasTrabajadas * PAGO_POR_HORA;
    }

    public double getPagoPorHora() {
        return PAGO_POR_HORA;
    }
}
