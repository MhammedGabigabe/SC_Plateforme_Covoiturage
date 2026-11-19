# Convention de logs (SLF4J)

Les classes utilisent l'annotation Lombok `@Slf4j`, qui crée un logger SLF4J nommé `log`.

| Niveau | Quand l'utiliser | Où |
|---|---|---|
| INFO | Action métier réussie (création, publication, réservation, annulation, fin de trajet) | Dans les services |
| WARN | Règle métier refusée, ressource introuvable, action interdite, erreur de validation | Dans `GlobalExceptionHandler` (un seul log par erreur) |
| ERROR | Erreur interne imprévue (500), avec la stack trace | Dans `GlobalExceptionHandler` |

Exemples dans un service :

    log.info("Trajet {} publié par le conducteur {}", trajet.getId(), conducteurId);
    log.info("Réservation {} confirmée : {} place(s), montant {} DH", reservation.getId(), nombrePlaces, montant);

Les services ne loguent pas les refus : ils lèvent une exception, et le handler la journalise en WARN.