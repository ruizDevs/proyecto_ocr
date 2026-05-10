package com.example.demo;

import com.example.demo.modelo.IneRegistro;
import com.example.demo.modelo.Usuario;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/procesar")
    public ResponseEntity<Map<String, Object>> procesarINE(@RequestBody OcrRequest request, HttpSession session) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado == null) {
                respuesta.put("status", "error");
                respuesta.put("mensaje", "No hay sesión activa, por favor inicie sesión.");
                return ResponseEntity.status(401).body(respuesta);
            }

            // RF-05: Storage Directory
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String base64Image = request.getImagen();
            if (base64Image != null && base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            
            // Generate a permanent file for RF-08 Evidencia
            String filename = UUID.randomUUID().toString() + ".jpg";
            Path imagePath = Paths.get(UPLOAD_DIR, filename);
            Files.write(imagePath, imageBytes);
            File targetFile = imagePath.toFile();

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(new File("tessdata").getAbsolutePath());
            tesseract.setLanguage("spa");

            String resultado = tesseract.doOCR(targetFile);

            // RF-02: Procesamiento OCR Mejorado (Lectura por líneas)
            String curp = "";
            String claveElector = "";
            String seccion = "";
            String estado = "";
            String direccion = "";
            String nombre = "";
            String apellidoPaterno = "";
            String apellidoMaterno = "";

            String[] lineas = resultado.split("\\r?\\n");
            for (int i = 0; i < lineas.length; i++) {
                String linea = lineas[i].trim().toUpperCase();
                
                if (linea.startsWith("NOMBRE") || linea.equals("NOMBRE")) {
                    if (i + 1 < lineas.length) apellidoPaterno = lineas[i+1].trim();
                    if (i + 2 < lineas.length) apellidoMaterno = lineas[i+2].trim();
                    if (i + 3 < lineas.length) nombre = lineas[i+3].trim();
                }
                else if (linea.startsWith("DOMICILIO") || linea.equals("DOMICILIO")) {
                    StringBuilder dom = new StringBuilder();
                    if (i + 1 < lineas.length) dom.append(lineas[i+1].trim()).append(" ");
                    if (i + 2 < lineas.length && !lineas[i+2].contains("CLAVE")) dom.append(lineas[i+2].trim()).append(" ");
                    if (i + 3 < lineas.length && !lineas[i+3].contains("CLAVE") && !lineas[i+3].contains("CURP")) dom.append(lineas[i+3].trim());
                    direccion = dom.toString().trim();
                }
                else if (linea.contains("CLAVE DE ELECTOR")) {
                    String[] partes = linea.split("ELECTOR");
                    if(partes.length > 1) claveElector = partes[1].replaceAll("[^A-Z0-9]", "").trim();
                }
                else if (linea.contains("CURP")) {
                    String[] partes = linea.split("CURP");
                    if(partes.length > 1) {
                        curp = partes[1].split("AÑO")[0].replaceAll("[^A-Z0-9]", "").trim();
                        if (curp.length() > 18) curp = curp.substring(0, 18);
                    }
                }
                
                // Múltiples datos pueden venir en la misma línea (ESTADO, MUNICIPIO, SECCION)
                if (linea.contains("ESTADO")) {
                    String est = extraerConRegex(linea, "ESTADO\\s*(\\d+)");
                    if (est != null) estado = est;
                }
                if (linea.contains("SECCION") || linea.contains("SECCI")) {
                    String sec = extraerConRegex(linea, "SECCI[OÓ0]N\\s*(\\d+)");
                    if (sec != null) seccion = sec;
                }
            }

            // Fallbacks con Regex puro por si las palabras clave no se leyeron bien
            if (curp.isEmpty()) curp = extraerConRegex(resultado, "[A-Z]{4}[0-9O]{6}[HM][A-Z]{5}[A-Z0-9]{2}");
            if (claveElector.isEmpty()) claveElector = extraerConRegex(resultado, "[A-Z]{6}[0-9O]{8}[A-Z0-9]{4}");
            if (seccion.isEmpty()) {
                String sec = extraerConRegex(resultado, "SECCI[OÓ0]N\\s*(\\d{4})");
                seccion = sec != null ? sec : extraerConRegex(resultado, "(\\d{4})");
            }

            IneRegistro registro = new IneRegistro();
            registro.setCurp(curp != null ? curp : "");
            registro.setClaveElector(claveElector != null ? claveElector : "");
            
            if (seccion != null && seccion.length() > 4) seccion = seccion.substring(0, 4);
            registro.setSeccion(seccion != null ? seccion : "0000");

            registro.setEstado(estado != null ? estado : "");
            registro.setDireccion(direccion != null ? direccion : "");

            registro.setNombre(nombre.isEmpty() ? "REVISAR" : nombre);
            registro.setApellidoPaterno(apellidoPaterno.isEmpty() ? "REVISAR" : apellidoPaterno);
            registro.setApellidoMaterno(apellidoMaterno.isEmpty() ? "REVISAR" : apellidoMaterno);
            
            // RF-04 Validación Confianza
            registro.setIndiceConfianza(curp != null && claveElector != null ? 88.0 : 40.0);
            registro.setRequiereRevisionManual(curp == null || claveElector == null);
            registro.setCapturadoPor(usuarioLogueado);
            registro.setRutaImagen(imagePath.toAbsolutePath().toString());

            respuesta.put("status", "ok");
            respuesta.put("mensaje", "Datos extraídos correctamente. Por favor verifica visualmente.");
            respuesta.put("registro", registro);

        } catch (Exception e) {
            respuesta.put("status", "error");
            respuesta.put("mensaje", "Error al procesar la imagen: " + e.getMessage());
        }

        return ResponseEntity.ok(respuesta);
    }

    private String extraerConRegex(String texto, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            if (matcher.groupCount() > 0) return matcher.group(1);
            return matcher.group();
        }
        return null;
    }
}