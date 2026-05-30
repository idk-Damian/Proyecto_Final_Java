/*
 * Vista: Panel Principal (Dashboard)
 * Muestra opciones según el perfil del usuario (Admin/Empleado)
 * Provee CRUD completo de empleados, registro de asistencia y consultas/reportes detallados
 */
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import miproyectoequipo.dao.*;
import miproyectoequipo.huella.ZKFingerprintManager;
import miproyectoequipo.modelo.*;

/**
 * Dashboard principal del sistema.
 * Admin: acceso total (registrar huellas, gestionar empleados, reportes de todos).
 * Empleado: registrar asistencia y ver reportes propios.
 *
 * @author Vladimir & Antigravity
 */
public class PanelPrincipalFrame extends JFrame {

    // Colores del tema oscuro premium
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

    // Componentes de CRUD Empleados
    private JTable tablaEmpleados;
    private JTextField txtCrudCedula;
    private JTextField txtCrudNombre;
    private JTextField txtCrudApellido;
    private JTextField txtCrudCargo;
    private JComboBox<String> cmbCrudTipoContrato;
    private JTextField txtCrudBuscar;
    private JLabel lblCrudResultado;

    public PanelPrincipalFrame(Usuario usuario) {
        this.usuarioActual = usuario;
        this.fingerprintManager = new ZKFingerprintManager();
        initComponents();
        iniciarReloj();
    }

