package com.example.demo.modelo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data // Esto de Lombok te ahorra escribir los Getters y Setters
public class IneRegistro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campos obligatorios según tu matriz
    @Column(unique = true, length = 18)
    private String curp;

    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;

    @Column(length = 4)
    private String seccion;

    @Column(unique = true, length = 18)
    private String claveElector;

    // Requerimiento de confianza (RF-04)
    private double indiceConfianza;
    private boolean requiereRevisionManual;

    @ManyToOne // Muchos registros pueden ser capturados por un mismo usuario
    @JoinColumn(name = "usuario_id")
    private Usuario capturadoPor;

    @Column(name = "fecha_captura")
    private java.time.LocalDateTime fechaCaptura = java.time.LocalDateTime.now();
}
