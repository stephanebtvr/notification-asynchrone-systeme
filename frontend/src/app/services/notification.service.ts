/* ============================================================================
   SERVICE HTTP POUR L'API REST
   ============================================================================
   Ce service Angular encapsule toutes les interactions HTTP avec le backend
   Spring Boot. Il utilise HttpClient d'Angular pour effectuer des requêtes
   REST vers l'API /api/notifications.
   
   Architecture :
   - Injectable() : Permet l'injection de dépendances Angular
   - HttpClient : Service Angular pour les requêtes HTTP
   - Observable : Pattern réactif RxJS pour gérer les réponses asynchrones
   - catchError : Opérateur RxJS pour la gestion centralisée des erreurs
   ============================================================================ */

// Imports Angular core et HTTP
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';

// Imports RxJS pour la programmation réactive
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

// Imports des modèles TypeScript personnalisés
import { Notification, NotificationRequest } from '../models/notification.model';

/**
 * DÉCORATEUR @Injectable
 *
 * Ce décorateur marque la classe comme injectable dans le système de
 * Dependency Injection (DI) d'Angular.
 *
 * providedIn: 'root' :
 * - Le service est un singleton disponible dans toute l'application
 * - Pas besoin de le déclarer dans les providers des modules
 * - Angular 18 utilise des standalone components, donc 'root' est idéal
 */
