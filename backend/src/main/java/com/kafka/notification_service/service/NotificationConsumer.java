package com.kafka.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.kafka.notification_service.dto.NotificationDto;

/**
 * Consumer Kafka pour écouter et traiter les notifications.
 * 
 * Responsabilités :
 * 1. Écouter le topic Kafka "notifications-topic"
 * 2. Désérialiser automatiquement les messages JSON en NotificationDTO
 * 3. Broadcaster les notifications via WebSocket à tous les clients connectés
 * 4. Logger les opérations pour monitoring et debugging
 * 
 * @Component : Marque cette classe comme un composant Spring géré automatiquement.
 *              Spring la détecte via component scan et l'instancie au démarrage.
 * 
 * Pourquoi un Component et pas un Service ?
 * - @Component est générique pour tous les composants Spring
 * - @Service est une spécialisation pour la logique métier
 * - Ici, c'est un listener (infrastructure), donc @Component est plus approprié
 * - Alternativement, on pourrait utiliser @Service (ça marche aussi)
 */
@Component
public class NotificationConsumer {
    
    /**
     * Logger SLF4J pour tracer les opérations du consumer.
     * 
     * Importance du logging côté consumer :
     * - Vérifier que les messages sont bien reçus
     * - Mesurer le débit (nombre de messages/seconde)
     * - Détecter les erreurs de traitement
     * - Audit : tracer qui a reçu quoi et quand
     */
    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);
    
    /**
     * SimpMessagingTemplate : Composant Spring WebSocket pour envoyer des messages.
     * 
     * "Simp" = Simple Messaging Protocol (sous-ensemble de STOMP)
     * STOMP = Simple Text Oriented Messaging Protocol
     * 
     * Fonctionnalités principales :
     * - convertAndSend(destination, payload) : Envoie un message à une destination
     * - Conversion automatique Java → JSON
     * - Broadcast à tous les clients abonnés à la destination
     * - Thread-safe (peut être utilisé en parallèle)
     * 
     * @Autowired : Injection de dépendances par Spring.
     *              Spring injecte automatiquement le bean SimpMessagingTemplate
     *              configuré dans WebSocketConfig.
     */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Destination WebSocket où broadcaster les notifications.
     * 
     * @Value : Lit la propriété depuis application.properties.
     * "/topic/notifications" : Convention STOMP pour les destinations broadcast.
     * 
     * Distinction /topic vs /queue :
     * - /topic/* : Publish-Subscribe (broadcast à TOUS les abonnés)
     * - /queue/* : Point-to-Point (message à UN SEUL client)
     * 
     * Pour nos notifications : /topic car on veut que TOUS les utilisateurs
     * connectés reçoivent la notification (comme un chat public).
     */
    @Value("${websocket.notification-destination}")
    private String notificationDestination;
    
    /**
     * Méthode listener Kafka - Point d'entrée pour les messages du topic.
     * 
     * @KafkaListener : Annotation magique de Spring Kafka.
     * 
     * Que fait @KafkaListener ?
     * 1. Au démarrage, Spring crée un consumer Kafka en arrière-plan
     * 2. Le consumer s'abonne au topic spécifié
     * 3. Dès qu'un message arrive, cette méthode est appelée automatiquement
     * 4. Spring désérialise le JSON en NotificationDTO (via JsonDeserializer)
     * 5. La méthode traite le message
     * 6. Si pas d'exception, Kafka commit l'offset (message marqué comme traité)
     * 
     * Paramètres de l'annotation :
     * 
     * - topics : Liste des topics à écouter (peut être plusieurs).
     *            ${...} : Lit depuis application.properties (kafka.topic.notifications)
     *            Avantage : Configuration externalisée, pas de hardcoding
     * 
     * - groupId : Identifiant du groupe de consommateurs.
     *             ${...} : Lit depuis application.properties (spring.kafka.consumer.group-id)
     *             
     *             Rôle du group-id :
     *             - Tous les consumers avec le MÊME group-id forment un groupe
     *             - Kafka distribue les partitions entre les membres du groupe
     *             - Chaque message est traité par UN SEUL consumer du groupe
     *             - Permet la scalabilité horizontale (ajouter des consumers)
     *             
     *             Exemple avec 3 partitions et 2 consumers dans le même groupe :
     *             - Consumer 1 traite partitions 0 et 1
     *             - Consumer 2 traite partition 2
     *             
     *             Si on a 2 applications avec des group-id DIFFÉRENTS :
     *             - Chaque groupe reçoit TOUS les messages (broadcast)
     * 
     * - containerFactory : (optionnel) Spécifie une factory custom.
     *                      Si omis, Spring utilise la config par défaut.
     *                      Utile pour avoir plusieurs configs Kafka différentes.
     * 
     * Thread model :
     * - Par défaut, Spring crée UN thread par partition assignée
     * - Les messages d'une partition sont traités séquentiellement
     * - Les messages de partitions différentes sont traités en parallèle
     * - Pour notre démo (1 partition) : traitement séquentiel simple
     * 
     * @param notification Le DTO désérialisé automatiquement depuis le JSON Kafka
     */
    @KafkaListener(
        topics = "${kafka.topic.notifications}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(NotificationDto notification) {
        
        /**
         * ÉTAPE 1 : Log de réception pour traçabilité.
         * 
         * info() : Niveau INFO car c'est une opération normale importante.
         * 
         * Informations loggées :
         * - ID de la notification (pour corrélation avec les logs du producer)
         * - Titre (pour comprendre rapidement le type de notification)
         * - Type (INFO/SUCCESS/WARNING/ERROR)
         * 
         * Utilité en production :
         * - Monitoring : Compter combien de messages sont consommés
         * - Debugging : Vérifier qu'un message spécifique est bien arrivé
         * - Audit : Prouver la réception d'une notification importante
         * 
         * Best practice : Logger AVANT le traitement (si traitement échoue, 
         * on sait au moins que le message est arrivé).
         */
        logger.info("📩 Received notification from Kafka - ID: {} | Title: {} | Type: {}",
                notification.id(),
                notification.title(),
                notification.type());

        /**
         * ÉTAPE 2 : Validation optionnelle (bonnes pratiques).
         * 
         * Même si Kafka a bien désérialisé, on peut vouloir valider :
         * - Champs obligatoires non null
         * - Format de l'ID (UUID valide ?)
         * - Type dans la liste autorisée (INFO, SUCCESS, WARNING, ERROR)
         * - Longueur des champs (pas de message de 10 Mo)
         * 
         * Exemple de validation (à décommenter si besoin) :
         */
        if (notification.title() == null || notification.title().isEmpty()) {
            logger.error("❌ Invalid notification received - Title is null or empty");
            // Option 1 : Return sans traiter (message sera commit quand même)
            // Option 2 : Throw exception (message sera retry puis envoyé en DLQ si configuré)
            return;
        }
        
        // Validation du type (optionnel)
        String type = notification.type();
        if (type == null || !type.matches("INFO|SUCCESS|WARNING|ERROR")) {
            logger.warn("⚠️  Unknown notification type: {} - Setting to INFO", type);
            notification =  NotificationDto.create(notification.id(), notification.title(), notification.message(), "INFO"); // Valeur par défaut
        }
        
        /**
         * ÉTAPE 3 : Broadcast via WebSocket à tous les clients connectés.
         * 
         * messagingTemplate.convertAndSend() : Méthode clé de Spring WebSocket.
         * 
         * Paramètres :
         * 1. destination : Chemin STOMP où envoyer ("/topic/notifications")
         * 2. payload : Objet Java à envoyer (sera converti en JSON automatiquement)
         * 
         * Que se passe-t-il en interne ?
         * 1. Spring trouve tous les clients WebSocket abonnés à "/topic/notifications"
         * 2. Convertit NotificationDTO en JSON (via Jackson MessageConverter)
         * 3. Envoie le JSON via WebSocket à chaque client
         * 4. Retourne immédiatement (envoi asynchrone)
         * 
         * Différence avec convertAndSendToUser() :
         * - convertAndSend() : Broadcast à TOUS les abonnés (public)
         * - convertAndSendToUser() : Envoie à UN utilisateur spécifique (privé)
         * 
         * Format du message STOMP envoyé :
         * ```
         * MESSAGE
         * destination:/topic/notifications
         * content-type:application/json
         * 
         * {"id":"...","title":"...","message":"...","type":"...","timestamp":"..."}
         * ```
         * 
         * Clients Angular/JavaScript recevront ce JSON via leur subscription.
         */
        messagingTemplate.convertAndSend(notificationDestination, notification);
        
        /**
         * ÉTAPE 4 : Log de succès du broadcast.
         * 
         * Confirme que le message a été envoyé aux WebSockets.
         * 
         * Important : Cela ne garantit PAS que les clients ont reçu !
         * - Si aucun client n'est connecté, le message est perdu (c'est normal)
         * - Si un client est déconnecté au moment de l'envoi, il ne reçoit pas
         * - STOMP ne garantit pas la livraison (contrairement à Kafka)
         * 
         * Pour garantir la livraison :
         * - Option 1 : Stocker les notifications en base (clients rattrapent au reconnect)
         * - Option 2 : Utiliser un vrai message broker (RabbitMQ, ActiveMQ)
         * - Option 3 : Implémenter un système de queue côté client
         * 
         * Pour notre démo : On accepte la perte potentielle (architecture simple).
         */
        logger.info("📤 Notification broadcasted via WebSocket to {}", notificationDestination);
        
        /**
         * ÉTAPE 5 : Traitement additionnel optionnel.
         * 
         * Exemples de traitements qu'on pourrait ajouter :
         * - Sauvegarder en base de données pour historique
         * - Envoyer un email pour les notifications ERROR
         * - Incrémenter des métriques (Prometheus/Micrometer)
         * - Appeler un webhook externe
         * - Déclencher d'autres événements métier
         * 
         * Exemple avec une base de données (à implémenter si besoin) :
         */
        // notificationRepository.save(notification);
        // logger.debug("Notification saved to database");
        
        /**
         * ÉTAPE 6 : Gestion des erreurs.
         * 
         * Que se passe-t-il si une exception est lancée dans cette méthode ?
         * 
         * Comportement par défaut de Spring Kafka :
         * 1. L'exception est loggée par Spring
         * 2. Le message N'EST PAS commit (reste dans Kafka)
         * 3. Le consumer retry le même message (boucle infinie possible !)
         * 
         * Solutions pour éviter les boucles infinies :
         * 
         * A) Utiliser un ErrorHandler custom :
         * ```java
         * @Bean
         * public ConcurrentKafkaListenerContainerFactory<String, NotificationDTO> kafkaListenerContainerFactory() {
         *     factory.setCommonErrorHandler(new DefaultErrorHandler(
         *         new FixedBackOff(1000L, 3L) // 3 retries avec 1s entre chaque
         *     ));
         * }
         * ```
         * 
         * B) Wrapper la méthode dans un try-catch :
         */
        try {
            // Traitement risqué ici
        } catch (Exception e) {
            logger.error("❌ Error processing notification ID {}: {}", 
                        notification.id(), e.getMessage(), e);
            // Le message sera quand même commit (on accepte la perte pour éviter le blocage)
            // Alternative : Envoyer vers une Dead Letter Queue (DLQ)
        }
        
        /**
         * ÉTAPE 7 : Commit de l'offset (automatique).
         * 
         * Si on arrive ici sans exception :
         * - Spring Kafka commit l'offset automatiquement (enable-auto-commit=true)
         * - Le message est marqué comme "traité" dans Kafka
         * - On ne le recevra plus jamais (sauf replay manuel)
         * 
         * Timing du commit :
         * - Par défaut : toutes les 1 seconde (auto-commit-interval=1000ms)
         * - Ou au prochain poll() si le batch est terminé
         * 
         * Offset commit :
         * - Stocké dans un topic interne Kafka : __consumer_offsets
         * - Format : (topic, partition, group-id) → offset
         * - Permet au consumer de reprendre où il s'était arrêté après redémarrage
         * 
         * Exemple :
         * - Message 1 traité → offset=1 commit
         * - Message 2 traité → offset=2 commit
         * - Crash du consumer
         * - Redémarrage → Reprend à offset=3 (messages 1 et 2 déjà traités)
         */
    }
    
    /**
     * Méthode utilitaire pour obtenir des statistiques (optionnel).
     * 
     * Peut être exposée via un endpoint REST pour monitoring.
     * Exemple : GET /api/stats → nombre de notifications traitées
     * 
     * Note : Pour compter, il faudrait ajouter un compteur :
     * private final AtomicLong processedCount = new AtomicLong(0);
     * Et l'incrémenter dans listen() : processedCount.incrementAndGet();
     */
    // public long getProcessedCount() {
    //     return processedCount.get();
    // }
}