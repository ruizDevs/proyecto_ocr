package com.example.demo.servicio;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GoogleVisionService {

    @Value("${google.cloud.api-key:}")
    private String apiKey;

    private ImageAnnotatorClient client;

    /**
     * Inicializa el cliente de Google Vision al arrancar la aplicación.
     * Esto ahorra varios segundos en cada petición posterior.
     */
    @PostConstruct
    public void init() {
        try {
            ImageAnnotatorSettings settings;
            if (apiKey != null && !apiKey.isEmpty()) {
                System.out.println("DEBUG: Inicializando Google Vision con API Key...");
                settings = ImageAnnotatorSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(null))
                        .setHeaderProvider(() -> Map.of("X-Goog-Api-Key", apiKey))
                        .build();
            } else if (Files.exists(Paths.get("google-credentials.json"))) {
                System.out.println("DEBUG: Inicializando Google Vision con JSON...");
                settings = ImageAnnotatorSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(
                                GoogleCredentials.fromStream(new FileInputStream("google-credentials.json"))))
                        .build();
            } else {
                System.out.println("DEBUG: Inicializando Google Vision con credenciales de sistema...");
                settings = ImageAnnotatorSettings.newBuilder().build();
            }
            this.client = ImageAnnotatorClient.create(settings);
            System.out.println("DEBUG: Cliente de Google Vision listo y conectado.");
        } catch (IOException e) {
            System.err.println("ERROR: No se pudo inicializar el cliente de Google Vision: " + e.getMessage());
        }
    }

    public String extraerTexto(byte[] imageBytes) throws IOException {
        if (this.client == null) {
            throw new IOException("El cliente de Google Vision no está inicializado.");
        }

        ByteString imgBytes = ByteString.copyFrom(imageBytes);
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        // La llamada batchAnnotateImages es muy rápida cuando el cliente ya está abierto
        BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        for (AnnotateImageResponse res : responses) {
            if (res.hasError()) {
                throw new IOException("Error de Google Vision: " + res.getError().getMessage());
            }
            if (res.hasFullTextAnnotation()) {
                return res.getFullTextAnnotation().getText();
            }
        }
        return "";
    }

    @PreDestroy
    public void close() {
        if (this.client != null) {
            this.client.close();
            System.out.println("DEBUG: Cliente de Google Vision cerrado.");
        }
    }
}
