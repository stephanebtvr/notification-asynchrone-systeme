/* ============================================================================
   MODÈLES DE DONNÉES (DATA TRANSFER OBJECTS - DTOs)
   ============================================================================
   Ces interfaces TypeScript définissent la structure exacte des données
   échangées entre le frontend Angular et le backend Spring Boot.
   
   Avantages du typage fort :
   - Autocomplétion dans l'IDE (IntelliSense)
   - Détection des erreurs à la compilation (avant l'exécution)
   - Documentation vivante du contrat d'API
   - Refactoring sécurisé
   ============================================================================ */

/**
 * ÉNUMÉRATION DES TYPES DE NOTIFICATION
 *
 * Les enums TypeScript sont compilés en objets JavaScript qui peuvent être
 * utilisés à la fois comme types et comme valeurs.
 *
 * Cette enum DOIT correspondre exactement à l'enum Java côté backend :
 * public enum NotificationType { INFO, SUCCESS, WARNING, ERROR }
 */
export enum NotificationType {
  INFO = 'INFO', // Notifications informatives (bleu)
  SUCCESS = 'SUCCESS', // Notifications de succès (vert)
  WARNING = 'WARNING', // Notifications d'avertissement (orange)
  ERROR = 'ERROR', // Notifications d'erreur (rouge)
}

/**
 * INTERFACE POUR LES REQUÊTES HTTP (envoi de notification)
 *
 * Cette interface représente le payload JSON envoyé au backend
 * lors de l'appel POST /api/notifications
 *
 * Exemple de payload JSON :
 * {
 *   "title": "Nouveau message",
 *   "message": "Vous avez reçu un nouveau message",
 *   "type": "INFO"
 * }
 */
export interface NotificationRequest {
  /**
   * Titre de la notification (obligatoire)
   * Correspond au champ @NotBlank String title côté Java
   */
  title: string;

  /**
   * Message détaillé de la notification (obligatoire)
   * Correspond au champ @NotBlank String message côté Java
   */
  message: string;

  /**
   * Type de notification (obligatoire)
   * Correspond au champ @NotNull NotificationType type côté Java
   */
  type: NotificationType;
}

/**
 * INTERFACE POUR LES RÉPONSES HTTP ET WEBSOCKET
 *
 * Cette interface représente la structure complète d'une notification
 * reçue depuis le backend (via HTTP ou WebSocket).
 *
 * Elle inclut tous les champs du DTO Java NotificationDTO :
 * - id : Identifiant unique généré par le backend
 * - title : Titre de la notification
 * - message : Contenu de la notification
 * - type : Type de notification (INFO, SUCCESS, WARNING, ERROR)
 * - timestamp : Date/heure de création (ISO 8601)
 */
export interface Notification {
  /**
   * Identifiant unique de la notification (UUID)
   * Généré automatiquement par le backend avec UUID.randomUUID()
   * Exemple : "123e4567-e89b-12d3-a456-426614174000"
   */
  id: string;

  /**
   * Titre de la notification
   */
  title: string;

  /**
   * Message détaillé de la notification
   */
  message: string;

  /**
   * Type de notification (enum)
   */
  type: NotificationType;

  /**
   * Horodatage de création au format ISO 8601
   * Exemple : "2024-01-15T14:30:00.000Z"
   * Généré par LocalDateTime.now() côté Java
   */
  timestamp: string;
}

/**
 * INTERFACE POUR LES ÉTATS DE CONNEXION WEBSOCKET
 *
 * Cette interface représente l'état actuel de la connexion WebSocket.
 * Elle est utilisée pour afficher un badge de statut dans l'interface.
 */
export interface WebSocketStatus {
  /**
   * État de connexion :
   * - 'connected' : WebSocket connecté et actif
   * - 'disconnected' : WebSocket déconnecté
   * - 'connecting' : Connexion en cours
   * - 'error' : Erreur de connexion
   */
  status: 'connected' | 'disconnected' | 'connecting' | 'error';

  /**
   * Message descriptif de l'état (optionnel)
   * Ex: "Connecté au serveur", "Tentative de reconnexion..."
   */
  message?: string;
}

/**
 * TYPE UTILITAIRE POUR LES ERREURS HTTP
 *
 * Ce type représente les erreurs retournées par HttpClient en cas
 * d'échec d'une requête HTTP (4xx, 5xx, erreur réseau, etc.)
 */
export interface HttpErrorResponse {
  /**
   * Code de statut HTTP (ex: 404, 500, 503)
   */
  status: number;

  /**
   * Texte du statut HTTP (ex: "Not Found", "Internal Server Error")
   */
  statusText: string;

  /**
   * Message d'erreur détaillé
   */
  message: string;

  /**
   * Corps de la réponse d'erreur (peut contenir des détails du backend)
   */
  error?: any;
}
