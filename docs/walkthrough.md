# 🎓 TP 21 : Architecture Microservices avec WebClient - Guide Complet

## 📋 Vue d'ensemble du projet

Ce projet implémente une **architecture microservices complète** démontrant la découverte de services via Eureka et la communication inter-services avec WebClient.

### Architecture déployée

```
┌─────────────────────────────────────────────────────────┐
│                   Eureka Server (8761)                   │
│              Registre centralisé de services             │
└─────────────────────────────────────────────────────────┘
                    ▲                    ▲
                    │ Heartbeat          │ Heartbeat
                    │                    │
        ┌───────────┴──────────┐    ┌───┴─────────────────┐
        │  SERVICE-CLIENT      │    │   SERVICE-CAR       │
        │      (8081)          │◄───┤      (8082)         │
        │                      │    │   WebClient +       │
        │  - API Clients       │    │   @LoadBalanced     │
        │  - MySQL DB          │    │                     │
        └──────────────────────┘    └─────────────────────┘
                 │                           │
                 ▼                           ▼
        ┌────────────────┐          ┌────────────────┐
        │ clientservicedb│          │  carservicedb  │
        └────────────────┘          └────────────────┘
```

---

## 🏗️ Service 1 : Eureka Server

### Rôle
Registre centralisé permettant aux microservices de se découvrir dynamiquement sans connaître leurs IPs.

### Configuration clé

**application.yml** :
```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false  # Ne s'enregistre pas lui-même
    fetch-registry: false          # Ne récupère pas de registre
```

### Classe principale

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**Point clé** : `@EnableEurekaServer` active toute la machinerie Eureka (dashboard, API REST, heartbeat).

### Validation

✅ Dashboard accessible : http://localhost:8761  
✅ Section "Instances currently registered" visible

---

## 🏗️ Service 2 : SERVICE-CLIENT (8081)

### Rôle
Microservice gérant les clients avec persistance MySQL.

### Entité Client

```java
@Entity
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private Float age;
}
```

### Configuration Eureka Client

**application.yml** :
```yaml
spring:
  application:
    name: SERVICE-CLIENT  # Nom utilisé dans Eureka

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

**Point clé** : `spring.application.name` devient l'identifiant du service dans Eureka.

### Endpoints REST

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/clients` | Liste tous les clients |
| GET | `/api/clients/{id}` | Récupère un client par ID |
| POST | `/api/clients` | Crée un nouveau client |

### Validation

✅ Service apparaît dans Eureka comme "SERVICE-CLIENT"  
✅ Base MySQL `clientservicedb` créée automatiquement  
✅ Table `client` générée par Hibernate DDL

---

## 🏗️ Service 3 : SERVICE-CAR (8082)

### Rôle
Microservice gérant les voitures, **enrichit automatiquement** les données avec les infos clients via WebClient.

### Entités

#### Car (persistée en MySQL)
```java
@Entity
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String marque;
    private String modele;
    private Long clientId;
    
    @Transient  // Non stocké en base !
    private Client client;
}
```

#### Client (POJO, non-JPA)
```java
public class Client {
    private Long id;
    private String nom;
    private Float age;
}
```

**Point clé** : `@Transient` signifie que `client` est calculé à la volée, pas stocké.

### Configuration WebClient (⭐ CRUCIAL)

**ServiceCarApplication.java** :
```java
@Bean
@LoadBalanced
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}
```

**Pourquoi @LoadBalanced ?**  
Sans cette annotation, WebClient ne peut pas résoudre les noms Eureka (`SERVICE-CLIENT`) en IP réelles.

### Communication inter-services

**ClientService.java** :
```java
@Service
public class ClientService {
    private final WebClient.Builder webClientBuilder;

    public Client findClientById(Long id) {
        return webClientBuilder.build()
            .get()
            .uri("http://SERVICE-CLIENT/api/clients/" + id)
            .retrieve()
            .bodyToMono(Client.class)
            .block();
    }
}
```

**Points clés** :
- `http://SERVICE-CLIENT` : nom Eureka, pas IP
- Eureka résout automatiquement vers `http://localhost:8081`
- `.block()` : rend l'appel synchrone (pour simplicité pédagogique)

### Enrichissement dans le Controller

**CarController.java** :
```java
@GetMapping
public List<Car> findAll() {
    List<Car> cars = carRepository.findAll();
    
    // Enrichissement automatique
    cars.forEach(car -> {
        if (car.getClientId() != null) {
            Client client = clientService.findClientById(car.getClientId());
            car.setClient(client);
        }
    });
    
    return cars;
}
```

**Flux détaillé** :
1. Récupère les voitures depuis MySQL (clientId stocké)
2. Pour chaque voiture, appelle SERVICE-CLIENT via WebClient
3. Assigne le client récupéré au champ `@Transient`
4. Retourne le JSON enrichi

---

## 🧪 Tests End-to-End

### Scénario complet

#### Étape 1 : Créer un client
```bash
POST http://localhost:8081/api/clients
Content-Type: application/json

{"nom": "Salma", "age": 22}

# Réponse
{"id": 3, "nom": "Salma", "age": 22.0}
```

