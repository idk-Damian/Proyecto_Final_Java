/*
 * Vista: Panel Principal (Dashboard)
 * Muestra opciones según el perfil del usuario (Admin/Empleado)
 */
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.swing.*;
import miproyectoequipo.dao.*;
import miproyectoequipo.huella.HuellaListener;
import miproyectoequipo.huella.ZKFingerprintManager;
import miproyectoequipo.modelo.*;

/**
 * Dashboard principal del sistema.
 * Admin: acceso total (registrar huellas, gestionar empleados, reportes).
 * Empleado: registrar asistencia y ver reportes propios.
 *
 * @author Vladimir
 */
public class PanelPrincipalFrame extends JFrame {

    // Colores del tema
    private static final Color COLOR_FONDO = new Color(15, 23, 42);
    private static final Color COLOR_SIDEBAR = new Color(20, 27, 45);
    private static final Color COLOR_PANEL = new Color(30, 41, 59);
    private static final Color COLOR_PANEL_CLARO = new Color(51, 65, 85);
    private static final Color COLOR_ACENTO = new Color(56, 189, 248);
    private static final Color COLOR_EXITO = new Color(34, 197, 94);
    private static final Color COLOR_ERROR = new Color(239, 68, 68);
    private static final Color COLOR_WARNING = new Color(234, 179, 8);
    private static final Color COLOR_TEXTO = new Color(226, 232, 240);
    private static final Color COLOR_TEXTO_SEC = new Color(148, 163, 184);

    // Estado
    private Usuario usuarioActual;
    private ZKFingerprintManager fingerprintManager;
    private JPanel panelContenido;
    private CardLayout cardLayout;
    private JLabel lblReloj;
    private Timer relojTimer;

    public PanelPrincipalFrame(Usuario usuario) {
        this.usuarioActual = usuario;
        this.fingerprintManager = new ZKFingerprintManager();
        initComponents();
        iniciarReloj();
    }

    private void initComponents() {
        setTitle("Sistema de Asistencia — " + usuarioActual.getNombre());
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout());

        // === Sidebar ===
        JPanel sidebar = crearSidebar();
        add(sidebar, BorderLayout.WEST);

        // === Contenido principal ===
        cardLayout = new CardLayout();
        panelContenido = new JPanel(cardLayout);
        panelContenido.setBackground(COLOR_FONDO);

        panelContenido.add(crearPanelBienvenida(), "BIENVENIDA");
        panelContenido.add(crearPanelRegistroEmpleado(), "REG_EMPLEADO");

        add(panelContenido, BorderLayout.CENTER);

