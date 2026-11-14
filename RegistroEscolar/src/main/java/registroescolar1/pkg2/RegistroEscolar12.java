package registroescolar1.pkg2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Aplicación principal: abre la pantalla de inicio de sesión.
 *
 * @author cuent
 */
public class RegistroEscolar12 extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/vista/login1.fxml"));
        Scene scene = new Scene(loader.load());

        stage.setScene(scene);
        stage.setTitle("Registro Escolar");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
