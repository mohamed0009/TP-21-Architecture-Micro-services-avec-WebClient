# 🚀 TP 21 : Architecture Microservices avec WebClient
<img width="1310" height="962" alt="1" src="https://github.com/user-attachments/assets/03cc172d-63e6-4570-af9c-1f72e910bc95" />
<img width="1310" height="962" alt="Screenshot 2025-12-19 171256" src="https://github.com/user-attachments/assets/c0310e59-7f86-4ecb-9bfd-b5bfab17f9c0" />
<img width="1617" height="803" alt="Screenshot 2025-12-19 172312" src="https://github.com/user-attachments/assets/e8c42284-f4be-41d3-b909-1a1d0882bc4b" />
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)

Projet pédagogique démontrant une **architecture microservices complète** avec **Eureka Server**, **WebClient** et **communication inter-services dynamique**.

---

|-----|-------------|
| http://localhost:8761 | Dashboard de visualisation |
| http://localhost:8761/eureka/apps | API REST des services enregistrés |

---

## 🛠️ Technologies

### Backend
- **Spring Boot 3.2.1** - Framework principal
- **Spring Cloud 2023.0.0** - Stack microservices
- **Spring Cloud Netflix Eureka** - Service discovery
- **Spring WebFlux** - WebClient réactif
- **Spring Data JPA** - Accès base de données
- **Hibernate** - ORM

### Base de données
- **MySQL 8.0+** - SGBD relationnel

### Build & Dépendances
- **Maven 3.6+** - Gestion des dépendances
- **Java 17** - Langage

---

## 📖 Documentation

### Guides complets

- **[Guide complet (Walkthrough)](docs/walkthrough.md)** : Tutoriel détaillé pas à pas
- **[Bonnes pratiques](docs/best_practices.md)** : `.block()` vs réactif, pourquoi éviter JPA inter-services
- **[Dépannage](docs/troubleshooting.md)** : Solutions aux 4 erreurs les plus fréquentes

### Rapports de validation

- [Validation Eureka Server](docs/validation_eureka.md)
- [Validation SERVICE-CLIENT](docs/validation_service_client.md)
- [Validation complète](docs/validation_complete.md)

---
