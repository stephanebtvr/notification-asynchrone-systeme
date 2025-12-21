/* ============================================================================
   POLYFILLS POUR SOCKJS-CLIENT
   ============================================================================
   SockJS est une bibliothèque Node.js qui attend des variables globales
   qui n'existent pas dans les navigateurs modernes (global, process, Buffer).
   
   Ces polyfills ajoutent ces variables au contexte global du navigateur
   pour assurer la compatibilité avec SockJS.
   ============================================================================ */

// Polyfill pour 'global' (attend window dans le navigateur)
(window as any).global = window;

// Polyfill pour 'process' (utilisé par SockJS pour détecter l'environnement)
(window as any).process = {
  env: { DEBUG: undefined },
  version: '', // Version de Node.js (vide dans le navigateur)
  nextTick: (fn: Function) => setTimeout(fn, 0), // Simulation de nextTick
};

// Polyfill pour 'Buffer' (si nécessaire, décommenter)
// import { Buffer } from 'buffer';
// (window as any).Buffer = Buffer;

console.log('✅ Polyfills SockJS chargés');
