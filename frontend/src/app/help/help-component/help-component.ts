import { Component } from '@angular/core';

@Component({
  selector: 'app-help',
  standalone: true,
  template: `<!-- help.component.html -->
    <div class="bg-white rounded-xl shadow-lg p-8">
      <h2 class="text-2xl font-bold text-gray-800 mb-6">Mode d’emploi rapide</h2>

      <div class="space-y-8">
        <!-- Étape 1 -->
        <div class="p-5 rounded-lg border-l-4 border-blue-500 bg-blue-50">
          <h3 class="text-xl font-semibold text-blue-800 mb-3">1. Se connecter</h3>
          <p class="text-gray-700">
            Cliquez sur le bouton <strong class="text-green-700">« Se connecter »</strong> (vert
            avec 🔌) en haut à droite. Le badge passe au vert quand la connexion WebSocket est
            active.
          </p>
        </div>

        <!-- Étape 2 -->
        <div class="p-5 rounded-lg border-l-4 border-yellow-500 bg-yellow-50">
          <h3 class="text-xl font-semibold text-yellow-800 mb-3">2. Envoyer une notification</h3>
          <p class="text-gray-700">
            Remplissez les champs :
            <strong>Titre</strong>, <strong>Message</strong> et <strong>Type</strong> (INFO,
            WARNING, ERROR). Cliquez sur <strong class="text-blue-600">« Envoyer »</strong> (le
            bouton est bleu et s’active seulement si tout est rempli).
          </p>
        </div>

        <!-- Étape 3 -->
        <div class="p-5 rounded-lg border-l-4 border-red-500 bg-red-50">
          <h3 class="text-xl font-semibold text-red-800 mb-3">3. Voir les notifications reçues</h3>
          <p class="text-gray-700">
            Elles s’affichent automatiquement à droite, avec une couleur selon le type :
            <span class="font-medium text-blue-600">bleu</span> pour INFO,
            <span class="font-medium text-yellow-600">jaune</span> pour WARNING,
            <span class="font-medium text-red-600">rouge</span> pour ERROR.
          </p>
        </div>
      </div>

      <div class="mt-8 text-center text-gray-600 text-sm">
        Une fois terminé, cliquez sur
        <strong class="text-red-600">« Se déconnecter »</strong> (bouton rouge).
      </div>
    </div> `,
})
export class HelpComponent {}
