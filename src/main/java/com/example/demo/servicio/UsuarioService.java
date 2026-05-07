package com.example.demo.servicio;

import com.example.demo.modelo.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    public Usuario validarLogin(String username, String password) {
        // Buscamos al usuario por su nombre en la base de datos
        Optional<Usuario> user = repository.findByUsername(username);

        // Si existe y la contraseña coincide, regresamos el usuario completo (con su rol)
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        }

        // Si no coincide o no existe, regresamos nulo
        return null;
    }
}