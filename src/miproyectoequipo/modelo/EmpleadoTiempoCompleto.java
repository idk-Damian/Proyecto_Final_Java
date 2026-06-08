
package miproyectoequipo.modelo;

public class EmpleadoTiempoCompleto extends Empleado {

    public static final double SUELDO_FIJO = 1500.00;
    public static final double DESCUENTO_POR_MINUTO_ATRASO = 0.20;

    public EmpleadoTiempoCompleto() {
        super();
        setTipoContrato(TipoContrato.TIEMPO_COMPLETO);
    }

    public EmpleadoTiempoCompleto(String cedula, String nombre, String apellido, String cargo) {
        super(cedula, nombre, apellido, cargo, TipoContrato.TIEMPO_COMPLETO);
    }

    @Override
    public double calcularSueldo(double horasTrabajadas, int minutosAtraso) {
        double descuento = minutosAtraso * DESCUENTO_POR_MINUTO_ATRASO;
        return SUELDO_FIJO - descuento;
    }

    public double getSueldoFijo() {
        return SUELDO_FIJO;
    }
}