#### Étape 2 : Créer une voiture liée
```bash
POST http://localhost:8082/api/cars
Content-Type: application/json

{"marque": "Toyota", "modele": "Yaris", "clientId": 3}

# Réponse
{"id": 1, "marque": "Toyota", "modele": "Yaris", "clientId": 3}
```

#### Étape 3 : Lire les voitures enrichies
```bash
GET http://localhost:8082/api/cars

# Réponse enrichie
[
  {
    "id": 1,
    "marque": "Toyota",
    "modele": "Yaris",
    "clientId": 3,
    "client": {
      "id": 3,
      "nom": "Salma",
      "age": 22.0
    }
  }
]
```

✅ **Succès** : Le champ `client` contient les données complètes récupérées depuis SERVICE-CLIENT !

---

## 🔑 Concepts clés démontrés

### 1. Service Discovery (Eureka)
- Les services s'enregistrent automatiquement au démarrage
- Eureka maintient un registre à jour via heartbeats
- Pas besoin de connaître les IPs en dur

### 2. @LoadBalanced
- Permet à WebClient de résoudre les noms Eureka
- Active le client-side load balancing (si plusieurs instances)
- Essentiel pour la communication inter-services

### 3. WebClient vs RestTemplate
- **WebClient** : moderne, réactif, supporté par Spring
- **RestTemplate** : déprécié, synchrone
- WebClient permet `.block()` (sync) ou `.subscribe()` (async)

### 4. Pattern d'enrichissement
- Stocke seulement l'ID de la relation (`clientId`)
- Enrichit les données à la lecture via API call
- Alternative au JOIN SQL classique en microservices

### 5. @Transient
- Champ non persisté en base de données
- Calculé dynamiquement lors de la sérialisation JSON
- Parfait pour les données enrichies

---

## 📁 Structure des projets

```
tp platfroms/
├── eureka-server/
│   ├── pom.xml (spring-cloud-starter-netflix-eureka-server)
│   ├── src/main/resources/application.yml
│   └── src/main/java/.../EurekaServerApplication.java
│
├── service-client/
│   ├── pom.xml (web, jpa, mysql, eureka-client)
│   ├── src/main/resources/application.yml
│   └── src/main/java/com/example/client/
│       ├── ServiceClientApplication.java
│       ├── entities/Client.java
│       ├── repositories/ClientRepository.java
│       └── web/ClientController.java
│
└── service-car/
    ├── pom.xml (web, jpa, mysql, eureka-client, webflux)
    ├── src/main/resources/application.yml
    └── src/main/java/com/example/car/
        ├── ServiceCarApplication.java (@LoadBalanced)
        ├── entities/
        │   ├── Car.java (@Transient client)
        │   └── Client.java (POJO)
        ├── repositories/CarRepository.java
        ├── services/ClientService.java (WebClient)
        └── web/CarController.java (enrichissement)
```

---

## 🚀 Ordre de lancement (IMPORTANT)

```bash
# Terminal 1 - TOUJOURS EN PREMIER
cd eureka-server
mvn spring-boot:run
# Attendre dashboard accessible sur :8761

# Terminal 2
cd service-client
mvn spring-boot:run
# Vérifier apparition dans Eureka

# Terminal 3
cd service-car
mvn spring-boot:run
# Vérifier apparition dans Eureka
```

**Pourquoi cet ordre ?**  
Si Eureka n'est pas démarré, les clients ne peuvent pas s'enregistrer et échouent au boot.

---

## 🔧 Dépendances Maven clés

### Eureka Server
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

### Eureka Client (dans service-client et service-car)
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### WebClient (service-car uniquement)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

## 💡 Troubleshooting commun

### "No instances available for SERVICE-CLIENT"
- Vérifier que SERVICE-CLIENT tourne et est enregistré dans Eureka
- Attendre 30s pour propagation du registre Eureka
- Vérifier `spring.application.name` correspond

### Données client null dans la réponse
- Vérifier que le `clientId` existe dans la base clientservicedb
- Vérifier les logs de WebClient pour erreurs HTTP
- Tester manuellement `http://localhost:8081/api/clients/{id}`

### Port déjà occupé
- Modifier `server.port` dans application.yml
- Ou arrêter le processus utilisant le port

---

## 📊 Résultats attendus

