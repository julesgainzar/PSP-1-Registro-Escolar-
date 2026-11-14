package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lee y escribe el RegistroEscolar en la base GESTIONESCOLAR.db.
 *
 * Equivalencias entre la app y la base:
 *   Estudiantes / Profesores  ->  tablas estudiante / profesor
 *   Cursos (nombre + periodo) ->  un grupo (curso + periodo + profesor)
 *   horario (texto)           ->  filas de la tabla horario (un día por fila)
 *   aula (texto)              ->  tabla aula
 *   estudiantes del curso     ->  tabla inscripcion
 *
 * Las bajas de estudiantes y profesores son lógicas (estado = 'INACTIVO'),
 * así no se pierde el historial. Todo guardado ocurre en una transacción.
 */
public class RegistroDAO {

    /** grupo (id) de cada Cursos cargado o guardado, para detectar cambios de nombre. */
    private final Map<Cursos, Integer> idGrupo = new IdentityHashMap<>();

    // ------------------------------------------------------------------
    // CARGAR
    // ------------------------------------------------------------------

    public RegistroEscolar cargar() throws SQLException {
        RegistroEscolar r = new RegistroEscolar();
        idGrupo.clear();

        try (Connection cn = Conexion.abrir()) {
            asegurarColumnaSemestre(cn);
            Map<Integer, Estudiantes> estudiantes = new HashMap<>();
            Map<Integer, Profesores> profesores = new HashMap<>();
            Map<Integer, Cursos> grupos = new HashMap<>();

            try (Statement st = cn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT id_estudiante, matricula, nombre, edad, "
                            + "correo, telefono, direccion FROM estudiante "
                            + "WHERE estado = 'ACTIVO' ORDER BY id_estudiante")) {
                while (rs.next()) {
                    Estudiantes e = new Estudiantes(rs.getString(2), rs.getString(3),
                            rs.getInt(4), txt(rs, 5), txt(rs, 6), txt(rs, 7));
                    estudiantes.put(rs.getInt(1), e);
                    r.getEstudiantes().add(e);
                }
            }

            try (Statement st = cn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT id_profesor, numero_empleado, nombre, edad, "
                            + "correo, telefono, direccion FROM profesor "
                            + "WHERE estado = 'ACTIVO' ORDER BY id_profesor")) {
                while (rs.next()) {
                    Profesores p = new Profesores(rs.getString(2), rs.getString(3),
                            rs.getInt(4), txt(rs, 5), txt(rs, 6), txt(rs, 7));
                    profesores.put(rs.getInt(1), p);
                    r.getProfesores().add(p);
                }
            }

