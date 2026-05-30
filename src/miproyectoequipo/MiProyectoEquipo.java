/*
 * Punto de entrada del Sistema de Registro de Empleados
 * Inicializa la base de datos y lanza la pantalla de login
 */
package miproyectoequipo;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import miproyectoequipo.dao.ConexionDB;
import miproyectoequipo.vista.LoginFrame;

/**
 * Clase principal del sistema.
 * Inicializa la BD SQL Server y abre la pantalla de login.
 *
 * @author Vladimir
 */
public class MiProyectoEquipo {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Intentar usar look and feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Usar default si falla
        }

        // Inicializar base de datos (crear tablas si no existen)
        System.out.println("=== Sistema de Registro de Empleados ===");
        System.out.println("Inicializando base de datos...");
        ConexionDB.getInstancia().inicializarTablas();

        // Lanzar interfaz gráfica en el EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }
}
