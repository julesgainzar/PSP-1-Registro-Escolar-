package modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexión a la base SQLite. El archivo se busca en la carpeta desde donde se
 * ejecuta la app, igual que antes se hacía con registro.dat. Se puede cambiar
 * la ruta al arrancar con:  -Dregistro.db=C:/ruta/GESTIONESCOLAR.db
 */
public final class Conexion {

    private static final String ARCHIVO
            = System.getProperty("registro.db", "GESTIONESCOLAR.db");

    private Conexion() {
    }

    public static Connection abrir() throws SQLException {
        // Evita que SQLite cree una base vacía si el archivo no está.
        if (!new java.io.File(ARCHIVO).isFile()) {
            throw new SQLException("No se encontró la base de datos: "
                    + new java.io.File(ARCHIVO).getAbsolutePath());
        }
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + ARCHIVO);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }
}
