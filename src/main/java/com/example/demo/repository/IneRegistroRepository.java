package com.example.demo.repository;

import com.example.demo.modelo.IneRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IneRegistroRepository extends JpaRepository<IneRegistro, Long> {
    java.util.List<IneRegistro> findByCapturadoPor(com.example.demo.modelo.Usuario capturadoPor);

    java.util.List<IneRegistro> findByCurp(String curp);
    java.util.List<IneRegistro> findByClaveElector(String claveElector);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM IneRegistro i WHERE " +
            "LOWER(i.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(i.apellidoPaterno) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(i.curp) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "i.seccion = :query")
    java.util.List<IneRegistro> searchByKeyword(@org.springframework.data.repository.query.Param("query") String query);
}