### Dashboard Eureka
![Dashboard showing both services](file:///C:/Users/ROG/.gemini/antigravity/brain/40d46776-1c7e-4ef7-906e-0cd0618d89b6/eureka_dashboard_check_1766161085474.png)

- SERVICE-CLIENT : UP (1)
- SERVICE-CAR : UP (1)

### Réponse enrichie
```json
{
  "id": 1,
  "marque": "Toyota",
  "modele": "Yaris",
  "clientId": 3,
  "client": {
    "id": 3,
    "nom": "Salma",
    "age": 22.0
  }
}
```

Le champ `client` est la **preuve** de la communication WebClient réussie.

---

## 🎓 Objectifs pédagogiques atteints

✅ Comprendre le rôle d'Eureka Server  
✅ Enregistrer des microservices comme clients Eureka  
✅ Utiliser `application.yml` pour configuration  
✅ Appeler un service par son nom Eureka avec WebClient  
✅ Implémenter le pattern d'enrichissement de données  
✅ Tester progressivement (dashboard + endpoints)  

---

## 📚 Étape 5 : Remarques Qualité & Bonnes Pratiques

### 5.1 Pourquoi `.block()` est accepté ici ?

#### Code utilisé dans le TP

```java
public Client findClientById(Long id) {
    return webClientBuilder.build()
        .get()
        .uri("http://SERVICE-CLIENT/api/clients/" + id)
        .retrieve()
        .bodyToMono(Client.class)
        .block();  // ⚠️ Approche synchrone
}
```

#### ✅ Acceptable pour ce TP

**Raisons pédagogiques** :
- **Simplicité** : Facilite la compréhension pour débutants
- **Focus** : L'objectif est Eureka/WebClient, pas la programmation réactive
- **Débogage** : Code linéaire plus facile à suivre

#### ⚠️ En production : éviter .block()

**Problèmes** :
- Bloque un thread pendant la requête HTTP
- Réduit la capacité de traitement concurrent
- Annule les bénéfices du modèle réactif

**Alternative production** (réactive pure) :

```java
// Service
public Mono<Client> findClientById(Long id) {
    return webClientBuilder.build()
        .get()
        .uri("http://SERVICE-CLIENT/api/clients/" + id)
        .retrieve()
        .bodyToMono(Client.class);
        // Pas de .block() !
}

// Controller
@GetMapping
public Mono<List<Car>> findAll() {
    return Flux.fromIterable(carRepository.findAll())
        .flatMap(car -> 
            clientService.findClientById(car.getClientId())
                .map(client -> {
                    car.setClient(client);
                    return car;
                })
        )
        .collectList();
}
```

**Avantages réactifs** :
- Non-bloquant, meilleure performance
- Scalabilité accrue
- Gestion d'erreurs réactive (retry, fallback, circuit breaker)

---

### 5.2 Pourquoi éviter les relations JPA inter-services ?

#### ❌ Ce qu'on NE fait PAS

```java
// INCORRECT en microservices !
@Entity
public class Car {
    @ManyToOne  // ❌ JPA ne peut pas joindre entre 2 BDD
    @JoinColumn(name = "client_id")
    private Client client;
}
```

#### 🚫 Raisons techniques

**Problème 1 : Deux bases de données séparées**

```
carservicedb (MySQL)     ❌ JOIN impossible ❌     clientservicedb (MySQL)
    table: car                                        table: client
```

**JPA/Hibernate ne peut PAS** :
- Faire de `JOIN` SQL entre deux bases différentes
- Gérer les transactions distribuées automatiquement
- Garantir l'intégrité référentielle

**Problème 2 : Couplage fort**

- Service-car dépendrait de la structure de clientservicedb
- Impossible de déployer/scaler indépendamment
- Violation du principe de microservices autonomes

#### ✅ Pattern correct : ID + HTTP Call

```java
@Entity
public class Car {
    private Long clientId;  // ✅ Simple FK logique
    
    @Transient              // ✅ Non persisté en base
    private Client client;  // Enrichi via HTTP
}
```

**Avantages** :
- ✅ Autonomie complète des services
- ✅ Scalabilité indépendante
- ✅ Résilience (panne d'un service n'affecte pas l'autre)
- ✅ Flexibilité technologique (chaque service peut utiliser un SGBD différent)

#### Production : ajouter résilience

```java
@CircuitBreaker(name = "clientService", fallbackMethod = "getDefaultClient")
@Cacheable(value = "clients", key = "#id")
public Client findClientById(Long id) {
    return webClient.get()
        .uri("http://SERVICE-CLIENT/api/clients/" + id)
        .retrieve()
        .bodyToMono(Client.class)
        .timeout(Duration.ofSeconds(2))
        .block();
}

public Client getDefaultClient(Long id, Exception e) {
    return new Client(id, "Unknown", 0.0f); // Fallback
}
```

**Améliorations production** :
- Circuit breaker (Resilience4j)
- Cache distribué (Redis)
- Timeout configuré
- Retry avec backoff
- Fallback gracieux

---

## 🏆 Conclusion

Ce TP démontre une **architecture microservices moderne et découplée** :

- **Pas d'IP en dur** : Communication par noms logiques
- **Centralisé** : Eureka comme source de vérité
- **Scalable** : @LoadBalanced permet multi-instances
- **Testable** : Chaque service fonctionne indépendamment
- **Production-ready** : Technologies standards Spring Cloud

**Technologies maîtrisées** :
- Spring Boot 3.2.1
- Spring Cloud Netflix Eureka
- Spring WebFlux (WebClient)
- Spring Data JPA
- MySQL avec auto-DDL
- Architecture microservices

---

**Durée de réalisation** : 2-3 heures  
**Niveau** : Intermédiaire  
**Prérequis** : Spring Boot, REST API, MySQL basics