        // Cerrar recursos al salir
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (fingerprintManager.isActivo()) {
                    fingerprintManager.cerrar();
                }
                if (relojTimer != null) relojTimer.stop();
                ConexionDB.getInstancia().cerrarConexion();
            }
        });
    }

    /**
     * Crea la barra lateral con navegación.
     */
    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(COLOR_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_PANEL_CLARO));

        // Header del sidebar
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel lblLogo = new JLabel("🏢 SisAsistencia");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLogo.setForeground(COLOR_ACENTO);
        lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Reloj
        lblReloj = new JLabel();
        lblReloj.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblReloj.setForeground(COLOR_TEXTO_SEC);
        lblReloj.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(lblLogo);
        header.add(Box.createRigidArea(new Dimension(0, 5)));
        header.add(lblReloj);

        sidebar.add(header);
        sidebar.add(crearSeparador());

        // Info del usuario
        JPanel userInfo = new JPanel();
        userInfo.setOpaque(false);
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        userInfo.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        userInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel lblUser = new JLabel("👤 " + usuarioActual.getNombre());
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(COLOR_TEXTO);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPerfil = new JLabel(
            usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR ? "🔑 Administrador" : "📋 Empleado"
        );
        lblPerfil.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPerfil.setForeground(
            usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR ? COLOR_WARNING : COLOR_ACENTO
        );
        lblPerfil.setAlignmentX(Component.LEFT_ALIGNMENT);

        userInfo.add(lblUser);
        userInfo.add(Box.createRigidArea(new Dimension(0, 3)));
        userInfo.add(lblPerfil);

        sidebar.add(userInfo);
        sidebar.add(crearSeparador());

        // Menú
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        sidebar.add(crearBotonMenu("🏠 Inicio", e -> cardLayout.show(panelContenido, "BIENVENIDA")));

        if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            JLabel lblAdmin = new JLabel("  ADMINISTRACIÓN");
            lblAdmin.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblAdmin.setForeground(COLOR_TEXTO_SEC);
            lblAdmin.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblAdmin.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 0));
            sidebar.add(lblAdmin);

            sidebar.add(crearBotonMenu("👆 Registrar Huella", e -> abrirRegistroHuella()));
            sidebar.add(crearBotonMenu("👥 Registrar Empleado", e -> cardLayout.show(panelContenido, "REG_EMPLEADO")));
            sidebar.add(crearBotonMenu("📋 Listar Empleados", e -> mostrarListaEmpleados()));
        }

        JLabel lblAsist = new JLabel("  ASISTENCIA");
        lblAsist.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblAsist.setForeground(COLOR_TEXTO_SEC);
        lblAsist.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAsist.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 0));
        sidebar.add(lblAsist);

        sidebar.add(crearBotonMenu("⏰ Registrar Asistencia", e -> registrarAsistencia()));

        sidebar.add(Box.createVerticalGlue());

        // Botón cerrar sesión
        sidebar.add(crearSeparador());
        sidebar.add(crearBotonMenu("🚪 Cerrar Sesión", e -> cerrarSesion()));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        return sidebar;
    }

    /**
     * Panel de bienvenida con información del sistema.
     */
    private JPanel crearPanelBienvenida() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_FONDO);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel lblBienvenida = new JLabel("Bienvenido, " + usuarioActual.getNombre());
        lblBienvenida.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblBienvenida.setForeground(COLOR_TEXTO);
        lblBienvenida.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblFecha = new JLabel("📅 " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy")));
        lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblFecha.setForeground(COLOR_TEXTO_SEC);
        lblFecha.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Cards de resumen
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        EmpleadoDAO empDAO = new EmpleadoDAO();
        HuellaDAO hDAO = new HuellaDAO();
        int totalEmpleados = empDAO.listarTodos().size();
        int totalHuellas = hDAO.obtenerTodasHuellas().size();

        cardsPanel.add(crearCard("👥", "Empleados", String.valueOf(totalEmpleados), COLOR_ACENTO));
        cardsPanel.add(crearCard("👆", "Huellas Registradas", String.valueOf(totalHuellas), COLOR_EXITO));
        cardsPanel.add(crearCard("📋", "Tu Perfil", usuarioActual.getPerfil().name(), COLOR_WARNING));

        JLabel lblInstrucciones = new JLabel("<html><body style='width: 400px'>"
            + "<p style='color:#94a3b8; font-size:12px;'>Use el menú lateral para navegar. "
            + (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR
                ? "Como administrador, puede registrar huellas, gestionar empleados y ver todos los reportes."
                : "Puede registrar su asistencia y consultar sus reportes.")
            + "</p></body></html>");
        lblInstrucciones.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lblBienvenida);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(lblFecha);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(cardsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 25)));
        panel.add(lblInstrucciones);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Panel para registrar un nuevo empleado (beta).
     */
    private JPanel crearPanelRegistroEmpleado() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_FONDO);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel lblTitulo = new JLabel("👥 Registrar Nuevo Empleado");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(COLOR_TEXTO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Formulario
        JTextField txtCedula = crearCampo("Cédula");
        JTextField txtNombre = crearCampo("Nombre");
        JTextField txtApellido = crearCampo("Apellido");
        JTextField txtCargo = crearCampo("Cargo");

        JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"TIEMPO_COMPLETO", "TIEMPO_PARCIAL"});
        cmbTipo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbTipo.setBackground(new Color(15, 23, 42));
        cmbTipo.setForeground(COLOR_TEXTO);
        cmbTipo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cmbTipo.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbTipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(COLOR_ACENTO);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(new Color(30, 41, 59));
                    c.setForeground(COLOR_TEXTO);
                }
                return c;
            }
        });

        JLabel lblResultado = new JLabel(" ");
        lblResultado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblResultado.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnGuardar = crearBotonAccion("💾 Guardar Empleado", COLOR_ACENTO);
        btnGuardar.addActionListener(e -> {
            String cedula = txtCedula.getText().trim();
            String nombre = txtNombre.getText().trim();
            String apellido = txtApellido.getText().trim();
            String cargo = txtCargo.getText().trim();

            if (cedula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
                lblResultado.setText("❌ Complete los campos obligatorios (Cédula, Nombre, Apellido).");
                lblResultado.setForeground(COLOR_ERROR);
                return;
            }

            Empleado.TipoContrato tipo = Empleado.TipoContrato.valueOf((String) cmbTipo.getSelectedItem());
            Empleado emp;
            if (tipo == Empleado.TipoContrato.TIEMPO_COMPLETO) {
                emp = new EmpleadoTiempoCompleto(cedula, nombre, apellido, cargo);
            } else {
                emp = new EmpleadoTiempoParcial(cedula, nombre, apellido, cargo);
            }

            EmpleadoDAO dao = new EmpleadoDAO();
            if (dao.insertar(emp)) {
                lblResultado.setText("✅ Empleado registrado exitosamente. Ahora puede registrar su huella.");
                lblResultado.setForeground(COLOR_EXITO);
                txtCedula.setText("");
                txtNombre.setText("");
                txtApellido.setText("");
                txtCargo.setText("");
            } else {
                lblResultado.setText("❌ Error al registrar. Verifique que la cédula no esté duplicada.");
                lblResultado.setForeground(COLOR_ERROR);
            }
        });

        panel.add(lblTitulo);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(crearLabel("Cédula *"));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtCedula);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(crearLabel("Nombre *"));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtNombre);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(crearLabel("Apellido *"));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtApellido);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(crearLabel("Cargo"));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtCargo);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(crearLabel("Tipo de Contrato"));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(cmbTipo);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(btnGuardar);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblResultado);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // =============================================
    // Acciones
    // =============================================

    private void abrirRegistroHuella() {
        // Inicializar el lector si no está activo
        if (!fingerprintManager.isActivo()) {
            new Thread(() -> {
                boolean ok = fingerprintManager.iniciar();
                if (ok) {
                    Map<String, String> huellas = new HuellaDAO().obtenerTodasHuellas();
                    fingerprintManager.cargarHuellasEnCache(huellas);
                }
                SwingUtilities.invokeLater(() -> {
                    RegistroHuellaFrame frame = new RegistroHuellaFrame(fingerprintManager);
                    frame.setOnCerrar(() -> fingerprintManager.setListener(null));
                    frame.setVisible(true);
                });
            }, "LectorInit-Enroll").start();
        } else {
            RegistroHuellaFrame frame = new RegistroHuellaFrame(fingerprintManager);
            frame.setOnCerrar(() -> fingerprintManager.setListener(null));
            frame.setVisible(true);
        }
    }

    private void registrarAsistencia() {
        String cedula = usuarioActual.getCedula();
        LocalTime ahora = LocalTime.now();
        LocalDate hoy = LocalDate.now();

        AsistenciaDAO asistDAO = new AsistenciaDAO();
        RegistroAsistencia reg = asistDAO.buscarPorCedulaYFecha(cedula, hoy);

        if (reg == null) {
            reg = new RegistroAsistencia(cedula);
        }

        // Determinar qué campo llenar según la hora
        String accion;
        if (ahora.isBefore(LocalTime.of(13, 0))) {
            if (reg.getHoraEntradaManana() == null) {
                reg.setHoraEntradaManana(ahora);
                accion = "Entrada Mañana: " + ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            } else {
                reg.setHoraSalidaManana(ahora);
                accion = "Salida Mañana: " + ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        } else {
            if (reg.getHoraEntradaTarde() == null) {
                reg.setHoraEntradaTarde(ahora);
                accion = "Entrada Tarde: " + ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            } else if (reg.getHoraSalidaTarde() == null) {
                reg.setHoraSalidaTarde(ahora);
                accion = "Salida Tarde: " + ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            } else {
                JOptionPane.showMessageDialog(this,
                    "Ya tiene todas las asistencias registradas para hoy.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        reg.calcularMinutosAtraso();

        if (asistDAO.registrarAsistencia(reg)) {
            JOptionPane.showMessageDialog(this,
                "✅ Asistencia registrada:\n" + accion +
                    (reg.getMinutosAtraso() > 0 ? "\n⚠ Atraso: " + reg.getMinutosAtraso() + " minutos" : ""),
                "Asistencia", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ Error al registrar la asistencia.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarListaEmpleados() {
        EmpleadoDAO dao = new EmpleadoDAO();
        java.util.List<Empleado> empleados = dao.listarTodos();

        String[] columnas = {"Cédula", "Nombre", "Apellido", "Cargo", "Tipo", "Huella"};
        HuellaDAO hDAO = new HuellaDAO();
        Object[][] datos = new Object[empleados.size()][6];

        for (int i = 0; i < empleados.size(); i++) {
            Empleado emp = empleados.get(i);
            datos[i] = new Object[]{
                emp.getCedula(), emp.getNombre(), emp.getApellido(),
                emp.getCargo(), emp.getTipoContrato().name(),
                hDAO.tieneHuella(emp.getCedula()) ? "✅" : "❌"
            };
        }

        JTable tabla = new JTable(datos, columnas);
        tabla.setBackground(COLOR_PANEL);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setGridColor(COLOR_PANEL_CLARO);
        tabla.setRowHeight(30);
        tabla.getTableHeader().setBackground(COLOR_PANEL_CLARO);
        tabla.getTableHeader().setForeground(COLOR_TEXTO);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(600, 300));
        scroll.getViewport().setBackground(COLOR_PANEL);

        JOptionPane.showMessageDialog(this, scroll,
            "Lista de Empleados (" + empleados.size() + ")", JOptionPane.PLAIN_MESSAGE);
    }

    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(this,
            "¿Desea cerrar sesión?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            if (fingerprintManager.isActivo()) {
                fingerprintManager.cerrar();
            }
            if (relojTimer != null) relojTimer.stop();

            LoginFrame login = new LoginFrame();
            login.setVisible(true);
            this.dispose();
        }
    }

    private void iniciarReloj() {
        relojTimer = new Timer(1000, e -> {
            lblReloj.setText("🕐 " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                + " — " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
        relojTimer.start();
    }

    // =============================================
    // UI Helpers
    // =============================================

    private JPanel crearCard(String icono, String titulo, String valor, Color color) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(COLOR_PANEL);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2d.setColor(COLOR_PANEL_CLARO);
                g2d.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblIcono = new JLabel(icono + " " + titulo);
        lblIcono.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblIcono.setForeground(COLOR_TEXTO_SEC);
        lblIcono.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValor.setForeground(color);
        lblValor.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblIcono);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(lblValor);

        return card;
    }

    private JButton crearBotonMenu(String texto, ActionListener action) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(COLOR_TEXTO_SEC);
        btn.setBackground(COLOR_SIDEBAR);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(COLOR_PANEL);
                btn.setForeground(COLOR_TEXTO);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(COLOR_SIDEBAR);
                btn.setForeground(COLOR_TEXTO_SEC);
            }
        });

        btn.addActionListener(action);
        return btn;
    }

    private JButton crearBotonAccion(String texto, Color colorFondo) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getModel().isRollover() ? colorFondo.brighter() : colorFondo);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2d.setColor(COLOR_TEXTO);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(250, 42));
        btn.setMaximumSize(new Dimension(250, 42));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private JSeparator crearSeparador() {
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_PANEL_CLARO);
        sep.setBackground(COLOR_SIDEBAR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(COLOR_TEXTO_SEC);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField crearCampo(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(COLOR_TEXTO);
        field.setBackground(new Color(15, 23, 42));
        field.setCaretColor(COLOR_TEXTO);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(71, 85, 105), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setMaximumSize(new Dimension(400, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }
}
