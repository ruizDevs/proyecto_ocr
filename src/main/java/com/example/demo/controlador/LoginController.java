package com.example.demo.controlador;

import com.example.demo.modelo.Usuario;
import com.example.demo.servicio.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/")
    public String index() {
        // Redirige directamente al login.html que está en la carpeta static
        return "redirect:/login.html";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        jakarta.servlet.http.HttpSession session,
                        Model model) {

        Usuario usuario = usuarioService.validarLogin(username, password);

        if (usuario != null) {
            // Guardar usuario en sesion
            session.setAttribute("usuarioLogueado", usuario);

            // Si el usuario existe, checamos el rol para saber a dónde mandarlo
            if (usuario.getRol().equals("ADMIN")) {
                return "redirect:/admin/dashboard.html";
            } else {
                return "redirect:/capturista/camara.html";
            }
        } else {
            // Si los datos son incorrectos, lo mandamos de vuelta con un error
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "redirect:/login.html?error=true";
        }
    }
}