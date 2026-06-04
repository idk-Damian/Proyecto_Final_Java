/*
 * Vista: Pantalla de Registro de Huella Dactilar
 * Solo accesible por Administrador.
 * Permite registrar la huella de un empleado (3 capturas + merge).
 *
 * Diseño: Swing nativo con identidad institucional UTA (rojo vinotinto).
 */
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import miproyectoequipo.dao.EmpleadoDAO;
import miproyectoequipo.dao.HuellaDAO;
import miproyectoequipo.dao.UsuarioDAO;
import miproyectoequipo.huella.HuellaListener;
import miproyectoequipo.huella.ZKFingerprintManager;
import miproyectoequipo.modelo.Empleado;
import miproyectoequipo.modelo.Usuario;

/**
 * Pantalla para registrar la huella dactilar de un empleado.
 * Proceso: Ingresar cédula -> 3 capturas del mismo dedo -> guardar en BD.
 *
 * @author Vladimir
 */
public class RegistroHuellaFrame extends JFrame implements HuellaListener {

    // Paleta institucional UTA sobre Look & Feel nativo
    private static final Color COLOR_UTA = new Color(122, 0, 30);
    private static final Color COLOR_UTA_OSCURO = new Color(90, 0, 22);
    private static final Color COLOR_BORDE = new Color(190, 190, 190);
    private static final Color COLOR_EXITO = new Color(0, 128, 0);
    private static final Color COLOR_ERROR = new Color(178, 0, 0);
    private static final Color COLOR_WARNING = new Color(180, 120, 0);
    private static final Color COLOR_TEXTO = Color.BLACK;
    private static final Color COLOR_TEXTO_SEC = new Color(90, 90, 90);

    // Componentes
    private JTextField txtCedula;
    private JButton btnBuscar;
    private JButton btnRegistrar;
    private JButton btnCancelar;
    private JLabel lblNombreEmpleado;
    private JLabel lblImagenHuella;
    private JLabel lblEstado;
    private JProgressBar progressBar;
    private JLabel lblProgreso;

    // Lógica
    private ZKFingerprintManager fingerprintManager;
    private HuellaDAO huellaDAO;
    private EmpleadoDAO empleadoDAO;
    private UsuarioDAO usuarioDAO;
    private String cedulaActual;
    private Runnable onCerrar; // Callback cuando se cierra

    public RegistroHuellaFrame(ZKFingerprintManager manager) {
        this.fingerprintManager = manager;
        this.huellaDAO = new HuellaDAO();
        this.empleadoDAO = new EmpleadoDAO();
        this.usuarioDAO = new UsuarioDAO();

        if (manager != null) {
            manager.setListener(this);
        }

        initComponents();
    }

    /**
     * Establece un callback para cuando se cierre esta ventana.
     */
    public void setOnCerrar(Runnable callback) {
        this.onCerrar = callback;
    }

