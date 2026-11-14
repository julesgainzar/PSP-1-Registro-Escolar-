package controlador;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import modelo.Cursos;
import modelo.Datos;
import modelo.Estudiantes;
import modelo.Profesores;
import modelo.RegistroEscolar;
import modelo.Validador;

/** Gestión de cursos: datos, profesor asignado e inscripción de estudiantes. */
public class CursosController implements Initializable {

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtPeriodo;
    @FXML
    private TextField txtHorario;
    @FXML
    private TextField txtAula;
    @FXML
    private ComboBox<Profesores> cmbProfesor;
    @FXML
    private TableView<Cursos> tabla;
    @FXML
    private TableColumn<Cursos, String> colNombre;
    @FXML
    private TableColumn<Cursos, String> colPeriodo;
    @FXML
    private TableColumn<Cursos, String> colHorario;
    @FXML
    private TableColumn<Cursos, String> colAula;
    @FXML
    private TableColumn<Cursos, String> colProfesor;
    @FXML
    private TableColumn<Cursos, String> colInscritos;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnEliminar;
    @FXML
    private ListView<Estudiantes> lstInscritos;
    @FXML
    private ComboBox<Estudiantes> cmbEstudiante;
    @FXML
    private Button btnInscribir;
    @FXML
    private Button btnBaja;
    @FXML
    private Button btnExportar;
    @FXML
    private Label lblInscritos;

