package modelo;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Punto único de acceso al registro en memoria.
 * Todas las pantallas comparten la misma instancia y llaman a guardar()
 * después de cada cambio. Los datos viven en la base SQLite (GESTIONESCOLAR.db).
 */
public final class Datos {

    private static final RegistroDAO DAO = new RegistroDAO();
    private static RegistroEscolar registro;

    private Datos() {
    }

    public static RegistroEscolar get() {
        if (registro == null) {
            try {
                registro = DAO.cargar();
            } catch (SQLException ex) {
                // No se usa un registro vacío: un guardado posterior
                // daría de baja todo lo que hay en la base.
                throw new IllegalStateException(
                        "No se pudo leer la base de datos: " + ex.getMessage(), ex);
            }
        }
        return registro;
    }

    public static void guardar() throws IOException {
        try {
            DAO.guardar(get());
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }
}
