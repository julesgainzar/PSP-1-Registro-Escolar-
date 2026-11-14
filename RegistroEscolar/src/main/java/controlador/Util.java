package controlador;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import modelo.Datos;

/** Utilidades compartidas por los controladores. */
public final class Util {

    private Util() {
    }

    /** Abre otra pantalla y cierra la ventana que contiene a "origen". */
    public static void abrir(String fxml, String titulo, Node origen) {
        try {
            FXMLLoader loader = new FXMLLoader(Util.class.getResource("/vista/" + fxml));
            Stage nueva = new Stage();
            nueva.setScene(new Scene(loader.load()));
            nueva.setTitle(titulo);
            Stage actual = (Stage) origen.getScene().getWindow();
            nueva.show();
            actual.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            error("No se pudo abrir la pantalla: " + ex.getMessage());
        }
    }

    /** Guarda el registro en disco; avisa al usuario si falla. */
    public static boolean guardar() {
        try {
            Datos.guardar();
            return true;
        } catch (IOException ex) {
            error("No se pudieron guardar los datos: " + ex.getMessage());
            return false;
        }
    }

    /** Valor entre comillas para un CSV. */
    public static String csv(String valor) {
        String v = valor == null ? "" : valor;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    public static void aviso(String mensaje) {
        mostrar(Alert.AlertType.WARNING, "Aviso", mensaje);
    }

    public static void error(String mensaje) {
        mostrar(Alert.AlertType.ERROR, "Error", mensaje);
    }

    public static void info(String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, "Información", mensaje);
    }

    public static boolean confirmar(String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje,
                ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmar");
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo, mensaje, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
