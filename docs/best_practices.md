# 📚 Bonnes Pratiques et Remarques Qualité - TP 21

## 🎯 Objectif de ce document

Ce TP utilise des **simplifications pédagogiques** pour faciliter la compréhension. Voici les compromis faits et comment améliorer le code en production.

---

## 1️⃣ Pourquoi `.block()` est accepté ici ?

### Code actuel (ClientService)

```java
public Client findClientById(Long id) {
    return webClientBuilder.build()
        .get()
        .uri("http://SERVICE-CLIENT/api/clients/" + id)
        .retrieve()
        .bodyToMono(Client.class)
        .block();  // ⚠️ BLOQUANT
}
```

### ✅ Avantages pour le TP (approche débutant)

| Aspect | Justification |
|--------|---------------|
| **Simplicité** | Facile à comprendre pour les débutants |
| **Synchrone** | Comportement linéaire, pas de callbacks |
| **Débogage** | Plus facile à suivre dans le débogueur |
| **Pédagogie** | Focus sur Eureka/WebClient, pas réactif |

### ⚠️ Problèmes en production

```java
.block(); // BLOQUE LE THREAD jusqu'à la réponse !
```

**Conséquences** :
- Thread bloqué pendant la requête HTTP (latence)
- Réduit la capacité de traitement concurrent
- Annule les bénéfices du modèle réactif
- Risque de deadlock si mal utilisé

### ✅ Version PRODUCTION (réactive pure)

#### Option 1 : Retourner Mono directement

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

**Avantages** :
- ✅ Non-bloquant, meilleure performance
- ✅ Scalabilité accrue (threads libres)
- ✅ Gestion des erreurs réactive (retry, fallback)

#### Option 2 : CompletableFuture (plus proche de Java standard)

```java
public CompletableFuture<Client> findClientById(Long id) {
    return webClientBuilder.build()
        .get()
        .uri("http://SERVICE-CLIENT/api/clients/" + id)
        .retrieve()
        .bodyToMono(Client.class)
        .toFuture();
}
```

### 📊 Comparaison des approches

```
┌─────────────────┬──────────┬────────────┬─────────────┐
│ Approche        │ Difficulté│ Performance│ Production? │
├─────────────────┼──────────┼────────────┼─────────────┤
│ .block()        │    ⭐     │     ⭐⭐    │     ❌      │
│ Mono/Flux       │   ⭐⭐⭐   │    ⭐⭐⭐⭐⭐ │     ✅      │
│ CompletableFuture│   ⭐⭐    │    ⭐⭐⭐⭐  │     ✅      │
└─────────────────┴──────────┴────────────┴─────────────┘
```

### 🎓 Conclusion pour .block()

**Pour ce TP** : `.block()` est **acceptable** car :
- Focus sur architecture microservices, pas programmation réactive
- Code plus lisible pour les débutants
- Permet de comprendre WebClient sans complexité additionnelle

**En production** : Migrer vers `Mono<T>` / `Flux<T>` pour performances optimales.

---

## 2️⃣ Pourquoi éviter les relations JPA inter-services ?

### ❌ Ce qu'on ne fait PAS

```java
// MAUVAIS - Ne fonctionne pas en microservices !
@Entity
public class Car {
    @Id
    private Long id;
    
    @ManyToOne  // ❌ JPA ne peut pas joindre entre 2 BDD
    @JoinColumn(name = "client_id")
    private Client client;
}
```

### 🚫 Pourquoi c'est impossible ?

#### Problème 1 : Deux bases de données séparées

```
┌────────────────────────┐       ┌────────────────────────┐
│  carservicedb (MySQL)  │       │ clientservicedb (MySQL)│
│  ┌──────────────┐      │       │  ┌──────────────┐      │
│  │ table: car   │      │  ❌   │  │ table: client│      │
│  └──────────────┘      │       │  └──────────────┘      │
└────────────────────────┘       └────────────────────────┘
        Serveur 1                        Serveur 2
```

**JPA/Hibernate ne peut PAS** :
- Faire de `JOIN` SQL entre deux bases différentes
- Gérer les transactions distribuées automatiquement
- Garantir l'intégrité référentielle inter-bases

#### Problème 2 : Couplage fort

```java
@ManyToOne
private Client client;  // service-car DOIT accéder à la table client
```

**Conséquences** :
- Service-car dépend physiquement de la base clientservicedb
- Impossible de déployer/scaler indépendamment
- Violation du principe de microservices autonomes

### ✅ Pattern correct : ID + HTTP Call

#### Stockage : seulement l'ID

```java
@Entity
public class Car {
    @Id
    private Long id;
    private String marque;
    
    private Long clientId;  // ✅ Simple FK logique
    
    @Transient              // ✅ Non persisté
    private Client client;  // Enrichi à la lecture
}
```

#### Récupération : via HTTP

