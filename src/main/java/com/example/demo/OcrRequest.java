package com.example.demo;

import lombok.Data;

@Data // Esto genera automáticamente getters y setters gracias a Lombok
public class OcrRequest {
    private String imagen; // Aquí se guarda el texto Base64 de la foto
}