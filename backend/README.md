# 🚀 Notification Service - Backend Spring Boot

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Lancement](#lancement)
- [API Documentation](#api-documentation)
- [Kafka Integration](#kafka-integration)
- [WebSocket Integration](#websocket-integration)
- [Tests](#tests)
- [Troubleshooting](#troubleshooting)
- [Déploiement](#déploiement)
- [Contribution](#contribution)

---

## 🎯 Vue d'ensemble

Service backend Spring Boot pour un système de notifications en temps réel utilisant Apache Kafka et WebSockets.

### Objectifs

- Démontrer la maîtrise de Spring Boot, Kafka et WebSockets
- Architecture découplée et scalable
- Communication asynchrone avec Kafka
- Push en temps réel via STOMP/WebSocket
- Facilement démonstrable pour un recruteur

### Cas d'usage

1. **API REST** reçoit une requête POST avec une notification
2. **Producer Kafka** envoie la notification au topic `notifications-topic`
3. **Consumer Kafka** lit le message du topic
4. **WebSocket** broadcast la notification à tous les clients connectés
5. **Frontend Angular** affiche la notification instantanément

---

## 🏗️ Architecture

```
┌─────────────────┐
│  Client (HTTP)  │
└────────┬────────┘
         │ POST /api/notifications
         ↓
┌─────────────────────────────────┐
│   NotificationController        │
│   (REST API)                    │
└────────┬────────────────────────┘
         │
         ↓
┌─────────────────────────────────┐
│   NotificationProducer          │
│   (Kafka Producer)              │
└────────┬────────────────────────┘
         │ send()
         ↓
┌─────────────────────────────────┐
│   Apache Kafka                  │
│   Topic: notifications-topic    │
└────────┬────────────────────────┘
         │ consume()
         ↓
┌─────────────────────────────────┐
│   NotificationConsumer          │
│   (@KafkaListener)              │
└────────┬────────────────────────┘
         │ convertAndSend()
         ↓
┌─────────────────────────────────┐
│   WebSocket/STOMP               │
│   Destination: /topic/...       │
└────────┬────────────────────────┘
         │
         ↓
┌─────────────────────────────────┐
│   Clients WebSocket connectés   │
│   (Frontend Angular)            │
└─────────────────────────────────┘
```

## 🛠️ Technologies

| Technologie      | Version | Rôle                     |
| ---------------- | ------- | ------------------------ |
| Java             | 21      | Langage backend          |
| Spring Boot      | 3.2.0   | Framework principal      |
| Spring Kafka     | 3.1.0   | Intégration Kafka        |
| Spring WebSocket | 3.2.0   | Communication temps réel |
| Apache Kafka     | 3.6.0   | Messagerie asynchrone    |
| Lombok           | 1.18.30 | Réduction du boilerplate |
| Maven            | 3.9+    | Gestion des dépendances  |
| SLF4J / Logback  | 2.0     | Logging                  |

## ✅ Prérequis

### Obligatoires

- Java 17 ou supérieur
- Maven 3.9+
- Docker et Docker Compose
- Git

### Optionnels

- IntelliJ IDEA / Eclipse / VS Code (IDE recommandé)
- Postman ou curl pour tester l'API
- Kafka UI (inclus dans docker compose)

## 📦 Installation

1. Cloner le repository

```
git clone https://github.com/votre-username/notification-system-demo.git
cd notification-system-demo/backend/notification-service
```

2. Compiler le projet

### Compilation sans tests

```
mvn clean compile
```

### Compilation avec tests

```
mvn clean install
```

### Packager en JAR exécutable

```
mvn clean package
```

### Le JAR sera dans target/notification-service-1.0.0.jar

3. Lancer Kafka avec Docker

Depuis la racine du projet

```
cd ../../docker
docker compose up -d
```

### Vérifier que tout tourne

```
docker compose ps
# Devrait afficher : zookeeper (Up), kafka (Up), kafka-ui (Up)
```

### Voir les logs Kafka

```
docker compose logs -f kafka
```

4. Vérifier Kafka UI
   Ouvre ton navigateur : http://localhost:8090
   Tu devrais voir :

Cluster "local" connecté
Aucun topic pour l'instant (sera créé automatiquement au premier message)