@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  /* ==========================================================================
     CONFIGURATION DE L'API
     ========================================================================== */

  /**
   * URL de base de l'API backend Spring Boot
   *
   * IMPORTANT : Cette URL doit correspondre à votre configuration backend :
   * - server.port=8080 dans application.properties
   * - @RequestMapping("/api/notifications") dans le Controller
   *
   * En production, remplacez par : https://votre-domaine.com/api/notifications
   */
  private readonly API_URL = 'http://localhost:8080/api/notifications';

  /**
   * Headers HTTP par défaut pour toutes les requêtes
   *
   * Content-Type: application/json :
   * - Indique au serveur que le body est au format JSON
   * - Spring Boot utilisera Jackson pour désérialiser automatiquement
   *
   * Accept: application/json :
   * - Indique au serveur que le client attend une réponse JSON
   */
  private readonly HTTP_OPTIONS = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      Accept: 'application/json',
    }),
  };

  /* ==========================================================================
     CONSTRUCTEUR ET INJECTION DE DÉPENDANCES
     ========================================================================== */

  /**
   * Constructeur avec injection de HttpClient
   *
   * @param http - Service Angular pour effectuer des requêtes HTTP
   *
   * Injection automatique par Angular :
   * Angular détecte le paramètre du constructeur et injecte automatiquement
   * une instance de HttpClient depuis le système de DI.
   */
  constructor(private http: HttpClient) {
    console.log('✅ NotificationService initialisé');
    console.log(`📡 API URL: ${this.API_URL}`);
  }

  /* ==========================================================================
     MÉTHODES PUBLIQUES DE L'API
     ========================================================================== */

  /**
   * ENVOYER UNE NOUVELLE NOTIFICATION
   *
   * Effectue une requête POST vers /api/notifications pour créer une nouvelle
   * notification. Le backend :
   * 1. Valide les données avec @Valid
   * 2. Génère un ID et un timestamp
   * 3. Envoie la notification à Kafka
   * 4. Retourne la notification créée
   *
   * @param request - Données de la notification (title, message, type)
   * @returns Observable<Notification> - Stream réactif de la notification créée
   *
   * Exemple d'utilisation dans un composant :
   * ```typescript
   * this.notificationService.sendNotification({
   *   title: 'Test',
   *   message: 'Message de test',
   *   type: NotificationType.INFO
   * }).subscribe({
   *   next: (notification) => console.log('Créée:', notification),
   *   error: (error) => console.error('Erreur:', error)
   * });
   * ```
   */
  sendNotification(request: NotificationRequest): Observable<Notification> {
    console.log('📤 Envoi de notification:', request);

    /**
     * HttpClient.post<T>() :
     * - Effectue une requête HTTP POST
     * - <Notification> : Le type de la réponse attendue (typage générique)
     * - Retourne un Observable qui émet la réponse puis se termine (complete)
     *
     * Pipe RxJS :
     * - tap() : Effet de bord (side effect) pour logger sans modifier le stream
     * - catchError() : Intercepte les erreurs et les transforme
     */
    return this.http
      .post<Notification>(
        this.API_URL, // URL de destination
        request, // Body de la requête (sera sérialisé en JSON)
        this.HTTP_OPTIONS // Headers HTTP
      )
      .pipe(
        // Tap : Loguer la réponse réussie (sans la modifier)
        tap((response: Notification) => {
          console.log('✅ Notification créée avec succès:', response);
          console.log(`   ID: ${response.id}`);
          console.log(`   Type: ${response.type}`);
          console.log(`   Timestamp: ${response.timestamp}`);
        }),

        // CatchError : Gestion centralisée des erreurs
        catchError((error: HttpErrorResponse) => this.handleError(error))
      );
  }

  /**
   * RÉCUPÉRER TOUTES LES NOTIFICATIONS (optionnel)
   *
   * Cette méthode pourrait être utilisée pour afficher l'historique des
   * notifications si vous ajoutez un endpoint GET dans votre backend.
   *
   * Exemple backend (à ajouter dans NotificationController.java) :
   * ```java
   * @GetMapping
   * public ResponseEntity<List<NotificationDTO>> getAllNotifications() {
   *     // Récupérer depuis une base de données ou cache
   *     return ResponseEntity.ok(notifications);
   * }
   * ```
   *
   * @returns Observable<Notification[]> - Stream réactif d'un tableau de notifications
   */
  getAllNotifications(): Observable<Notification[]> {
    console.log('📥 Récupération de toutes les notifications');

    return this.http.get<Notification[]>(this.API_URL, this.HTTP_OPTIONS).pipe(
      tap((notifications: Notification[]) => {
        console.log(`✅ ${notifications.length} notification(s) récupérée(s)`);
      }),
      catchError((error: HttpErrorResponse) => this.handleError(error))
    );
  }

  /* ==========================================================================
     GESTION CENTRALISÉE DES ERREURS
     ========================================================================== */

  /**
   * GÉRER LES ERREURS HTTP
   *
   * Cette méthode centralise la gestion des erreurs HTTP pour éviter la
   * duplication de code. Elle analyse l'erreur et retourne un message
   * compréhensible pour l'utilisateur final.
   *
   * Types d'erreurs possibles :
   * - Erreurs réseau (ex: serveur inaccessible) → error.status === 0
   * - Erreurs client 4xx (ex: 400 Bad Request, 404 Not Found)
   * - Erreurs serveur 5xx (ex: 500 Internal Server Error, 503 Service Unavailable)
   *
   * @param error - Objet HttpErrorResponse d'Angular
   * @returns Observable<never> - Stream d'erreur qui émet immédiatement une erreur
   *
   * throwError() :
   * - Crée un Observable qui émet immédiatement une erreur
   * - Permet de propager l'erreur dans la chaîne Observable
   * - Le composant peut catcher l'erreur avec subscribe({error: ...})
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = '';

    /**
     * ERREUR CÔTÉ CLIENT OU RÉSEAU
     *
     * status === 0 indique :
     * - Impossible de contacter le serveur (serveur éteint)
     * - CORS bloqué par le navigateur
     * - Problème de connexion réseau
     * - Timeout de requête
     */
    if (error.status === 0) {
      console.error('❌ Erreur réseau:', error.error);
      errorMessage = `Impossible de contacter le serveur. Vérifiez que le backend est démarré sur ${this.API_URL}`;
    } else {

    /**
     * ERREUR CÔTÉ SERVEUR
     *
     * Le serveur a retourné un code d'erreur HTTP (4xx ou 5xx)
     */
      console.error(`❌ Erreur HTTP ${error.status}:`, error.error);

      // Switch sur les codes d'erreur courants
      switch (error.status) {
        case 400:
          // Bad Request : Données invalides (échec de @Valid côté backend)
          errorMessage = 'Données invalides. Vérifiez le formulaire.';
          break;
        case 404:
          // Not Found : URL introuvable
          errorMessage = "Endpoint API introuvable. Vérifiez l'URL du backend.";
          break;
        case 500:
          // Internal Server Error : Erreur interne du serveur
          errorMessage = 'Erreur serveur interne. Consultez les logs backend.';
          break;
        case 503:
          // Service Unavailable : Service temporairement indisponible
          errorMessage = 'Service temporairement indisponible. Kafka est-il démarré ?';
          break;
        default:
          // Autres erreurs
          errorMessage = `Erreur serveur: ${error.message}`;
      }
    }

    /**
     * AFFICHAGE DANS LA CONSOLE POUR DEBUG
     *
     * Console structurée pour faciliter le débogage :
     * - Status : Code HTTP
     * - StatusText : Texte du statut
     * - Message : Message détaillé
     * - Error : Objet erreur complet
     */
    console.group("🔍 Détails de l'erreur HTTP");
    console.log('Status:', error.status);
    console.log('StatusText:', error.statusText);
    console.log('Message:', errorMessage);
    console.log('Error Object:', error);
    console.groupEnd();

    /**
     * PROPAGATION DE L'ERREUR
     *
     * throwError() retourne un Observable qui émet immédiatement l'erreur.
     * Le composant qui subscribe() pourra la catcher dans le callback error:
     *
     * this.service.sendNotification(...).subscribe({
     *   next: (data) => {...},
     *   error: (err) => { // Cette fonction recevra 'errorMessage' }
     * });
     */
    return throwError(() => errorMessage);
  }
}
