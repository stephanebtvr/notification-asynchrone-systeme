package com.kafka.notification_service.service;
import com.kafka.notification_service.dto.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service Producer Kafka pour l'envoi de notifications.
 * Adapté pour fonctionner avec NotificationDTO en tant que record (immutable).
 */
@Service
public class NotificationProducer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationProducer.class);

    @Autowired
    private KafkaTemplate<String, NotificationDto> kafkaTemplate;

    @Value("${kafka.topic.notifications}")
    private String notificationTopic;

    /**
     * Envoie une notification en l'enrichissant (ID + timestamp) avant envoi.
     * Comme NotificationDTO est un record (immutable), on crée une nouvelle instance.
     *
     * @param partialNotification DTO partiel (typiquement sans id ni timestamp)
     */
    public void sendNotification(NotificationDto partialNotification) {

        // Étape 1 : Enrichissement → création d'une nouvelle instance complète
        String generatedId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        NotificationDto enrichedNotification = new NotificationDto(
                generatedId,
                partialNotification.title(),
                partialNotification.message(),
                partialNotification.type(),
                now
        );

        // Alternative plus lisible avec la factory method (recommandée)
        // NotificationDTO enrichedNotification = NotificationDTO.create(
        //         generatedId,
        //         partialNotification.title(),
        //         partialNotification.message(),
        //         partialNotification.type()
        // );

        logger.info("Sending notification to Kafka topic '{}': {}", notificationTopic, enrichedNotification);

        CompletableFuture<SendResult<String, NotificationDto>> future =
                kafkaTemplate.send(notificationTopic, enrichedNotification);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Notification sent successfully - ID: {} | Topic: {} | Partition: {} | Offset: {}",
                        enrichedNotification.id(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send notification - ID: {} | Error: {}",
                        enrichedNotification.id(), ex.getMessage());
            }
        });
    }

    /**
     * Méthode de convenance : envoi direct avec les champs séparés.
     * Crée d'abord un DTO partiel, puis délègue à la méthode principale.
     */
    public void sendNotification(String title, String message, String type) {
        // Utilisation du constructeur compact (sans id ni timestamp)
        NotificationDto partial = new NotificationDto(title, message, type);
        sendNotification(partial);
    }

    // Bonus : méthodes ultra-simples pour les cas courants
    public void sendInfo(String title, String message) {
        sendNotification(NotificationDto.info(title, message));
    }

    public void sendSuccess(String title, String message) {
        sendNotification(NotificationDto.success(title, message));
    }

    public void sendWarning(String title, String message) {
        sendNotification(NotificationDto.warning(title, message));
    }

    public void sendError(String title, String message) {
        sendNotification(NotificationDto.error(title, message));
    }
}