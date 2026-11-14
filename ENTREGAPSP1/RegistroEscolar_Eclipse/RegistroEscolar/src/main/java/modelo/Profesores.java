/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package modelo;

/**
 *
 * @author cuent
 */
public class Profesores extends Persona {

    private static final long serialVersionUID = 1L;

    private String numEmpleado;

    public Profesores(String numEmpleado,
            String nombre, int edad,
            String correo, String telefono,
            String direccion) {

        super(nombre, edad, correo, telefono, direccion);
        this.numEmpleado = numEmpleado;
    }

    public String getNumEmpleado() {
        return numEmpleado;
    }

    public void setNumEmpleado(String numEmpleado) {
        this.numEmpleado = numEmpleado;
    }

    @Override
    public String toString() {
        return numEmpleado + " - " + getNombre();
    }
}
