/* ============================================================================
   COMPOSANT PRINCIPAL AVEC BOUTONS DE CONNEXION/DÉCONNEXION
   ============================================================================ */

import { CommonModule } from '@angular/common';
import { Component, effect } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { NotificationService } from './services/notification.service';
import { WebsocketService } from './services/websocket.service';
import { NotificationRequest } from './models/notification.model';
import { HelpComponent } from './help/help-component/help-component';

@Component({
  selector: 'app-root',
  imports: [CommonModule, ReactiveFormsModule, HelpComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  /* ==========================================================================
     PROPRIÉTÉS
     ========================================================================== */

  /**
   * Formulaire réactif pour envoyer des notifications
   */
  notificationForm: FormGroup;

  /**
   * Signals exposés du service WebSocket
   */
  notifications: any;
  isConnected: any;
  isConnecting: any;
  error: any;

  /**
   * Flag pour afficher un message de succès après envoi
   */
  showSuccessMessage = false;

  /* ==========================================================================
     CONSTRUCTEUR
     ========================================================================== */

  constructor(
    private fb: FormBuilder,
    private notificationService: NotificationService,
    private websocketService: WebsocketService
  ) {
    this.notifications = this.websocketService.notifications;
    this.isConnected = this.websocketService.isConnected;
    this.isConnecting = this.websocketService.isConnecting;
    this.error = this.websocketService.error;

    /**
     * Initialisation du formulaire avec validation
     */
    this.notificationForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      message: ['', [Validators.required, Validators.minLength(5)]],
      type: ['INFO', Validators.required],
    });

    /**
     * Effet pour logger les nouvelles notifications
     */
    effect(() => {
      const notifs = this.notifications();
      if (notifs.length > 0) {
        console.log('📬 Nouvelle notification reçue:', notifs[0]);
      }
    });
  }

  /* ==========================================================================
     MÉTHODES DE CONNEXION WEBSOCKET
     ========================================================================== */

  /**
   * CONNECTER LE WEBSOCKET
   *
   * Appelé par le bouton "Se connecter au WebSocket"
   */
  connectWebSocket(): void {
    console.log('🔘 Bouton "Se connecter" cliqué');
    this.websocketService.connect();
  }

  /**
   * DÉCONNECTER LE WEBSOCKET
   *
   * Appelé par le bouton "Se déconnecter"
   */
  disconnectWebSocket(): void {
    console.log('🔘 Bouton "Se déconnecter" cliqué');
    this.websocketService.disconnect();
  }

  /* ==========================================================================
     ENVOI DE NOTIFICATION
     ========================================================================== */

  /**
   * ENVOYER UNE NOTIFICATION VIA L'API REST
   *
   * Appelé lors de la soumission du formulaire
   */
  onSubmit(): void {
    // Vérifier la validité du formulaire
    if (this.notificationForm.invalid) {
      console.log('⚠️  Formulaire invalide');
      return;
    }

    // Extraire les données du formulaire
    const notification: NotificationRequest = this.notificationForm.value;

    console.log('📤 Envoi de notification via API REST:', notification);

    // Envoyer via le service HTTP
    this.notificationService.sendNotification(notification).subscribe({
      next: (response) => {
        console.log('✅ Notification envoyée avec succès:', response);

        // Réinitialiser le formulaire
        this.notificationForm.reset({ type: 'INFO' });

        // Afficher un message de succès
        this.showSuccessMessage = true;

        // Cacher le message après 3 secondes
        setTimeout(() => {
          this.showSuccessMessage = false;
        }, 3000);
      },
      error: (err) => {
        console.error("❌ Erreur lors de l'envoi:", err);
        alert(`Erreur: ${err}`);
      },
    });
  }

  /* ==========================================================================
     GETTERS POUR LE TEMPLATE
     ========================================================================== */

  /**
   * Getter pour accéder facilement aux contrôles du formulaire dans le template
   */
  get f() {
    return this.notificationForm.controls;
  }
}
