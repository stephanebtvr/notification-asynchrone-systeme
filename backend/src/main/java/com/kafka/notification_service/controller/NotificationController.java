package com.kafka.notification_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kafka.notification_service.dto.NotificationDto;
import com.kafka.notification_service.service.NotificationProducer;

import jakarta.validation.Valid;


/**
 * Contrôleur REST pour gérer les notifications.
 * 
 * Responsabilités :
 * - Exposer une API REST pour envoyer des notifications
 * - Valider les requêtes entrantes
 * - Déléguer le traitement au NotificationProducer
 * - Retourner des réponses HTTP appropriées
 * - Gérer les erreurs de manière centralisée
 * 
 * @RestController : Combinaison de @Controller + @ResponseBody.
 *                   Indique que TOUTES les méthodes retournent du JSON (pas des vues HTML).
 *                   Spring sérialise automatiquement les objets de retour en JSON.
 * 
 * @RequestMapping : Préfixe commun pour toutes les routes de ce contrôleur.
 *                   "/api/notifications" = base URL
 *                   Toutes les méthodes héritent de ce préfixe.
 * 
 * @CrossOrigin : Permet les requêtes cross-origin depuis l'origine spécifiée.
 *                Alternative à la config globale dans application.properties.
 *                Utile pour configurer CORS au niveau du contrôleur.
 *                
 *                Paramètres :
 *                - origins : Liste des origines autorisées
 *                - maxAge : Durée de cache de la réponse preflight (secondes)
 *                - allowedHeaders : Headers autorisés dans les requêtes
 *                - methods : Méthodes HTTP autorisées
 * 
 * Architecture MVC (Model-View-Controller) :
 * - Model : NotificationDTO (données)
 * - View : JSON (sérialisation automatique par Jackson)
 * - Controller : Cette classe (logique de routing et validation)
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class NotificationController {
    
    /**
     * Logger SLF4J pour tracer les requêtes API.
     * 
     * Utilité côté Controller :
     * - Tracer qui appelle l'API (IP, user agent si configuré)
     * - Mesurer le temps de traitement des requêtes
     * - Logger les erreurs de validation
     * - Audit des opérations sensibles
     */
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    /**
     * Service Producer Kafka pour envoyer les notifications.
     * 
     * @Autowired : Injection de dépendances par Spring.
     *              Spring trouve le bean NotificationProducer et l'injecte ici.
     * 
     * Pattern de conception : Dependency Injection (DI)
     * Avantages :
     * - Découplage : Controller ne crée pas le Producer (inversion de contrôle)
     * - Testabilité : On peut injecter un mock du Producer pour les tests
     * - Maintenabilité : Changement d'implémentation sans modifier le Controller
     * 
     * Note moderne : Depuis Spring 4.3, @Autowired est optionnel sur les constructeurs.
     * On pourrait écrire :
     * 
     * private final NotificationProducer producer;
     * 
     * public NotificationController(NotificationProducer producer) {
     *     this.producer = producer;
     * }
     * 
     * Cette approche (injection par constructeur) est recommandée car :
     * - Rend les dépendances explicites
     * - Permet l'immutabilité (final)
     * - Facilite les tests (pas besoin de reflection)
     */
    @Autowired
    private NotificationProducer notificationProducer;
    
    /**
     * ENDPOINT 1 : Envoyer une notification.
     * 
     * HTTP POST /api/notifications
     * Content-Type: application/json
     * Body: {"title": "...", "message": "...", "type": "..."}
     * 
     * @PostMapping : Spécifie que cette méthode gère les requêtes POST.
     *                Equivalent à @RequestMapping(method = RequestMethod.POST)
     * 
     * Annotations de paramètres :
     * 
     * @RequestBody : Indique que le paramètre doit être désérialisé depuis le body HTTP.
     *                Spring utilise Jackson pour convertir JSON → NotificationDTO.
     *                
     *                Processus :
     *                1. Client envoie JSON dans le body
     *                2. Jackson parse le JSON
     *                3. Jackson crée une instance de NotificationDTO
     *                4. Jackson appelle les setters pour remplir les champs
     *                5. L'objet est passé à cette méthode
     * 
     * @Valid : Active la validation JSR-303/Bean Validation.
     *          Spring valide automatiquement l'objet avec les contraintes définies.
     *          
     *          Annotations de validation disponibles :
     *          - @NotNull : Champ ne doit pas être null
     *          - @NotEmpty : String/Collection ne doit pas être vide
     *          - @Size(min, max) : Contrainte de taille
     *          - @Pattern(regexp) : Validation par regex
     *          - @Email : Validation d'email
     *          - @Min, @Max : Valeurs numériques
     *          
     *          Exemple dans NotificationDTO :
     *          
     *          @NotNull(message = "Title is required")
     *          @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
     *          private String title;
     *          
     *          Si validation échoue :
     *          - Spring lance une MethodArgumentNotValidException
     *          - Réponse HTTP 400 Bad Request automatique
     *          - Body contient les détails des erreurs de validation
     * 
     * ResponseEntity<NotificationDTO> : Type de retour enrichi.
     * 
     * Pourquoi ResponseEntity plutôt que juste NotificationDTO ?
     * - Permet de contrôler le status HTTP (200, 201, 400, 500, etc.)
     * - Permet d'ajouter des headers personnalisés
     * - Plus expressif : code HTTP + body + headers dans un seul objet
     * 
     * Alternatives :
     * - Retour direct : NotificationDTO → Status 200 automatique
     * - @ResponseStatus(HttpStatus.CREATED) + retour NotificationDTO
     * - ResponseEntity donne le contrôle maximal
     * 
     * @param notification Le DTO reçu et désérialisé depuis le JSON
     * @return ResponseEntity contenant le DTO enrichi et le status HTTP 201
     */
    @PostMapping
    public ResponseEntity<NotificationDto> sendNotification(
            @Valid @RequestBody NotificationDto notification) {
        
        /**
         * ÉTAPE 1 : Log de la requête entrante.
         * 
         * Informations loggées :
         * - Endpoint appelé (POST /api/notifications)
         * - Données reçues (titre, message, type)
         * 
         * Utilité :
         * - Audit : Tracer qui a envoyé quoi
         * - Debugging : Vérifier les données reçues
         * - Monitoring : Compter le nombre d'appels API
         * 
         * Best practice : Logger AVANT le traitement pour capturer toutes les tentatives.
         */
        logger.info("📨 POST /api/notifications - Sending notification: title='{}', type='{}'",
                notification.title(),
                notification.type());
        
        /**
         * ÉTAPE 2 : Validation métier optionnelle.
         * 
         * @Valid gère la validation syntaxique (champs requis, formats).
         * Ici, on peut ajouter des validations métier spécifiques.
         * 
         * Exemples :
         * - Vérifier que le type est dans une liste autorisée
         * - Vérifier que le message n'est pas vide après trim()
         * - Valider des règles métier complexes
         * 
         * Pattern : Fail-fast (échouer rapidement si données invalides).
         */
        String type = notification.type();
        if (type == null || type.isEmpty()) {
            type = "INFO"; // Valeur par défaut
            notification = NotificationDto.create(
                    notification.id(),
                    notification.title(),
                    notification.message(),
                    type
            );

            logger.debug("Notification type was null/empty, defaulting to INFO");
        }
        
        // Validation du type (optionnel, dépend des besoins métier)
        if (!type.matches("INFO|SUCCESS|WARNING|ERROR")) {
            logger.warn("⚠️  Invalid notification type received: '{}' - Using INFO", type);
           NotificationDto correctedNotification = NotificationDto.create(
                    notification.id(),
                    notification.title(),
                    notification.message(),
                    "INFO"
            );
            notification = correctedNotification;
        }
        
        // Trim des espaces (nettoyage des données)
        if (notification.title() != null) {
             NotificationDto correctedNotification = NotificationDto.create(
                    notification.id(),
                    notification.title().trim(),
                    notification.message(),
                    notification.type()
            );
            notification = correctedNotification;
        }
        if (notification.message() != null) {
             NotificationDto correctedNotification = NotificationDto.create(
                    notification.id(),
                    notification.title(),
                    notification.message().trim(),
                    notification.type()
            );
            notification = correctedNotification;
        }
    
        
        /**
         * ÉTAPE 3 : Délégation au service Producer.
         * 
         * sendNotification() : Méthode du NotificationProducer.
         * - Enrichit la notification (ID, timestamp)
         * - Envoie à Kafka de manière asynchrone
         * - Retourne immédiatement (non-bloquant)
         * 
         * Important : Cette méthode retourne AVANT que Kafka ait confirmé !
         * 
         * Timeline :
         * 1. Cette ligne s'exécute [instant T]
         * 2. Message mis dans le buffer Kafka [T + quelques µs]
         * 3. Méthode retourne [T + < 1ms]
         * 4. Réponse HTTP 201 envoyée au client [T + quelques ms]
         * 5. Kafka confirme réellement la réception [T + 10-100ms]
         * 6. Callback du Producer loggue le succès/échec [T + 10-100ms]
         * 
         * Conséquence : Le client reçoit 201 Created AVANT que Kafka confirme.
         * 
         * Alternatives si on veut attendre Kafka :
         * 
         * CompletableFuture<SendResult> future = notificationProducer.sendNotificationAsync(notification);
         * try {
         *     SendResult result = future.get(5, TimeUnit.SECONDS); // Attend max 5s
         *     logger.info("Kafka confirmed: offset={}", result.getRecordMetadata().offset());
         * } catch (TimeoutException e) {
         *     return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
         * }
         * 
         * Pour notre démo : Approche asynchrone (fire-and-forget) est suffisante.
         */
        notificationProducer.sendNotification(notification);
        
        /**
         * ÉTAPE 4 : Préparation de la réponse HTTP.
         * 
         * ResponseEntity.status(HttpStatus.CREATED) : Status 201 Created.
         * 
         * Pourquoi 201 plutôt que 200 ?
         * - Sémantique HTTP : 201 = Ressource créée avec succès
         * - 200 = Succès générique
         * - 201 communique mieux l'intention (création d'une notification)
         * 
         * .body(notification) : Retourne le DTO enrichi dans le body.
         * 
         * Le DTO retourné contient maintenant :
         * - id : UUID généré par le Producer
         * - timestamp : Date/heure d'envoi
         * - title, message, type : Valeurs originales (potentiellement nettoyées)
         * 
         * Utilité pour le client :
         * - Confirmation des données envoyées
         * - Récupération de l'ID pour tracking
         * - Vérification du timestamp serveur
         * 
         * Format de la réponse HTTP :
         * 
         * HTTP/1.1 201 Created
         * Content-Type: application/json
         * 
         * {
         *   "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
         *   "title": "Test Notification",
         *   "message": "Ceci est un test",
         *   "type": "INFO",
         *   "timestamp": "2024-01-15T14:30:05"
         * }
         */
        logger.info("✅ Notification accepted - ID: {}", notification.id());
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(notification);
        
        /**
         * Note sur les headers HTTP :
         * 
         * On pourrait ajouter des headers personnalisés :
         * 
         * return ResponseEntity
         *     .status(HttpStatus.CREATED)
         *     .header("X-Notification-ID", notification.getId())
         *     .header("Location", "/api/notifications/" + notification.getId())
         *     .body(notification);
         * 
         * Header "Location" : Standard REST pour indiquer l'URL de la ressource créée.
         */
    }
    
    /**
     * ENDPOINT 2 : Endpoint de santé / test (optionnel mais utile).
     * 
     * HTTP GET /api/notifications/health
     * 
     * @GetMapping : Gère les requêtes GET.
     * 
     * Utilité :
     * - Vérifier que l'API répond
     * - Health check pour monitoring (Kubernetes, Docker, etc.)
     * - Test rapide sans envoyer de vraie notification
     * 
     * @return String simple avec status OK
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.debug("Health check endpoint called");
        return ResponseEntity.ok("Notification Service is UP and running! 🚀");
    }
    
    /**
     * ENDPOINT 3 : Obtenir une notification (simulation, optionnel).
     * 
     * HTTP GET /api/notifications/{id}
     * 
     * @GetMapping("/{id}") : Route avec paramètre de chemin.
     * 
     * @PathVariable : Extrait la valeur du chemin URL.
     * 
     * Exemple : GET /api/notifications/abc-123
     * → id = "abc-123"
     * 
     * Pour une vraie implémentation :
     * - Chercher dans une base de données
     * - Retourner 404 si non trouvé
     * 
     * Pour notre démo sans BDD :
     * - On simule ou on retourne 501 Not Implemented
     * 
     * @param id L'identifiant de la notification
     * @return ResponseEntity avec la notification ou 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> getNotification(@PathVariable String id) {
        logger.info("GET /api/notifications/{} - Fetching notification", id);
        
        // Sans base de données, on ne peut pas récupérer les notifications
        // On retourne 501 Not Implemented
        return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .body("Get notification by ID not implemented (no database in demo)");
        
        /**
         * Avec une base de données, on ferait :
         * 
         * Optional<NotificationDTO> notif = notificationRepository.findById(id);
         * 
         * if (notif.isPresent()) {
         *     return ResponseEntity.ok(notif.get());
         * } else {
         *     return ResponseEntity.notFound().build();
         * }
         */
    }
}
    /**
     * GESTION GLOBALE DES ERREURS (optionnel mais recommandé).
     * 
     * @ExceptionHandler : Gère les exceptions lancées dans ce contrôleur.
     * 
     * Exemple : Gestion des erreurs de validation.
     * 
     * @ExceptionHandler(MethodArgumentNotValidException.class)
     * public ResponseEntity<Map<String, String>> handleValidationErrors(
     *         MethodArgumentNotValidException ex) {
     *     
     *     Map<String, String> errors = new HashMap<>();
     *     
     *     ex.getBindingResult().getFieldErrors().forEach(error -> {
     *         errors.put(error.getField(), error.getDefaultMessage());
     *     });
     *     
     *     logger.error("Validation failed: {}", errors);
     *     
     *     return ResponseEntity
         .badRequest()
         *         .body(errors);
     * }
     * 
     * Réponse pour validation échouée :
     * 
     * HTTP/1.1 400 Bad Request
     * {
     *   "title": "Title is required",
     *   "message": "Message must not be empty"
     * }
     * 
     * Alternative : @ControllerAdvice pour gestion globale dans toute l'app.
     */