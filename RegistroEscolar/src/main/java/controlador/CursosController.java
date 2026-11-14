package controlador;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
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
    private TableColumn<Cursos, String> colSemestre;
    @FXML
    private ComboBox<Integer> cmbSemestre;
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
    @FXML
    private Label lblVista;
    @FXML
    private Label lblResumen;
    @FXML
    private FlowPane panelSemestres;
    @FXML
    private Button btnListaGeneral;
    @FXML
    private Button btnReporte;

    private final RegistroEscolar registro = Datos.get();
    private final ObservableList<Cursos> datos = FXCollections.observableArrayList();
    private final ObservableList<Estudiantes> inscritos = FXCollections.observableArrayList();
    private static final int SEMESTRES = 10;
    private Cursos seleccionado;
    /** Semestre que se está viendo; null = lista general. */
    private Integer semestreActivo = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreCurso()));
        colPeriodo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAnioSemestre()));
        colSemestre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSemestre() == 0 ? "-" : String.valueOf(c.getValue().getSemestre())));
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
        for (int s = 1; s <= SEMESTRES; s++) {
            cmbSemestre.getItems().add(s);
        }

        tabla.getSelectionModel().selectedItemProperty()
                .addListener((obs, viejo, nuevo) -> {
                    if (nuevo != null) {
                        editar(nuevo);
                    }
                });

        refrescar();
        nuevo();
    }

    /** Reconstruye los botones de semestre y la tabla según la vista activa. */
    private void refrescar() {
        construirSemestres();
        List<Cursos> lista = new ArrayList<>();
        for (Cursos c : registro.getCursos()) {
            if (semestreActivo == null || c.getSemestre() == semestreActivo) {
                lista.add(c);
            }
        }
        lista.sort(Comparator.comparingInt((Cursos c) -> c.getSemestre() == 0 ? SEMESTRES + 1 : c.getSemestre())
                .thenComparing(Cursos::getNombreCurso));
        datos.setAll(lista);
        actualizarResumen();
    }

    private int cantidadEn(int semestre) {
        int n = 0;
        for (Cursos c : registro.getCursos()) {
            if (c.getSemestre() == semestre) {
                n++;
            }
        }
        return n;
    }

    /** Un botón por semestre (1 a 10) con su número de grupos; "Sin sem." solo si hay. */
    private void construirSemestres() {
        panelSemestres.getChildren().clear();
        ToggleGroup grupo = new ToggleGroup();
        for (int s = 0; s <= SEMESTRES; s++) {
            if (s == 0 && cantidadEn(0) == 0) {
                continue;
            }
            final int semestre = s;
            ToggleButton b = new ToggleButton((s == 0 ? "Sin sem." : "Sem. " + s) + " (" + cantidadEn(s) + ")");
            b.setToggleGroup(grupo);
            b.setSelected(semestreActivo != null && semestreActivo == s);
            b.setOnAction(ev -> {
                b.setSelected(true);
                semestreActivo = semestre;
                nuevo();
                refrescar();
            });
            panelSemestres.getChildren().add(b);
        }
    }

    private void actualizarResumen() {
        lblVista.setText(semestreActivo == null ? "Lista general de grupos"
                : semestreActivo == 0 ? "Grupos sin semestre" : "Grupos del semestre " + semestreActivo);
        Set<Estudiantes> distintos = new HashSet<>();
        int inscripciones = 0;
        for (Cursos c : datos) {
            inscripciones += c.getEstudiantes().size();
            distintos.addAll(c.getEstudiantes());
        }
        lblResumen.setText(datos.size() + " grupo(s)  |  " + inscripciones
                + " inscripción(es)  |  " + distintos.size() + " alumno(s) distintos");
        btnReporte.setDisable(datos.isEmpty());
    }

    @FXML
    private void listaGeneral() {
        semestreActivo = null;
        nuevo();
        refrescar();
    }

    /** CSV con los alumnos inscritos por grupo de lo que se está viendo (semestre o lista general). */
    @FXML
    private void reporteGrupos() {
        if (datos.isEmpty()) {
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte de alumnos por grupo");
        fc.setInitialFileName(semestreActivo == null ? "alumnos_por_grupo_general.csv"
                : "alumnos_por_grupo_semestre_" + semestreActivo + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File destino = fc.showSaveDialog(btnReporte.getScene().getWindow());
        if (destino == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("\uFEFFSemestre,Curso,Periodo,Profesor,Aula,Horario,"
                + "Alumnos inscritos\n");
        int total = 0;
        for (Cursos c : datos) {
            int n = c.getEstudiantes().size();
            total += n;
            Profesores p = c.getProfesorAsignado();
            sb.append(Util.csv(c.getSemestre() == 0 ? "Sin semestre" : String.valueOf(c.getSemestre()))).append(',')
                    .append(Util.csv(c.getNombreCurso())).append(',')
                    .append(Util.csv(c.getAnioSemestre())).append(',')
                    .append(Util.csv(p == null ? "Sin asignar" : p.getNombre())).append(',')
                    .append(Util.csv(c.getAula())).append(',')
                    .append(Util.csv(c.getHorario())).append(',')
                    .append(n).append('\n');
        }
        sb.append(",,,,,Total de inscripciones,").append(total).append('\n');
        try {
            Files.write(destino.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            Util.info("Reporte guardado en:\n" + destino.getAbsolutePath());
        } catch (IOException ex) {
            Util.error("No se pudo guardar el reporte: " + ex.getMessage());
        }
    }

    private void editar(Cursos c) {
        seleccionado = c;
        txtNombre.setText(c.getNombreCurso());
        txtPeriodo.setText(c.getAnioSemestre());
        txtHorario.setText(c.getHorario());
        txtAula.setText(c.getAula());
        cmbSemestre.setValue(c.getSemestre() == 0 ? null : c.getSemestre());
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
        cmbSemestre.setValue(null);
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

        if (cmbSemestre.getValue() == null) {
            Util.aviso("Elija el semestre (1 a 10) al que pertenece el grupo.");
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
        curso.setSemestre(cmbSemestre.getValue());
        curso.asignarProfesor(profesor);

        Util.guardar();
        if (semestreActivo != null) {
            semestreActivo = curso.getSemestre();
        }
        refrescar();
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
        construirSemestres();
        actualizarResumen();
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
            construirSemestres();
            actualizarResumen();
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
