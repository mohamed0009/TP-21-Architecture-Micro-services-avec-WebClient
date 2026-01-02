# Eureka Server - Service de Découverte

## 📋 Vue d'ensemble

Ce module implémente le serveur Eureka, qui agit comme **registre central de services** dans notre architecture microservices. Il permet aux différents services de se découvrir et de communiquer entre eux de manière dynamique, sans configuration IP en dur.

## 🎯 Rôle dans l'architecture

Le serveur Eureka joue un rôle critique :
- **Registre de services** : Maintient une liste à jour de tous les microservices actifs
- **Health checking** : Surveille la disponibilité des services enregistrés
- **Load balancing** : Permet la répartition des requêtes entre instances multiples
- **Découverte dynamique** : Les services se trouvent par leur nom logique, pas par IP

## 🛠️ Configuration technique

### Port d'écoute
```yaml
server.port: 8761
```
Port standard pour Eureka Server, reconnu dans l'écosystème Spring Cloud.

### Mode autonome
```yaml
eureka.client.register-with-eureka: false
eureka.client.fetch-registry: false
```
Ces paramètres empêchent le serveur de s'enregistrer lui-même comme client, évitant ainsi une boucle inutile en mode standalone.

### Logging optimisé
```yaml
logging.level.com.netflix.eureka: OFF
```
Désactivation des logs verbeux Netflix pour une sortie console plus claire en développement.

## 🚀 Démarrage

### Prérequis
- Java 17+
- Maven 3.6+

### Lancement
```bash
cd eureka-server
mvn clean install
mvn spring-boot:run
```

### Vérification
Accédez au dashboard : [http://localhost:8761](http://localhost:8761)

Vous devriez voir l'interface Eureka avec "Instances currently registered" vide au démarrage (normal).

## 📊 Dashboard Eureka

Le dashboard web affiche :
- Liste des services enregistrés
- Nombre d'instances par service
- Statut de santé (UP/DOWN)
- Métadonnées de chaque instance

## 🔧 Dépendances clés

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

Cette dépendance unique apporte :
- Le serveur Eureka complet
- Le dashboard web intégré
- L'API REST pour l'enregistrement des clients

## 💡 Points techniques importants

### @EnableEurekaServer
Cette annotation active toute la machinerie Eureka :
- Démarre le serveur de registre
- Expose le dashboard web
- Active l'API d'enregistrement REST
- Configure le heartbeat checking

### Architecture haute disponibilité
En production, on déploierait plusieurs instances Eureka en mode cluster :
```yaml
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka-peer1:8761/eureka/,http://eureka-peer2:8762/eureka/
```

## 🔍 Troubleshooting

### Port déjà occupé
Si le port 8761 est utilisé, modifiez `server.port` dans `application.yml`.

### Erreur de dépendances
Vérifiez que `spring-cloud.version` correspond bien à votre version de Spring Boot.

### Dashboard inaccessible
Vérifiez que le firewall autorise les connexions sur le port 8761.

## 📚 Prochaines étapes

Une fois Eureka Server opérationnel, vous pourrez :
1. Créer des microservices clients Eureka
2. Les enregistrer automatiquement au démarrage
3. Les faire communiquer par nom de service
4. Bénéficier du load balancing automatique

---

**Version** : 1.0.0  
**Spring Boot** : 3.2.1  
**Spring Cloud** : 2023.0.0  
**Java** : 17
