# Service Car - Microservice avec Communication WebClient

## 📋 Vue d'ensemble

Ce microservice gère les voitures et démontre la **communication inter-services via WebClient** et **Eureka**. Il enrichit automatiquement les données de voiture avec les informations du client propriétaire en appelant `SERVICE-CLIENT`.

## 🎯 Fonctionnalités clés

- **Gestion des voitures** : CRUD sur les entités Car
- **Enrichissement de données** : Récupération automatique des infos client
- **WebClient + Eureka** : Communication par nom de service (pas d'IP en dur)
- **Load Balancing** : Support multi-instances via @LoadBalanced

## 🛠️ Stack technique

| Technologie | Rôle |
|-------------|------|
| **Spring Boot 3.2.1** | Framework principal |
| **Spring Data JPA** | Persistance MySQL |
| **Spring WebFlux** | WebClient pour HTTP client réactif |
| **Eureka Client** | Découverte de services |
| **MySQL** | Base de données |

## 📊 Modèle de données

### Entité Car

```java
@Entity
public class Car {
    @Id @GeneratedValue
    private Long id;
    private String marque;
    private String modele;
    private Long clientId;          // Foreign key logique
    
    @Transient                       // Non persisté en base
    private Client client;           // Enrichi via WebClient
}
```

### POJO Client (non-JPA)

```java
public class Client {
    private Long id;
    private String nom;
    private Float age;
}
```

Ce POJO reçoit les données JSON de `SERVICE-CLIENT`.

## 🔌 Communication WebClient

### Configuration

```java
@Bean
@LoadBalanced
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}
```

**@LoadBalanced** permet à WebClient de résoudre les noms Eureka en IP réelles.

### Appel REST

```java
webClientBuilder.build()
    .get()
    .uri("http://SERVICE-CLIENT/api/clients/" + id)
    .retrieve()
    .bodyToMono(Client.class)
    .block();
```

**Avantages** :
- ✅ Nom de service au lieu d'IP
- ✅ Eureka résout automatiquement l'adresse
- ✅ Load balancing si plusieurs instances
- ✅ Pas de couplage fort

## 🔗 API REST

Base URL : `http://localhost:8082/api/cars`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/cars` | Liste toutes les voitures **enrichies** |
| `GET` | `/api/cars/{id}` | Récupère une voiture **enrichie** |
| `POST` | `/api/cars` | Crée une nouvelle voiture |

### Exemples

**Créer une voiture** :
```bash
POST http://localhost:8082/api/cars
Content-Type: application/json

{
  "marque": "Toyota",
  "modele": "Yaris",
  "clientId": 1
}
```

**Récupérer toutes les voitures (enrichies)** :
```bash
GET http://localhost:8082/api/cars
```

**Réponse** :
```json
[
  {
    "id": 1,
    "marque": "Toyota",
    "modele": "Yaris",
    "clientId": 1,
    "client": {
      "id": 1,
      "nom": "Salma",
      "age": 22.0
    }
  }
]
```

## ⚙️ Configuration

```yaml
server:
  port: 8082

spring:
  application:
    name: SERVICE-CAR

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

## 🚀 Démarrage

### Prérequis
- Eureka Server actif (port 8761)
- SERVICE-CLIENT actif (port 8081)
- MySQL actif (port 3306)

### Ordre de lancement
```bash
# 1. Eureka Server
cd eureka-server
mvn spring-boot:run

# 2. Service Client
cd service-client
mvn spring-boot:run

# 3. Service Car
cd service-car
mvn clean install
mvn spring-boot:run
```

### Vérifications
1. Dashboard Eureka → http://localhost:8761
   - Voir `SERVICE-CLIENT` et `SERVICE-CAR` enregistrés
2. Tester GET → http://localhost:8082/api/cars

## 🏗️ Architecture de communication

```
Client (navigateur)
        ↓
   CarController
        ↓
   CarRepository → MySQL (table car)
        ↓
   ClientService
        ↓
   WebClient (@LoadBalanced)
        ↓
   Eureka Server (résolution SERVICE-CLIENT → localhost:8081)
        ↓
   SERVICE-CLIENT /api/clients/{id}
        ↓
   Client JSON retourné
```

## 💡 Points techniques clés

### @Transient
```java
@Transient
private Client client;
```
Ce champ n'est **pas stocké en MySQL**. Il est calculé à la volée lors de la lecture.

### .block()
```java
.bodyToMono(Client.class).block();
```
Convertit l'appel réactif en appel synchrone (bloquant). En production, utilisez plutôt `.subscribe()` ou retournez `Mono<Car>`.

### Eureka Service Name
```java
uri("http://SERVICE-CLIENT/api/clients/" + id)
```
`SERVICE-CLIENT` est le nom déclaré dans `application.yml` du service distant. Eureka le résout en `http://localhost:8081`.

## 🔧 Troubleshooting

### Erreur "No instances available for SERVICE-CLIENT"
- Vérifier que SERVICE-CLIENT est enregistré dans Eureka
- Attendre 30s après démarrage pour la propagation du registre

### Données client null
- Vérifier que le clientId existe dans la base clientservicedb
- Vérifier les logs pour erreurs WebClient

### Port 8082 occupé
- Changer `server.port` dans application.yml

## 📚 Scénario de test complet

```bash
# 1. Créer un client
POST http://localhost:8081/api/clients
{"nom": "Salma", "age": 22}

# 2. Noter l'ID retourné (ex: 1)

# 3. Créer une voiture liée
POST http://localhost:8082/api/cars
{"marque": "Toyota", "modele": "Yaris", "clientId": 1}

# 4. Lire les voitures enrichies
GET http://localhost:8082/api/cars
# → client.nom="Salma" apparaît automatiquement !
```

---

**Version** : 1.0.0  
**Port** : 8082  
**Base de données** : carservicedb  
**Dépend de** : SERVICE-CLIENT (via Eureka)
