-- MySQL dump 10.13  Distrib 9.4.0, for Win64 (x86_64)
--
-- Host: localhost    Database: demo_db
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ine_registro`
--

DROP TABLE IF EXISTS `ine_registro`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ine_registro` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apellido_materno` varchar(255) DEFAULT NULL,
  `apellido_paterno` varchar(255) DEFAULT NULL,
  `clave_elector` varchar(255) DEFAULT NULL,
  `curp` varchar(18) DEFAULT NULL,
  `indice_confianza` double NOT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `requiere_revision_manual` bit(1) NOT NULL,
  `seccion` varchar(4) DEFAULT NULL,
  `fecha_captura` datetime(6) DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `estado` varchar(255) DEFAULT NULL,
  `ruta_imagen` varchar(255) DEFAULT NULL,
  `cic` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKem5c6cku7bw2ngfb89nhlyjam` (`clave_elector`),
  UNIQUE KEY `UKh8oqlq24b4tvx37plvy7x84v5` (`curp`),
  KEY `FK303w17m9f9jb6ondtj3ievrq` (`usuario_id`),
  KEY `idx_curp` (`curp`),
  KEY `idx_clave` (`clave_elector`),
  KEY `idx_nombre` (`nombre`),
  CONSTRAINT `FK303w17m9f9jb6ondtj3ievrq` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ine_registro`
--

LOCK TABLES `ine_registro` WRITE;
/*!40000 ALTER TABLE `ine_registro` DISABLE KEYS */;
INSERT INTO `ine_registro` VALUES (1,'GUERRERO','HERNANDEZ','CteOHH//Z/ZZI7QTB9qBKc34ofZHgTNCF6HbqN3NHv4=','HEGM990811HJCRRR03',88,'MARTIN',_binary '\0','2728','2026-05-11 17:32:49.286258',2,'C DE LA CANTERA ORIENTE 2829 FRACC JARDINES DE LA REINA 45410 TONALA, JAL.','14','C:\\Users\\LENOVO\\Downloads\\demo\\uploads\\5b31dacb-c007-449d-9fc9-7cc3ed760297.jpg',NULL);
/*!40000 ALTER TABLE `ine_registro` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `rol` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK863n1y3x0jalatoir4325ehal` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (1,'1234','admin','ADMIN'),(2,'1234','captura1','CAPTURISTA'),(3,'Martin','Martin','CAPTURISTA');
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14  9:28:49
