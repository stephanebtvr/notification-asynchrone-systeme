package com.kafka.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * DTO immutable pour les notifications, implémenté comme un record (Java 21).
 *
 * Avantages en Java 21 :
 * - Records pleinement matures et optimisés
 * - Excellente intégration avec Jackson (sérialisation/désérialisation native)
 * - Immutabilité garantie
 * - Code extrêmement concis sans Lombok
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // Ne sérialise pas les champs null (id/timestamp si absents)
public record NotificationDto(

        /** Identifiant unique de la notification (généré côté backend) */
        String id,

        /** Titre de la notification */
        String title,

        /** Message détaillé */
        String message,

        /** Type : INFO, SUCCESS, WARNING, ERROR */
        String type,

        /** Date/heure de création au format ISO 8601 */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp

) {

    /**
     * Constructeur compact pour créer une notification sans id ni timestamp.
     * Utile depuis un controller ou un service avant enrichissement.
     */
    public NotificationDto(String title, String message, String type) {
        this(null, title, message, type, null);
    }

    /**
     * Factory method : crée une notification complète avec timestamp actuel.
     */
    public static NotificationDto create(String id, String title, String message, String type) {
        return new NotificationDto(id, title, message, type, LocalDateTime.now());
    }

    /**
     * Factory method avec timestamp personnalisé (utile pour tests ou relecture d'événements Kafka).
     */
    public static NotificationDto of(String id, String title, String message, String type, LocalDateTime timestamp) {
        return new NotificationDto(id, title, message, type, timestamp);
    }

    /**
     * Factory method simple pour les cas les plus courants (ex: notifications système).
     */
    public static NotificationDto info(String title, String message) {
        return new NotificationDto(null, title, message, "INFO", LocalDateTime.now());
    }

    public static NotificationDto success(String title, String message) {
        return new NotificationDto(null, title, message, "SUCCESS", LocalDateTime.now());
    }

    public static NotificationDto warning(String title, String message) {
        return new NotificationDto(null, title, message, "WARNING", LocalDateTime.now());
    }

    public static NotificationDto error(String title, String message) {
        return new NotificationDto(null, title, message, "ERROR", LocalDateTime.now());
    }
}
