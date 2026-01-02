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

## 📋 Table des matières

- [Vue d'ensemble](#-vue-densemble)
- [Architecture](#-architecture)
- [Fonctionnalités](#-fonctionnalités)
- [Screenshots](#-screenshots)
- [Installation](#-installation)
- [Utilisation](#-utilisation)
- [Tests End-to-End](#-tests-end-to-end)
- [Technologies](#-technologies)
- [Documentation](#-documentation)
- [Dépannage](#-dépannage)

---

## 🎯 Vue d'ensemble

Ce projet illustre les concepts clés des **microservices modernes** :

- ✅ **Service Discovery** avec Eureka Server
- ✅ **Communication inter-services** via WebClient et @LoadBalanced
- ✅ **Pattern d'enrichissement** de données distribuées
- ✅ **Bases de données séparées** par microservice
- ✅ **Découverte dynamique** sans IP en dur

### Microservices implémentés

| Service | Port | Rôle |
|---------|------|------|
| **Eureka Server** | 8761 | Registre et découverte de services |
| **SERVICE-CLIENT** | 8081 | Gestion des clients (MySQL) |
| **SERVICE-CAR** | 8082 | Gestion des voitures + WebClient enrichissement |

---

## 🏗️ Architecture

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
        │                      │    │                     │
        │  API REST Clients    │    │  WebClient +        │
        │  MySQL Database      │    │  @LoadBalanced      │
        └──────────────────────┘    │  Enrichissement     │
                 │                   └─────────────────────┘
                 ▼                           │
        ┌────────────────┐                  ▼
        │ clientservicedb│          ┌────────────────┐
        └────────────────┘          │  carservicedb  │
                                    └────────────────┘
```

### Flux de communication WebClient

```
1. GET /api/cars (SERVICE-CAR)
        ↓
2. Récupération voitures depuis MySQL
        ↓
3. Pour chaque voiture.clientId
        ↓
4. WebClient.get("http://SERVICE-CLIENT/api/clients/{id}")
        ↓
5. Eureka résout "SERVICE-CLIENT" → localhost:8081
        ↓
6. HTTP GET vers SERVICE-CLIENT
        ↓
7. Récupération données client
        ↓
8. Enrichissement: car.setClient(client)
        ↓
9. Retour JSON enrichi au navigateur
```

---

## ✨ Fonctionnalités

### 🔍 Service Discovery
- Enregistrement automatique dans Eureka au démarrage
- Heartbeat périodique pour health checking
- Dashboard web de visualisation

### 🌐 Communication inter-services
- **WebClient** avec résolution Eureka
- **@LoadBalanced** pour découverte dynamique
- Pas d'IP/port en dur dans le code
- Support du load balancing automatique

### 📊 Enrichissement de données
- Pattern **ID + HTTP Call** au lieu de JPA @ManyToOne
- Champ **@Transient** pour données calculées
- Agrégation de données provenant de services différents

### 💾 Persistance
- **MySQL** avec Spring Data JPA
- Auto-création des bases de données
- Hibernate DDL automatique (développement)

---

## 📸 Screenshots

### Dashboard Eureka - Services enregistrés

![Eureka Dashboard](images/eureka_dashboard.png)

Les deux microservices **SERVICE-CLIENT** et **SERVICE-CAR** apparaissent avec le statut **UP**.

### Test API - Enrichissement WebClient

![Test API End-to-End](images/api_test.webp)

Réponse enrichie montrant le champ `client` récupéré automatiquement depuis SERVICE-CLIENT via WebClient.

### Test Client API

![Client API Test](images/client_api_test.webp)

Démonstration des tests POST et GET sur le service CLIENT.

### Scénario End-to-End

```json
GET http://localhost:8082/api/cars

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

✅ Le champ `client` est la **preuve** que WebClient a récupéré les données depuis SERVICE-CLIENT !

---

## 🚀 Installation

### Prérequis

- **Java 17+** : `java -version`
- **Maven 3.6+** : `mvn -version`
- **MySQL 8.0+** : Service actif sur port 3306
- **Git** : Pour cloner le projet

### Cloner le projet

```bash
git clone https://github.com/RadimYassin/TP-21-Architecture-Microservices-avec-WebClient---Guide-Complet.git
cd TP-21-Architecture-Microservices-avec-WebClient---Guide-Complet
```

### Configuration MySQL

```sql
-- Connectez-vous à MySQL
mysql -u root -p

-- Créez les bases (optionnel, auto-créées par Spring)
CREATE DATABASE clientservicedb;
CREATE DATABASE carservicedb;

-- Vérifiez
SHOW DATABASES;
```

### Build des projets

```bash
# Build Eureka Server
cd eureka-server
mvn clean install

# Build SERVICE-CLIENT
cd ../service-client
mvn clean install

# Build SERVICE-CAR
cd ../service-car
mvn clean install
```

---

## 🎮 Utilisation

### Démarrage des services (ORDRE IMPORTANT)

#### 1️⃣ Eureka Server (TOUJOURS EN PREMIER)

```bash
cd eureka-server
mvn spring-boot:run
```

Attendez le message : `Started EurekaServerApplication in X.XXX seconds`

Dashboard accessible : **http://localhost:8761**

#### 2️⃣ SERVICE-CLIENT

```bash
cd service-client
mvn spring-boot:run
```

Attendez le message : `Started ServiceClientApplication in X.XXX seconds`

✅ Vérifiez dans Eureka : **SERVICE-CLIENT** doit apparaître

#### 3️⃣ SERVICE-CAR

```bash
cd service-car
mvn spring-boot:run
```

Attendez le message : `Started ServiceCarApplication in X.XXX seconds`

✅ Vérifiez dans Eureka : **SERVICE-CAR** doit apparaître

---

## 🧪 Tests End-to-End

### Scénario complet de test

#### Étape 1 : Créer un client

```bash
POST http://localhost:8081/api/clients
Content-Type: application/json

{
  "nom": "Salma",
  "age": 22
}
```

**Réponse attendue** :
```json
{
  "id": 1,
  "nom": "Salma",
  "age": 22.0
}
```

#### Étape 2 : Créer une voiture liée au client

```bash
POST http://localhost:8082/api/cars
Content-Type: application/json

{
  "marque": "Toyota",
  "modele": "Yaris",
  "clientId": 1
}
```

**Réponse attendue** :
```json
{
  "id": 1,
  "marque": "Toyota",
  "modele": "Yaris",
  "clientId": 1
}
```

#### Étape 3 : Récupérer les voitures enrichies (⭐ IMPORTANT)

```bash
GET http://localhost:8082/api/cars
```

**Réponse enrichie via WebClient** :
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

✅ **SUCCÈS** : Le champ `client` contient les données complètes récupérées depuis SERVICE-CLIENT !

---

## 📚 API Endpoints

### SERVICE-CLIENT (port 8081)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/clients` | Liste tous les clients |
| GET | `/api/clients/{id}` | Récupère un client par ID |
| POST | `/api/clients` | Crée un nouveau client |

**Exemple de body POST** :
```json
{
  "nom": "Ahmed",
  "age": 30
}
```

### SERVICE-CAR (port 8082)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/cars` | Liste toutes les voitures **enrichies** |
| GET | `/api/cars/{id}` | Récupère une voiture **enrichie** |
| POST | `/api/cars` | Crée une nouvelle voiture |

**Exemple de body POST** :
```json
{
  "marque": "Renault",
  "modele": "Clio",
  "clientId": 1
}
```

### Eureka Server (port 8761)

| URL | Description |
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

## 🔧 Dépannage

### ❌ Erreur : "No instances available for SERVICE-CLIENT"

**Solutions** :
1. Vérifiez `@LoadBalanced` sur `WebClient.Builder`
2. Vérifiez que SERVICE-CLIENT apparaît dans Eureka
3. Attendez 30 secondes pour propagation du registre

### ❌ Erreur : 404 Not Found

**Solutions** :
1. Vérifiez le bon port (8081 pour clients, 8082 pour cars)
2. Vérifiez que le service est complètement démarré
3. Vérifiez l'URL exacte : `/api/clients` vs `/api/cars`

### ❌ Erreur : MySQL connection failed

**Solutions** :
1. Vérifiez que MySQL tourne : `services.msc` (Windows)
2. Vérifiez username/password dans `application.yml`
3. Créez manuellement les bases si nécessaire

**Pour plus de détails**, consultez le [Guide de dépannage complet](docs/troubleshooting.md).

---

## 🎓 Concepts clés démontrés

### 1. Service Discovery
- Pas d'IP/port en dur dans le code
- Enregistrement automatique via Eureka
- Résolution dynamique des noms de services

### 2. @LoadBalanced
```java
@Bean
@LoadBalanced
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}
```
Permet à WebClient de résoudre `http://SERVICE-CLIENT` en `http://localhost:8081`

### 3. Pattern ID + HTTP Call
```java
@Entity
public class Car {
    private Long clientId;  // ✅ Stocker seulement l'ID
    
    @Transient
    private Client client;  // ✅ Enrichir via HTTP
}
```

Pourquoi **pas de @ManyToOne** ?
- Bases de données séparées (JPA ne peut pas JOIN)
- Services autonomes et indépendants
- Scalabilité et résilience

### 4. .block() vs Réactif
```java
// TP (pédagogique)
.bodyToMono(Client.class).block();

// Production (réactif)
.bodyToMono(Client.class);  // Retourner Mono<Client>
```

---

## 📊 Structure du projet

```
.
├── eureka-server/              # Port 8761
│   ├── src/main/java/
│   │   └── .../EurekaServerApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── service-client/             # Port 8081
│   ├── src/main/java/com/example/client/
│   │   ├── ServiceClientApplication.java
│   │   ├── entities/Client.java
│   │   ├── repositories/ClientRepository.java
│   │   └── web/ClientController.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── service-car/                # Port 8082
│   ├── src/main/java/com/example/car/
│   │   ├── ServiceCarApplication.java
│   │   ├── entities/
│   │   │   ├── Car.java
│   │   │   └── Client.java (POJO)
│   │   ├── repositories/CarRepository.java
│   │   ├── services/ClientService.java (WebClient)
│   │   └── web/CarController.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── docs/                       # Documentation
└── README.md                   # Ce fichier
```

---

## 🤝 Contribution

Ce projet est à but pédagogique. Les contributions sont les bienvenues !

1. Fork le projet
2. Créez votre branche (`git checkout -b feature/amelioration`)
3. Committez vos changements (`git commit -m 'Ajout fonctionnalité'`)
4. Push vers la branche (`git push origin feature/amelioration`)
5. Ouvrez une Pull Request

---

## 📝 License

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

---

## 👤 Auteur

**Radim Yassin**

- GitHub: [@RadimYassin](https://github.com/RadimYassin)
- Projet: [TP 21 Microservices](https://github.com/RadimYassin/TP-21-Architecture-Microservices-avec-WebClient---Guide-Complet)

---

## 🌟 Remerciements

- Spring Boot Team pour l'excellent framework
- Netflix OSS pour Eureka
- Communauté Spring Cloud

---

## 📌 Liens utiles

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
- [WebClient Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client)

---

**⭐ Si ce projet vous a aidé, n'oubliez pas de laisser une étoile !**

---

**Dernière mise à jour** : 19 Décembre 2025  
**Version** : 1.0.0  
**Statut** : ✅ Production Ready

"# TP-21-Architecture-Micro-services-avec-WebClient" 
