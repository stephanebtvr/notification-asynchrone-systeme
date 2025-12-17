# Real-Time Notifications Demo

## 1. Objectif du projet

Ce projet est une **application de démonstration full‑stack** visant à illustrer, de manière visuelle et concrète, la maîtrise des technologies suivantes :

- **Spring Boot (Java)** : API REST, intégration Kafka, WebSockets/STOMP
- **Apache Kafka** : messagerie asynchrone, découplage producer/consumer
- **Angular** : frontend réactif, WebSockets, RxJS
- **Docker / Docker Compose** : infrastructure locale reproductible

L’application implémente un **système de notifications en temps réel** :

1. Un utilisateur envoie une notification via une API REST.
2. Le backend publie cette notification dans un topic Kafka.
3. Un consumer Kafka consomme le message.
4. Le message est diffusé instantanément aux clients connectés via WebSockets.
5. L’interface Angular affiche la notification en temps réel, sans rechargement.

🎯 **But principal** : fournir à un recruteur une preuve tangible de compétences backend, frontend et messaging distribué, via une application simple, lisible et rapide à lancer.

---

## 2. Périmètre fonctionnel

### Fonctionnalités

- Envoi de notifications via formulaire (Angular → API REST)
- Publication asynchrone via Kafka
- Consommation Kafka côté backend
- Push temps réel via WebSockets (STOMP)
- Affichage instantané des notifications dans l’UI

### Hors périmètre (choix assumés)

- ❌ Pas de base de données persistante (stockage en mémoire)
- ❌ Pas d’authentification
- ❌ Pas de gestion multi‑topics ou partitions avancées

Ces choix garantissent un **focus maximal sur Kafka, Spring Boot et Angular**, sans complexité inutile.

---

## 3. Architecture globale

### Vue d’ensemble

```
┌─────────────┐        REST        ┌──────────────────┐
│   Angular   │ ─────────────────▶ │  Spring Boot API │
│   Frontend  │                    │  (Producer Kafka)│
│             │                    └─────────┬────────┘
│             │                              │
│             │                              ▼
│             │                    ┌────────────────────┐
│             │                    │   Kafka Broker     │
│             │                    │ notifications-topic│
│             │                    └─────────┬──────────┘
│             │                              │
│             │                              ▼
│             │                    ┌──────────────────┐
│             │   WebSocket / STOMP│ Kafka Consumer + │
│             │ ◀───────────────── │ WebSocket Broker │
└─────────────┘                    └──────────────────┘
```

### Pourquoi Kafka ?

- Découplage strict entre producteurs et consommateurs
- Traitement asynchrone
- Scalabilité horizontale naturelle
- Cas d’usage réaliste en architecture microservices

Même dans une application simple, Kafka démontre une **architecture professionnelle et extensible**.

---

## 4. Stack technique

### Backend

- Java 21
- Spring Boot

  - spring-boot-starter-web
  - spring-kafka
  - spring-boot-starter-websocket

- Maven

### Frontend

- Angular 18+
- TypeScript
- RxJS
- STOMP.js + SockJS
- Tailwind CSS (UI moderne, utilitaire)

### Infrastructure

- Apache Kafka
- Zookeeper
- Docker & Docker Compose

---

## 5. Prérequis système

Assurez-vous d’avoir installé :

- **Java 21**

  ```bash
  java -version
  ```

- **Maven 3.8+**

  ```bash
  mvn -version
  ```

- **Node.js 18+ & npm**

  ```bash
  node -v
  npm -v
  ```

- **Angular CLI**

  ```bash
  npm install -g @angular/cli
  ```

- **Docker & Docker Compose**

  ```bash
  docker --version
  docker compose version
  ```

---

## 6. Lancement du projet en local

### 6.1 Démarrage de Kafka

```bash
docker compose up -d
```

Cela démarre :

- Zookeeper
- Kafka Broker
- Topic `notifications-topic` (créé automatiquement)

---

### 6.2 Lancement du backend Spring Boot

```bash
cd backend
mvn clean spring-boot:run
```

Le backend démarre sur :

```
http://localhost:8080
```

Endpoints exposés :

- `POST /api/notifications`
- WebSocket : `/ws-notifications`

---

### 6.3 Lancement du frontend Angular

```bash
cd frontend
npm install
ng serve
```

Application disponible sur :

```
http://localhost:4200
```

---

## 7. Scénario de démonstration

1. Ouvrir deux navigateurs ou onglets distincts
2. Accéder à `http://localhost:4200`
3. Dans l’un des onglets :

   - Saisir un message
   - Cliquer sur **Envoyer**

4. Observer :

   - La notification apparaît instantanément dans **tous** les clients connectés
   - Les logs backend montrent la production et la consommation Kafka

🎉 Démonstration visuelle immédiate de l’asynchrone et du temps réel.

---

## 8. Logs et observabilité

### Backend

Les logs affichent clairement :

- Production Kafka
- Consommation Kafka
- Broadcast WebSocket

Exemple :

```
[KafkaProducer] Sending notification: Hello Kafka
[KafkaConsumer] Received notification: Hello Kafka
[WebSocket] Broadcasting notification
```

Cela facilite l’explication lors d’un entretien technique.

---

## 9. Structure du repository

```
notifications-asynchrone-system-demo/
│
├── backend/
│   ├── src/main/java/...
│   ├── src/main/resources/
│   ├── pom.xml
│
├── frontend/
│   ├── src/app/
│   ├── angular.json
│   ├── package.json
│
├── docker-compose.yml
├── README.md
```

---

## 10. Améliorations possibles (hors démo)

- Ajout de plusieurs topics Kafka
- Persistence (PostgreSQL / MongoDB)
- Authentification (JWT, OAuth2)
- Monitoring Kafka (AKHQ, Confluent Control Center)
- Tests automatisés (JUnit, Testcontainers, Cypress)

Ces pistes montrent que l’architecture est **prête pour l’échelle**.

---

## 11. Valeur pour un recruteur

Ce projet démontre :

- Une **architecture orientée événements**
- Une maîtrise des **patterns asynchrones**
- Une capacité à intégrer **backend, messaging et frontend temps réel**
- Une approche pragmatique : simple, claire, démontrable

Il peut être compris en moins de 5 minutes par un recruteur, tout en restant techniquement solide.

---

## 12. Contact & démo

- Code source : [lien à fournir vers le GitHub](https://github.com/stephanebtvr/notification-asynchrone-systeme.git)
- Démo vidéo : 2–3 minutes (à venir)
- Auteur : BETTAVER Stéphane

---

> Ce projet est volontairement minimaliste pour maximiser la lisibilité et l’impact pédagogique lors d’une démonstration technique.