            try (Statement st = cn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT g.id_grupo, c.nombre, p.nombre, g.id_profesor, g.semestre "
                            + "FROM grupo g "
                            + "JOIN curso c ON c.id_curso = g.id_curso "
                            + "JOIN periodo p ON p.id_periodo = g.id_periodo "
                            + "WHERE c.estado = 'ACTIVO' ORDER BY g.id_grupo")) {
                while (rs.next()) {
                    int g = rs.getInt(1);
                    String nombre = rs.getString(2);
                    String periodo = rs.getString(3);
                    if (r.buscarCurso(nombre, periodo) != null) {
                        System.err.println("Grupo " + g + " omitido: ya hay un curso "
                                + nombre + " en el periodo " + periodo);
                        continue;
                    }
                    String[] ha = textoHorario(cn, g);
                    Cursos c = new Cursos(nombre, periodo, ha[0], ha[1]);
                    c.setSemestre(rs.getInt(5));
                    int idProf = rs.getInt(4);
                    if (!rs.wasNull()) {
                        c.asignarProfesor(profesores.get(idProf));
                    }
                    r.getCursos().add(c);
                    grupos.put(g, c);
                    idGrupo.put(c, g);
                }
            }

            try (Statement st = cn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT id_grupo, id_estudiante FROM inscripcion "
                            + "WHERE estado = 'ACTIVA' ORDER BY id_inscripcion")) {
                while (rs.next()) {
                    Cursos c = grupos.get(rs.getInt(1));
                    Estudiantes e = estudiantes.get(rs.getInt(2));
                    if (c != null && e != null) {
                        c.inscribirEstudiante(e);
                    }
                }
            }
        }
        return r;
    }

    /** Devuelve {horario en texto, aulas en texto} de un grupo. */
    private String[] textoHorario(Connection cn, int grupo) throws SQLException {
        List<HorarioTexto.Franja> franjas = new ArrayList<>();
        Set<String> aulas = new LinkedHashSet<>();
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT h.dia, h.hora_inicio, h.hora_fin, a.nombre FROM horario h "
                + "JOIN aula a ON a.id_aula = h.id_aula WHERE h.id_grupo = ? ORDER BY h.id_horario")) {
            ps.setInt(1, grupo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    franjas.add(new HorarioTexto.Franja(rs.getString(1), rs.getString(2), rs.getString(3)));
                    aulas.add(rs.getString(4));
                }
            }
        }
        return new String[]{HorarioTexto.formatear(franjas), String.join(", ", aulas)};
    }

    // ------------------------------------------------------------------
    // GUARDAR
    // ------------------------------------------------------------------

    public void guardar(RegistroEscolar r) throws SQLException {
        try (Connection cn = Conexion.abrir()) {
            cn.setAutoCommit(false);
            try {
                Map<String, Integer> idEst = sincronizarPersonas(cn, "estudiante", "id_estudiante",
                        "matricula", personasE(r));
                Map<String, Integer> idProf = sincronizarPersonas(cn, "profesor", "id_profesor",
                        "numero_empleado", personasP(r));
                Map<Cursos, Integer> nuevos = sincronizarCursos(cn, r, idEst, idProf);
                cn.commit();
                idGrupo.clear();
                idGrupo.putAll(nuevos);
            } catch (SQLException | RuntimeException ex) {
                cn.rollback();
                throw ex;
            }
        }
    }

    private static Map<String, Persona> personasE(RegistroEscolar r) {
        Map<String, Persona> m = new java.util.LinkedHashMap<>();
        for (Estudiantes e : r.getEstudiantes()) {
            m.put(e.getMatricula(), e);
        }
        return m;
    }

    private static Map<String, Persona> personasP(RegistroEscolar r) {
        Map<String, Persona> m = new java.util.LinkedHashMap<>();
        for (Profesores p : r.getProfesores()) {
            m.put(p.getNumEmpleado(), p);
        }
        return m;
    }

    /**
     * Inserta o actualiza cada persona y da de baja (INACTIVO) a las que ya no están.
     * Devuelve clave (en minúsculas) -> id.
     */
    private Map<String, Integer> sincronizarPersonas(Connection cn, String tabla, String colId,
            String colClave, Map<String, Persona> personas) throws SQLException {

        Map<String, Integer> ids = new HashMap<>();
        for (Map.Entry<String, Persona> en : personas.entrySet()) {
            String clave = en.getKey();
            Persona p = en.getValue();

            Integer id = null;
            try (PreparedStatement ps = cn.prepareStatement("SELECT " + colId + " FROM " + tabla
                    + " WHERE " + colClave + " = ? COLLATE NOCASE")) {
                ps.setString(1, clave);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                }
            }

            if (id != null) {
                try (PreparedStatement ps = cn.prepareStatement("UPDATE " + tabla
                        + " SET nombre=?, edad=?, correo=?, telefono=?, direccion=?, estado='ACTIVO' "
                        + "WHERE " + colId + "=?")) {
                    llenar(ps, p);
                    ps.setInt(6, id);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = cn.prepareStatement("INSERT INTO " + tabla
                        + " (nombre, edad, correo, telefono, direccion, estado, " + colClave
                        + ") VALUES (?,?,?,?,?,'ACTIVO',?)", Statement.RETURN_GENERATED_KEYS)) {
                    llenar(ps, p);
                    ps.setString(6, clave);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        id = k.getInt(1);
                    }
                }
            }
            ids.put(clave.toLowerCase(), id);
        }

        try (Statement st = cn.createStatement()) {
            st.executeUpdate("UPDATE " + tabla + " SET estado='INACTIVO' WHERE estado='ACTIVO'"
                    + fueraDe(colId, ids.values()));
        }
        return ids;
    }

    private static void llenar(PreparedStatement ps, Persona p) throws SQLException {
        ps.setString(1, p.getNombre());
        ps.setInt(2, p.getEdad());
        ps.setString(3, p.getCorreo());
        ps.setString(4, p.getTelefono());
        ps.setString(5, p.getDireccion());
    }

    private static String fueraDe(String col, java.util.Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" AND " + col + " NOT IN (");
        boolean primero = true;
        for (Integer i : ids) {
            sb.append(primero ? "" : ",").append(i.intValue());
            primero = false;
        }
        return sb.append(")").toString();
    }

    private Map<Cursos, Integer> sincronizarCursos(Connection cn, RegistroEscolar r,
            Map<String, Integer> idEst, Map<String, Integer> idProf) throws SQLException {

        Map<Cursos, Integer> nuevos = new IdentityHashMap<>();
        Set<Integer> conocidos = new HashSet<>(idGrupo.values());
        Set<Integer> reclamados = new HashSet<>();

        for (Cursos c : r.getCursos()) {
            int periodo = idPeriodo(cn, c.getAnioSemestre().trim());
            int curso = idCurso(cn, c.getNombreCurso().trim());

            Integer g = idGrupo.get(c);
            if (g == null || reclamados.contains(g)) {
                g = grupoExistente(cn, curso, periodo, conocidos, reclamados);
            }

            Integer prof = null;
            if (c.getProfesorAsignado() != null) {
                prof = idProf.get(c.getProfesorAsignado().getNumEmpleado().toLowerCase());
            }

            if (g == null) {
                int n = 1;
                try (PreparedStatement ps = cn.prepareStatement(
                        "SELECT COUNT(*) FROM grupo WHERE id_curso=? AND id_periodo=?")) {
                    ps.setInt(1, curso);
                    ps.setInt(2, periodo);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        n = rs.getInt(1) + 1;
                    }
                }
                try (PreparedStatement ps = cn.prepareStatement("INSERT INTO grupo "
                        + "(id_curso, id_periodo, id_profesor, numero_grupo, semestre) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, curso);
                    ps.setInt(2, periodo);
                    asignar(ps, 3, prof);
                    ps.setString(4, String.valueOf(curso * 100 + n));
                    asignar(ps, 5, semestreODefault(c));
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        g = k.getInt(1);
                    }
                }
            } else {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE grupo SET id_curso=?, id_periodo=?, id_profesor=?, semestre=? WHERE id_grupo=?")) {
                    ps.setInt(1, curso);
                    ps.setInt(2, periodo);
                    asignar(ps, 3, prof);
                    asignar(ps, 4, semestreODefault(c));
                    ps.setInt(5, g);
                    ps.executeUpdate();
                }
            }

            reclamados.add(g);
            nuevos.put(c, g);
            sincronizarHorario(cn, g, c);
            sincronizarInscripciones(cn, g, c, idEst);
        }

        // Cursos que la app tenía y el usuario eliminó.
        for (Integer g : conocidos) {
            if (!reclamados.contains(g)) {
                for (String tabla : new String[]{"horario", "inscripcion"}) {
                    try (PreparedStatement ps = cn.prepareStatement(
                            "DELETE FROM " + tabla + " WHERE id_grupo=?")) {
                        ps.setInt(1, g);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = cn.prepareStatement("DELETE FROM grupo WHERE id_grupo=?")) {
                    ps.setInt(1, g);
                    ps.executeUpdate();
                }
            }
        }
        return nuevos;
    }

    /** Semestre a guardar: null (sin semestre) cuando vale 0. */
    private static Integer semestreODefault(Cursos c) {
        return c.getSemestre() == 0 ? null : c.getSemestre();
    }

    /** Agrega la columna grupo.semestre si la base es anterior a esta versión. */
    private static void asegurarColumnaSemestre(Connection cn) throws SQLException {
        boolean existe = false;
        try (Statement st = cn.createStatement();
                ResultSet rs = st.executeQuery("PRAGMA table_info(grupo)")) {
            while (rs.next()) {
                if ("semestre".equalsIgnoreCase(rs.getString("name"))) {
                    existe = true;
                }
            }
        }
        if (!existe) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("ALTER TABLE grupo ADD COLUMN semestre INTEGER");
            }
        }
    }

    private static void asignar(PreparedStatement ps, int pos, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(pos, java.sql.Types.INTEGER);
        } else {
            ps.setInt(pos, valor);
        }
    }

    private Integer grupoExistente(Connection cn, int curso, int periodo,
            Set<Integer> conocidos, Set<Integer> reclamados) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_grupo FROM grupo WHERE id_curso=? AND id_periodo=? ORDER BY id_grupo")) {
            ps.setInt(1, curso);
            ps.setInt(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int g = rs.getInt(1);
                    if (conocidos.contains(g) && !reclamados.contains(g)) {
                        return g;
                    }
                }
            }
        }
        return null;
    }

    private void sincronizarHorario(Connection cn, int g, Cursos c) throws SQLException {
        String horario = c.getHorario() == null ? "" : c.getHorario().trim();
        String aula = c.getAula() == null ? "" : c.getAula().trim();

        String[] actual = textoHorario(cn, g);
        if (horario.equals(actual[0]) && aula.equals(actual[1])) {
            return; // sin cambios
        }

        List<HorarioTexto.Franja> franjas = HorarioTexto.parsear(horario);
        if (franjas == null || aula.isEmpty()) {
            throw new SQLException("Horario o aula no válidos en el curso " + c.getNombreCurso()
                    + ". Formato de horario: Lun y Mié 10:00-12:00");
        }
        int idAula = idAula(cn, aula);

        try (PreparedStatement ps = cn.prepareStatement("DELETE FROM horario WHERE id_grupo=?")) {
            ps.setInt(1, g);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO horario "
                + "(id_grupo, id_aula, dia, hora_inicio, hora_fin) VALUES (?,?,?,?,?)")) {
            for (HorarioTexto.Franja f : franjas) {
                ps.setInt(1, g);
                ps.setInt(2, idAula);
                ps.setString(3, f.dia);
                ps.setString(4, f.inicio);
                ps.setString(5, f.fin);
                ps.executeUpdate();
            }
        }
    }

    private void sincronizarInscripciones(Connection cn, int g, Cursos c,
            Map<String, Integer> idEst) throws SQLException {

        Set<Integer> pendientes = new LinkedHashSet<>();
        for (Estudiantes e : c.getEstudiantes()) {
            Integer id = idEst.get(e.getMatricula().toLowerCase());
            if (id != null) {
                pendientes.add(id);
            }
        }
        String hoy = LocalDate.now().toString();

        Map<Integer, String> existentes = new HashMap<>();
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_estudiante, estado FROM inscripcion WHERE id_grupo=?")) {
            ps.setInt(1, g);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existentes.put(rs.getInt(1), rs.getString(2));
                }
            }
        }

        try (PreparedStatement upd = cn.prepareStatement(
                "UPDATE inscripcion SET estado=?, fecha_inscripcion=COALESCE(?, fecha_inscripcion) "
                + "WHERE id_grupo=? AND id_estudiante=?")) {
            for (Map.Entry<Integer, String> en : existentes.entrySet()) {
                boolean deseado = pendientes.remove(en.getKey());
                String estado = en.getValue();
                if (deseado && !"ACTIVA".equals(estado)) {
                    upd.setString(1, "ACTIVA");
                    upd.setString(2, hoy);
                } else if (!deseado && "ACTIVA".equals(estado)) {
                    upd.setString(1, "BAJA");
                    upd.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    continue;
                }
                upd.setInt(3, g);
                upd.setInt(4, en.getKey());
                upd.executeUpdate();
            }
        }

        try (PreparedStatement ins = cn.prepareStatement("INSERT INTO inscripcion "
                + "(id_estudiante, id_grupo, fecha_inscripcion, estado) VALUES (?,?,?, 'ACTIVA')")) {
            for (Integer id : pendientes) {
                ins.setInt(1, id);
                ins.setInt(2, g);
                ins.setString(3, hoy);
                ins.executeUpdate();
            }
        }
    }

    // ------------------------------------------------------------------
    // Catálogos: se busca por nombre y, si no existe, se crea.
    // ------------------------------------------------------------------

    private int idPeriodo(Connection cn, String nombre) throws SQLException {
        Integer id = buscar(cn, "SELECT id_periodo FROM periodo WHERE nombre = ? COLLATE NOCASE", nombre);
        if (id != null) {
            return id;
        }
        LocalDate hoy = LocalDate.now();
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO periodo "
                + "(nombre, fecha_inicio, fecha_fin, estado) VALUES (?,?,?, 'ACTIVO')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, hoy.toString());
            ps.setString(3, hoy.plusMonths(4).toString());
            ps.executeUpdate();
            return clave(ps);
        }
    }

    private int idCurso(Connection cn, String nombre) throws SQLException {
        Integer id = buscar(cn, "SELECT id_curso FROM curso WHERE nombre = ? COLLATE NOCASE", nombre);
        if (id != null) {
            return id;
        }
        StringBuilder base = new StringBuilder();
        for (String palabra : nombre.split("\\s+")) {
            if (palabra.length() > 2 && base.length() < 4) {
                base.append(Character.toUpperCase(palabra.charAt(0)));
            }
        }
        if (base.length() == 0) {
            base.append("CUR");
        }
        int n = 1;
        String clave;
        do {
            clave = base + String.valueOf(100 + n++);
        } while (buscar(cn, "SELECT id_curso FROM curso WHERE clave_curso = ?", clave) != null);

        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO curso (clave_curso, nombre, estado) VALUES (?,?, 'ACTIVO')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, clave);
            ps.setString(2, nombre);
            ps.executeUpdate();
            return clave(ps);
        }
    }

    private int idAula(Connection cn, String nombre) throws SQLException {
        Integer id = buscar(cn, "SELECT id_aula FROM aula WHERE nombre = ? COLLATE NOCASE", nombre);
        if (id != null) {
            return id;
        }
        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO aula (nombre, capacidad, estado) VALUES (?, 30, 'ACTIVA')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            return clave(ps);
        }
    }

    private static Integer buscar(Connection cn, String sql, String valor) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private static int clave(PreparedStatement ps) throws SQLException {
        try (ResultSet k = ps.getGeneratedKeys()) {
            k.next();
            return k.getInt(1);
        }
    }

    private static String txt(ResultSet rs, int col) throws SQLException {
        String s = rs.getString(col);
        return s == null ? "" : s;
    }
}
