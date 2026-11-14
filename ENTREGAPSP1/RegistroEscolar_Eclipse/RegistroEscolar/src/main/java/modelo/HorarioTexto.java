package modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte entre el texto de horario que escribe el usuario
 * ("Lun y Mié 10:00-12:00") y las filas de la tabla horario (un día por fila).
 * Varias franjas se separan con ";"  Ej: "Lun y Mié 08:00-10:00; Vie 12:00-14:00"
 */
public final class HorarioTexto {

    /** Una franja: un día con hora de inicio y fin. */
    public static final class Franja {
        public final String dia;     // nombre completo: "Miércoles"
        public final String inicio;  // "08:00"
        public final String fin;     // "10:00"

        public Franja(String dia, String inicio, String fin) {
            this.dia = dia;
            this.inicio = inicio;
            this.fin = fin;
        }
    }

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles",
        "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final String[] ABREV = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private static final Pattern FRANJA = Pattern.compile(
            "^\\s*(.+?)\\s+(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})\\s*$");

    private HorarioTexto() {
    }

    /** Devuelve las franjas, o null si el texto no tiene un formato válido. */
    public static List<Franja> parsear(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        List<Franja> franjas = new ArrayList<>();
        for (String parte : texto.split(";")) {
            Matcher m = FRANJA.matcher(parte);
            if (!m.matches()) {
                return null;
            }
            int h1 = Integer.parseInt(m.group(2));
            int m1 = Integer.parseInt(m.group(3));
            int h2 = Integer.parseInt(m.group(4));
            int m2 = Integer.parseInt(m.group(5));
            if (h1 > 23 || h2 > 23 || m1 > 59 || m2 > 59
                    || h1 * 60 + m1 >= h2 * 60 + m2) {
                return null;
            }
            String inicio = String.format("%02d:%02d", h1, m1);
            String fin = String.format("%02d:%02d", h2, m2);
            for (String d : m.group(1).split("\\s*(?:,|/|\\by\\b)\\s*")) {
                String dia = normalizarDia(d);
                if (dia == null) {
                    return null;
                }
                franjas.add(new Franja(dia, inicio, fin));
            }
        }
        return franjas;
    }

    /** Arma el texto a partir de franjas: agrupa los días con el mismo horario. */
    public static String formatear(List<Franja> franjas) {
        List<String> claves = new ArrayList<>();
        List<List<String>> dias = new ArrayList<>();
        for (Franja f : franjas) {
            String clave = f.inicio + "-" + f.fin;
            int i = claves.indexOf(clave);
            if (i < 0) {
                claves.add(clave);
                dias.add(new ArrayList<>());
                i = claves.size() - 1;
            }
            dias.get(i).add(abreviar(f.dia));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < claves.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(String.join(" y ", dias.get(i))).append(' ').append(claves.get(i));
        }
        return sb.toString();
    }

    private static String normalizarDia(String d) {
        String t = sinAcentos(d.trim().toLowerCase());
        if (t.length() < 3) {
            return null;
        }
        for (int i = 0; i < DIAS.length; i++) {
            String completo = sinAcentos(DIAS[i].toLowerCase());
            if (completo.equals(t) || completo.substring(0, 3).equals(t)) {
                return DIAS[i];
            }
        }
        return null;
    }

    private static String abreviar(String dia) {
        for (int i = 0; i < DIAS.length; i++) {
            if (DIAS[i].equalsIgnoreCase(dia)) {
                return ABREV[i];
            }
        }
        return dia;
    }

    private static String sinAcentos(String s) {
        return s.replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u');
    }
}
