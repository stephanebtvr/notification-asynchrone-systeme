package com.kafka.notification_service.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration Spring WebSocket avec STOMP (Simple Text Oriented Messaging Protocol).
 * 
 * Architecture WebSocket :
 * 
 * ```
 * Client (Angular)
 *     ↓ WebSocket Handshake
 * STOMP Endpoint (/ws)
 *     ↓ CONNECT frame
 * Message Broker (in-memory)
 *     ↓ SUBSCRIBE to /topic/notifications
 * Application
 *     ↓ SEND message to /topic/notifications
 * Message Broker
 *     ↓ MESSAGE frame (broadcast)
 * All subscribed clients receive the message
 * ```
 * 
 * Pourquoi STOMP over WebSocket ?
 * 
 * - WebSocket seul : Protocole bas niveau (frames binaires)
 * - STOMP : Protocole haut niveau avec sémantique publish/subscribe
 * - Avantages de STOMP :
 *   - Format texte lisible (debugging facile)
 *   - Concepts familiers (destinations, subscriptions)
 *   - Interopérable (clients JavaScript, Java, Python, etc.)
 *   - Gestion automatique des heartbeats
 * 
 * Comparaison avec d'autres protocoles :
 * - WebSocket brut : Plus rapide mais plus complexe à gérer
 * - Server-Sent Events (SSE) : Unidirectionnel uniquement (serveur → client)
 * - Long polling : Plus lent, plus de charge serveur
 * - STOMP over WebSocket : Bon compromis performance/simplicité
 * 
 * @Configuration : Indique à Spring que cette classe contient des configurations.
 * 
 * @EnableWebSocketMessageBroker : Active le support WebSocket avec message broker.
 *                                  Déclenche la création automatique de composants :
 *                                  - WebSocketHandler
 *                                  - SimpMessagingTemplate
 *                                  - Message broker in-memory
 *                                  - Convertisseurs de messages (JSON)
 * 
 * WebSocketMessageBrokerConfigurer : Interface à implémenter pour personnaliser.
 *                                     Fournit des méthodes de callback appelées
 *                                     par Spring au démarrage.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

      public WebSocketConfig() {
        System.out.println("🔌 WebSocketConfig LOADED ✅");
    }
    
    /**
     * Endpoint WebSocket où les clients se connectent.
     * 
     * @Value : Lit depuis application.properties (websocket.endpoint=/ws)
     * 
     * Exemple : ws://localhost:8080/ws
     * 
     * Pourquoi externaliser dans properties ?
     * - Changeable par environnement (dev/prod peuvent différer)
     * - Évite le hardcoding
     * - Centralisé avec les autres configs
     */
    @Value("${websocket.endpoint}")
    private String websocketEndpoint;
    
    /**
     * Origine autorisée pour les connexions WebSocket (CORS).
     * 
     * Problème résolu : Sécurité des navigateurs (Same-Origin Policy).
     * 
     * Sans cette config :
     * - Frontend Angular sur http://localhost:4200
     * - Backend WebSocket sur http://localhost:8080
     * - Navigateur bloque la connexion (origines différentes)
     * 
     * Avec setAllowedOrigins("http://localhost:4200") :
     * - Navigateur autorise la connexion cross-origin
     * 
     * En production :
     * - Spécifier l'URL exacte du frontend (ex: https://app.example.com)
     * - JAMAIS "*" en production (faille de sécurité)
     * - Possibilité de patterns : setAllowedOriginPatterns("https://*.example.com")
     */
    @Value("${spring.web.cors.allowed-origins}")
    private String allowedOrigins;
    
    /**
     * Préfixe des destinations topic pour le broadcast.
     * 
     * @Value : Lit depuis application.properties (websocket.topic-prefix=/topic)
     * 
     * Convention STOMP :
     * - /topic/* : Destinations de type publish-subscribe (broadcast)
     * - /queue/* : Destinations de type point-to-point (privé)
     * - /app/* : Destinations pour messages vers l'application
     * 
     * Exemple : Si prefix=/topic, les clients s'abonnent à "/topic/notifications"
     */
    @Value("${websocket.topic-prefix}")
    private String topicPrefix;
    
    /**
     * MÉTHODE 1 : Enregistrement des endpoints STOMP.
     * 
     * Cette méthode est appelée par Spring au démarrage pour configurer
     * les points d'entrée WebSocket où les clients peuvent se connecter.
     * 
     * @param registry Registre fourni par Spring pour enregistrer les endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        
        /**
         * Enregistrement de l'endpoint principal.
         * 
         * addEndpoint(path) : Crée un endpoint WebSocket à l'URL spécifiée.
         * 
         * Exemple : websocketEndpoint = "/ws"
         * → Endpoint accessible à : ws://localhost:8080/ws
         * 
         * Processus de connexion :
         * 1. Client envoie une requête HTTP GET à /ws
         * 2. Header "Upgrade: websocket" demande l'upgrade du protocole
         * 3. Serveur répond avec "101 Switching Protocols"
         * 4. Connexion HTTP devient connexion WebSocket persistante
         * 5. Client et serveur peuvent s'échanger des frames STOMP
         */
        registry.addEndpoint(websocketEndpoint)
            
            /**
             * setAllowedOrigins() : Configure CORS pour WebSocket.
             * 
             * Autorise les connexions depuis l'origine spécifiée.
             * 
             * Paramètre : allowedOrigins = "http://localhost:4200"
             * 
             * Important : Séparer par virgule pour plusieurs origines :
             * setAllowedOrigins("http://localhost:4200", "https://app.example.com")
             * 
             * Alternative : setAllowedOriginPatterns("http://localhost:*")
             * 
             * Sécurité :
             * - Valider TOUTES les origines autorisées
             * - Ne JAMAIS utiliser "*" en production
             * - Préférer des URLs complètes plutôt que des patterns
             */
            .setAllowedOriginPatterns(allowedOrigins)
            
            /**
             * withSockJS() : Active le fallback SockJS.
             * 
             * Problème résolu : Compatibilité avec anciens navigateurs ou proxies.
             * 
             * WebSocket natif peut être bloqué par :
             * - Navigateurs anciens (IE < 10)
             * - Proxies d'entreprise (filtrent WebSocket)
             * - Firewalls restrictifs
             * 
             * SockJS : Bibliothèque JavaScript qui fournit des fallbacks :
             * 1. Essaie WebSocket natif en premier
             * 2. Si échec, essaie : xhr-streaming
             * 3. Si échec, essaie : xhr-polling
             * 4. Si échec, essaie : jsonp-polling
             * 
             * Avantages :
             * - Transparence : L'API reste la même côté client
             * - Résilience : Fonctionne dans presque tous les environnements
             * - Fallback automatique sans code additionnel
             * 
             * Inconvénients :
             * - Léger overhead (détection du meilleur transport)
             * - Performances moindres en mode fallback (polling)
             * 
             * En production moderne : WebSocket natif fonctionne partout,
             * mais SockJS reste utile pour la compatibilité maximale.
             * 
             * Configuration SockJS (optionnelle) :
             */
            // .withSockJS()
            //     .setStreamBytesLimit(512 * 1024)     // Limite du streaming (512 KB)
            //     .setHttpMessageCacheSize(1000)       // Cache des messages HTTP
            //     .setDisconnectDelay(30 * 1000);      // Délai avant disconnect (30s)
            .withSockJS();

              System.out.println(allowedOrigins);
                System.out.println(websocketEndpoint);
         System.out.println("✅ WebSocket endpoint registered: /ws");
        
        /**
         * Note : On peut enregistrer plusieurs endpoints si nécessaire.
         * 
         * Exemple avec authentification différenciée :
         * 
         * registry.addEndpoint("/ws-public")
         *     .setAllowedOrigins(allowedOrigins)
         *     .withSockJS();
         * 
         * registry.addEndpoint("/ws-secure")
         *     .setAllowedOrigins(allowedOrigins)
         *     .addInterceptors(authInterceptor)  // Vérification JWT
         *     .withSockJS();
         */
    }
    
    /**
     * MÉTHODE 2 : Configuration du message broker.
     * 
     * Le message broker est responsable de :
     * - Router les messages vers les bonnes destinations
     * - Gérer les subscriptions des clients
     * - Broadcaster les messages aux abonnés
     * 
     * @param config Registre fourni par Spring pour configurer le broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        
        /**
         * enableSimpleBroker() : Active un broker in-memory simple.
         * 
         * "Simple" = Implémentation légère incluse dans Spring.
         * Pas un vrai message broker comme RabbitMQ ou ActiveMQ.
         * 
         * Fonctionnalités :
         * - Gestion des subscriptions en mémoire
         * - Broadcast des messages aux abonnés
         * - Pas de persistance (messages perdus si redémarrage)
         * - Pas de clustering (pas de partage entre serveurs)
         * 
         * Paramètre : topicPrefix = "/topic"
         * 
         * Signification : Le broker gère toutes les destinations commençant par "/topic".
         * 
         * Exemple :
         * - Client s'abonne à "/topic/notifications" → Géré par le broker
         * - Client s'abonne à "/app/something" → PAS géré par le broker (voir ci-dessous)
         * 
         * Pourquoi /topic ?
         * - Convention STOMP pour publish-subscribe
         * - Indique clairement que c'est un broadcast (plusieurs destinataires)
         * 
         * Alternative : enableStompBrokerRelay() pour un vrai broker externe.
         * Exemple avec RabbitMQ :
         * 
         * config.enableStompBrokerRelay("/topic", "/queue")
         *     .setRelayHost("localhost")
         *     .setRelayPort(61613)
         *     .setClientLogin("guest")
         *     .setClientPasscode("guest");
         * 
         * Avantages du broker externe :
         * - Persistance des messages
         * - Scalabilité (clustering)
         * - Fonctionnalités avancées (dead letter queues, etc.)
         * 
         * Pour notre démo : Simple broker suffit largement.
         */
        config.enableSimpleBroker(topicPrefix);
        
        /**
         * setApplicationDestinationPrefixes() : Préfixe pour les messages vers l'app.
         * 
         * Définit le préfixe des destinations où les clients ENVOIENT des messages
         * vers l'application (pas vers d'autres clients).
         * 
         * Paramètre : "/app"
         * 
         * Distinction importante :
         * 
         * - Destinations avec /app/* :
         *   → Messages envoyés PAR les clients VERS l'application
         *   → Routés vers des méthodes @MessageMapping dans les Controllers
         *   → Traitement côté serveur avant éventuel broadcast
         * 
         * - Destinations avec /topic/* :
         *   → Messages envoyés PAR l'application VERS les clients
         *   → Gérés par le broker (broadcast direct aux abonnés)
         * 
         * Exemple d'utilisation (Controller) :
         * 
         * @Controller
         * public class ChatController {
         *     
         *     @MessageMapping("/chat")  // Client envoie à /app/chat
         *     @SendTo("/topic/messages")  // Réponse broadcastée à /topic/messages
         *     public ChatMessage handleMessage(ChatMessage message) {
         *         // Traitement (ex: validation, enrichissement)
         *         return message;
         *     }
         * }
         * 
         * Flux :
         * 1. Client envoie STOMP SEND à /app/chat avec payload
         * 2. Spring route vers @MessageMapping("/chat")
         * 3. Méthode handleMessage() s'exécute
         * 4. Retour envoyé automatiquement à /topic/messages
         * 5. Tous les clients abonnés à /topic/messages reçoivent
         * 
         * Dans notre projet :
         * - On n'utilise PAS /app car on envoie via API REST
         * - Le consumer Kafka envoie directement à /topic/notifications
         * - Mais on le configure quand même (bonne pratique pour évolutions futures)
         * 
         * Exemple d'extension : Permettre aux clients de changer leurs préférences.
         * 
         * Frontend :
         * ```javascript
         * stompClient.send("/app/preferences", {}, JSON.stringify({theme: "dark"}));
         * ```
         * 
         * Backend :
         * ```java
         * @MessageMapping("/preferences")
         * public void updatePreferences(Preferences prefs, SimpMessageHeaderAccessor headers) {
         *     String sessionId = headers.getSessionId();
         *     // Sauvegarder les préférences pour cette session
         * }
         * ```
         */
        config.setApplicationDestinationPrefixes("/app");
        System.out.println(topicPrefix);
         System.out.println("✅ Message broker configured");
        
        /**
         * Configuration optionnelle : Heartbeats.
         * 
         * setHeartbeatValue() : Configure les ping/pong entre client et serveur.
         * 
         * Format : [outgoing, incoming] en millisecondes
         * 
         * Exemple : setHeartbeatValue(new long[]{10000, 10000})
         * - Serveur envoie un heartbeat toutes les 10s
         * - Serveur attend un heartbeat client toutes les 10s
         * 
         * Utilité :
         * - Détecter les connexions mortes (zombies)
         * - Éviter les timeouts proxy
         * - Libérer les ressources des clients déconnectés
         * 
         * Par défaut : [25000, 25000] (25 secondes)
         * 
         * Pour notre démo : Valeurs par défaut suffisent.
         */
        
        /**
         * Configuration optionnelle : Taille des messages.
         * 
         * setMessageSizeLimit() : Taille max d'un message (bytes)
         *
         * Par défaut : 64 KB
     * 
     * setSendBufferSizeLimit() : Taille du buffer d'envoi par client
     *                            Par défaut : 512 KB
     * 
     * setSendTimeLimit() : Timeout d'envoi (millisecondes)
     *                      Par défaut : 10000 (10s)
     * 
     * Exemple pour messages plus gros :
     * 
     * config.setMessageSizeLimit(128 * 1024)        // 128 KB
     *       .setSendBufferSizeLimit(1024 * 1024)    // 1 MB
     *       .setSendTimeLimit(20000);               // 20s
     * 
     * Pour notre démo : Notifications sont petites, valeurs par défaut OK.
     */
}}

