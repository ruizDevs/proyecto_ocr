package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ine_registro", indexes = {
    @Index(name = "idx_curp", columnList = "curp"),
    @Index(name = "idx_clave", columnList = "claveElector"),
    @Index(name = "idx_nombre", columnList = "nombre")
})
@Data
public class IneRegistro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 18)
    private String curp;

    @Column(length = 255)
    private String nombre;

    @Column(length = 255)
    private String apellidoPaterno;

    @Column(length = 255)
    private String apellidoMaterno;

    @Column(length = 4)
    private String seccion;

    @Convert(converter = StringCryptoConverter.class)
    @Column(unique = true, length = 255)
    private String claveElector;
    
    // Nuevos campos según RF-02
    private String estado;
    private String direccion;
    private String rutaImagen; // RF-05: Almacenamiento en File System

    // Requerimiento de confianza (RF-04)
    private double indiceConfianza;
    private boolean requiereRevisionManual;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario capturadoPor;

    @Column(name = "fecha_captura")
    private java.time.LocalDateTime fechaCaptura = java.time.LocalDateTime.now();
}
