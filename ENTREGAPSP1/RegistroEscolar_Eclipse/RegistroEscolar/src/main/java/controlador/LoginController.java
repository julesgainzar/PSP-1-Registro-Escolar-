package controlador;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author cuent
 */
public class LoginController implements Initializable {

    private static final String USUARIO = "Rectoria";
    private static final String CLAVE = "admin";

    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtContraseña;
    @FXML
    private Button btnIniciar;
    @FXML
    private Label lblError;

    /** Se ejecuta con el botón Iniciar o al presionar Enter en los campos. */
    @FXML
    private void eventButton(ActionEvent event) {

        String usuario = txtUsuario.getText().trim();
        String contraseña = txtContraseña.getText();

        if (usuario.equals(USUARIO) && contraseña.equals(CLAVE)) {
            Util.abrir("MenuPrincipal.fxml", "Menú Principal", btnIniciar);
        } else {
            lblError.setText("Usuario o contraseña incorrectos");
            txtContraseña.clear();
            txtContraseña.requestFocus();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setText("");
    }
}
