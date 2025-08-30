package org.escuelaing.edu.co.annotations;

@RestController
public class HelloController {

    @GetMapping("/saludo")
    public String saludo() {
        return "<h1>¡Hola desde el controlador!</h1>";
    }

    @GetMapping("/otra")
    public String otraRuta() {
        return "<p>Otra ruta anotada :)</p>";
    }
}