    private final RegistroEscolar registro = Datos.get();
    private final ObservableList<Cursos> datos = FXCollections.observableArrayList();
    private final ObservableList<Estudiantes> inscritos = FXCollections.observableArrayList();
    private Cursos seleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreCurso()));
        colPeriodo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAnioSemestre()));
        colHorario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorario()));
        colAula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAula()));
        colProfesor.setCellValueFactory(c -> {
            Profesores p = c.getValue().getProfesorAsignado();
            return new SimpleStringProperty(p == null ? "Sin asignar" : p.getNombre());
        });
        colInscritos.setCellValueFactory(c
                -> new SimpleStringProperty(String.valueOf(c.getValue().getEstudiantes().size())));

        tabla.setItems(datos);
        lstInscritos.setItems(inscritos);
        cmbProfesor.setItems(FXCollections.observableArrayList(registro.getProfesores()));

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
        datos.setAll(registro.getCursos());
    }

    private void editar(Cursos c) {
        seleccionado = c;
        txtNombre.setText(c.getNombreCurso());
        txtPeriodo.setText(c.getAnioSemestre());
        txtHorario.setText(c.getHorario());
        txtAula.setText(c.getAula());
        cmbProfesor.setValue(c.getProfesorAsignado());
        btnGuardar.setText("Actualizar");
        btnEliminar.setDisable(false);
        actualizarInscripciones();
    }

    /** Refresca la lista de inscritos y los estudiantes disponibles. */
    private void actualizarInscripciones() {
        boolean hay = seleccionado != null;
        btnInscribir.setDisable(!hay);
        btnBaja.setDisable(!hay);
        btnExportar.setDisable(!hay);
        cmbEstudiante.setDisable(!hay);
        lstInscritos.setDisable(!hay);

        inscritos.clear();
        List<Estudiantes> disponibles = new ArrayList<>();
        if (hay) {
            inscritos.setAll(seleccionado.getEstudiantes());
            for (Estudiantes e : registro.getEstudiantes()) {
                if (!seleccionado.estaInscrito(e)) {
                    disponibles.add(e);
                }
            }
        }
        cmbEstudiante.setItems(FXCollections.observableArrayList(disponibles));
        cmbEstudiante.setValue(null);
        lblInscritos.setText(hay
                ? "Inscritos en " + seleccionado.getNombreCurso() + " (" + inscritos.size() + ")"
                : "Seleccione un curso para inscribir estudiantes");
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        tabla.getSelectionModel().clearSelection();
        txtNombre.clear();
        txtPeriodo.clear();
        txtHorario.clear();
        txtAula.clear();
        cmbProfesor.setValue(null);
        btnGuardar.setText("Agregar");
        btnEliminar.setDisable(true);
        actualizarInscripciones();
        txtNombre.requestFocus();
    }

    @FXML
    private void quitarProfesor() {
        cmbProfesor.setValue(null);
    }

    @FXML
    private void guardar() {

        String error = Validador.validarCurso(txtNombre.getText(),
                txtPeriodo.getText(), txtHorario.getText(), txtAula.getText());
        if (error != null) {
            Util.aviso(error);
            return;
        }

        String nombre = txtNombre.getText().trim();
        String periodo = txtPeriodo.getText().trim();
        String horario = txtHorario.getText().trim();
        String aula = txtAula.getText().trim();
        Profesores profesor = cmbProfesor.getValue();

        Cursos existente = registro.buscarCurso(nombre, periodo);
        if (existente != null && existente != seleccionado) {
            Util.aviso("Ya existe el curso " + nombre + " en el periodo " + periodo + ".");
            return;
        }

        Cursos curso = seleccionado;
        if (curso == null) {
            curso = new Cursos(nombre, periodo, horario, aula);
            registro.agregarCurso(curso);
        } else {
            curso.setNombreCurso(nombre);
            curso.setAnioSemestre(periodo);
            curso.setHorario(horario);
            curso.setAula(aula);
        }
        curso.asignarProfesor(profesor);

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
        int n = seleccionado.getEstudiantes().size();
        String msg = "¿Eliminar el curso " + seleccionado + "?";
        if (n > 0) {
            msg += "\nSe perderá la inscripción de " + n + " estudiante(s).";
        }
        if (Util.confirmar(msg)) {
            registro.eliminarCurso(seleccionado);
            Util.guardar();
            refrescar();
            nuevo();
        }
    }

    // INSCRIPCIONES

    @FXML
    private void inscribir() {
        Estudiantes e = cmbEstudiante.getValue();
        if (seleccionado == null || e == null) {
            Util.aviso("Elija un estudiante de la lista.");
            return;
        }
        seleccionado.inscribirEstudiante(e);
        Util.guardar();
        actualizarInscripciones();
        tabla.refresh();
    }

    @FXML
    private void darDeBaja() {
        Estudiantes e = lstInscritos.getSelectionModel().getSelectedItem();
        if (seleccionado == null || e == null) {
            Util.aviso("Seleccione al estudiante inscrito que desea dar de baja.");
            return;
        }
        if (Util.confirmar("¿Dar de baja a " + e.getNombre() + " de este curso?")) {
            seleccionado.bajaEstudiante(e);
            Util.guardar();
            actualizarInscripciones();
            tabla.refresh();
        }
    }

    /** Exporta la lista de inscritos a un CSV que abre bien en Excel. */
    @FXML
    private void exportar() {
        if (seleccionado == null) {
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar lista de inscritos");
        fc.setInitialFileName("lista_" + seleccionado.getNombreCurso().replaceAll("\\W+", "_") + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File destino = fc.showSaveDialog(btnExportar.getScene().getWindow());
        if (destino == null) {
            return;
        }

        StringBuilder sb = new StringBuilder("\uFEFFMatrícula,Nombre,Correo,Teléfono\n");
        for (Estudiantes e : seleccionado.getEstudiantes()) {
            sb.append(csv(e.getMatricula())).append(',')
                    .append(csv(e.getNombre())).append(',')
                    .append(csv(e.getCorreo())).append(',')
                    .append(csv(e.getTelefono())).append('\n');
        }
        try {
            Files.write(destino.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            Util.info("Lista exportada en:\n" + destino.getAbsolutePath());
        } catch (IOException ex) {
            Util.error("No se pudo exportar: " + ex.getMessage());
        }
    }

    private static String csv(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    @FXML
    private void volver(ActionEvent e) {
        Util.abrir("MenuPrincipal.fxml", "Menú Principal", (Node) e.getSource());
    }
}
