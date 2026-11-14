/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package modelo;

/**
 *
 * @author cuent
 */
public class Estudiantes extends Persona {

    private static final long serialVersionUID = 1L;

    private String matricula;

    public Estudiantes(String matricula, String nombre,
            int edad, String correo,
            String telefono, String direccion) {

        super(nombre, edad, correo, telefono, direccion);
        this.matricula = matricula;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    @Override
    public String toString() {
        return matricula + " - " + getNombre();
    }
}
