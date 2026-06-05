import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * SGTA - Sistema de Gestión de Tareas Académicas
 * Versión consola que implementa los conceptos de calidad del Proyecto 2:
 * - Ciclo PHVA (Planificar, Hacer, Verificar, Actuar) simulado
 * - Gestión de configuración (cada acción se asocia a un Issue)
 * - Validación de reglas de negocio (fecha futura, campos obligatorios)
 * - Métricas básicas (se muestran al salir)
 */
public class SGTA {

    // ------------------ MODELOS ------------------
    static class Usuario {
        String email;
        String password;
        List<Tarea> tareas;

        Usuario(String email, String password) {
            this.email = email;
            this.password = password;
            this.tareas = new ArrayList<>();
        }
    }

    static class Tarea {
        String id;
        String titulo;
        String descripcion;
        String prioridad; // "alta", "media", "baja"
        LocalDate fechaLimite;
        boolean completada;

        Tarea(String titulo, String descripcion, String prioridad, LocalDate fechaLimite) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.prioridad = prioridad;
            this.fechaLimite = fechaLimite;
            this.completada = false;
        }
    }

    // ------------------ "BASE DE DATOS" EN MEMORIA ------------------
    private static List<Usuario> usuarios = new ArrayList<>();
    private static Usuario usuarioActual = null;

    // ------------------ UTILIDADES ------------------
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static LocalDate fechaActual() {
        return LocalDate.now();
    }

    private static boolean esFechaFutura(LocalDate fecha) {
        return !fecha.isBefore(fechaActual());
    }

    private static void pausa() {
        System.out.print("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }

    private static void mostrarExito(String mensaje) {
        System.out.println("✅ " + mensaje);
    }

    private static void mostrarError(String mensaje) {
        System.out.println("❌ " + mensaje);
    }

    // ------------------ SERVICIOS DE USUARIO ------------------
    private static void registrarUsuario() {
        System.out.print("Correo electrónico: ");
        String email = scanner.nextLine().trim();
        if (buscarUsuarioPorEmail(email) != null) {
            mostrarError("El correo ya está registrado. (Issue #004 - mejora pendiente)");
            return;
        }
        System.out.print("Contraseña: ");
        String pass = scanner.nextLine();
        System.out.print("Confirmar contraseña: ");
        String confirm = scanner.nextLine();
        if (!pass.equals(confirm)) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }
        Usuario nuevo = new Usuario(email, pass);
        usuarios.add(nuevo);
        mostrarExito("Registro exitoso. Ahora inicia sesión. (#001 cerrado - mensaje de éxito)");
    }

    private static Usuario buscarUsuarioPorEmail(String email) {
        for (Usuario u : usuarios) {
            if (u.email.equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    private static void iniciarSesion() {
        System.out.print("Correo: ");
        String email = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String pass = scanner.nextLine();
        Usuario u = buscarUsuarioPorEmail(email);
        if (u == null || !u.password.equals(pass)) {
            mostrarError("Correo o contraseña incorrectos. (#001 - mensaje de error implementado)");
            return;
        }
        usuarioActual = u;
        mostrarExito("Bienvenido " + u.email + " (#001 cerrado - login correcto)");
    }

    // ------------------ CRUD DE TAREAS ------------------
    private static void agregarTarea() {
        System.out.print("Título (obligatorio): ");
        String titulo = scanner.nextLine().trim();
        if (titulo.isEmpty()) {
            mostrarError("El título es obligatorio. (CP-06 validado)");
            return;
        }
        System.out.print("Descripción (opcional): ");
        String desc = scanner.nextLine();
        System.out.print("Prioridad (alta/media/baja): ");
        String prioridad = scanner.nextLine().toLowerCase();
        if (!prioridad.equals("alta") && !prioridad.equals("media") && !prioridad.equals("baja")) {
            prioridad = "media";
            System.out.println("Prioridad no reconocida, se asigna 'media'.");
        }
        System.out.print("Fecha límite (YYYY-MM-DD) o Enter para omitir: ");
        String fechaStr = scanner.nextLine().trim();
        LocalDate fecha = null;
        if (!fechaStr.isEmpty()) {
            try {
                fecha = LocalDate.parse(fechaStr, dateFormatter);
                if (!esFechaFutura(fecha)) {
                    mostrarError("La fecha límite no puede ser anterior a hoy. (#002 - validación implementada)");
                    return;
                }
            } catch (Exception e) {
                mostrarError("Formato de fecha inválido. Use YYYY-MM-DD");
                return;
            }
        }
        Tarea t = new Tarea(titulo, desc, prioridad, fecha);
        usuarioActual.tareas.add(t);
        mostrarExito("Tarea agregada con ID: " + t.id + " (CP-05, CP-07 superados)");
    }

    private static void listarTareas() {
        if (usuarioActual.tareas.isEmpty()) {
            System.out.println("📭 No hay tareas. Agrega una nueva.");
            return;
        }
        System.out.print("Filtrar por prioridad? (alta/media/baja/todas): ");
        String filtro = scanner.nextLine().toLowerCase();
        List<Tarea> filtradas = new ArrayList<>();
        for (Tarea t : usuarioActual.tareas) {
            if (filtro.equals("todas") || t.prioridad.equals(filtro)) {
                filtradas.add(t);
            }
        }
        if (filtradas.isEmpty()) {
            System.out.println("No hay tareas con esa prioridad.");
            return;
        }
        System.out.println("\n=== LISTA DE TAREAS ===");
        for (Tarea t : filtradas) {
            String estado = t.completada ? "[✔] COMPLETADA" : "[ ] PENDIENTE";
            String fechaStr = (t.fechaLimite == null) ? "Sin fecha" : t.fechaLimite.format(dateFormatter);
            System.out.printf("%s | %s | %s | Prioridad: %s | Vence: %s%n",
                    t.id, estado, t.titulo, t.prioridad, fechaStr);
            if (!t.descripcion.isEmpty()) System.out.println("     📝 " + t.descripcion);
        }
    }

    private static Tarea buscarTareaPorId(String id) {
        for (Tarea t : usuarioActual.tareas) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    private static void marcarCompletada() {
        System.out.print("ID de la tarea a marcar como completada: ");
        String id = scanner.nextLine();
        Tarea t = buscarTareaPorId(id);
        if (t == null) {
            mostrarError("Tarea no encontrada.");
            return;
        }
        if (t.completada) {
            System.out.println("Ya estaba completada.");
        } else {
            t.completada = true;
            mostrarExito("Tarea completada. (CP-10 superado)");
        }
    }

    private static void editarTarea() {
        System.out.print("ID de la tarea a editar: ");
        String id = scanner.nextLine();
        Tarea t = buscarTareaPorId(id);
        if (t == null) {
            mostrarError("Tarea no encontrada.");
            return;
        }
        System.out.print("Nuevo título (dejar vacío para no cambiar): ");
        String nuevoTitulo = scanner.nextLine();
        if (!nuevoTitulo.isBlank()) t.titulo = nuevoTitulo;
        System.out.print("Nueva descripción: ");
        String nuevaDesc = scanner.nextLine();
        if (!nuevaDesc.isBlank()) t.descripcion = nuevaDesc;
        System.out.print("Nueva prioridad (alta/media/baja): ");
        String nuevaPri = scanner.nextLine().toLowerCase();
        if (nuevaPri.matches("alta|media|baja")) t.prioridad = nuevaPri;
        System.out.print("Nueva fecha límite (YYYY-MM-DD) o Enter: ");
        String nuevaFechaStr = scanner.nextLine().trim();
        if (!nuevaFechaStr.isEmpty()) {
            try {
                LocalDate nuevaFecha = LocalDate.parse(nuevaFechaStr, dateFormatter);
                if (!esFechaFutura(nuevaFecha)) {
                    mostrarError("Fecha no válida (no puede ser anterior a hoy). Se mantiene la anterior.");
                } else {
                    t.fechaLimite = nuevaFecha;
                }
            } catch (Exception e) {
                mostrarError("Formato inválido, fecha no modificada.");
            }
        }
        mostrarExito("Tarea actualizada. (Issue #004 - mejora UX aplicada)");
    }

    private static void eliminarTarea() {
        System.out.print("ID de la tarea a eliminar: ");
        String id = scanner.nextLine();
        Tarea t = buscarTareaPorId(id);
        if (t == null) {
            mostrarError("Tarea no encontrada.");
            return;
        }
        System.out.print("¿Confirmar eliminación? (s/n): ");
        String confirm = scanner.nextLine();
        if (confirm.equalsIgnoreCase("s")) {
            usuarioActual.tareas.remove(t);
            mostrarExito("Tarea eliminada. (CP-09 - diálogo de confirmación)");
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    // ------------------ MÉTRICAS Y REPORTE FINAL ------------------
    private static void mostrarMetricas() {
        if (usuarioActual == null) return;
        int total = usuarioActual.tareas.size();
        long completadas = usuarioActual.tareas.stream().filter(t -> t.completada).count();
        double porcentajeExito = (total == 0) ? 100 : (completadas * 100.0 / total);
        System.out.println("\n===== MÉTRICAS DE CALIDAD =====");
        System.out.println("M01 - % tareas completadas: " + String.format("%.1f%%", porcentajeExito) + " (meta ≥ 95%)");
        System.out.println("M05 - Seguridad: usuarios con contraseña (simulado) - 0 brechas");
        System.out.println("M07 - Usabilidad: se implementaron mensajes claros (#001) y validación de fecha (#002)");
        System.out.println("=================================");
    }

    // ------------------ MENÚ PRINCIPAL ------------------
    private static void menuNoLogeado() {
        while (true) {
            System.out.println("\n=== SGTA - SISTEMA DE TAREAS ACADÉMICAS ===");
            System.out.println("1. Iniciar sesión");
            System.out.println("2. Registrar nuevo usuario");
            System.out.println("3. Salir");
            System.out.print("Opción: ");
            String op = scanner.nextLine();
            switch (op) {
                case "1": iniciarSesion(); if (usuarioActual != null) return; break;
                case "2": registrarUsuario(); break;
                case "3": System.out.println("¡Hasta luego!"); System.exit(0);
                default: mostrarError("Opción inválida.");
            }
        }
    }

    private static void menuLogeado() {
        while (true) {
            System.out.println("\n=== MENÚ PRINCIPAL (Usuario: " + usuarioActual.email + ") ===");
            System.out.println("1. Agregar tarea");
            System.out.println("2. Listar tareas (con filtro)");
            System.out.println("3. Marcar tarea como completada");
            System.out.println("4. Editar tarea");
            System.out.println("5. Eliminar tarea");
            System.out.println("6. Mostrar métricas de calidad");
            System.out.println("7. Cerrar sesión");
            System.out.print("Opción: ");
            String op = scanner.nextLine();
            switch (op) {
                case "1": agregarTarea(); break;
                case "2": listarTareas(); break;
                case "3": marcarCompletada(); break;
                case "4": editarTarea(); break;
                case "5": eliminarTarea(); break;
                case "6": mostrarMetricas(); break;
                case "7": usuarioActual = null; mostrarExito("Sesión cerrada."); return;
                default: mostrarError("Opción no válida.");
            }
            pausa();
        }
    }

    // ------------------ MAIN (PUNTO DE ENTRADA) ------------------
    public static void main(String[] args) {
        System.out.println("=== BIENVENIDO AL SISTEMA DE GESTIÓN DE TAREAS ACADÉMICAS (SGTA) ===");
        System.out.println("Proyecto 2 - Ciclo PHVA y mejora continua (simulación)");
        // Opcional: precargar un usuario de demostración
        if (usuarios.isEmpty()) {
            Usuario demo = new Usuario("demo@ues.edu", "123");
            demo.tareas.add(new Tarea("Estudiar ISO 25010", "Revisar métricas", "alta", LocalDate.now().plusDays(3)));
            demo.tareas.add(new Tarea("Practicar GitFlow", "Simular ramas feature", "media", LocalDate.now().plusDays(5)));
            usuarios.add(demo);
        }
        while (true) {
            menuNoLogeado();
            while (usuarioActual != null) {
                menuLogeado();
            }
        }
    }
}