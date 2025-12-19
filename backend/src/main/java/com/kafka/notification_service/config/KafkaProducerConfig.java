package com.kafka.notification_service.config;

import com.kafka.notification_service.dto.NotificationDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration du Producer Kafka.
 * 
 * Pourquoi une classe de configuration dédiée ?
 * - Sépare les préoccupations (config vs logique métier)
 * - Permet de personnaliser finement le comportement Kafka
 * - Facilite les tests (on peut mock le KafkaTemplate)
 * - Rend explicite la configuration (plutôt que magique via properties)
 * 
 * @Configuration : Indique à Spring que cette classe contient des Beans
 * Les méthodes @Bean seront appelées au démarrage pour créer les composants
 */
@Configuration
public class KafkaProducerConfig {
    
    /**
     * Injection de la configuration depuis application.properties.
     * 
     * @Value : Annotation Spring pour lire une propriété
     * ${...} : Expression SpEL (Spring Expression Language)
     * 
     * Pourquoi injecter plutôt que hardcoder ?
     * - Permet de changer la config sans recompiler
     * - Facilite les différents environnements (dev/prod)
     * - Respecte le principe "Configuration externalisée"
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    /**
     * Configuration des propriétés du Producer Kafka.
     * 
     * Cette méthode crée une Map contenant toutes les configurations
     * nécessaires pour se connecter et envoyer des messages à Kafka.
     * 
     * @return Map<String, Object> contenant les configurations Kafka
     */
    private Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        
        /**
         * BOOTSTRAP_SERVERS_CONFIG : Liste des brokers Kafka à contacter.
         * Le client Kafka utilisera cette adresse pour découvrir l'ensemble du cluster.
         * Format : "host1:port1,host2:port2,..."
         * 
         * Même avec plusieurs brokers, un seul suffit pour découvrir les autres.
         */
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        /**
         * KEY_SERIALIZER_CLASS_CONFIG : Classe pour sérialiser les clés des messages.
         * 
         * StringSerializer : Convertit une String Java en bytes UTF-8.
         * La clé permet de :
         * - Partitionner les messages (messages avec même clé → même partition)
         * - Garantir l'ordre au sein d'une partition
         * 
         * Dans notre cas, on n'utilise pas vraiment les clés (peuvent être null).
         */
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        /**
         * VALUE_SERIALIZER_CLASS_CONFIG : Classe pour sérialiser les valeurs (nos messages).
         * 
         * JsonSerializer : Sérialise automatiquement nos objets Java en JSON.
         * Avantages :
         * - Pas besoin de convertir manuellement avec ObjectMapper
         * - Type safety : on envoie des NotificationDTO, pas des Strings
         * - Kafka stocke du JSON lisible (facilite le debug avec Kafka UI)
         */
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        /**
         * ACKS_CONFIG : Niveau d'accusé de réception requis.
         * 
         * "all" (ou "-1") : Le leader ET tous les replicas doivent confirmer.
         * Garantit la durabilité maximale mais ajoute de la latence.
         * 
         * Alternatives :
         * - "0" : Pas d'attente (rapide mais risque de perte)
         * - "1" : Seul le leader confirme (compromis)
         * 
         * Pour une démo : "all" montre la maîtrise des concepts.
         */
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        
        /**
         * RETRIES_CONFIG : Nombre de tentatives en cas d'échec.
         * 
         * 3 = Retry jusqu'à 3 fois avant de lancer une exception.
         * Utile pour les erreurs transitoires (réseau, broker temporairement down).
         * 
         * Kafka attend entre chaque retry (backoff exponentiel par défaut).
         */
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        /**
         * LINGER_MS_CONFIG : Délai d'attente avant envoi (millisecondes).
         * 
         * 1ms : Attend 1ms pour regrouper plusieurs messages en un seul batch.
         * Améliore le throughput (débit) au prix d'une latence légèrement plus élevée.
         * 
         * 0 = envoi immédiat (latence minimale)
         * 10-100ms = bon compromis en production
         */
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        
        /**
         * BATCH_SIZE_CONFIG : Taille maximale d'un batch (bytes).
         * 
         * 16384 bytes (16 KB) : Taille par défaut.
         * Kafka regroupe plusieurs messages jusqu'à cette taille avant envoi.
         * 
         * Plus grand = meilleur throughput mais plus de mémoire utilisée.
         */
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        
        /**
         * BUFFER_MEMORY_CONFIG : Mémoire totale allouée au buffer (bytes).
         * 
         * 33554432 bytes (32 MB) : Buffer pour les messages en attente d'envoi.
         * Si le buffer est plein, send() bloque ou lance une exception.
         * 
         * Doit être dimensionné selon le volume de messages attendu.
         */
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return props;
    }
    
    /**
     * Factory pour créer des instances de Producer Kafka.
     * 
     * ProducerFactory : Interface Spring Kafka pour abstraire la création de producers.
     * DefaultKafkaProducerFactory : Implémentation par défaut.
     * 
     * Génériques <String, NotificationDTO> :
     * - String : Type de la clé
     * - NotificationDTO : Type de la valeur (notre objet métier)
     * 
     * @Bean : Cette méthode sera appelée par Spring au démarrage.
     *         Le résultat sera géré comme un singleton dans le contexte Spring.
     * 
     * @return ProducerFactory configurée
     */
    @Bean
    public ProducerFactory<String, NotificationDto> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }
    
    /**
     * KafkaTemplate : Classe principale pour envoyer des messages à Kafka.
     * 
     * C'est l'équivalent de JdbcTemplate pour les bases de données.
     * Simplifie l'envoi de messages avec une API haut niveau.
     * 
     * Fonctionnalités principales :
     * - send() : Envoie un message de manière asynchrone
     * - sendDefault() : Envoie vers un topic par défaut
     * - Gestion automatique des erreurs et callbacks
     * - Thread-safe (peut être injecté et partagé)
     * 
     * Pourquoi c'est un Bean ?
     * - Injection de dépendances : on peut l'injecter partout avec @Autowired
     * - Singleton : une seule instance partagée (économie de ressources)
     * - Facilite les tests : on peut mock KafkaTemplate
     * 
     * @param producerFactory La factory créée précédemment
     * @return KafkaTemplate prêt à l'emploi
     */
    @Bean
    public KafkaTemplate<String, NotificationDto> kafkaTemplate(
            ProducerFactory<String, NotificationDto> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}