# Service Client - Microservice de Gestion des Clients

## 📋 Vue d'ensemble

Ce microservice gère les opérations CRUD sur les clients. Il s'enregistre automatiquement dans **Eureka Server** pour être découvrable par les autres services de l'architecture.

## 🎯 Fonctionnalités

- **Gestion des clients** : Création, lecture, modification des données clients
- **Persistance MySQL** : Base de données relationnelle avec JPA/Hibernate
- **Découverte de services** : Enregistrement automatique dans Eureka
- **API REST** : Endpoints standardisés pour communication inter-services

## 🛠️ Stack technique

| Technologie | Rôle |
|-------------|------|
| **Spring Boot 3.2.1** | Framework principal |
| **Spring Data JPA** | Couche d'abstraction base de données |
| **MySQL** | Base de données relationnelle |
| **Eureka Client** | Client de découverte de services |
| **Spring Web** | API REST |

## 📊 Modèle de données

### Entité Client

```java
@Entity
public class Client {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private Float age;
}
```

**Table MySQL créée automatiquement** : `client`

## 🔌 API REST

Base URL : `http://localhost:8081/api/clients`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/clients` | Liste tous les clients |
| `GET` | `/api/clients/{id}` | Récupère un client par ID |
| `POST` | `/api/clients` | Crée un nouveau client |

### Exemples de requêtes

**Créer un client** :
```bash
POST http://localhost:8081/api/clients
Content-Type: application/json

{
  "nom": "Ahmed",
  "age": 30
}
```

**Récupérer tous les clients** :
```bash
GET http://localhost:8081/api/clients
```

## ⚙️ Configuration

### Port
```yaml
server.port: 8081
```

### Nom de service Eureka
```yaml
spring.application.name: SERVICE-CLIENT
```
Ce nom apparaît dans le dashboard Eureka et peut être utilisé par WebClient pour appeler ce service.

### Base de données
```yaml
spring.datasource.url: jdbc:mysql://localhost:3306/clientservicedb?createDatabaseIfNotExist=true
```
- La base `clientservicedb` est créée automatiquement si elle n'existe pas
- `ddl-auto: update` crée/met à jour le schéma de table automatiquement

### Eureka Client
```yaml
eureka.client.service-url.defaultZone: http://localhost:8761/eureka
```
Le service s'enregistre automatiquement au démarrage et envoie des heartbeats périodiques.

## 🚀 Démarrage

### Prérequis
- Java 17+
- Maven 3.6+
- MySQL actif sur port 3306
- Eureka Server lancé sur port 8761

### Lancement
```bash
cd service-client
mvn clean install
mvn spring-boot:run
```

### Vérifications
1. **Application démarrée** : Console affiche `Started ServiceClientApplication`
2. **Base créée** : Table `client` existe dans MySQL
3. **Enregistré dans Eureka** : Dashboard http://localhost:8761 montre `SERVICE-CLIENT`

## 🔍 Architecture layered

```
web/
  ClientController.java      → Couche REST (HTTP)
      ↓
repositories/
  ClientRepository.java       → Couche accès données (JPA)
      ↓
entities/
  Client.java                 → Modèle de domaine (ORM)
```

## 💡 Points techniques clés

### Auto-registration Eureka
Avec Spring Cloud 2023+, **pas besoin de @EnableEurekaClient** explicite. La présence de la dépendance `eureka-client` suffit.

### JpaRepository avantages
```java
interface ClientRepository extends JpaRepository<Client, Long>
```
Fournit gratuitement :
- `findAll()`, `findById()`
- `save()`, `delete()`
- Pagination, tri, requêtes dérivées

### Hibernate DDL auto
```yaml
ddl-auto: update
```
En production, utilisez `validate` + Flyway/Liquibase pour contrôler les migrations.

## 🔐 Améliorations futures

- [ ] Validation des données (@Valid, @NotNull, etc.)
- [ ] Gestion d'erreurs centralisée (@ControllerAdvice)
- [ ] Pagination des résultats
- [ ] Endpoints DELETE et PUT
- [ ] Tests unitaires et d'intégration
- [ ] Documentation OpenAPI/Swagger

## 🔧 Troubleshooting

### Le service n'apparaît pas dans Eureka
- Vérifier que Eureka Server tourne sur port 8761
- Vérifier `defaultZone` dans application.yml
- Vérifier que `spring.application.name` est défini

### Erreur MySQL
- Vérifier que MySQL est démarré
- Vérifier username/password dans application.yml
- Vérifier droits de création de base

### Port 8081 déjà occupé
- Changer `server.port` dans application.yml

---

**Version** : 1.0.0  
**Port** : 8081  
**Base de données** : clientservicedb