```java
// 1. Lire de la base locale
Car car = carRepository.findById(1).get();

// 2. Appeler le service distant
Client client = webClient.get()
    .uri("http://SERVICE-CLIENT/api/clients/" + car.getClientId())
    .retrieve()
    .bodyToMono(Client.class)
    .block();

// 3. Enrichir l'objet
car.setClient(client);
```

### 📐 Architecture distribuée correcte

```
┌─────────────────────────────────────────────────────┐
│                  SERVICE-CAR (8082)                  │
│  ┌──────────┐         ┌─────────────────┐          │
│  │   Car    │         │  ClientService  │          │
│  │----------|         │  (WebClient)    │          │
│  │ clientId │─────────▶│                 │          │
│  │ @Transient│        │  HTTP GET       │          │
│  │  client  │◀────────│  /clients/{id}  │          │
│  └──────────┘         └─────────────────┘          │
└─────────────────────────────────────────────────────┘
                            │
                            │ HTTP Request
                            ▼
┌─────────────────────────────────────────────────────┐
│              SERVICE-CLIENT (8081)                   │
│  ┌──────────────────────────────────────┐           │
│  │      ClientController                │           │
│  │    GET /api/clients/{id}             │           │
│  │    ─────────────────────             │           │
│  │    return clientRepository           │           │
│  │           .findById(id)               │           │
│  └──────────────────────────────────────┘           │
└─────────────────────────────────────────────────────┘
```

### 🎯 Avantages du pattern ID + HTTP

| Avantage | Explication |
|----------|-------------|
| **Autonomie** | Chaque service gère sa propre base |
| **Scalabilité** | Services scalent indépendamment |
| **Indépendance** | Déploiements séparés possibles |
| **Résilience** | Panne d'un service n'affecte pas les autres |
| **Technologies** | Chaque base peut utiliser un SGBD différent |

### 🔧 Gestion des cas d'erreur

```java
public List<Car> findAll() {
    List<Car> cars = carRepository.findAll();
    
    cars.forEach(car -> {
        try {
            Client client = clientService.findClientById(car.getClientId());
            car.setClient(client);
        } catch (WebClientException e) {
            // SERVICE-CLIENT indisponible
            car.setClient(null); // Dégradation gracieuse
            log.warn("Client {} unavailable", car.getClientId());
        }
    });
    
    return cars;
}
```

**En production, ajouter** :
- Circuit breaker (Resilience4j)
- Cache (Redis) pour réduire les appels
- Fallback values
- Retry avec backoff

---

## 3️⃣ Autres améliorations production

### Cache Redis pour réduire les appels

```java
@Cacheable(value = "clients", key = "#id")
public Client findClientById(Long id) {
    return webClient.get()...
}
```

### Circuit Breaker (Resilience4j)

```java
@CircuitBreaker(name = "clientService", fallbackMethod = "getDefaultClient")
public Client findClientById(Long id) {
    return webClient.get()...
}

public Client getDefaultClient(Long id, Exception e) {
    return new Client(id, "Unknown", 0.0f);
}
```

### Timeout configuré

```java
.timeout(Duration.ofSeconds(2))
.onErrorReturn(new Client())
```

### Pagination pour grandes listes

```java
@GetMapping
public Page<Car> findAll(Pageable pageable) {
    return carRepository.findAll(pageable);
}
```

---

## 📝 Tableau récapitulatif

| Pattern / Choix | TP (Pédagogie) | Production |
|----------------|----------------|------------|
| **WebClient.block()** | ✅ Simple | ❌ Privilégier Mono/Flux |
| **Relations JPA inter-services** | ❌ Impossible | ❌ Utiliser ID + HTTP |
| **Gestion d'erreurs** | ⚠️ Basique | ✅ Circuit breaker + retry |
| **Cache** | ❌ Absent | ✅ Redis recommandé |
| **Pagination** | ❌ Absente | ✅ Nécessaire |
| **Timeout** | ❌ Par défaut | ✅ Configurer explicitement |

---

## 🎓 Conclusion

### Ce TP vous a appris

✅ **Architecture microservices** avec Eureka  
✅ **Communication inter-services** via WebClient  
✅ **Pattern correct** : ID + HTTP au lieu de JPA foreign key  
✅ **Service discovery** dynamique  

### Pour aller plus loin en production

- [ ] Migrer vers programmation réactive complète (Mono/Flux)
- [ ] Ajouter Resilience4j pour circuit breaker
- [ ] Implémenter un cache distribué (Redis)
- [ ] Gérer les erreurs avec fallback gracieux
- [ ] Ajouter OpenAPI/Swagger pour documentation
- [ ] Implémenter observabilité (Zipkin, Prometheus)
- [ ] Sécuriser avec Spring Security + JWT

---

**Rappel important** : Les simplifications de ce TP sont **intentionnelles** pour faciliter l'apprentissage. En production, privilégiez toujours les patterns réactifs et robustes.

**Date de rédaction** : 19/12/2025  
**Niveau** : Intermédiaire → Avancé  
**Prérequis pour production** : Programmation réactive, patterns de résilience
