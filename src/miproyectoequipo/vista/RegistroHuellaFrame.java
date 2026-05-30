/*
 * Vista: Pantalla de Registro de Huella Dactilar
 * Solo accesible por Administrador
 * Permite registrar la huella de un empleado (3 capturas + merge)
 */
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
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
 * Proceso: Ingresar cédula → 3 capturas del mismo dedo → guardar en BD.
 *
 * @author Vladimir
 */
public class RegistroHuellaFrame extends JFrame implements HuellaListener {

    // Colores del tema (mismo que LoginFrame)
    private static final Color COLOR_FONDO = new Color(15, 23, 42);
    private static final Color COLOR_PANEL = new Color(30, 41, 59);
    private static final Color COLOR_PANEL_CLARO = new Color(51, 65, 85);
    private static final Color COLOR_ACENTO = new Color(56, 189, 248);
    private static final Color COLOR_EXITO = new Color(34, 197, 94);
    private static final Color COLOR_ERROR = new Color(239, 68, 68);
    private static final Color COLOR_WARNING = new Color(234, 179, 8);
    private static final Color COLOR_TEXTO = new Color(226, 232, 240);
    private static final Color COLOR_TEXTO_SEC = new Color(148, 163, 184);
    private static final Color COLOR_INPUT_BG = new Color(15, 23, 42);
    private static final Color COLOR_INPUT_BORDER = new Color(71, 85, 105);

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
        setSize(550, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout());

        // === Panel Superior: Título ===
        JPanel panelTitulo = new JPanel();
        panelTitulo.setBackground(COLOR_FONDO);
        panelTitulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panelTitulo.setLayout(new BoxLayout(panelTitulo, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("👆 Registro de Huella", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(COLOR_TEXTO);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDesc = new JLabel("Registre la huella dactilar de un empleado", SwingConstants.CENTER);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDesc.setForeground(COLOR_TEXTO_SEC);
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelTitulo.add(lblTitulo);
        panelTitulo.add(Box.createRigidArea(new Dimension(0, 5)));
        panelTitulo.add(lblDesc);
        add(panelTitulo, BorderLayout.NORTH);

        // === Panel Central ===
        JPanel panelCentral = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = 30, y = 5, w = getWidth() - 60, h = getHeight() - 10;
                g2d.setColor(COLOR_PANEL);
                g2d.fill(new RoundRectangle2D.Float(x, y, w, h, 20, 20));
                g2d.setColor(COLOR_PANEL_CLARO);
                g2d.draw(new RoundRectangle2D.Float(x, y, w, h, 20, 20));
                g2d.dispose();
            }
        };
        panelCentral.setOpaque(false);
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(20, 60, 20, 60));

        // Búsqueda de empleado
        JLabel lblCedula = new JLabel("Cédula del Empleado");
        lblCedula.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCedula.setForeground(COLOR_TEXTO_SEC);
        lblCedula.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setOpaque(false);
        panelBusqueda.setLayout(new BoxLayout(panelBusqueda, BoxLayout.X_AXIS));
        panelBusqueda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panelBusqueda.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtCedula = new JTextField();
        txtCedula.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCedula.setForeground(COLOR_TEXTO);
        txtCedula.setBackground(COLOR_INPUT_BG);
        txtCedula.setCaretColor(COLOR_TEXTO);
        txtCedula.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        txtCedula.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        btnBuscar = crearBotonPeq("🔍 Buscar", COLOR_ACENTO);
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
        lblImagenHuella = new JLabel();
        lblImagenHuella.setPreferredSize(new Dimension(180, 200));
        lblImagenHuella.setMaximumSize(new Dimension(180, 200));
        lblImagenHuella.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagenHuella.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblImagenHuella.setBorder(BorderFactory.createLineBorder(COLOR_PANEL_CLARO, 2, true));

