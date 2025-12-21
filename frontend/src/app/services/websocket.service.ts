/* ============================================================================
   SERVICE WEBSOCKET AVEC CONNEXION MANUELLE
   ============================================================================
   Modifications :
   - Suppression de la connexion automatique dans le constructeur
   - Ajout d'un flag pour empêcher les connexions multiples
   - Méthode connect() devient publique et peut être appelée à la demande
   ============================================================================ */

import { Injectable, OnDestroy, effect, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { Notification } from '../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService implements OnDestroy {
  /* ==========================================================================
     CONFIGURATION
     ========================================================================== */

  private readonly SOCKET_URL = 'http://localhost:8080/ws';
  private readonly NOTIFICATION_TOPIC = '/topic/notifications';
  private readonly RECONNECT_DELAY = 5000;

  /* ==========================================================================
     SIGNALS RÉACTIFS
     ========================================================================== */

  /**
   * Signal pour l'état de connexion
   * false au démarrage (non connecté)
   */
  private isConnectedSignal = signal<boolean>(false);

  /**
   * Signal pour indiquer si une connexion est en cours
   * Évite les clics multiples sur le bouton de connexion
   */
  private isConnectingSignal = signal<boolean>(false);

  /**
   * Signal pour la dernière notification reçue
   */
  private notificationSignal = signal<Notification | null>(null);

  /**
   * Signal pour la liste de toutes les notifications
   */
  private notificationsList = signal<Notification[]>([]);

  /**
   * Signal pour les messages d'erreur
   * Permet d'afficher les erreurs dans l'UI
   */
  private errorSignal = signal<string | null>(null);

  /* ==========================================================================
     GETTERS PUBLICS (READ-ONLY)
     ========================================================================== */

  public isConnected = this.isConnectedSignal.asReadonly();
  public isConnecting = this.isConnectingSignal.asReadonly();
  public latestNotification = this.notificationSignal.asReadonly();
  public notifications = this.notificationsList.asReadonly();
  public error = this.errorSignal.asReadonly();

  /* ==========================================================================
     OBJETS STOMP
     ========================================================================== */

  private stompClient: Client | null = null;
  private subscription: StompSubscription | undefined;

  /* ==========================================================================
     CONSTRUCTEUR (SANS CONNEXION AUTOMATIQUE)
     ========================================================================== */

  /**
   * Constructeur du service
   *
   * ⚠️ IMPORTANT : La connexion n'est PLUS automatique !
   * Le composant doit appeler connect() manuellement via un bouton.
   */
  constructor() {
    console.log('🔌 WebSocketService initialisé (mode manuel)');
    console.log(`📡 Socket URL: ${this.SOCKET_URL}`);
    console.log(`📢 Topic: ${this.NOTIFICATION_TOPIC}`);
    console.log('⏸️  Connexion manuelle requise - Utilisez connect()');

    /**
     * Effet réactif pour logger les changements d'état
     */
    effect(() => {
      const connected = this.isConnected();
      const connecting = this.isConnecting();

      if (connected) {
        console.log('📊 État: ✅ Connecté');
      } else if (connecting) {
        console.log('📊 État: 🔄 Connexion en cours...');
      } else {
        console.log('📊 État: ❌ Déconnecté');
      }
    });

    /**
     * Effet pour afficher les erreurs
     */
    effect(() => {
      const error = this.error();
      if (error) {
        console.error('❌ Erreur WebSocket:', error);
      }
    });
  }

  /* ==========================================================================
     MÉTHODE CONNECT - CONNEXION MANUELLE
     ========================================================================== */

  /**
   * ÉTABLIR LA CONNEXION WEBSOCKET
   *
   * ⭐ NOUVEAUTÉ : Méthode publique appelée par un bouton dans l'UI
   *
   * Protections :
   * - Empêche les connexions multiples si déjà connecté
   * - Empêche les clics multiples pendant la connexion
   * - Gère les erreurs et affiche des messages clairs
   */
  connect(): void {
    /* ========================================================================
       VÉRIFICATIONS PRÉLIMINAIRES
       ======================================================================== */

    // Si déjà connecté, ne rien faire
    if (this.stompClient?.connected) {
      console.log('⚠️  Déjà connecté au WebSocket');
      this.errorSignal.set('Déjà connecté au serveur');
      return;
    }

    // Si connexion en cours, ne rien faire (évite double-clic)
    if (this.isConnectingSignal()) {
      console.log('⚠️  Connexion déjà en cours, veuillez patienter');
      return;
    }

    /* ========================================================================
       INITIALISATION DE LA CONNEXION
       ======================================================================== */

    console.log('🔄 Démarrage de la connexion WebSocket...');

    // Réinitialiser les erreurs précédentes
    this.errorSignal.set(null);

    // Marquer la connexion comme en cours
    this.isConnectingSignal.set(true);

    /* ========================================================================
       CRÉATION DU CLIENT STOMP AVEC SOCKJS
       ======================================================================== */

    this.stompClient = new Client({
      /**
       * Factory SockJS (correction du bug 'global is not defined')
       */
      webSocketFactory: () => {
        console.log('🏭 Création de la connexion SockJS...');
        return new SockJS.default(this.SOCKET_URL);
      },

      /**
       * Configuration de la reconnexion automatique
       */
      reconnectDelay: this.RECONNECT_DELAY,

      /**
       * Configuration des heartbeats
       */
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      /**
       * Debug logging (désactiver en production)
       */
      debug: (str: string) => {
        console.log('🔍 [STOMP]', str);
      },

      /* ======================================================================
         CALLBACK : onConnect (CONNEXION RÉUSSIE)
         ====================================================================== */

      onConnect: (frame) => {
        console.log('✅ Connexion WebSocket établie avec succès !');
        console.log('📄 Frame CONNECTED:', frame);

        // Marquer comme connecté
        this.isConnectedSignal.set(true);

        // Connexion terminée (plus en cours)
        this.isConnectingSignal.set(false);

        // Réinitialiser les erreurs
        this.errorSignal.set(null);

        /* ==================================================================
           S'ABONNER AU TOPIC DES NOTIFICATIONS
           ================================================================== */

        this.subscription = this.stompClient?.subscribe(
          this.NOTIFICATION_TOPIC,
          (message: IMessage) => {
            this.handleIncomingNotification(message);
          }
        );

        console.log('📢 Abonné au topic:', this.NOTIFICATION_TOPIC);
      },

      /* ======================================================================
         CALLBACK : onStompError (ERREUR STOMP)
         ====================================================================== */

      onStompError: (frame) => {
        console.error('❌ Erreur STOMP détectée !');
        console.error('   Message:', frame.headers['message']);

        // Mettre à jour les états
        this.isConnectedSignal.set(false);
        this.isConnectingSignal.set(false);

        // Définir le message d'erreur
        const errorMsg = frame.headers['message'] || 'Erreur STOMP inconnue';
        this.errorSignal.set(`Erreur STOMP: ${errorMsg}`);
      },

      /* ======================================================================
         CALLBACK : onDisconnect (DÉCONNEXION)
         ====================================================================== */

      onDisconnect: (frame) => {
        console.log('⚠️  Déconnexion WebSocket');

        // Mettre à jour les états
        this.isConnectedSignal.set(false);
        this.isConnectingSignal.set(false);

        // Message informatif (pas une erreur si déconnexion volontaire)
        this.errorSignal.set('Déconnecté du serveur');
      },

      /* ======================================================================
         CALLBACK : onWebSocketClose (WEBSOCKET FERMÉ)
         ====================================================================== */

      onWebSocketClose: (event) => {
        console.log('🔌 WebSocket fermé');

        // Mettre à jour les états
        this.isConnectedSignal.set(false);
        this.isConnectingSignal.set(false);
      },

      /* ======================================================================
         CALLBACK : onWebSocketError (ERREUR WEBSOCKET)
         ====================================================================== */

      onWebSocketError: (event) => {
        console.error('❌ Erreur WebSocket !');
        console.error('   Event:', event);

        // Mettre à jour les états
        this.isConnectedSignal.set(false);
        this.isConnectingSignal.set(false);

        // Message d'erreur détaillé
        this.errorSignal.set(
          'Impossible de se connecter au serveur. ' +
            'Vérifiez que le backend Spring Boot est démarré sur http://localhost:8080'
        );
      },
    });

    /* ========================================================================
       ACTIVATION DU CLIENT STOMP
       ======================================================================== */

    this.stompClient.activate();
    console.log('🚀 Client STOMP activé - Connexion en cours...');
  }

  /* ==========================================================================
     TRAITER UNE NOTIFICATION REÇUE
     ========================================================================== */

  private handleIncomingNotification(message: IMessage): void {
    if (!message.body) {
      console.warn('⚠️  Message reçu sans body, ignoré');
      return;
    }

    try {
      console.log('📨 Notification WebSocket reçue !');

      // Parser le JSON
      const notification: Notification = JSON.parse(message.body);

      console.log('✅ Notification parsée:', notification);

      // Mettre à jour les Signals
      this.notificationSignal.set(notification);
      this.notificationsList.update((list) => [notification, ...list]);

      console.log('📤 Signals mis à jour');
    } catch (error) {
      console.error('❌ Erreur parsing notification:', (error as Error).message);
      this.errorSignal.set("Erreur lors du traitement d'une notification");
    }
  }

  /* ==========================================================================
     DÉCONNEXION MANUELLE
     ========================================================================== */

  /**
   * FERMER LA CONNEXION WEBSOCKET
   *
   * Peut être appelée par un bouton "Se déconnecter" dans l'UI
   */
  disconnect(): void {
    console.log('🔌 Déconnexion du WebSocket...');

    // Se désabonner du topic
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = undefined;
      console.log('📢 Désabonnement effectué');
    }

    // Désactiver le client STOMP
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      console.log('🔌 Client STOMP désactivé');
    }

    // Mettre à jour les états
    this.isConnectedSignal.set(false);
    this.isConnectingSignal.set(false);
    this.errorSignal.set(null);

    console.log('✅ Déconnexion terminée');
  }

  /* ==========================================================================
     CLEANUP
     ========================================================================== */

  ngOnDestroy(): void {
    console.log('🗑️  WebSocketService détruit - Cleanup...');
    this.disconnect();
  }
}