/**
 * MÉTHODE 3 (optionnelle) : Configuration des intercepteurs.
 * 
 * Les intercepteurs permettent d'inspecter/modifier les messages STOMP.
 * 
 * Cas d'usage :
 * - Authentification : Vérifier JWT dans les headers STOMP
 * - Logging : Tracer tous les messages pour audit
 * - Enrichissement : Ajouter des métadonnées (timestamp, userId)
 * - Rate limiting : Limiter le nombre de messages par client
 * 
 * Exemple d'intercepteur d'authentification :
 * 
 * @Override
 * public void configureClientInboundChannel(ChannelRegistration registration) {
 *     registration.interceptors(new ChannelInterceptor() {
 *         @Override
 *         public Message<?> preSend(Message<?> message, MessageChannel channel) {
 *             StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
 *                 message, StompHeaderAccessor.class
 *             );
 *             
 *             if (StompCommand.CONNECT.equals(accessor.getCommand())) {
 *                 String token = accessor.getFirstNativeHeader("Authorization");
 *                 
 *                 if (token != null && jwtService.validate(token)) {
 *                     String username = jwtService.extractUsername(token);
 *                     accessor.setUser(new UsernamePasswordAuthenticationToken(username, null));
 *                 } else {
 *                     throw new MessagingException("Invalid token");
 *                 }
 *             }
 *             
 *             return message;
 *         }
 *     });
 * }
 * 
 * Avec cet intercepteur :
 * - Chaque CONNECT est vérifié
 * - Token JWT doit être valide
 * - Utilisateur est attaché à la session WebSocket
 * - Messages ultérieurs peuvent identifier l'utilisateur
 * 
 * Pour notre démo : Pas d'auth, donc pas d'intercepteur nécessaire.
 */