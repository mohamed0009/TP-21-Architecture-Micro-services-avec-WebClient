# 🔧 Guide de Dépannage - TP 21 Microservices

## 🎯 Les 4 erreurs les plus fréquentes

Ce guide vous aide à résoudre rapidement les problèmes courants rencontrés dans ce TP.

---

## ❌ Erreur 1 : "No instances available for SERVICE-CLIENT"

### Symptôme

```
WebClientRequestException: No instances available for SERVICE-CLIENT
```

ou

```
java.net.UnknownHostException: SERVICE-CLIENT
```

### 🔍 Diagnostic

Ouvrez le dashboard Eureka (http://localhost:8761) :
- **Si SERVICE-CLIENT est visible** → Problème de configuration WebClient
- **Si SERVICE-CLIENT est absent** → Problème d'enregistrement Eureka

---

### ✅ Solution 1.1 : @LoadBalanced absent

**Cause** : WebClient ne sait pas résoudre les noms Eureka.

**Vérification** :

```java
// ServiceCarApplication.java
@Bean
@LoadBalanced  // ⚠️ Cette annotation est OBLIGATOIRE
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}
```

**Si l'annotation manque** :
1. Ajoutez `@LoadBalanced` au-dessus de `@Bean`
2. Ajoutez l'import : `import org.springframework.cloud.client.loadbalancer.LoadBalanced;`
3. Redémarrez service-car

---

### ✅ Solution 1.2 : Dépendance LoadBalancer absente

**Cause** : Spring Cloud LoadBalancer n'est pas dans le classpath.

**Vérification** du `pom.xml` :

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- Cette dépendance contient LoadBalancer -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**Si WebFlux manque** :
1. Ajoutez la dépendance dans `pom.xml`
2. Exécutez `mvn clean install`
3. Redémarrez le service

---

### ✅ Solution 1.3 : SERVICE-CLIENT non enregistré dans Eureka

**Cause 1** : Eureka Client non configuré

**Vérification** du `pom.xml` de service-client :

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Vérification** de `application.yml` :

```yaml
spring:
  application:
    name: SERVICE-CLIENT  # Ne PAS oublier

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka  # Vérifier l'URL
```

**Cause 2** : Eureka Server non démarré

1. Vérifiez que Eureka Server tourne : `http://localhost:8761`
2. Si absent, démarrez-le : `cd eureka-server && mvn spring-boot:run`
3. Attendez 30 secondes pour la propagation du registre

**Cause 3** : SERVICE-CLIENT a crashé après démarrage

Vérifiez les logs :
```bash
# Cherchez "Started ServiceClientApplication" dans les logs
# Si absent, vérifiez les erreurs MySQL ou autres exceptions
```

---

## ❌ Erreur 2 : Service visible dans Eureka mais WebClient échoue

### Symptôme

- Dashboard Eureka montre SERVICE-CLIENT avec status UP
- Mais WebClient retourne 404 ou timeout

```
WebClientResponseException$NotFound: 404 Not Found
```

### ✅ Solution 2.1 : Endpoint incorrect

**Vérification** de l'URI dans ClientService :

```java
.uri("http://SERVICE-CLIENT/api/clients/" + id)
//   ^^^^^^^^^^^^^^^^^^^^^^^^ Nom Eureka
//                         ^^^^^^^^^^^^^^ Chemin endpoint
```

**Points à vérifier** :
1. Nom du service correspond à `spring.application.name` :
   ```yaml
   spring:
     application:
       name: SERVICE-CLIENT  # Doit correspondre exactement
   ```

2. Chemin de l'endpoint existe dans ClientController :
   ```java
   @RestController
   @RequestMapping("/api/clients")  // ✅ Correspond
   public class ClientController {
       @GetMapping("/{id}")  // ✅ Donne /api/clients/{id}
   ```

**Test manuel** :
```bash
# Testez directement avec l'IP
curl http://localhost:8081/api/clients/1

# Si ça marche, le problème est dans WebClient
# Si ça échoue, le problème est dans le Controller
```

---

### ✅ Solution 2.2 : SERVICE-CLIENT démarré mais crash

**Symptôme** : Service apparaît dans Eureka puis disparaît.

**Vérification des logs** :

```bash
# Cherchez les exceptions après "Started ServiceClientApplication"
# Erreurs courantes :
# - NullPointerException
# - SQL syntax error
# - Connection pool exhausted
```

**Action** :
1. Arrêtez service-client
2. Corrigez l'erreur identifiée dans les logs
3. Redémarrez et vérifiez qu'il reste UP

---

### ✅ Solution 2.3 : Problèmes réseau (rare en local)

**Si vous utilisez un proxy/VPN** :

Ajoutez dans `application.yml` de service-car :

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
  instance:
    prefer-ip-address: true  # Force l'utilisation de l'IP
```

**Si vous avez modifié /etc/hosts** :

Vérifiez qu'il n'y a pas d'entrée conflictuelle :
```bash
# Windows
notepad C:\Windows\System32\drivers\etc\hosts

# Supprimez toute ligne mentionnant localhost autre que 127.0.0.1
```

---

## ❌ Erreur 3 : Problème MySQL au démarrage

### Symptôme

```
com.mysql.cj.jdbc.exceptions.CommunicationsException: 
Communications link failure
```

ou

```
Access denied for user 'root'@'localhost'
```

### ✅ Solution 3.1 : MySQL arrêté

**Vérification** :

```bash
# Windows
services.msc
# Cherchez "MySQL" et vérifiez qu'il est "Démarré"

# Ou testez la connexion
mysql -u root -p
```

**Si MySQL est arrêté** :
1. Démarrez le service MySQL
2. Attendez 10 secondes
3. Relancez votre microservice

---

### ✅ Solution 3.2 : Mauvais password

**Vérification** de `application.yml` :

```yaml
spring:
  datasource:
    username: root
    password:      # ⚠️ Vérifiez votre mot de passe MySQL
```

**Test manuel** :
```bash
mysql -u root -p
Enter password: [votre_password]

# Si connexion OK : password correct
# Si connexion échouée : corrigez dans application.yml
```

**Password vide** :
```yaml
password:   # Laissez vide si pas de password
# ou
password: ""
```

---

### ✅ Solution 3.3 : Base non créée et droits insuffisants

**Symptôme** : Même avec `createDatabaseIfNotExist=true`, erreur de création.

**Cause** : L'utilisateur MySQL n'a pas les droits CREATE DATABASE.

**Solution** : Créer manuellement la base

```sql
-- Connectez-vous à MySQL
mysql -u root -p

-- Créez les bases
CREATE DATABASE clientservicedb;
CREATE DATABASE carservicedb;

-- Vérifiez
SHOW DATABASES;

-- Quittez
EXIT;
```

**Ou donnez les droits à l'utilisateur** :

```sql
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

---

### ✅ Solution 3.4 : Port MySQL incorrect

**Vérification** :

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/clientservicedb
    #                          ^^^^ Port MySQL (défaut = 3306)
```

**Si MySQL tourne sur un autre port** :
1. Trouvez le port : `SHOW VARIABLES LIKE 'port';` dans MySQL
2. Modifiez dans `application.yml`

---

## ❌ Erreur 4 : 404 sur endpoints REST

### Symptôme

```
GET http://localhost:8081/api/clients
→ 404 Not Found
```

### ✅ Solution 4.1 : Chemin Controller incorrect

**Vérification** du Controller :

```java
@RestController
@RequestMapping("/api/clients")  // ✅ Base path
public class ClientController {
    
    @GetMapping  // ✅ GET /api/clients
    public List<Client> findAll() { ... }
    
    @GetMapping("/{id}")  // ✅ GET /api/clients/{id}
    public Client findById(@PathVariable Long id) { ... }
}
```

**Erreurs fréquentes** :

❌ Oubli de `@RestController` → Controller ne répond pas  
❌ `@Controller` au lieu de `@RestController` → Cherche une vue JSP  
❌ Oubli de `@RequestMapping` → Endpoints à la racine `/`  
❌ Doublon `/api/api/clients` :
```java
@RequestMapping("/api")
@GetMapping("/api/clients")  // ❌ Donne /api/api/clients
```

---

### ✅ Solution 4.2 : Erreur de port

**Symptôme** : Vous testez le mauvais service.

**Vérification** :

| Service | Port | Endpoints |
|---------|------|-----------|
| Eureka Server | 8761 | http://localhost:8761 (dashboard) |
| SERVICE-CLIENT | 8081 | http://localhost:8081/api/clients |
| SERVICE-CAR | 8082 | http://localhost:8082/api/cars |

**Erreur courante** :
```bash
# ❌ Mauvais port
GET http://localhost:8081/api/cars  
→ 404 (car est sur 8082, pas 8081)

# ✅ Bon port
GET http://localhost:8082/api/cars
```

**Vérification du port dans application.yml** :

```yaml
server:
  port: 8081  # SERVICE-CLIENT
# ou
  port: 8082  # SERVICE-CAR
```

---

### ✅ Solution 4.3 : Service pas complètement démarré

**Symptôme** : Requête trop tôt après démarrage.

**Vérification des logs** :

```bash
# Attendez ce message avant de tester
Started ServiceClientApplication in X.XXX seconds
```

**Si le démarrage prend > 30s** :
- Problème de connexion MySQL (timeout)
- Problème de connexion Eureka (retry)
- Vérifiez les logs pour identifier la cause

---

## 🔍 Checklist de diagnostic général

Utilisez cette checklist pour tout problème :

### 1. Services démarrés ?

```bash
# Vérifiez les 3 terminaux
Terminal 1: eureka-server    → "Started EurekaServerApplication"
Terminal 2: service-client   → "Started ServiceClientApplication"
Terminal 3: service-car      → "Started ServiceCarApplication"
```

### 2. Eureka Dashboard OK ?

```
http://localhost:8761
→ Voir SERVICE-CLIENT et SERVICE-CAR avec status UP (1)
```

### 3. MySQL actif ?

```bash
mysql -u root -p
mysql> SHOW DATABASES;
# Voir clientservicedb et carservicedb
```

### 4. Endpoints accessibles ?

```bash
GET http://localhost:8081/api/clients  → [...]
GET http://localhost:8082/api/cars     → [...]
```

### 5. Logs propres ?

```bash
# Pas d'exceptions rouges après "Started ..."
# Heartbeat Eureka visible toutes les 30s
```

---

## 🛠️ Commandes de dépannage utiles

### Vérifier les ports occupés

```bash
# Windows
netstat -ano | findstr :8761
netstat -ano | findstr :8081
netstat -ano | findstr :8082
```

### Tuer un processus sur port occupé

```bash
# Windows (PID trouvé avec netstat)
taskkill /PID [PID] /F
```

### Vérifier MySQL tourne

```bash
# Windows
sc query MySQL80
# État doit être "RUNNING"
```

### Logs Spring Boot verbeux

Ajoutez dans `application.yml` :

```yaml
logging:
  level:
    org.springframework.web: DEBUG
    com.netflix.discovery: DEBUG
```

---

## 📞 Que faire si rien ne marche ?

### 1. Clean restart complet

```bash
# Arrêtez TOUS les services (Ctrl+C dans chaque terminal)

# Nettoyez Maven
cd service-client
mvn clean

cd ../service-car
mvn clean

cd ../eureka-server
mvn clean

# Rebuild tout
cd ../service-client
mvn install

cd ../service-car
mvn install

cd ../eureka-server
mvn install

# Relancez dans l'ordre
cd ../eureka-server
mvn spring-boot:run

# Attendez dashboard Eureka accessible

cd ../service-client
mvn spring-boot:run

# Attendez apparition dans Eureka

cd ../service-car
mvn spring-boot:run

# Attendez apparition dans Eureka
```

### 2. Vérifiez les versions

**pom.xml** :

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>  <!-- Vérifiez cohérence -->
</parent>

<properties>
    <java.version>17</java.version>  <!-- Java 17+ requis -->
    <spring-cloud.version>2023.0.0</spring-cloud.version>
</properties>
```

### 3. Vérifiez Java version

```bash
java -version
# Doit afficher Java 17 ou supérieur
```

---

## 🎓 Résumé des erreurs

| Erreur | Cause probable | Solution rapide |
|--------|----------------|-----------------|
| **No instances available** | @LoadBalanced manquant | Ajouter annotation + restart |
| **404 Not Found** | Mauvais port ou chemin | Vérifier URL complète |
| **MySQL connection** | Service MySQL arrêté | Démarrer MySQL |
| **Service non visible Eureka** | defaultZone incorrect | Vérifier application.yml |

---

**Date de mise à jour** : 19/12/2025  
**Pour** : TP 21 - Architecture Microservices avec WebClient
