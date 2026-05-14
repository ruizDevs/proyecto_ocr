package com.example.demo;

import com.example.demo.modelo.IneRegistro;
import com.example.demo.modelo.Usuario;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import com.example.demo.servicio.GoogleVisionService;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private GoogleVisionService googleVisionService;

    @Autowired
    private com.example.demo.repository.IneRegistroRepository ineRepository;

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

            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String base64Image = request.getImagen();
            if (base64Image != null && base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            System.out.println("DEBUG: Recibida petición OCR. Tamaño base64: " + (base64Image != null ? base64Image.length() : 0));
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Guardar imagen original
            String filename = UUID.randomUUID().toString() + ".jpg";
            Path imagePath = Paths.get(UPLOAD_DIR, filename);
            Files.write(imagePath, imageBytes);
            File originalFile = imagePath.toFile();
            System.out.println("DEBUG: Imagen original guardada en: " + originalFile.getAbsolutePath());

            // ──────────────────────────────────────────────────────────────
            // OCR: GOOGLE CLOUD VISION (IA)
            // ──────────────────────────────────────────────────────────────
            String motorUtilizado = "Google Vision";
            String resultado = "";
            long startOcr = System.currentTimeMillis();
            try {
                System.out.println("DEBUG: Iniciando OCR con Google Cloud Vision...");
                resultado = googleVisionService.extraerTexto(imageBytes);
                long endOcr = System.currentTimeMillis();
                System.out.println("DEBUG: Google Vision completado en " + (endOcr - startOcr) + " ms.");
                if (resultado == null || resultado.trim().isEmpty()) {
                    throw new Exception("Google Vision retornó texto vacío.");
                }
            } catch (Exception e) {
                System.err.println("WARNING: Google Vision falló o vacío: " + e.getMessage());
                System.out.println("DEBUG: Usando Tesseract como fallback...");
                motorUtilizado = "Tesseract (Fallback)";
                
                // FALLBACK A TESSERACT (Local)
                long startTess = System.currentTimeMillis();
                File targetFile = preprocesarImagen(originalFile);
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath(new File("tessdata").getAbsolutePath());
                tesseract.setLanguage("spa");
                tesseract.setPageSegMode(3); // Cambiado a 3 (Automático) para mejor detección de bloques
                resultado = tesseract.doOCR(targetFile);
                long endTess = System.currentTimeMillis();
                System.out.println("DEBUG: Tesseract fallback completado en " + (endTess - startTess) + " ms.");
            }

            System.out.println("DEBUG: OCR Finalizado. Texto extraído: " + (resultado != null ? resultado.length() : 0) + " caracteres.");
            System.out.println("--- INICIO TEXTO OCR ---\n" + resultado + "\n--- FIN TEXTO OCR ---");

            // ──────────────────────────────────────────────────────────────
            // EXTRACCIÓN ROBUSTA MULTI-ESTRATEGIA
            // ──────────────────────────────────────────────────────────────
            String curp            = "";
            String claveElector    = "";
            String seccion         = "";
            String estado          = "";
            String direccion       = "";
            String nombre          = "";
            String apellidoPaterno = "";
            String apellidoMaterno = "";

            String[] lineas = resultado.split("\\r?\\n");

            // ── ESTRATEGIA 1: Búsqueda Global por Patrones (Lo más seguro para INE) ──
            String resultadoUp = resultado.toUpperCase();
            
            // CURP: 4 letras, 6 números, H/M, 2 letras estado, 3 consonantes, 2 dígitos
            curp = limpiarNull(extraerConRegex(resultadoUp, "[A-Z]{4}\\d{6}[HM][A-Z]{5}\\d{2}"));
            
            // Clave Elector: 6 letras, 8 números, H/M, 3 números
            claveElector = limpiarNull(extraerConRegex(resultadoUp, "[A-Z]{6}\\d{8}[HM]\\d{3}"));

            // CIC (Reverso): 9 dígitos aislados o dentro de la cadena OCR
            String cic = limpiarNull(extraerConRegex(resultadoUp, "(?<!\\d)(\\d{9})(?!\\d)"));
            if (cic.isEmpty()) {
                // Intentar extraer de la cadena OCR del reverso (IDMEX...<<9digitos...)
                cic = limpiarNull(extraerConRegex(resultadoUp, "IDMEX[^<]*<<(\\d{9})"));
            }

            // ── ESTRATEGIA 2: Lectura línea por línea para campos de texto ──
            for (int i = 0; i < lineas.length; i++) {
                String lineaRaw = lineas[i];
                String linea    = lineaRaw.trim().toUpperCase();

                // ---- NOMBRE ----
                if (linea.contains("NOMBRE") && !linea.contains("INSTITUTO") && !linea.contains("NACIONAL")) {
                    int found = 0;
                    for (int j = i; j < lineas.length && j < i + 5 && found < 3; j++) {
                        String clean = limpiarNombre(lineas[j].replace("NOMBRE", ""));
                        if (!clean.isEmpty()) {
                            if (found == 0) apellidoPaterno = clean;
                            else if (found == 1) apellidoMaterno = clean;
                            else if (found == 2) nombre = clean;
                            found++;
                        }
                    }
                }

                // ---- DOMICILIO ----
                if (direccion.isEmpty() && (linea.contains("DOMICILIO") || linea.contains("DIRECCI"))) {
                    StringBuilder dom = new StringBuilder();
                    for (int j = i + 1; j <= i + 3 && j < lineas.length; j++) {
                        String sig = lineas[j].trim().toUpperCase();
                        if (sig.length() < 5) continue; 
                        if (sig.contains("CLAVE") || sig.contains("CURP") || sig.contains("ESTADO") || sig.contains("SECCION")) break;
                        dom.append(lineas[j].trim()).append(" ");
                    }
                    direccion = dom.toString().trim();
                }

                // ---- ESTADO / SECCION ----
                if (estado.isEmpty() && linea.contains("ESTADO")) {
                    estado = extraerConRegex(linea, "ESTADO\\s*(\\d+)");
                }
                if (seccion.isEmpty() && (linea.contains("SECCION") || linea.contains("SECCI"))) {
                    seccion = extraerConRegex(linea, "SECCI[OÓN]{1,2}\\s*(\\d+)");
                }
            }

            if (seccion == null || seccion.isEmpty()) {
                seccion = limpiarNull(extraerConRegex(resultadoUp, "SECCI[OÓO]N[\\s:]*?(\\d{1,4})"));
            }

            // ── Limpieza final ──
            // limpiarNombre filtra dígitos y símbolos: evita que fechas como
            // "11/08/1999" o "SEXO H" contaminen los campos de nombre.
            nombre          = limpiarNombre(nombre);
            apellidoPaterno = limpiarNombre(apellidoPaterno);
            apellidoMaterno = limpiarNombre(apellidoMaterno);
            direccion       = limpiarTexto(direccion);
            curp            = limpiarNull(curp).toUpperCase().replaceAll("[^A-Z0-9]", "");
            claveElector    = limpiarNull(claveElector).toUpperCase().replaceAll("[^A-Z0-9]", "");
            if (seccion != null && seccion.length() > 4) seccion = seccion.substring(0, 4);

            // ── Construir registro de respuesta ──
            IneRegistro registro = new IneRegistro();
            registro.setCurp(curp);
            registro.setClaveElector(claveElector);
            registro.setCic(cic);
            registro.setSeccion(seccion == null || seccion.isEmpty() ? "0000" : seccion);
            registro.setEstado(limpiarNull(estado));
            registro.setDireccion(limpiarNull(direccion));
            registro.setNombre(nombre.isEmpty()           ? "REVISAR" : nombre);
            registro.setApellidoPaterno(apellidoPaterno.isEmpty() ? "REVISAR" : apellidoPaterno);
            registro.setApellidoMaterno(apellidoMaterno.isEmpty() ? "REVISAR" : apellidoMaterno);

            // RF-04 Validación Confianza
            boolean datosCompletos = !curp.isEmpty() && !claveElector.isEmpty()
                    && !nombre.isEmpty() && !apellidoPaterno.isEmpty();
            registro.setIndiceConfianza(datosCompletos ? 88.0 : 40.0);
            registro.setRequiereRevisionManual(!datosCompletos);
            registro.setCapturadoPor(usuarioLogueado);
            registro.setRutaImagen(imagePath.toAbsolutePath().toString());

            // ── VALIDACIÓN DE DUPLICADOS ──
            boolean yaExiste = false;
            if (curp != null && !curp.isEmpty() && !ineRepository.findByCurp(curp).isEmpty()) yaExiste = true;
            if (!yaExiste && claveElector != null && !claveElector.isEmpty() && !ineRepository.findByClaveElector(claveElector).isEmpty()) yaExiste = true;

            // Limpiamos el objeto usuario para la respuesta (evita errores de memoria/serialización)
            registro.setCapturadoPor(null);
            
            respuesta.put("status", "ok");
            if (yaExiste) {
                respuesta.put("mensaje", "⚠️ ATENCIÓN: Esta persona ya se encuentra registrada en el sistema.");
                respuesta.put("yaExiste", true);
            } else {
                respuesta.put("mensaje", "Datos extraídos con " + motorUtilizado + ". Por favor verifica.");
                respuesta.put("yaExiste", false);
            }
            respuesta.put("registro", registro);
            respuesta.put("motor", motorUtilizado);
            // Texto OCR crudo para diagnóstico en el frontend (F12 → Network)
            respuesta.put("ocrTextoRaw", resultado);

        } catch (Exception e) {
            respuesta.put("status", "error");
            respuesta.put("mensaje", "Error al procesar la imagen: " + e.getMessage());
        }

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Pre-procesa la imagen para mejorar el OCR en documentos con hologramas:
     * 1. Escala 1.5x (balance velocidad/calidad: más píxeles sin saturar la memoria)
     * 2. Convierte a escala de grises
     * 3. Aumenta el contraste (factor 1.5, offset -30)
     */
    private File preprocesarImagen(File original) throws Exception {
        BufferedImage img = ImageIO.read(original);

        // 1. Escalar 1.5x (no 2x, para evitar imágenes demasiado grandes que cuelgan el OCR)
        int w2 = (int)(img.getWidth() * 1.5);
        int h2 = (int)(img.getHeight() * 1.5);
        BufferedImage scaled = new BufferedImage(w2, h2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(img, 0, 0, w2, h2, null);
        g2d.dispose();

        // 2. Convertir a escala de grises
        BufferedImage gray = new BufferedImage(w2, h2, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g3 = gray.createGraphics();
        g3.drawImage(scaled, 0, 0, null);
        g3.dispose();

        // 3. Aumentar contraste: factor=1.5, offset=-30
        RescaleOp rescale = new RescaleOp(1.5f, -30f, null);
        BufferedImage contrasted = rescale.filter(gray, null);

        // Guardar como PNG (sin pérdida de calidad)
        File preprocessed = new File(UPLOAD_DIR + "pre_" + original.getName().replace(".jpg", ".png"));
        ImageIO.write(contrasted, "PNG", preprocessed);
        return preprocessed;
    }

    /** Extrae la primera coincidencia de un regex; retorna grupo 1 si existe, si no el grupo 0. */
    private String extraerConRegex(String texto, String regex) {
        if (texto == null) return null;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            if (matcher.groupCount() > 0) return matcher.group(1);
            return matcher.group();
        }
        return null;
    }

    /** Convierte null en string vacío. */
    private String limpiarNull(String s) {
        return s == null ? "" : s;
    }

    /**
     * Limpieza para campos de NOMBRE/APELLIDO:
     * - Solo letras y espacios (sin dígitos ni símbolos)
     * - Evita que fechas "11/08/1999" o valores como "SEXO H" contaminen el campo
     * - Descarta el resultado si tiene menos de 2 caracteres (ruido OCR)
     */
    private String limpiarNombre(String s) {
        if (s == null) return "";
        // Quitar etiquetas comunes que se cuelan por el diseño de la INE
        String limpio = s.toUpperCase()
                .replaceAll("\\b(?:FECHA|NACIMIENTO|SEXO|DOMICILIO|CLAVE|ELECTOR|CURP|ESTADO|SECCI[OÓN]|MUNICIPIO|LOCALIDAD|EMISI[OÓN]|VIGENCIA)\\b.*", "")
                .replaceAll("[^A-ZÁÉÍÓÚÑÜ \\-]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return limpio.length() < 2 ? "" : limpio;
    }

    /** Limpieza general para dirección (permite dígitos y algunos símbolos). */
    private String limpiarTexto(String s) {
        if (s == null) return "";
        return s.toUpperCase()
                .replaceAll("[^A-ZÁÉÍÓÚÑÜ0-9 \\-./,]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}