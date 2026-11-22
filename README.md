# Plateforme de covoiturage multi-étapes

API REST permettant aux **conducteurs** de publier des trajets composés de plusieurs étapes et aux **passagers** de réserver des places sur **tout ou partie** du parcours.

Exemple : un conducteur publie Rabat → Casablanca → Settat → Marrakech. Un passager peut réserver uniquement Settat → Marrakech et ne payer que ce segment.

> Projet individuel YouCode (Simplon Maghreb), brief « Plateforme de covoiturage multi-étapes ».

---

## Sommaire

1. [Fonctionnalités](#1-fonctionnalités)
2. [Technologies](#2-technologies)
3. [Prérequis](#3-prérequis)
4. [Installation et lancement](#4-installation-et-lancement)
5. [Base de données et Liquibase](#5-base-de-données-et-liquibase)
6. [Documentation de l'API (Swagger)](#6-documentation-de-lapi-swagger)
7. [Identification simulée](#7-identification-simulée)
8. [Endpoints](#8-endpoints)
9. [Règles métier](#9-règles-métier)
10. [Gestion des erreurs](#10-gestion-des-erreurs)
11. [Modèle de données](#11-modèle-de-données)
12. [Architecture du projet](#12-architecture-du-projet)
13. [Données de test](#13-données-de-test)
14. [Scénarios de validation](#14-scénarios-de-validation)
15. [Tests unitaires](#15-tests-unitaires)
16. [Choix techniques](#16-choix-techniques)
17. [Gestion de projet](#17-gestion-de-projet)
18. [Problèmes fréquents](#18-problèmes-fréquents)
19. [Auteur](#19-auteur)

---

## 1. Fonctionnalités

- Gérer les **conducteurs**, les **passagers** et les **véhicules**.
- **Publier un trajet** avec plusieurs points d'arrêt (ville, heure de passage, prix depuis le départ).
- **Rechercher** des trajets entre deux villes à une date donnée (résultats paginés et triés par heure de départ).
- **Réserver** une ou plusieurs places entre un point de montée et un point de descente.
- **Calculer automatiquement** le montant d'une réservation et les places restantes.
- **Annuler** une réservation (jusqu'à 24 h avant le départ) ou **annuler / terminer** un trajet.
- Consulter l'historique des réservations d'un passager et les réservations d'un trajet (conducteur).

## 2. Technologies

| Domaine | Technologie |
|---|---|
| Langage | Java 17 |
| Framework | Spring Boot 4.1.2 (Spring MVC, Spring Data JPA, Bean Validation) |
| Base de données | PostgreSQL 16 lancée avec Docker Compose |
| ORM | Hibernate (Spring Data JPA) |
| Migrations | Liquibase (fichiers YAML) |
| Conversion DTO | MapStruct 1.6.3 (DTO sous forme de `record`) |
| Réduction du code | Lombok |
| Documentation | springdoc-openapi 3.2.0 (Swagger UI) |
| Tests | JUnit 5, Mockito, AssertJ |
| Build | Maven (Maven Wrapper fourni) |
| Logs | SLF4J |

## 3. Prérequis

- **JDK 17** ou plus récent (`java -version`)
- **Docker Desktop** démarré (`docker --version`)
- **Git**
- Aucun Maven à installer : le Maven Wrapper (`mvnw`) est inclus.

## 4. Installation et lancement

**1. Récupérer le projet**

```bash
git clone https://github.com/MhammedGabigabe/SC_Plateforme_Covoiturage.git
cd SC_Plateforme_Covoiturage
```

**2. Démarrer PostgreSQL avec Docker Compose**

```bash
docker compose up -d
docker compose ps
```

Le conteneur `covoiturage-db` doit être au statut **healthy**.

**3. Lancer l'application**

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows (PowerShell)
.\mvnw spring-boot:run
```

Au démarrage, **Liquibase crée automatiquement les tables et insère les données de test**. L'application écoute sur `http://localhost:8080`.

**4. Ouvrir Swagger UI**

```
http://localhost:8080/swagger-ui.html
```

**Arrêter et réinitialiser**

```bash
docker compose down        # arrêter les conteneurs (les données sont conservées)
docker compose down -v     # arrêter et SUPPRIMER les données (base vierge au prochain démarrage)
```

### Configuration

Le fichier `src/main/resources/application.yaml` contient la configuration :

| Paramètre | Valeur |
|---|---|
| URL de la base | `jdbc:postgresql://localhost:5433/covoiturage` |
| Utilisateur / mot de passe | `covoiturage` / `covoiturage` |
| Port PostgreSQL exposé | `5433` (le conteneur utilise 5432 en interne, pour éviter un conflit avec un PostgreSQL local) |
| Port de l'application | `8080` |
| `spring.jpa.hibernate.ddl-auto` | `validate` : Hibernate ne crée aucune table, le schéma vient uniquement de Liquibase |
| `spring.liquibase.contexts` | `dev` : exécute aussi le changeset de données de test |

## 5. Base de données et Liquibase

Le schéma est **versionné avec Liquibase**. Les fichiers sont dans `src/main/resources/db/changelog/` :

| Fichier | Contenu |
|---|---|
| `db.changelog-master.yaml` | Liste des changesets dans l'ordre d'exécution |
| `changes/000-init.yaml` | Initialisation (tag de départ) |
| `changes/001-create-conducteur.yaml` | Table `conducteur` |
| `changes/002-create-passager.yaml` | Table `passager` |
| `changes/003-create-vehicule.yaml` | Table `vehicule` (clé étrangère vers `conducteur`) |
| `changes/004-create-trajet.yaml` | Table `trajet` (clé étrangère vers `vehicule`) |
| `changes/005-create-point-arret.yaml` | Table `point_arret` (clé étrangère vers `trajet`) |
| `changes/006-create-reservation.yaml` | Table `reservation` (clés étrangères vers `passager`, `trajet` et deux vers `point_arret`) |
| `changes/007-constraints.yaml` | Contraintes d'unicité, contraintes `CHECK` et index unique partiel |
| `changes/008-seed-data.yaml` + `.sql` | Données de test (contexte `dev` uniquement) |

**Contraintes en base**

- Unicité : email du conducteur et du passager, numéro de permis, immatriculation, ordre et ville d'un point par trajet.
- `CHECK` : capacité d'un véhicule entre 1 et 8, places proposées ≥ 1, prix ≥ 0, nombre de places réservées ≥ 1.
- **Index unique partiel** : un passager n'a qu'**une seule réservation `CONFIRMEE` par trajet**. Une ancienne réservation `ANNULEE` n'empêche pas de réserver à nouveau.

```sql
CREATE UNIQUE INDEX uk_reservation_confirmee
ON reservation (passager_id, trajet_id) WHERE statut = 'CONFIRMEE';
```

**Règles de travail Liquibase**

- Ne jamais modifier un changeset déjà exécuté : en ajouter un nouveau (sinon Liquibase signale une erreur de checksum).
- Ajouter le nouveau fichier dans `db.changelog-master.yaml`.
- L'historique des exécutions est dans la table `databasechangelog`.

```sql
SELECT orderexecuted, id, author, dateexecuted FROM databasechangelog ORDER BY orderexecuted;
```

## 6. Documentation de l'API (Swagger)

| Ressource | Adresse |
|---|---|
| Swagger UI (tester les endpoints) | http://localhost:8080/swagger-ui.html |
| Description OpenAPI (JSON) | http://localhost:8080/v3/api-docs |

Chaque endpoint est documenté (description, paramètres, codes de réponse), et les corps de requête sont pré-remplis avec des exemples. Dans Swagger UI : déployer un endpoint, **Try it out**, renseigner les paramètres, puis **Execute**.

## 7. Identification simulée

Le brief ne demande pas d'authentification. L'appelant est donc **identifié par un en-tête HTTP** :

| En-tête | Rôle | Utilisé pour |
|---|---|---|
| `X-Conducteur-Id` | Conducteur connecté | Modifier son profil, gérer ses véhicules, publier / terminer / annuler un trajet, voir les réservations de ses trajets |
| `X-Passager-Id` | Passager connecté | Modifier son profil, réserver, annuler sa réservation, consulter ses réservations |

Un conducteur ne peut agir que sur **ses** ressources, et un passager ne voit que **ses** réservations : sinon l'API répond **403**.

## 8. Endpoints

### Conducteurs : `/api/conducteurs`

| Méthode | URL | Description | En-tête |
|---|---|---|---|
| POST | `/api/conducteurs` | Créer un conducteur | |
| GET | `/api/conducteurs` | Lister les conducteurs | |
| GET | `/api/conducteurs/{id}` | Consulter un conducteur | |
| PUT | `/api/conducteurs/{id}` | Modifier son profil | `X-Conducteur-Id` |
| GET | `/api/conducteurs/recherche?nom=&email=` | Rechercher par nom ou par email | |

### Passagers : `/api/passagers`

| Méthode | URL | Description | En-tête |
|---|---|---|---|
| POST | `/api/passagers` | Créer un passager | |
| GET | `/api/passagers` | Lister les passagers | |
| GET | `/api/passagers/{id}` | Consulter un passager | |
| PUT | `/api/passagers/{id}` | Modifier son profil | `X-Passager-Id` |
| GET | `/api/passagers/recherche?email=` | Rechercher par email | |

### Véhicules : `/api/vehicules`

| Méthode | URL | Description | En-tête |
|---|---|---|---|
| POST | `/api/vehicules` | Ajouter un véhicule | `X-Conducteur-Id` |
| GET | `/api/vehicules?conducteurId=` | Lister les véhicules d'un conducteur | |
| GET | `/api/vehicules/{id}` | Consulter un véhicule | |
| PUT | `/api/vehicules/{id}` | Modifier un véhicule | `X-Conducteur-Id` |
| DELETE | `/api/vehicules/{id}` | Supprimer un véhicule (si aucun trajet associé) | `X-Conducteur-Id` |

### Trajets : `/api/trajets`

| Méthode | URL | Description | En-tête |
|---|---|---|---|
| POST | `/api/trajets` | Publier un trajet avec ses points d'arrêt | `X-Conducteur-Id` |
| GET | `/api/trajets/{id}` | Détail : parcours, places restantes, conducteur | |
| GET | `/api/trajets/recherche` | Rechercher des trajets (paginé) | |
| GET | `/api/trajets/{id}/reservations` | Réservations d'un trajet | `X-Conducteur-Id` |
| PATCH | `/api/trajets/{id}/terminer` | Terminer un trajet (après l'heure de départ) | `X-Conducteur-Id` |
| PATCH | `/api/trajets/{id}/annulation` | Annuler un trajet et toutes ses réservations | `X-Conducteur-Id` |

Paramètres de `GET /api/trajets/recherche` :

| Paramètre | Obligatoire | Description |
|---|---|---|
| `villeDepart` | oui | Ville de montée (n'importe quel point du parcours) |
| `villeArrivee` | oui | Ville de descente (doit être **après** la ville de départ) |
| `date` | oui | Date du trajet, au format `AAAA-MM-JJ` |
| `nombrePlaces` | non (1) | Nombre de places souhaitées |
| `prixMax` | non | Prix maximum du **segment recherché** |
| `page` | non (0) | Numéro de page |
| `taille` | non (10) | Taille de la page (1 à 50) |

### Réservations : `/api/reservations`

| Méthode | URL | Description | En-tête |
|---|---|---|---|
| POST | `/api/reservations` | Réserver des places entre un point de montée et un point de descente | `X-Passager-Id` |
| GET | `/api/reservations` | Consulter mes réservations (historique) | `X-Passager-Id` |
| PATCH | `/api/reservations/{id}/annulation` | Annuler ma réservation | `X-Passager-Id` |

Exemple de corps de `POST /api/reservations` :

```json
{
  "trajetId": 1,
  "pointMonteeId": 3,
  "pointDescenteId": 4,
  "nombrePlaces": 1
}
```

Les identifiants des points d'arrêt sont visibles dans `GET /api/trajets/{id}`.

## 9. Règles métier

**Publication d'un trajet**

- Au moins 2 points : le premier est le départ (prix 0), le dernier est l'arrivée.
- Le départ est dans le futur, les heures de passage sont strictement croissantes.
- Une ville n'apparaît qu'une seule fois sur le parcours, les prix depuis le départ sont strictement croissants.
- Les places proposées vont de 1 à la capacité du véhicule.
- Le conducteur publie uniquement avec l'un de ses propres véhicules.

**Prix et places** (calculés, jamais stockés)

- Prix du segment = prix du point de descente − prix du point de montée.
- Montant total = prix du segment × nombre de places (`BigDecimal`, 2 décimales).
- Places restantes = places proposées − places des réservations `CONFIRMEE`.

Exemple (trajet de Hamid) : Rabat 0 DH, Casablanca 40 DH, Settat 70 DH, Marrakech 120 DH.

| Segment | Prix d'une place |
|---|---|
| Rabat → Marrakech | 120 − 0 = **120 DH** |
| Casablanca → Settat | 70 − 40 = **30 DH** |
| Settat → Marrakech | 120 − 70 = **50 DH** |
| Settat → Casablanca | impossible (mauvais sens) |

**Réservation**

- On ne réserve que sur un trajet `PUBLIE`.
- Les deux points appartiennent au trajet et la montée est avant la descente.
- Le nombre de places réservées ne dépasse jamais les places proposées.
- Un passager n'a qu'une seule réservation `CONFIRMEE` par trajet.
- Le trajet passe à `COMPLET` quand il ne reste plus de place, et repasse à `PUBLIE` quand une place se libère.

**Annulation et cycle de vie**

- Un passager annule jusqu'à **24 heures avant le départ**.
- Un trajet se termine uniquement **après son heure de départ**.
- Annuler un trajet `PUBLIE` ou `COMPLET` passe toutes ses réservations `CONFIRMEE` à `ANNULEE` **dans la même transaction** (tout réussit ou tout échoue).
- `TERMINE` et `ANNULE` sont des statuts finaux.

## 10. Gestion des erreurs

Toutes les erreurs sont centralisées dans un `@RestControllerAdvice` et renvoyées en JSON au format **ProblemDetail** :

```json
{
  "detail": "Le trajet 1 n'est pas ouvert à la réservation (statut COMPLET)",
  "instance": "/api/reservations",
  "status": 409,
  "title": "Règle métier violée",
  "timestamp": "2026-11-22T14:06:48.123"
}
```

| Code | Signification |
|---|---|
| 400 | Erreur de validation, corps illisible, en-tête ou paramètre manquant, règle de saisie (montée après descente, trop de places proposées...) |
| 403 | Action sur la ressource d'un autre conducteur ou passager |
| 404 | Ressource inexistante |
| 405 | Méthode HTTP non autorisée |
| 409 | Règle métier violée (trajet complet, délai de 24 h dépassé, statut final, doublon, unicité...) |
| 500 | Erreur interne |

**Logs (SLF4J)** : `INFO` pour les actions métier réussies, `WARN` pour les règles refusées, `ERROR` pour les erreurs internes.

## 11. Modèle de données

![Diagramme de classes](docs/diagramme-de-classes.png)

| Entité | Rôle |
|---|---|
| `Utilisateur` (abstraite) | Champs communs : nom, prénom, email, téléphone |
| `Conducteur`, `Passager` | Héritent d'`Utilisateur` (deux tables distinctes) |
| `Vehicule` | Appartient à un conducteur, capacité de 1 à 8 places passagers |
| `Trajet` | Véhicule, date de départ, places proposées, statut (`PUBLIE`, `COMPLET`, `TERMINE`, `ANNULE`) |
| `PointArret` | Ville, ordre (0 = départ), heure de passage, prix depuis le départ ; composition avec `Trajet` |
| `Reservation` | Passager, trajet, point de montée, point de descente, places, montant, statut (`CONFIRMEE`, `ANNULEE`) |

Valeurs **déduites**, jamais stockées : ville de départ et d'arrivée d'un trajet (premier et dernier point), prix d'un segment, places restantes. Le conducteur d'un trajet est celui de son véhicule.

## 12. Architecture du projet

Architecture en couches : **Controller → Service → Repository → Entity**, avec des DTO (records) et des mappers MapStruct.

```
src/main/java/ma/youcode/covoiturage/
├── config/        Configuration OpenAPI
├── controller/    Endpoints REST documentés avec Swagger
├── dto/
│   ├── request/   Corps des requêtes (validation Bean Validation)
│   └── response/  Réponses de l'API
├── entity/        Entités JPA (+ enums de statut)
├── exception/     Exceptions métier et gestion centralisée des erreurs
├── mapper/        Conversion entité / DTO avec MapStruct
├── repository/    Accès aux données (Spring Data JPA, requêtes JPQL)
└── service/       Interfaces des services
    └── impl/      Règles métier

src/main/resources/
├── application.yaml
└── db/changelog/  Migrations Liquibase

src/test/java/     Tests unitaires (JUnit 5, Mockito, AssertJ)
docs/              Diagramme de classes
```

## 13. Données de test

Insérées automatiquement par Liquibase (contexte `dev`) : au moins 5 enregistrements par table.

| Conducteur | Véhicule | Trajet (identifiants des points) |
|---|---|---|
| 1 Hamid Alaoui | 1 Dacia Logan (4 places) | **Trajet 1** : Rabat (1) → Casablanca (2) → Settat (3) → Marrakech (4), 3 places proposées, sans réservation |
| 2 Youssef Benali | 2 Renault Clio (3 places) | Trajet 2 : Casablanca (5) → Settat (6) → Marrakech (7) |
| 3 Nadia Chraibi | 3 Peugeot 208 | Trajet 3 : Rabat (8) → Meknès (9) → Fès (10) |
| 4 Omar Tazi | 4 Hyundai i10 | Trajet 4 : Tanger (11) → Rabat (12) → Casablanca (13) |
| 5 Sara Idrissi | 5 Toyota Corolla | Trajet 5 : Marrakech (14) → Agadir (15) |

Passagers : 1 Salma Bennani, 2 Karim Haddad, 3 Imane Berrada, 4 Yassine Lahlou, 5 Meryem Fassi.

Les prix du trajet 1 sont 0, 40, 70 et 120 DH. Les dates de départ sont **relatives au jour de l'exécution du seed** (de 2 à 7 jours plus tard) : vérifier la date réelle avec `GET /api/trajets/1` avant une recherche.

## 14. Scénarios de validation

À exécuter dans Swagger UI, sur une base vierge (`docker compose down -v` puis `docker compose up -d`).

| # | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| 1 | Salma réserve 1 place Settat → Marrakech | `POST /api/reservations`, `X-Passager-Id: 1`, `{"trajetId":1,"pointMonteeId":3,"pointDescenteId":4,"nombrePlaces":1}` | **201**, montant 50 DH, 2 places restantes |
| 2 | Karim recherche Settat → Casablanca | `GET /api/trajets/recherche?villeDepart=Settat&villeArrivee=Casablanca&date=<date du trajet 1>` | **200**, liste vide |
| 3 | Karim réserve 2 places Casablanca → Marrakech | `POST /api/reservations`, `X-Passager-Id: 2`, `{"trajetId":1,"pointMonteeId":2,"pointDescenteId":4,"nombrePlaces":2}` | **201**, montant 160 DH, trajet `COMPLET` |
| 4 | Imane réserve sur le trajet complet | `POST /api/reservations`, `X-Passager-Id: 3`, `{"trajetId":1,"pointMonteeId":3,"pointDescenteId":4,"nombrePlaces":1}` | **409** |
| 5 | Karim annule sa réservation 3 jours avant | `PATCH /api/reservations/{id}/annulation`, `X-Passager-Id: 2` | **204**, trajet de nouveau `PUBLIE` |
| 6 | Montée à Marrakech, descente à Settat | `POST /api/reservations`, `X-Passager-Id: 3`, `{"trajetId":1,"pointMonteeId":4,"pointDescenteId":3,"nombrePlaces":1}` | **400** |
| 7 | Hamid propose 5 places pour une capacité de 4 | `POST /api/trajets`, `X-Conducteur-Id: 1`, `vehiculeId: 1`, `placesProposees: 5` | **400** |
| 8 | Hamid publie avec le véhicule d'un autre | `POST /api/trajets`, `X-Conducteur-Id: 1`, `vehiculeId: 2` | **403** |
| 9 | Hamid termine le trajet avant l'heure de départ | `PATCH /api/trajets/1/terminer`, `X-Conducteur-Id: 1` | **409** |
| 10 | Salma annule 2 heures avant le départ | Publier un trajet partant dans environ 2 h, y réserver avec Salma, puis `PATCH /api/reservations/{id}/annulation`, `X-Passager-Id: 1` | **409**, délai de 24 h dépassé |
| 11 | Hamid annule un trajet qui a deux réservations | `PATCH /api/trajets/1/annulation`, `X-Conducteur-Id: 1` (après les scénarios 1 et 3, avant le 5) | **204**, trajet et réservations annulés ensemble |

Pour vérifier en base : `SELECT id, passager_id, trajet_id, statut FROM reservation;` et `SELECT id, statut FROM trajet;`.

## 15. Tests unitaires

```bash
./mvnw test        # Windows : .\mvnw test
```

| Classe | Tests | Contenu |
|---|---|---|
| `TrajetServiceImplTest` | 21 | Publication, consultation, terminer, annuler : cas nominaux et règles refusées |
| `ReservationServiceImplTest` | 19 | Réservation (montants 50 et 160 DH, `COMPLET`), annulation (retour à `PUBLIE`, 24 h), historique : cas nominaux et règles refusées |
| `CovoiturageApplicationTests` | 1 | Chargement du contexte Spring (nécessite la base démarrée avec `docker compose up -d`) |

Les services sont testés **sans base de données** : les repositories et mappers sont remplacés par des mocks Mockito, et les vérifications utilisent AssertJ.

## 16. Choix techniques

- **Recherche en une seule requête JPQL** : deux jointures sur les points d'arrêt du même trajet (`depart.ordre < arrivee.ordre`), prix du segment et places restantes calculés dans la requête, pagination avec requête de comptage dédiée.
- **Pas de problème N+1** : le détail d'un trajet et les listes de réservations utilisent `JOIN FETCH`.
- **Pas de surbooking** : le trajet est verrouillé (`PESSIMISTIC_WRITE`) pendant une réservation, une annulation, ou une clôture. Deux réservations simultanées de la dernière place ne peuvent pas réussir toutes les deux.
- **Transactions** : réservation, annulation de réservation et annulation de trajet sont transactionnelles (`@Transactional`).
- **Unicité garantie à deux niveaux** : règle vérifiée dans le service (message clair) et contrainte en base (index unique partiel).
- **Montants** : `BigDecimal` avec 2 décimales (`NUMERIC(10,2)` en base).
- **`open-in-view: false`** : pas de chargement paresseux caché dans les controllers.

## 17. Gestion de projet

**Jira** (projet `CME`) : 11 epics, avec stories (« En tant que… je veux… afin de… ») et tâches estimées.

| Epic | Contenu |
|---|---|
| CME-1 | Setup et environnement |
| CME-2 | Modèle de données et Liquibase |
| CME-3 | Gestion des erreurs et logs |
| CME-4 | Conducteurs et passagers |
| CME-5 | Véhicules |
| CME-6 | Publication des trajets |
| CME-7 | Recherche de trajets |
| CME-8 | Réservations |
| CME-9 | Cycle de vie du trajet |
| CME-10 | Tests unitaires |
| CME-11 | Livrables et soutenance |

**Git** : commits en français au format *Conventional Commits* avec la clé Jira, un commit par fichier ou par tâche.

```
feat(reservation): réserver des places sur un segment (CME-51, CME-52, CME-53)
test(trajet): ajouter les tests unitaires des cas nominaux du service (CME-69)
```

Les tests unitaires ont été développés sur des branches `feature/CME-10-*`, fusionnées dans `main`.

## 18. Problèmes fréquents

| Symptôme | Cause et solution |
|---|---|
| `Connection refused` au démarrage | La base n'est pas lancée : `docker compose up -d`, puis vérifier `docker compose ps` |
| Port 5433 ou 8080 déjà utilisé | Arrêter le service en conflit, ou changer le port dans `docker-compose.yml` / `application.yaml` |
| Erreur de checksum Liquibase | Un changeset déjà exécuté a été modifié : `docker compose down -v` puis relancer (base de développement uniquement) |
| `Schema-validation: missing table` | Liquibase n'a pas créé le schéma : vérifier `spring.liquibase.change-log` et les logs de démarrage |
| La recherche ne renvoie rien | Vérifier la date des trajets du seed (relatives au jour d'exécution), le sens des villes et le statut `PUBLIE` |
| 400 « L'heure de départ doit être dans le futur » | Adapter les dates de l'exemple de publication |

## 19. Auteur

**Mhammed Gabigabe**, YouCode (Simplon Maghreb)
Dépôt : https://github.com/MhammedGabigabe/SC_Plateforme_Covoiturage