    private void initComponents() {
        setTitle("Registrar Huella Dactilar");
        setSize(520, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // === Banner superior (rojo UTA) ===
        JPanel panelTitulo = new JPanel();
        panelTitulo.setBackground(COLOR_UTA);
        panelTitulo.setLayout(new BoxLayout(panelTitulo, BoxLayout.Y_AXIS));
        panelTitulo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_UTA_OSCURO),
            BorderFactory.createEmptyBorder(16, 0, 16, 0)));

        JLabel lblTitulo = new JLabel("Registro de Huella", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDesc = new JLabel("Registre la huella dactilar de un empleado", SwingConstants.CENTER);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDesc.setForeground(new Color(255, 220, 220));
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelTitulo.add(lblTitulo);
        panelTitulo.add(Box.createRigidArea(new Dimension(0, 4)));
        panelTitulo.add(lblDesc);
        add(panelTitulo, BorderLayout.NORTH);

        // === Panel Central ===
        JPanel panelCentral = new JPanel();
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));
        panelCentral.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 50, 20, 50),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_UTA), "Datos del Empleado")));

        // Búsqueda de empleado
        JLabel lblCedula = new JLabel("Cédula del Empleado");
        lblCedula.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCedula.setForeground(COLOR_TEXTO_SEC);
        lblCedula.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setLayout(new BoxLayout(panelBusqueda, BoxLayout.X_AXIS));
        panelBusqueda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panelBusqueda.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtCedula = new JTextField();
        txtCedula.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCedula.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDE, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        txtCedula.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscarEmpleado());

        panelBusqueda.add(txtCedula);
        panelBusqueda.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBusqueda.add(btnBuscar);

        // Nombre del empleado encontrado
        lblNombreEmpleado = new JLabel(" ");
        lblNombreEmpleado.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNombreEmpleado.setForeground(COLOR_TEXTO);
        lblNombreEmpleado.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Imagen de huella
        lblImagenHuella = new JLabel("(Huella)", SwingConstants.CENTER);
        lblImagenHuella.setPreferredSize(new Dimension(180, 200));
        lblImagenHuella.setMaximumSize(new Dimension(180, 200));
        lblImagenHuella.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagenHuella.setForeground(COLOR_TEXTO_SEC);
        lblImagenHuella.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblImagenHuella.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));

        // Barra de progreso
        progressBar = new JProgressBar(0, 3);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0/3 capturas");
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setForeground(COLOR_UTA);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Estado
        lblEstado = new JLabel("Busque un empleado para comenzar", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblEstado.setForeground(COLOR_TEXTO_SEC);
        lblEstado.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Progreso textual
        lblProgreso = new JLabel(" ", SwingConstants.CENTER);
        lblProgreso.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblProgreso.setForeground(COLOR_TEXTO_SEC);
        lblProgreso.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botones
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.X_AXIS));
        panelBotones.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelBotones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        btnRegistrar = new JButton("Iniciar Registro");
        btnRegistrar.setEnabled(false);
        btnRegistrar.addActionListener(e -> iniciarRegistro());

        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> cancelar());

        panelBotones.add(btnRegistrar);
        panelBotones.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBotones.add(btnCancelar);

        // Agregar todo al panel
        panelCentral.add(lblCedula);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 5)));
        panelCentral.add(panelBusqueda);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 8)));
        panelCentral.add(lblNombreEmpleado);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentral.add(lblImagenHuella);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentral.add(progressBar);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 10)));
        panelCentral.add(lblEstado);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 5)));
        panelCentral.add(lblProgreso);
        panelCentral.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentral.add(panelBotones);

        add(panelCentral, BorderLayout.CENTER);

        // Callback al cerrar
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (onCerrar != null) {
                    onCerrar.run();
                }
            }
        });
    }

    // =============================================
    // Lógica
    // =============================================

    private void buscarEmpleado() {
        String cedula = txtCedula.getText().trim();
        if (cedula.isEmpty()) {
            lblNombreEmpleado.setText("Ingrese una cédula");
            lblNombreEmpleado.setForeground(COLOR_WARNING);
            return;
        }

        Empleado emp = empleadoDAO.buscarPorCedula(cedula);
        if (emp != null) {
            cedulaActual = cedula;
            lblNombreEmpleado.setText(emp.getNombreCompleto() + " - " + emp.getTipoContrato());
            lblNombreEmpleado.setForeground(COLOR_EXITO);
            btnRegistrar.setEnabled(true);

            // Verificar si ya tiene huella
            if (huellaDAO.tieneHuella(cedula)) {
                lblEstado.setText("Este empleado ya tiene huella. Se reemplazará.");
                lblEstado.setForeground(COLOR_WARNING);
            } else {
                lblEstado.setText("Listo. Presione 'Iniciar Registro'.");
                lblEstado.setForeground(COLOR_TEXTO);
            }
        } else {
            lblNombreEmpleado.setText("Empleado no encontrado con cédula: " + cedula);
            lblNombreEmpleado.setForeground(COLOR_ERROR);
            btnRegistrar.setEnabled(false);
            cedulaActual = null;
        }
    }

    private void iniciarRegistro() {
        if (cedulaActual == null) return;

        if (fingerprintManager == null || !fingerprintManager.isActivo()) {
            lblEstado.setText("Conecte el lector de huellas primero.");
            lblEstado.setForeground(COLOR_ERROR);
            return;
        }

        progressBar.setValue(0);
        progressBar.setString("0/3 capturas");
        lblEstado.setText("Coloque el dedo en el lector (captura 1 de 3)");
        lblEstado.setForeground(COLOR_UTA);
        btnRegistrar.setEnabled(false);
        txtCedula.setEnabled(false);
        btnBuscar.setEnabled(false);

        fingerprintManager.iniciarEnroll();
    }

    private void cancelar() {
        if (fingerprintManager != null && fingerprintManager.isEnrollando()) {
            fingerprintManager.cancelarEnroll();
        }
        resetUI();
    }

    private void resetUI() {
        progressBar.setValue(0);
        progressBar.setString("0/3 capturas");
        lblEstado.setText("Busque un empleado para comenzar");
        lblEstado.setForeground(COLOR_TEXTO_SEC);
        lblProgreso.setText(" ");
        btnRegistrar.setEnabled(cedulaActual != null);
        txtCedula.setEnabled(true);
        btnBuscar.setEnabled(true);
        lblImagenHuella.setIcon(null);
        lblImagenHuella.setText("(Huella)");
    }

    // =============================================
    // HuellaListener
    // =============================================

    @Override
    public void onCapturaExitosa(byte[] imagen, byte[] template, String base64) {
        try {
            fingerprintManager.guardarImagenBMP(imagen, "temp_enroll.bmp");
            BufferedImage img = ImageIO.read(new File("temp_enroll.bmp"));
            if (img != null) {
                Image scaled = img.getScaledInstance(160, 180, Image.SCALE_SMOOTH);
                lblImagenHuella.setText(null);
                lblImagenHuella.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            // No es crítico
        }
    }

    @Override
    public void onCapturaError(String mensaje) {
        lblEstado.setText(mensaje);
        lblEstado.setForeground(COLOR_ERROR);
        lblProgreso.setText("Intente de nuevo");
    }

    @Override
    public void onDispositivoListo(int ancho, int alto) {
        lblEstado.setText("Lector conectado y listo.");
        lblEstado.setForeground(COLOR_EXITO);
    }

    @Override
    public void onDispositivoError(String mensaje) {
        lblEstado.setText(mensaje);
        lblEstado.setForeground(COLOR_ERROR);
    }

    @Override
    public void onProgresoEnroll(int capturaActual, int totalCapturas) {
        progressBar.setValue(capturaActual);
        progressBar.setString(capturaActual + "/" + totalCapturas + " capturas");

        if (capturaActual < totalCapturas) {
            lblEstado.setText("Captura " + capturaActual + " exitosa. Levante y coloque el dedo de nuevo.");
            lblEstado.setForeground(COLOR_EXITO);
            lblProgreso.setText("Captura " + (capturaActual + 1) + " de " + totalCapturas);
        }
    }

    @Override
    public void onEnrollCompleto(String templateBase64) {
        progressBar.setValue(3);
        progressBar.setString("3/3 Completo");
        lblEstado.setText("¡Huella registrada exitosamente!");
        lblEstado.setForeground(COLOR_EXITO);

        // Guardar en BD
        boolean ok = huellaDAO.guardarHuella(cedulaActual, templateBase64);
        if (ok) {
            lblProgreso.setText("Template guardado en la base de datos.");
            // Agregar al cache del SDK
            fingerprintManager.agregarHuellaAlCache(cedulaActual, templateBase64);

            // También crear/asegurar usuario si no existe
            UsuarioDAO uDao = new UsuarioDAO();
            if (uDao.buscarPorCedula(cedulaActual) == null) {
                Empleado emp = empleadoDAO.buscarPorCedula(cedulaActual);
                if (emp != null) {
                    Usuario nuevoUsuario = new Usuario(
                        cedulaActual, emp.getNombreCompleto(), "1234", Usuario.Perfil.EMPLEADO
                    );
                    uDao.insertar(nuevoUsuario);
                    lblProgreso.setText("Template guardado + usuario creado (contraseña: 1234).");
                }
            }

            JOptionPane.showMessageDialog(this,
                "Huella registrada exitosamente para el empleado con cédula: " + cedulaActual,
                "Registro Exitoso", JOptionPane.INFORMATION_MESSAGE);
        } else {
            lblProgreso.setText("Error al guardar en la base de datos.");
            lblEstado.setForeground(COLOR_ERROR);
        }

        // Reset para siguiente registro
        Timer t = new Timer(2000, e -> resetUI());
        t.setRepeats(false);
        t.start();
    }

    @Override
    public void onIdentificacionResultado(String cedula, int score) {
        // No usado aquí
    }
}
