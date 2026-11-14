package modelo;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Curso con su profesor asignado y la lista de estudiantes inscritos.
 *
 * @author cuent
 */
public class Cursos implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombreCurso;
    private String anioSemestre;
    private String horario;
    private String aula;

    private Profesores profesorAsignado;

    private ArrayList<Estudiantes> estudiantes;

    public Cursos(String nombreCurso,
            String anioSemestre,
            String horario,
            String aula) {

        this.nombreCurso = nombreCurso;
        this.anioSemestre = anioSemestre;
        this.horario = horario;
        this.aula = aula;

        estudiantes = new ArrayList<>();
    }

    /** Asigna el profesor; null deja el curso sin profesor. */
    public void asignarProfesor(Profesores profesor) {
        profesorAsignado = profesor;
    }

    /** Inscribe al estudiante. Devuelve false si ya estaba inscrito. */
    public boolean inscribirEstudiante(Estudiantes estudiante) {
        if (estudiante == null || estudiantes.contains(estudiante)) {
            return false;
        }
        return estudiantes.add(estudiante);
    }

    public boolean bajaEstudiante(Estudiantes estudiante) {
        return estudiantes.remove(estudiante);
    }

    public boolean estaInscrito(Estudiantes estudiante) {
        return estudiantes.contains(estudiante);
    }

    public ArrayList<Estudiantes> getEstudiantes() {
        return estudiantes;
    }

    public Profesores getProfesorAsignado() {
        return profesorAsignado;
    }

    public String getNombreCurso() {
        return nombreCurso;
    }

    public void setNombreCurso(String nombreCurso) {
        this.nombreCurso = nombreCurso;
    }

    public String getAnioSemestre() {
        return anioSemestre;
    }

    public void setAnioSemestre(String anioSemestre) {
        this.anioSemestre = anioSemestre;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    @Override
    public String toString() {
        return nombreCurso + " (" + anioSemestre + ")";
    }
}
