package controlador;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import modelo.Datos;
import modelo.RegistroEscolar;

/**
 * @author cuent
 */
public class MenuPrincipalController implements Initializable {

    @FXML
    private Label lblResumen;

    @FXML
    private void abrirEstudiantes(ActionEvent e) {
        Util.abrir("Estudiantes.fxml", "Estudiantes", (Node) e.getSource());
    }

    @FXML
    private void abrirProfesores(ActionEvent e) {
        Util.abrir("Profesores.fxml", "Profesores", (Node) e.getSource());
    }

    @FXML
    private void abrirCursos(ActionEvent e) {
        Util.abrir("Cursos.fxml", "Cursos", (Node) e.getSource());
    }

    @FXML
    private void cerrarSesion(ActionEvent e) {
        Util.abrir("login1.fxml", "Registro Escolar", (Node) e.getSource());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        RegistroEscolar r = Datos.get();
        lblResumen.setText(plural(r.getEstudiantes().size(), "estudiante", "estudiantes") + "  |  "
                + plural(r.getProfesores().size(), "profesor", "profesores") + "  |  "
                + plural(r.getCursos().size(), "curso", "cursos"));
    }

    private static String plural(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