        // Barra de progreso
        progressBar = new JProgressBar(0, 3);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0/3 capturas");
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setForeground(COLOR_ACENTO);
        progressBar.setBackground(COLOR_PANEL_CLARO);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
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
        panelBotones.setOpaque(false);
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.X_AXIS));
        panelBotones.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnRegistrar = crearBoton("👆 Iniciar Registro", COLOR_ACENTO);
        btnRegistrar.setEnabled(false);
        btnRegistrar.addActionListener(e -> iniciarRegistro());

        btnCancelar = crearBoton("Cancelar", COLOR_PANEL_CLARO);
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
            lblNombreEmpleado.setText("⚠ Ingrese una cédula");
            lblNombreEmpleado.setForeground(COLOR_WARNING);
            return;
        }

        Empleado emp = empleadoDAO.buscarPorCedula(cedula);
        if (emp != null) {
            cedulaActual = cedula;
            lblNombreEmpleado.setText("👤 " + emp.getNombreCompleto() + " — " + emp.getTipoContrato());
            lblNombreEmpleado.setForeground(COLOR_EXITO);
            btnRegistrar.setEnabled(true);

            // Verificar si ya tiene huella
            if (huellaDAO.tieneHuella(cedula)) {
                lblEstado.setText("⚠ Este empleado ya tiene huella. Se reemplazará.");
                lblEstado.setForeground(COLOR_WARNING);
            } else {
                lblEstado.setText("Listo. Presione 'Iniciar Registro'.");
                lblEstado.setForeground(COLOR_TEXTO);
            }
        } else {
            lblNombreEmpleado.setText("❌ Empleado no encontrado con cédula: " + cedula);
            lblNombreEmpleado.setForeground(COLOR_ERROR);
            btnRegistrar.setEnabled(false);
            cedulaActual = null;
        }
    }

    private void iniciarRegistro() {
        if (cedulaActual == null) return;

        if (fingerprintManager == null || !fingerprintManager.isActivo()) {
            lblEstado.setText("❌ Conecte el lector de huellas primero.");
            lblEstado.setForeground(COLOR_ERROR);
            return;
        }

        progressBar.setValue(0);
        progressBar.setString("0/3 capturas");
        lblEstado.setText("Coloque el dedo en el lector (captura 1 de 3)");
        lblEstado.setForeground(COLOR_ACENTO);
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
                lblImagenHuella.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            // No es crítico
        }
    }

    @Override
    public void onCapturaError(String mensaje) {
        lblEstado.setText("❌ " + mensaje);
        lblEstado.setForeground(COLOR_ERROR);
        lblProgreso.setText("Intente de nuevo");
    }

    @Override
    public void onDispositivoListo(int ancho, int alto) {
        lblEstado.setText("✅ Lector conectado y listo.");
        lblEstado.setForeground(COLOR_EXITO);
    }

    @Override
    public void onDispositivoError(String mensaje) {
        lblEstado.setText("❌ " + mensaje);
        lblEstado.setForeground(COLOR_ERROR);
    }

    @Override
    public void onProgresoEnroll(int capturaActual, int totalCapturas) {
        progressBar.setValue(capturaActual);
        progressBar.setString(capturaActual + "/" + totalCapturas + " capturas");

        if (capturaActual < totalCapturas) {
            lblEstado.setText("✅ Captura " + capturaActual + " exitosa. Levante y coloque el dedo de nuevo.");
            lblEstado.setForeground(COLOR_EXITO);
            lblProgreso.setText("Captura " + (capturaActual + 1) + " de " + totalCapturas);
        }
    }

    @Override
    public void onEnrollCompleto(String templateBase64) {
        progressBar.setValue(3);
        progressBar.setString("3/3 ✅ Completo");
        lblEstado.setText("✅ ¡Huella registrada exitosamente!");
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

    // =============================================
    // UI Helpers
    // =============================================

    private JButton crearBoton(String texto, Color colorFondo) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = isEnabled() ? (getModel().isRollover() ? colorFondo.brighter() : colorFondo) : COLOR_PANEL_CLARO.darker();
                g2d.setColor(c);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2d.setColor(isEnabled() ? COLOR_TEXTO : COLOR_TEXTO_SEC);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(180, 38));
        btn.setMaximumSize(new Dimension(180, 38));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton crearBotonPeq(String texto, Color colorFondo) {
        JButton btn = crearBoton(texto, colorFondo);
        btn.setPreferredSize(new Dimension(120, 38));
        btn.setMaximumSize(new Dimension(120, 38));
        btn.setMinimumSize(new Dimension(120, 38));
        return btn;
    }
}