    private void initComponents() {
        setTitle("Sistema de Asistencia — " + usuarioActual.getNombre());
        setSize(1000, 700);
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
        if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            panelContenido.add(crearPanelGestionEmpleados(), "GESTION_EMPLEADOS");
        }
        panelContenido.add(crearPanelReportes(), "REPORTES");

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
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblLogo.setForeground(COLOR_ACENTO);
        lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Reloj
        lblReloj = new JLabel();
        lblReloj.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
            sidebar.add(crearBotonMenu("👥 Gestión Empleados", e -> {
                actualizarTablaEmpleados();
                cardLayout.show(panelContenido, "GESTION_EMPLEADOS");
            }));
        }

        JLabel lblAsist = new JLabel("  OPERACIONES");
        lblAsist.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblAsist.setForeground(COLOR_TEXTO_SEC);
        lblAsist.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAsist.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 0));
        sidebar.add(lblAsist);

        sidebar.add(crearBotonMenu("⏰ Marcar Asistencia", e -> registrarAsistencia()));
        sidebar.add(crearBotonMenu("📊 Consultas y Reportes", e -> cardLayout.show(panelContenido, "REPORTES")));

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
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        EmpleadoDAO empDAO = new EmpleadoDAO();
        HuellaDAO hDAO = new HuellaDAO();
        int totalEmpleados = empDAO.listarTodos().size();
        int totalHuellas = hDAO.obtenerTodasHuellas().size();

        cardsPanel.add(crearCard("👥", "Empleados Activos", String.valueOf(totalEmpleados), COLOR_ACENTO));
        cardsPanel.add(crearCard("👆", "Huellas Registradas", String.valueOf(totalHuellas), COLOR_EXITO));
        cardsPanel.add(crearCard("📋", "Tu Perfil", usuarioActual.getPerfil().name(), COLOR_WARNING));

        JLabel lblInstrucciones = new JLabel("<html><body style='width: 500px'>"
            + "<h3 style='color:#38bdf8; font-family:Segoe UI;'>Instrucciones de Uso del Sistema</h3>"
            + "<p style='color:#e2e8f0; font-size:12px; line-height:1.6;'>"
            + "Este sistema le permite registrar y consultar de forma exacta los horarios de entrada y salida, "
            + "así como gestionar el personal y visualizar reportes detallados con cálculo automático de sueldos.<br><br>"
            + "<b>⏰ Control de Asistencias Oficial:</b><br>"
            + "• Entrada Mañana: 08:00 AM a 13:00 PM<br>"
            + "• Almuerzo (obligatorio libre): 13:00 PM a 14:00 PM<br>"
            + "• Entrada Tarde: 14:00 PM a 17:00 PM<br>"
            + "• Total Horas Diarias: 8 horas.<br><br>"
            + "<b>💡 Compensación y Descuentos:</b><br>"
            + "• Empleado Tiempo Completo: Sueldo mensual fijo de $1500. Se descuentan 20 centavos ($0.20) por cada minuto de atraso en los ingresos.<br>"
            + "• Empleado Tiempo Parcial: Se le pagan $5.00 por hora laborada (minutos ajustados proporcionalmente) sin superar 8 horas diarias de labor."
            + "</p></body></html>");
        lblInstrucciones.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lblBienvenida);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(lblFecha);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(cardsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(lblInstrucciones);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Panel unificado de Gestión de Empleados (CRUD completo).
     */
    private JPanel crearPanelGestionEmpleados() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Título del Panel
        JLabel lblTitulo = new JLabel("👥 Gestión Integral de Empleados");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(COLOR_TEXTO);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(lblTitulo, BorderLayout.NORTH);

        // --- Panel Central Izquierda: Tabla y Buscador ---
        JPanel panelIzquierda = new JPanel(new BorderLayout(0, 10));
        panelIzquierda.setOpaque(false);

        // Buscador
        JPanel panelBuscador = new JPanel(new BorderLayout(10, 0));
        panelBuscador.setOpaque(false);
        JLabel lblBuscar = new JLabel("🔍 Buscar:");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBuscar.setForeground(COLOR_TEXTO_SEC);
        txtCrudBuscar = crearCampo("Cédula o Nombre...");
        txtCrudBuscar.setPreferredSize(new Dimension(200, 32));
        txtCrudBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarTablaEmpleados(txtCrudBuscar.getText().trim());
            }
        });
        panelBuscador.add(lblBuscar, BorderLayout.WEST);
        panelBuscador.add(txtCrudBuscar, BorderLayout.CENTER);
        panelIzquierda.add(panelBuscador, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {"Cédula", "Nombre", "Apellido", "Cargo", "Tipo Contrato"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaEmpleados = new JTable(model);
        tablaEmpleados.setBackground(COLOR_PANEL);
        tablaEmpleados.setForeground(COLOR_TEXTO);
        tablaEmpleados.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaEmpleados.setGridColor(COLOR_PANEL_CLARO);
        tablaEmpleados.setRowHeight(30);
        tablaEmpleados.getTableHeader().setBackground(COLOR_PANEL_CLARO);
        tablaEmpleados.getTableHeader().setForeground(COLOR_TEXTO);
        tablaEmpleados.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaEmpleados.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = tablaEmpleados.getSelectedRow();
            if (selectedRow >= 0) {
                txtCrudCedula.setText((String) tablaEmpleados.getValueAt(selectedRow, 0));
                txtCrudCedula.setEditable(false); // No editar la cédula (clave única)
                txtCrudNombre.setText((String) tablaEmpleados.getValueAt(selectedRow, 1));
                txtCrudApellido.setText((String) tablaEmpleados.getValueAt(selectedRow, 2));
                txtCrudCargo.setText((String) tablaEmpleados.getValueAt(selectedRow, 3));
                cmbCrudTipoContrato.setSelectedItem((String) tablaEmpleados.getValueAt(selectedRow, 4));
            }
        });

        JScrollPane scroll = new JScrollPane(tablaEmpleados);
        scroll.getViewport().setBackground(COLOR_PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(COLOR_PANEL_CLARO));
        panelIzquierda.add(scroll, BorderLayout.CENTER);

        // --- Panel Central Derecha: Formulario CRUD ---
        JPanel panelDerecha = new JPanel();
        panelDerecha.setBackground(COLOR_PANEL);
        panelDerecha.setPreferredSize(new Dimension(320, 0));
        panelDerecha.setLayout(new BoxLayout(panelDerecha, BoxLayout.Y_AXIS));
        panelDerecha.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblSub = new JLabel("Formulario de Datos");
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSub.setForeground(COLOR_ACENTO);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtCrudCedula = crearCampo("Cédula");
        txtCrudNombre = crearCampo("Nombre");
        txtCrudApellido = crearCampo("Apellido");
        txtCrudCargo = crearCampo("Cargo");

        cmbCrudTipoContrato = new JComboBox<>(new String[]{"TIEMPO_COMPLETO", "TIEMPO_PARCIAL"});
        cmbCrudTipoContrato.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbCrudTipoContrato.setBackground(COLOR_FONDO);
        cmbCrudTipoContrato.setForeground(COLOR_TEXTO);
        cmbCrudTipoContrato.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cmbCrudTipoContrato.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblCrudResultado = new JLabel(" ");
        lblCrudResultado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCrudResultado.setForeground(COLOR_TEXTO_SEC);
        lblCrudResultado.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Botones de acción
        JPanel panelBotones = new JPanel(new GridLayout(3, 1, 0, 8));
        panelBotones.setOpaque(false);
        panelBotones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        panelBotones.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnGuardar = crearBotonAccion("💾 Registrar Nuevo", COLOR_ACENTO);
        btnGuardar.addActionListener(e -> crudRegistrar());

        JButton btnModificar = crearBotonAccion("✏️ Modificar Seleccionado", COLOR_EXITO);
        btnModificar.addActionListener(e -> crudModificar());

        JButton btnEliminar = crearBotonAccion("❌ Eliminar / Dar de Baja", COLOR_ERROR);
        btnEliminar.addActionListener(e -> crudEliminar());

        panelBotones.add(btnGuardar);
        panelBotones.add(btnModificar);
        panelBotones.add(btnEliminar);

        JButton btnLimpiar = crearBotonAccion("🧹 Limpiar Campos", COLOR_PANEL_CLARO);
        btnLimpiar.addActionListener(e -> limpiarCamposCrud());

        panelDerecha.add(lblSub);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 15)));
        panelDerecha.add(crearLabel("Cédula *"));
        panelDerecha.add(txtCrudCedula);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 8)));
        panelDerecha.add(crearLabel("Nombre *"));
        panelDerecha.add(txtCrudNombre);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 8)));
        panelDerecha.add(crearLabel("Apellido *"));
        panelDerecha.add(txtCrudApellido);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 8)));
        panelDerecha.add(crearLabel("Cargo"));
        panelDerecha.add(txtCrudCargo);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 8)));
        panelDerecha.add(crearLabel("Tipo Contrato"));
        panelDerecha.add(cmbCrudTipoContrato);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 15)));
        panelDerecha.add(panelBotones);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 8)));
        panelDerecha.add(btnLimpiar);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 10)));
        panelDerecha.add(lblCrudResultado);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierda, panelDerecha);
        splitPane.setOpaque(false);
        splitPane.setDividerLocation(580);
        splitPane.setDividerSize(5);
        panel.add(splitPane, BorderLayout.CENTER);

        actualizarTablaEmpleados();
        return panel;
    }

    /**
     * Panel de consultas e informes mensuales/por fechas.
     */
    private JPanel crearPanelReportes() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel lblTitulo = new JLabel("📊 Módulo de Consultas e Informes Financieros");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(COLOR_TEXTO);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(lblTitulo, BorderLayout.NORTH);

        // JTabbedPane Premium
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(COLOR_PANEL);
        tabbedPane.setForeground(COLOR_TEXTO);

        tabbedPane.addTab("💼 Tiempo Completo", crearSubPanelReporteTC());
        tabbedPane.addTab("⏳ Tiempo Parcial", crearSubPanelReporteTP());
        tabbedPane.addTab("📅 Asistencias por Rango", crearSubPanelAsistenciasFechas());

        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearSubPanelReporteTC() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Buscador superior
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panelSuperior.setOpaque(false);

        JTextField txtCedulaReport = crearCampo("Ingresar Cédula");
        txtCedulaReport.setPreferredSize(new Dimension(150, 32));
        if (usuarioActual.getPerfil() == Usuario.Perfil.EMPLEADO) {
            txtCedulaReport.setText(usuarioActual.getCedula());
            txtCedulaReport.setEditable(false);
        }

        JComboBox<String> cmbMes = new JComboBox<>(new String[]{
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        });
        cmbMes.setBackground(COLOR_FONDO);
        cmbMes.setForeground(COLOR_TEXTO);

        JComboBox<String> cmbAnio = new JComboBox<>(new String[]{"2026", "2027", "2028"});
        cmbAnio.setBackground(COLOR_FONDO);
        cmbAnio.setForeground(COLOR_TEXTO);

        JButton btnGenerar = crearBotonAccion("🔍 Generar Reporte", COLOR_ACENTO);
        btnGenerar.setPreferredSize(new Dimension(160, 32));

        panelSuperior.add(crearLabel("Cédula:"));
        panelSuperior.add(txtCedulaReport);
        panelSuperior.add(crearLabel("Mes:"));
        panelSuperior.add(cmbMes);
        panelSuperior.add(crearLabel("Año:"));
        panelSuperior.add(cmbAnio);
        panelSuperior.add(btnGenerar);

        panel.add(panelSuperior, BorderLayout.NORTH);

        // Tabla de reporte diario
        String[] columnas = {"Fecha", "Ent. Mañana", "Sal. Mañana", "Ent. Tarde", "Sal. Tarde", "Atraso (min)", "Descuento ($)"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(model);
        tabla.setBackground(COLOR_FONDO);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setGridColor(COLOR_PANEL_CLARO);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setBackground(COLOR_PANEL_CLARO);
        tabla.getTableHeader().setForeground(COLOR_TEXTO);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(COLOR_FONDO);
        panel.add(scroll, BorderLayout.CENTER);

        // Panel inferior de métricas acumuladas
        JPanel panelMetricas = new JPanel(new GridLayout(1, 4, 15, 0));
        panelMetricas.setOpaque(false);
        panelMetricas.setPreferredSize(new Dimension(0, 80));

        JPanel cardBase = crearCardMini("Sueldo Base", "$1500.00", COLOR_ACENTO);
        JPanel cardAtraso = crearCardMini("Total Atrasos", "0 min", COLOR_WARNING);
        JPanel cardDescuento = crearCardMini("Descuentos", "$0.00", COLOR_ERROR);
        JPanel cardNeto = crearCardMini("Sueldo Neto a Recibir", "$1500.00", COLOR_EXITO);

        panelMetricas.add(cardBase);
        panelMetricas.add(cardAtraso);
        panelMetricas.add(cardDescuento);
        panelMetricas.add(cardNeto);

        panel.add(panelMetricas, BorderLayout.SOUTH);

        // Lógica de generación del reporte
        btnGenerar.addActionListener(e -> {
            String cedula = txtCedulaReport.getText().trim();
            if (cedula.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar una cédula", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null || emp.getTipoContrato() != Empleado.TipoContrato.TIEMPO_COMPLETO) {
                JOptionPane.showMessageDialog(this, "Empleado no encontrado o no es de Tiempo Completo", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int mesNum = cmbMes.getSelectedIndex() + 1;
            int anioNum = Integer.parseInt((String) cmbAnio.getSelectedItem());

            LocalDate inicioMes = LocalDate.of(anioNum, mesNum, 1);
            LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

            java.util.List<RegistroAsistencia> registros = new AsistenciaDAO().listarPorCedulaYRango(cedula, inicioMes, finMes);
            model.setRowCount(0);

            int totalAtraso = 0;
            for (RegistroAsistencia r : registros) {
                int atrasoDia = r.getMinutosAtraso();
                totalAtraso += atrasoDia;
                double descDia = atrasoDia * 0.20;

                model.addRow(new Object[]{
                    r.getFecha().toString(),
                    r.getHoraEntradaManana() != null ? r.getHoraEntradaManana().toString() : "—",
                    r.getHoraSalidaManana() != null ? r.getHoraSalidaManana().toString() : "—",
                    r.getHoraEntradaTarde() != null ? r.getHoraEntradaTarde().toString() : "—",
                    r.getHoraSalidaTarde() != null ? r.getHoraSalidaTarde().toString() : "—",
                    atrasoDia + " min",
                    String.format("$%.2f", descDia)
                });
            }

            double descTotal = totalAtraso * 0.20;
            double sueldoNeto = Math.max(1500.00 - descTotal, 0.00);

            ((JLabel) cardAtraso.getComponent(1)).setText(totalAtraso + " min");
            ((JLabel) cardDescuento.getComponent(1)).setText(String.format("$%.2f", descTotal));
            ((JLabel) cardNeto.getComponent(1)).setText(String.format("$%.2f", sueldoNeto));
        });

        return panel;
    }

    private JPanel crearSubPanelReporteTP() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Buscador superior
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panelSuperior.setOpaque(false);

        JTextField txtCedulaReport = crearCampo("Ingresar Cédula");
        txtCedulaReport.setPreferredSize(new Dimension(150, 32));
        if (usuarioActual.getPerfil() == Usuario.Perfil.EMPLEADO) {
            txtCedulaReport.setText(usuarioActual.getCedula());
            txtCedulaReport.setEditable(false);
        }

        JComboBox<String> cmbMes = new JComboBox<>(new String[]{
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        });
        cmbMes.setBackground(COLOR_FONDO);
        cmbMes.setForeground(COLOR_TEXTO);

        JComboBox<String> cmbAnio = new JComboBox<>(new String[]{"2026", "2027", "2028"});
        cmbAnio.setBackground(COLOR_FONDO);
        cmbAnio.setForeground(COLOR_TEXTO);

        JButton btnGenerar = crearBotonAccion("🔍 Generar Reporte", COLOR_ACENTO);
        btnGenerar.setPreferredSize(new Dimension(160, 32));

        panelSuperior.add(crearLabel("Cédula:"));
        panelSuperior.add(txtCedulaReport);
        panelSuperior.add(crearLabel("Mes:"));
        panelSuperior.add(cmbMes);
        panelSuperior.add(crearLabel("Año:"));
        panelSuperior.add(cmbAnio);
        panelSuperior.add(btnGenerar);

        panel.add(panelSuperior, BorderLayout.NORTH);

        // Tabla de reporte diario
        String[] columnas = {"Fecha", "Ent. Mañana", "Sal. Mañana", "Ent. Tarde", "Sal. Tarde", "Horas Laboradas"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(model);
        tabla.setBackground(COLOR_FONDO);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setGridColor(COLOR_PANEL_CLARO);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setBackground(COLOR_PANEL_CLARO);
        tabla.getTableHeader().setForeground(COLOR_TEXTO);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(COLOR_FONDO);
        panel.add(scroll, BorderLayout.CENTER);

        // Panel inferior de métricas acumuladas
        JPanel panelMetricas = new JPanel(new GridLayout(1, 3, 15, 0));
        panelMetricas.setOpaque(false);
        panelMetricas.setPreferredSize(new Dimension(0, 80));

        JPanel cardHoras = crearCardMini("Total Horas Trabajadas", "0.0 hrs", COLOR_ACENTO);
        JPanel cardTarifa = crearCardMini("Tarifa por Hora", "$5.00", COLOR_WARNING);
        JPanel cardNeto = crearCardMini("Sueldo a Recibir", "$0.00", COLOR_EXITO);

        panelMetricas.add(cardHoras);
        panelMetricas.add(cardTarifa);
        panelMetricas.add(cardNeto);

        panel.add(panelMetricas, BorderLayout.SOUTH);

        // Lógica de generación del reporte
        btnGenerar.addActionListener(e -> {
            String cedula = txtCedulaReport.getText().trim();
            if (cedula.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar una cédula", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null || emp.getTipoContrato() != Empleado.TipoContrato.TIEMPO_PARCIAL) {
                JOptionPane.showMessageDialog(this, "Empleado no encontrado o no es de Tiempo Parcial", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int mesNum = cmbMes.getSelectedIndex() + 1;
            int anioNum = Integer.parseInt((String) cmbAnio.getSelectedItem());

            LocalDate inicioMes = LocalDate.of(anioNum, mesNum, 1);
            LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

            java.util.List<RegistroAsistencia> registros = new AsistenciaDAO().listarPorCedulaYRango(cedula, inicioMes, finMes);
            model.setRowCount(0);

            double totalHoras = 0;
            for (RegistroAsistencia r : registros) {
                double horasDia = r.calcularHorasTrabajadas();
                totalHoras += horasDia;

                model.addRow(new Object[]{
                    r.getFecha().toString(),
                    r.getHoraEntradaManana() != null ? r.getHoraEntradaManana().toString() : "—",
                    r.getHoraSalidaManana() != null ? r.getHoraSalidaManana().toString() : "—",
                    r.getHoraEntradaTarde() != null ? r.getHoraEntradaTarde().toString() : "—",
                    r.getHoraSalidaTarde() != null ? r.getHoraSalidaTarde().toString() : "—",
                    String.format("%.2f hrs", horasDia)
                });
            }

            double sueldoTotal = totalHoras * 5.00;

            ((JLabel) cardHoras.getComponent(1)).setText(String.format("%.2f hrs", totalHoras));
            ((JLabel) cardNeto.getComponent(1)).setText(String.format("$%.2f", sueldoTotal));
        });

        return panel;
    }

    private JPanel crearSubPanelAsistenciasFechas() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Buscador superior
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panelSuperior.setOpaque(false);

        JTextField txtFechaInicio = crearCampo(LocalDate.now().minusDays(7).toString());
        txtFechaInicio.setPreferredSize(new Dimension(120, 32));

        JTextField txtFechaFin = crearCampo(LocalDate.now().toString());
        txtFechaFin.setPreferredSize(new Dimension(120, 32));

        JButton btnConsultar = crearBotonAccion("🔍 Consultar Rango", COLOR_ACENTO);
        btnConsultar.setPreferredSize(new Dimension(160, 32));

        panelSuperior.add(crearLabel("Fecha Inicio (YYYY-MM-DD):"));
        panelSuperior.add(txtFechaInicio);
        panelSuperior.add(crearLabel("Fecha Fin (YYYY-MM-DD):"));
        panelSuperior.add(txtFechaFin);
        panelSuperior.add(btnConsultar);

        panel.add(panelSuperior, BorderLayout.NORTH);

        // Tabla de asistencias
        String[] columnas = {"Empleado", "Cédula", "Fecha", "Ent. Mañana", "Sal. Mañana", "Ent. Tarde", "Sal. Tarde"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(model);
        tabla.setBackground(COLOR_FONDO);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setGridColor(COLOR_PANEL_CLARO);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setBackground(COLOR_PANEL_CLARO);
        tabla.getTableHeader().setForeground(COLOR_TEXTO);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(COLOR_FONDO);
        panel.add(scroll, BorderLayout.CENTER);

        btnConsultar.addActionListener(e -> {
            try {
                LocalDate desde = LocalDate.parse(txtFechaInicio.getText().trim());
                LocalDate hasta = LocalDate.parse(txtFechaFin.getText().trim());

                AsistenciaDAO asistDAO = new AsistenciaDAO();
                EmpleadoDAO empDAO = new EmpleadoDAO();

                java.util.List<RegistroAsistencia> registros;
                if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
                    registros = asistDAO.listarTodosPorRango(desde, hasta);
                } else {
                    registros = asistDAO.listarPorCedulaYRango(usuarioActual.getCedula(), desde, hasta);
                }

                model.setRowCount(0);

                for (RegistroAsistencia r : registros) {
                    Empleado emp = empDAO.buscarPorCedula(r.getCedulaEmpleado());
                    String nombreEmpleado = emp != null ? emp.getNombreCompleto() : "Desconocido";

                    model.addRow(new Object[]{
                        nombreEmpleado,
                        r.getCedulaEmpleado(),
                        r.getFecha().toString(),
                        r.getHoraEntradaManana() != null ? r.getHoraEntradaManana().toString() : "—",
                        r.getHoraSalidaManana() != null ? r.getHoraSalidaManana().toString() : "—",
                        r.getHoraEntradaTarde() != null ? r.getHoraEntradaTarde().toString() : "—",
                        r.getHoraSalidaTarde() != null ? r.getHoraSalidaTarde().toString() : "—"
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Utilice AAAA-MM-DD", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // =============================================
    // CRUD Lógica y Auxiliares
    // =============================================

    private void actualizarTablaEmpleados() {
        if (tablaEmpleados == null) return;
        DefaultTableModel model = (DefaultTableModel) tablaEmpleados.getModel();
        model.setRowCount(0);

        EmpleadoDAO dao = new EmpleadoDAO();
        java.util.List<Empleado> lista = dao.listarTodos();
        for (Empleado e : lista) {
            model.addRow(new Object[]{
                e.getCedula(),
                e.getNombre(),
                e.getApellido(),
                e.getCargo(),
                e.getTipoContrato().name()
            });
        }
    }

    private void filtrarTablaEmpleados(String texto) {
        DefaultTableModel model = (DefaultTableModel) tablaEmpleados.getModel();
        model.setRowCount(0);

        EmpleadoDAO dao = new EmpleadoDAO();
        java.util.List<Empleado> lista = dao.listarTodos();
        for (Empleado e : lista) {
            if (e.getCedula().contains(texto) || e.getNombre().toLowerCase().contains(texto.toLowerCase()) || e.getApellido().toLowerCase().contains(texto.toLowerCase())) {
                model.addRow(new Object[]{
                    e.getCedula(),
                    e.getNombre(),
                    e.getApellido(),
                    e.getCargo(),
                    e.getTipoContrato().name()
                });
            }
        }
    }

    private void crudRegistrar() {
        String cedula = txtCrudCedula.getText().trim();
        String nombre = txtCrudNombre.getText().trim();
        String apellido = txtCrudApellido.getText().trim();
        String cargo = txtCrudCargo.getText().trim();

        if (cedula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
            lblCrudResultado.setText("❌ Complete cédula, nombre y apellido.");
            lblCrudResultado.setForeground(COLOR_ERROR);
            return;
        }

        Empleado.TipoContrato tipo = Empleado.TipoContrato.valueOf((String) cmbCrudTipoContrato.getSelectedItem());
        Empleado emp;
        if (tipo == Empleado.TipoContrato.TIEMPO_COMPLETO) {
            emp = new EmpleadoTiempoCompleto(cedula, nombre, apellido, cargo);
        } else {
            emp = new EmpleadoTiempoParcial(cedula, nombre, apellido, cargo);
        }

        EmpleadoDAO dao = new EmpleadoDAO();
        if (dao.insertar(emp)) {
            // También crear un usuario para que pueda iniciar sesión
            Usuario user = new Usuario(cedula, nombre + " " + apellido, "clave123", Usuario.Perfil.EMPLEADO);
            new UsuarioDAO().insertar(user);

            lblCrudResultado.setText("✅ Registrado exitosamente (Usuario: " + cedula + ", Clave: clave123)");
            lblCrudResultado.setForeground(COLOR_EXITO);
            limpiarCamposCrud();
            actualizarTablaEmpleados();
        } else {
            lblCrudResultado.setText("❌ Error. ¿Cédula duplicada?");
            lblCrudResultado.setForeground(COLOR_ERROR);
        }
    }

    private void crudModificar() {
        String cedula = txtCrudCedula.getText().trim();
        String nombre = txtCrudNombre.getText().trim();
        String apellido = txtCrudApellido.getText().trim();
        String cargo = txtCrudCargo.getText().trim();

        if (cedula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
            lblCrudResultado.setText("❌ Seleccione un empleado y complete campos.");
            lblCrudResultado.setForeground(COLOR_ERROR);
            return;
        }

        Empleado.TipoContrato tipo = Empleado.TipoContrato.valueOf((String) cmbCrudTipoContrato.getSelectedItem());
        Empleado emp;
        if (tipo == Empleado.TipoContrato.TIEMPO_COMPLETO) {
            emp = new EmpleadoTiempoCompleto(cedula, nombre, apellido, cargo);
        } else {
            emp = new EmpleadoTiempoParcial(cedula, nombre, apellido, cargo);
        }

        EmpleadoDAO dao = new EmpleadoDAO();
        if (dao.modificar(emp)) {
            lblCrudResultado.setText("✅ Modificado correctamente.");
            lblCrudResultado.setForeground(COLOR_EXITO);
            limpiarCamposCrud();
            actualizarTablaEmpleados();
        } else {
            lblCrudResultado.setText("❌ Error al modificar.");
            lblCrudResultado.setForeground(COLOR_ERROR);
        }
    }

    private void crudEliminar() {
        String cedula = txtCrudCedula.getText().trim();
        if (cedula.isEmpty()) {
            lblCrudResultado.setText("❌ Seleccione empleado a eliminar.");
            lblCrudResultado.setForeground(COLOR_ERROR);
            return;
        }

        int op = JOptionPane.showConfirmDialog(this, "¿Está seguro de eliminar a este empleado?", "Eliminar", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.YES_OPTION) {
            EmpleadoDAO dao = new EmpleadoDAO();
            if (dao.eliminar(cedula)) {
                lblCrudResultado.setText("✅ Empleado dado de baja.");
                lblCrudResultado.setForeground(COLOR_EXITO);
                limpiarCamposCrud();
                actualizarTablaEmpleados();
            } else {
                lblCrudResultado.setText("❌ Error al eliminar.");
                lblCrudResultado.setForeground(COLOR_ERROR);
            }
        }
    }

    private void limpiarCamposCrud() {
        txtCrudCedula.setText("");
        txtCrudCedula.setEditable(true);
        txtCrudNombre.setText("");
        txtCrudApellido.setText("");
        txtCrudCargo.setText("");
        cmbCrudTipoContrato.setSelectedIndex(0);
    }

    // =============================================
    // Acciones Lector y Asistencia
    // =============================================

    private void abrirRegistroHuella() {
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
        
        // Si es Admin, puede marcar asistencia para sí mismo o para un empleado ingresando su cédula
        if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            String inputCedula = JOptionPane.showInputDialog(this, "Ingrese la cédula del empleado para registrar asistencia:", cedula);
            if (inputCedula == null || inputCedula.trim().isEmpty()) return;
            cedula = inputCedula.trim();
        }

        // Verificar que el empleado existe
        Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
        if (emp == null) {
            JOptionPane.showMessageDialog(this, "Empleado con cédula " + cedula + " no registrado o inactivo.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalTime ahora = LocalTime.now();
        LocalDate hoy = LocalDate.now();

        AsistenciaDAO asistDAO = new AsistenciaDAO();
        RegistroAsistencia reg = asistDAO.buscarPorCedulaYFecha(cedula, hoy);

        if (reg == null) {
            reg = new RegistroAsistencia(cedula);
        }

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
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValor.setForeground(color);
        lblValor.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblIcono);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(lblValor);

        return card;
    }

    private JPanel crearCardMini(String titulo, String valor, Color colorValor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(COLOR_FONDO);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2d.setColor(COLOR_PANEL_CLARO);
                g2d.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTit.setForeground(COLOR_TEXTO_SEC);
        lblTit.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblVal = new JLabel(valor);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblVal.setForeground(colorValor);
        lblVal.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTit);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(lblVal);

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
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2d.setColor(COLOR_TEXTO);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(200, 36));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
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
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXTO_SEC);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField crearCampo(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(COLOR_TEXTO);
        field.setBackground(COLOR_FONDO);
        field.setCaretColor(COLOR_TEXTO);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(71, 85, 105), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }
}
