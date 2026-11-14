package controlador;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import modelo.Cursos;
import modelo.Datos;
import modelo.Profesores;
import modelo.RegistroEscolar;
import modelo.Validador;

/** Alta, edición, baja y búsqueda de profesores. */
public class ProfesoresController implements Initializable {

    @FXML
    private TextField txtNumEmpleado;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtEdad;
    @FXML
    private TextField txtCorreo;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextField txtDireccion;
    @FXML
    private TextField txtBuscar;
    @FXML
    private TableView<Profesores> tabla;
    @FXML
    private TableColumn<Profesores, String> colNumEmpleado;
    @FXML
    private TableColumn<Profesores, String> colNombre;
    @FXML
    private TableColumn<Profesores, String> colCorreo;
    @FXML
    private TableColumn<Profesores, String> colTelefono;
    @FXML
    private TableColumn<Profesores, Integer> colEdad;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnEliminar;

    private final RegistroEscolar registro = Datos.get();
    private final ObservableList<Profesores> datos
            = FXCollections.observableArrayList();
    private Profesores seleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        colNumEmpleado.setCellValueFactory(new PropertyValueFactory<>("numEmpleado"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEdad.setCellValueFactory(new PropertyValueFactory<>("edad"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        // Búsqueda en vivo por número de empleado, nombre o correo
        FilteredList<Profesores> filtrados = new FilteredList<>(datos, e -> true);
        txtBuscar.textProperty().addListener((obs, viejo, nuevo) -> {
            String q = nuevo.trim().toLowerCase();
            filtrados.setPredicate(e -> q.isEmpty()
                    || e.getNumEmpleado().toLowerCase().contains(q)
                    || e.getNombre().toLowerCase().contains(q)
                    || e.getCorreo().toLowerCase().contains(q));
        });
        SortedList<Profesores> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);

        tabla.getSelectionModel().selectedItemProperty()
                .addListener((obs, viejo, nuevo) -> {
                    if (nuevo != null) {
                        editar(nuevo);
                    }
                });

        refrescar();
        nuevo();
    }

    private void refrescar() {
        datos.setAll(registro.getProfesores());
    }

    private void editar(Profesores e) {
        seleccionado = e;
        txtNumEmpleado.setText(e.getNumEmpleado());
        txtNumEmpleado.setDisable(true); // la número de empleado es la clave: no se cambia
        txtNombre.setText(e.getNombre());
        txtEdad.setText(String.valueOf(e.getEdad()));
        txtCorreo.setText(e.getCorreo());
        txtTelefono.setText(e.getTelefono());
        txtDireccion.setText(e.getDireccion());
        btnGuardar.setText("Actualizar");
        btnEliminar.setDisable(false);
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        tabla.getSelectionModel().clearSelection();
        txtNumEmpleado.clear();
        txtNumEmpleado.setDisable(false);
        txtNombre.clear();
        txtEdad.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtDireccion.clear();
        btnGuardar.setText("Agregar");
        btnEliminar.setDisable(true);
        txtNumEmpleado.requestFocus();
    }

    @FXML
    private void guardar() {

        String error = Validador.validarPersona("Núm. empleado", txtNumEmpleado.getText(),
                txtNombre.getText(), txtEdad.getText(), txtCorreo.getText(),
                txtTelefono.getText(), 18, 100);
        if (error != null) {
            Util.aviso(error);
            return;
        }

        String numEmpleado = txtNumEmpleado.getText().trim();
        String nombre = txtNombre.getText().trim();
        int edad = Integer.parseInt(txtEdad.getText().trim());
        String correo = txtCorreo.getText().trim();
        String telefono = Validador.soloDigitos(txtTelefono.getText());
        String direccion = txtDireccion.getText().trim();

        if (seleccionado == null) {
            Profesores nuevo = new Profesores(numEmpleado, nombre, edad,
                    correo, telefono, direccion);
            if (!registro.agregarProfesor(nuevo)) {
                Util.aviso("Ya existe un profesor con el número de empleado " + numEmpleado + ".");
                return;
            }
        } else {
            seleccionado.setNombre(nombre);
            seleccionado.setEdad(edad);
            seleccionado.setCorreo(correo);
            seleccionado.setTelefono(telefono);
            seleccionado.setDireccion(direccion);
        }

        Util.guardar();
        refrescar();
        tabla.refresh();
        nuevo();
    }

    @FXML
    private void eliminar() {
        if (seleccionado == null) {
            return;
        }
        ArrayList<Cursos> cursos = registro.cursosDe(seleccionado);
        String msg = "¿Eliminar a " + seleccionado.getNombre() + "?";
        if (!cursos.isEmpty()) {
            msg += "\nQuedará sin profesor en " + cursos.size() + " curso(s).";
        }
        if (Util.confirmar(msg)) {
            registro.eliminarProfesor(seleccionado.getNumEmpleado());
            Util.guardar();
            refrescar();
            nuevo();
        }
    }

    @FXML
    private void volver(ActionEvent e) {
        Util.abrir("MenuPrincipal.fxml", "Menú Principal", (Node) e.getSource());
    }
}
