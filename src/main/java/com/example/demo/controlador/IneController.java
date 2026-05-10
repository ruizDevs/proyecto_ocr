package com.example.demo.controlador;

import com.example.demo.modelo.IneRegistro;
import com.example.demo.modelo.Usuario;
import com.example.demo.repository.IneRegistroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ine")
public class IneController {

    @Autowired
    private IneRegistroRepository ineRepository;

    @GetMapping("/registros")
    public ResponseEntity<?> obtenerRegistros(@RequestParam(required = false) String query, HttpSession session) {
        List<IneRegistro> registros;
        if (query != null && !query.trim().isEmpty()) {
            registros = ineRepository.searchByKeyword(query.trim());
        } else {
            registros = ineRepository.findAll();
        }
        return ResponseEntity.ok(registros);
    }

    @GetMapping("/mis-registros")
    public ResponseEntity<?> obtenerMisRegistros(HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return ResponseEntity.status(401).body("No autorizado");
        }
        List<IneRegistro> registros = ineRepository.findByCapturadoPor(usuarioLogueado);
        return ResponseEntity.ok(registros);
    }

    @PostMapping("/guardar")
    public ResponseEntity<?> guardarRegistro(@RequestBody IneRegistro registro, HttpSession session) {
        Map<String, Object> respuesta = new HashMap<>();
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        
        if (usuarioLogueado == null) {
            respuesta.put("status", "error");
            respuesta.put("mensaje", "No hay sesión activa");
            return ResponseEntity.status(401).body(respuesta);
        }

        try {
            registro.setCapturadoPor(usuarioLogueado);
            registro.setFechaCaptura(java.time.LocalDateTime.now());
            // RF-03: Normalización de Datos
            if (registro.getNombre() != null) registro.setNombre(registro.getNombre().toUpperCase().replaceAll("[^A-ZÁÉÍÓÚÑ ]", ""));
            if (registro.getApellidoPaterno() != null) registro.setApellidoPaterno(registro.getApellidoPaterno().toUpperCase().replaceAll("[^A-ZÁÉÍÓÚÑ ]", ""));
            if (registro.getApellidoMaterno() != null) registro.setApellidoMaterno(registro.getApellidoMaterno().toUpperCase().replaceAll("[^A-ZÁÉÍÓÚÑ ]", ""));
            if (registro.getCurp() != null) registro.setCurp(registro.getCurp().toUpperCase().replaceAll("[^A-Z0-9]", ""));
            if (registro.getClaveElector() != null) registro.setClaveElector(registro.getClaveElector().toUpperCase().replaceAll("[^A-Z0-9]", ""));
            if (registro.getEstado() != null) registro.setEstado(registro.getEstado().toUpperCase());
            if (registro.getDireccion() != null) registro.setDireccion(registro.getDireccion().toUpperCase());

            ineRepository.save(registro);
            respuesta.put("status", "ok");
            respuesta.put("mensaje", "Registro guardado exitosamente");
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            respuesta.put("status", "error");
            respuesta.put("mensaje", "Error al guardar (posible duplicado): " + e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);
        }
    }

    @GetMapping("/imagen")
    public ResponseEntity<Resource> obtenerImagen(@RequestParam String ruta) {
        try {
            Path path = Paths.get(ruta);
            Resource resource = new FileSystemResource(path.toFile());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            }
        } catch (Exception e) {
            // Log error
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/exportar")
    public ResponseEntity<String> exportarCsv(HttpSession session) {
        List<IneRegistro> registros = ineRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,CURP,Clave Elector,Nombre,Paterno,Materno,Seccion,Estado,Indice Confianza,Requiere Revision,Fecha Captura\n");
        for (IneRegistro r : registros) {
            csv.append(r.getId()).append(",")
               .append(r.getCurp()).append(",")
               .append(r.getClaveElector()).append(",")
               .append(r.getNombre()).append(",")
               .append(r.getApellidoPaterno()).append(",")
               .append(r.getApellidoMaterno()).append(",")
               .append(r.getSeccion()).append(",")
               .append(r.getEstado() != null ? r.getEstado() : "").append(",")
               .append(r.getIndiceConfianza()).append(",")
               .append(r.isRequiereRevisionManual() ? "SI" : "NO").append(",")
               .append(r.getFechaCaptura()).append("\n");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=registros_ocr.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        return ResponseEntity.ok().headers(headers).body(csv.toString());
    }
}
