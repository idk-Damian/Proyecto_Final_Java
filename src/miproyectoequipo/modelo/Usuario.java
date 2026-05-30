/*
 * Modelo: Usuario del sistema
 * Vincula un empleado con su perfil de acceso y huella digital
 */
package miproyectoequipo.modelo;

/**
 * Representa un usuario del sistema con su perfil de acceso.
 * Cada usuario está vinculado a un empleado por su cédula.
 * 
 * @author Vladimir
 */
public class Usuario {

    public enum Perfil {
        ADMINISTRADOR,
        EMPLEADO
    }

    private int id;
    private String cedula;
    private String nombre;
    private String contrasena; // fallback si no hay lector
    private Perfil perfil;
    private boolean activo;

    public Usuario() {
        this.activo = true;
    }

    public Usuario(String cedula, String nombre, String contrasena, Perfil perfil) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.contrasena = contrasena;
        this.perfil = perfil;
        this.activo = true;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Usuario{" + "cedula=" + cedula + ", nombre=" + nombre + ", perfil=" + perfil + '}';
    }
}
