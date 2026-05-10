package com.example.demo.controlador;

import com.example.demo.modelo.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/crear")
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario nuevoUsuario, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        
        // Validar que un ADMIN está logueado
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || !"ADMIN".equals(logueado.getRol())) {
            response.put("error", "No tienes permisos para realizar esta acción.");
            return ResponseEntity.status(403).body(response);
        }

        if (usuarioRepository.findByUsername(nuevoUsuario.getUsername()).isPresent()) {
            response.put("error", "El nombre de usuario ya existe.");
            return ResponseEntity.badRequest().body(response);
        }

        // Si no se envía rol, por defecto es CAPTURISTA
        if (nuevoUsuario.getRol() == null || nuevoUsuario.getRol().isEmpty()) {
            nuevoUsuario.setRol("CAPTURISTA");
        }

        usuarioRepository.save(nuevoUsuario);
        response.put("mensaje", "Usuario creado exitosamente.");
        return ResponseEntity.ok(response);
    }
}
