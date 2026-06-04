package miproyectoequipo.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Gestor de conexión a la base de datos MySQL (Patrón Singleton).
 * Lee las credenciales de db.properties y asegura que las tablas existan.
 */
public class ConexionDB {

    private static ConexionDB instancia;
    private Connection conexion;
    
    private String url;
    private String user;
    private String password;

    private ConexionDB() {
        cargarPropiedades();
        conectar();
    }

    private void cargarPropiedades() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
            this.url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/sistema_empleados?createDatabaseIfNotExist=true&serverTimezone=UTC");
            this.user = props.getProperty("db.user", "root");
            this.password = props.getProperty("db.password", "");
        } catch (IOException e) {
            System.err.println("[DB] Archivo db.properties no encontrado. Usando default localhost/root.");
            this.url = "jdbc:mysql://localhost:3306/sistema_empleados?createDatabaseIfNotExist=true&serverTimezone=UTC";
            this.user = "root";
            this.password = "";
        }
    }

    public static ConexionDB getInstancia() {
        if (instancia == null) {
            instancia = new ConexionDB();
        }
        return instancia;
    }

    private void conectar() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(url, user, password);
                System.out.println("[DB] Conectado exitosamente a MySQL (XAMPP).");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al conectar a la base de datos: " + e.getMessage());
        }
    }

    public Connection getConexion() {
        conectar(); // Re-conecta si se cerró
        return conexion;
    }

    public void cerrarConexion() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("[DB] Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }

    /**
     * Crea las tablas necesarias en MySQL si no existen.
     */
    public void inicializarTablas() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "cedula VARCHAR(20) UNIQUE NOT NULL, "
                + "nombre VARCHAR(100) NOT NULL, "
                + "contrasena VARCHAR(255) NOT NULL, "
                + "perfil VARCHAR(20) NOT NULL, " // ADMINISTRADOR o EMPLEADO
                + "activo BOOLEAN DEFAULT TRUE)";

        String sqlEmpleados = "CREATE TABLE IF NOT EXISTS empleados ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "cedula VARCHAR(20) UNIQUE NOT NULL, "
                + "nombre VARCHAR(100) NOT NULL, "
                + "apellido VARCHAR(100) NOT NULL, "
                + "cargo VARCHAR(100), "
                + "tipo_contrato VARCHAR(20) NOT NULL, " // TIEMPO_COMPLETO o TIEMPO_PARCIAL
                + "activo BOOLEAN DEFAULT TRUE)";

        String sqlHuellas = "CREATE TABLE IF NOT EXISTS huellas_digitales ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "cedula_empleado VARCHAR(20) UNIQUE NOT NULL, "
                + "template_base64 LONGTEXT NOT NULL, "
                + "fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (cedula_empleado) REFERENCES empleados(cedula) ON DELETE CASCADE)";

        String sqlAsistencias = "CREATE TABLE IF NOT EXISTS asistencias ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "cedula_empleado VARCHAR(20) NOT NULL, "
                + "fecha DATE NOT NULL, "
                + "hora_entrada_manana TIME, "
                + "hora_salida_manana TIME, "
                + "hora_entrada_tarde TIME, "
                + "hora_salida_tarde TIME, "
                + "minutos_atraso INT DEFAULT 0, "
                + "FOREIGN KEY (cedula_empleado) REFERENCES empleados(cedula) ON DELETE CASCADE)";

        // Insertar usuario administrador por defecto si no existe
        String sqlAdmin = "INSERT IGNORE INTO usuarios (cedula, nombre, contrasena, perfil) "
                + "VALUES ('admin', 'Administrador Sistema', 'admin123', 'ADMINISTRADOR')";

        // Insertar empleados de prueba
        String sqlEmpTCUser = "INSERT IGNORE INTO usuarios (cedula, nombre, contrasena, perfil) "
                + "VALUES ('12345', 'Juan Perez', 'emp123', 'EMPLEADO')";
        String sqlEmpTCEmpleado = "INSERT IGNORE INTO empleados (cedula, nombre, apellido, cargo, tipo_contrato) "
                + "VALUES ('12345', 'Juan', 'Perez', 'Desarrollador', 'TIEMPO_COMPLETO')";

        String sqlEmpTPUser = "INSERT IGNORE INTO usuarios (cedula, nombre, contrasena, perfil) "
                + "VALUES ('67890', 'Maria Gomez', 'emp456', 'EMPLEADO')";
        String sqlEmpTPEmpleado = "INSERT IGNORE INTO empleados (cedula, nombre, apellido, cargo, tipo_contrato) "
                + "VALUES ('67890', 'Maria', 'Gomez', 'Disenadora', 'TIEMPO_PARCIAL')";

        Connection conn = getConexion();
        if (conn == null) {
            System.err.println("[DB] ERROR CRÍTICO: No se pudo establecer conexión con la base de datos MySQL.");
            System.err.println("[DB] Por favor verifique:");
            System.err.println("     1. Que su servidor MySQL (XAMPP/WampServer) esté encendido.");
            System.err.println("     2. Configure sus credenciales correctas en el archivo 'db.properties'.");
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlEmpleados);
            stmt.execute(sqlHuellas);
            stmt.execute(sqlAsistencias);
            stmt.execute(sqlAdmin);
            stmt.execute(sqlEmpTCUser);
            stmt.execute(sqlEmpTCEmpleado);
            stmt.execute(sqlEmpTPUser);
            stmt.execute(sqlEmpTPEmpleado);
            System.out.println("[DB] Tablas e información de prueba inicializadas correctamente (MySQL).");
        } catch (SQLException e) {
            System.err.println("[DB] Error al inicializar tablas: " + e.getMessage());
        }
    }
}
