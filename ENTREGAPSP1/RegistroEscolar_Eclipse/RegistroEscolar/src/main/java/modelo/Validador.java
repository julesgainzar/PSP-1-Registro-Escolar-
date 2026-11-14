package modelo;

import java.util.regex.Pattern;

/**
 * Validaciones de formulario. Cada método devuelve null si todo está bien,
 * o el mensaje de error para mostrar al usuario.
 */
public final class Validador {

    private static final Pattern CORREO
            = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern CLAVE
            = Pattern.compile("^[A-Za-z0-9-]{3,20}$");

    private Validador() {
    }

    /** Deja solo los dígitos de un teléfono (quita espacios y guiones). */
    public static String soloDigitos(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("\\D", "");
    }

    public static String validarPersona(String etiquetaClave, String clave,
            String nombre, String edadTxt, String correo, String telefono,
            int edadMin, int edadMax) {

        if (clave == null || !CLAVE.matcher(clave.trim()).matches()) {
            return etiquetaClave + " inválida: use de 3 a 20 letras, números o guiones.";
        }
        if (nombre == null || nombre.trim().length() < 3) {
            return "El nombre debe tener al menos 3 caracteres.";
        }
        int edad;
        try {
            edad = Integer.parseInt(edadTxt.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return "La edad debe ser un número entero.";
        }
        if (edad < edadMin || edad > edadMax) {
            return "La edad debe estar entre " + edadMin + " y " + edadMax + ".";
        }
        if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
            return "El correo electrónico no es válido.";
        }
        if (soloDigitos(telefono).length() != 10) {
            return "El teléfono debe tener 10 dígitos.";
        }
        return null;
    }

    public static String validarCurso(String nombre, String periodo,
            String horario, String aula) {

        if (nombre == null || nombre.trim().isEmpty()) {
            return "El nombre del curso es obligatorio.";
        }
        if (periodo == null || periodo.trim().isEmpty()) {
            return "El periodo (año/semestre) es obligatorio. Ej: 2026-2";
        }
        if (horario == null || horario.trim().isEmpty()) {
            return "El horario es obligatorio. Ej: Lun y Mié 10:00-12:00";
        }
        if (HorarioTexto.parsear(horario) == null) {
            return "Horario no válido. Use por ejemplo: Lun y Mié 10:00-12:00 "
                    + "(varias franjas separadas por ;)";
        }
        if (aula == null || aula.trim().isEmpty()) {
            return "El aula es obligatoria.";
        }
        return null;
    }
}
