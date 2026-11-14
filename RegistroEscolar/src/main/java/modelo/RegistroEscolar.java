package modelo;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Contenedor principal: estudiantes, profesores y cursos.
 * Mantiene la integridad entre ellos (p. ej. al eliminar un estudiante
 * también se le da de baja de todos sus cursos).
 *
 * @author cuent
 */
public class RegistroEscolar implements Serializable {

    private static final long serialVersionUID = 1L;

    private ArrayList<Estudiantes> estudiantes;
    private ArrayList<Profesores> profesores;
    private ArrayList<Cursos> cursos;

    public RegistroEscolar() {
        estudiantes = new ArrayList<>();
        profesores = new ArrayList<>();
        cursos = new ArrayList<>();
    }

    public ArrayList<Estudiantes> getEstudiantes() {
        return estudiantes;
    }

    public ArrayList<Profesores> getProfesores() {
        return profesores;
    }

    public ArrayList<Cursos> getCursos() {
        return cursos;
    }

    // ESTUDIANTES

    /** Devuelve false si ya existe un estudiante con esa matrícula. */
    public boolean agregarEstudiante(Estudiantes e) {
        if (buscarEstudiante(e.getMatricula()) != null) {
            return false;
        }
        return estudiantes.add(e);
    }

    /** Elimina al estudiante y lo da de baja de todos los cursos. */
    public boolean eliminarEstudiante(String matricula) {
        Estudiantes e = buscarEstudiante(matricula);
        if (e == null) {
            return false;
        }
        for (Cursos c : cursos) {
            c.bajaEstudiante(e);
        }
        return estudiantes.remove(e);
    }

    public Estudiantes buscarEstudiante(String matricula) {
        for (Estudiantes e : estudiantes) {
            if (e.getMatricula().equalsIgnoreCase(matricula)) {
                return e;
            }
        }
        return null;
    }

    /** Cursos en los que está inscrito el estudiante. */
    public ArrayList<Cursos> cursosDe(Estudiantes e) {
        ArrayList<Cursos> lista = new ArrayList<>();
        for (Cursos c : cursos) {
            if (c.estaInscrito(e)) {
                lista.add(c);
            }
        }
        return lista;
    }

    // PROFESORES

    /** Devuelve false si ya existe un profesor con ese número de empleado. */
    public boolean agregarProfesor(Profesores p) {
        if (buscarProfesor(p.getNumEmpleado()) != null) {
            return false;
        }
        return profesores.add(p);
    }

    /** Elimina al profesor y deja sin profesor los cursos que impartía. */
    public boolean eliminarProfesor(String numEmpleado) {
        Profesores p = buscarProfesor(numEmpleado);
        if (p == null) {
            return false;
        }
        for (Cursos c : cursos) {
            if (c.getProfesorAsignado() == p) {
                c.asignarProfesor(null);
            }
        }
        return profesores.remove(p);
    }

    public Profesores buscarProfesor(String numEmpleado) {
        for (Profesores p : profesores) {
            if (p.getNumEmpleado().equalsIgnoreCase(numEmpleado)) {
                return p;
            }
        }
        return null;
    }

    /** Cursos que imparte el profesor. */
    public ArrayList<Cursos> cursosDe(Profesores p) {
        ArrayList<Cursos> lista = new ArrayList<>();
        for (Cursos c : cursos) {
            if (c.getProfesorAsignado() == p) {
                lista.add(c);
            }
        }
        return lista;
    }

    // CURSOS

    /** Devuelve false si ya existe un curso con el mismo nombre y periodo. */
    public boolean agregarCurso(Cursos curso) {
        if (buscarCurso(curso.getNombreCurso(), curso.getAnioSemestre()) != null) {
            return false;
        }
        return cursos.add(curso);
    }

    public boolean eliminarCurso(Cursos curso) {
        return cursos.remove(curso);
    }

    public Cursos buscarCurso(String nombre, String periodo) {
        for (Cursos c : cursos) {
            if (c.getNombreCurso().equalsIgnoreCase(nombre)
                    && c.getAnioSemestre().equalsIgnoreCase(periodo)) {
                return c;
            }
        }
        return null;
    }
}
