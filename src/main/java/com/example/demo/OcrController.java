package com.example.demo;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @PostMapping("/procesar")
    public ResponseEntity<Map<String, String>> procesarINE(@RequestBody OcrRequest request) {
        Map<String, String> respuesta = new HashMap<>();

        try {
            // 1. Convertir Base64 a archivo de imagen temporal
            String base64Image = request.getImagen().split(",")[1];
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            File tempFile = File.createTempFile("captura_ine", ".jpg");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(imageBytes);
            }

            // 2. Configurar Tesseract
            Tesseract tesseract = new Tesseract();
            // ¡OJO! Aquí pones la ruta donde instalaste Tesseract en el paso 2
            tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
            tesseract.setLanguage("spa"); // Idioma español

            // 3. Ejecutar OCR
            String resultado = tesseract.doOCR(tempFile);
            System.out.println("Texto extraído: " + resultado);

            respuesta.put("status", "ok");
            respuesta.put("mensaje", "Texto extraído con éxito");
            respuesta.put("texto", resultado); // Aquí va lo que leyó de la INE

        } catch (Exception e) {
            respuesta.put("status", "error");
            respuesta.put("mensaje", "Error al procesar: " + e.getMessage());
        }

        return ResponseEntity.ok(respuesta);
    }
}