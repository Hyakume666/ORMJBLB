# GuideResto ORM

Application Java de gestion de restaurants avec évaluations, utilisant Hibernate/JPA pour la persistance des données.

## Description

GuideResto ORM est une application de gestion de restaurants permettant de consulter, créer, modifier et évaluer des établissements gastronomiques. Le projet implémente une architecture en couches avec une couche de persistance basée sur Hibernate/JPA et une couche service contenant la logique métier.

## Fonctionnalités principales

- Gestion complète des restaurants (CRUD)
- Recherche de restaurants par nom, ville ou type gastronomique
- Système d'évaluation double :
    - Évaluation simple (like/dislike)
    - Évaluation complète avec commentaire et notes détaillées par critère
- Gestion des villes et types gastronomiques
- Gestion des critères d'évaluation
- Validation métier et gestion des transactions
- Gestion de la concurrence avec verrouillage optimiste

## Technologies utilisées

| Technologie | Version | Description |
|-------------|---------|-------------|
| Java | 21 | Langage de programmation |
| Hibernate | 7.0.0 | ORM (Object-Relational Mapping) |
| JPA | 2.0 | Java Persistence API |
| Oracle Database | - | Base de données relationnelle |
| Maven | - | Gestion des dépendances et build |
| Log4j2 | 2.24.3 | Framework de logging |

## Architecture du projet

### Structure des packages

```
ch.hearc.ig.guideresto/
├── business/
│   ├── City.java
│   ├── EvaluationCriteria.java
│   ├── Restaurant.java
│   ├── RestaurantType.java
│   ├── Localisation.java
│   ├── Grade.java
│   ├── Evaluation.java (classe abstraite)
│   ├── BasicEvaluation.java
│   ├── CompleteEvaluation.java
│   └── IBusinessObject.java
├── persistence/
│   ├── dao/
│   │   ├── IDao.java
│   │   ├── AbstractDao.java
│   │   ├── CityDao.java
│   │   ├── RestaurantTypeDao.java
│   │   ├── EvaluationCriteriaDao.java
│   │   └── RestaurantDao.java
│   └── jpa/
│       ├── JpaUtils.java
│       └── BooleanConverter.java
├── service/
│   ├── CityService.java
│   ├── RestaurantTypeService.java
│   ├── EvaluationCriteriaService.java
│   ├── RestaurantService.java
│   └── EvaluationService.java
└── presentation/
    ├── Application.java
    ├── DaoTest.java
    ├── ServiceTest.java
    ├── ServiceTransactionTest.java
    └── VerrouillageTest.java
```

### Couches applicatives

#### Couche Business (Entités)

- Entités JPA annotées représentant le modèle de données
- Relations entre entités (OneToMany, ManyToOne, Embedded)
- Stratégie d'héritage TABLE_PER_CLASS pour les évaluations
- Classe Localisation embarquée pour gérer l'adresse des restaurants
- Verrouillage optimiste via @Version pour la gestion de concurrence

#### Couche Persistence (DAO)

- Interface IDao définissant les opérations CRUD de base
- AbstractDao implémentant les opérations communes
- DAO spécifiques pour chaque entité avec Named Queries
- JpaUtils pour la gestion de l'EntityManager et des transactions
- BooleanConverter pour la conversion des booléens en base Oracle (T/F)

#### Couche Service

- Logique métier centralisée dans des services dédiés
- Validations et règles métier (unicité, cohérence des données)
- Gestion des transactions atomiques
- Séparation des responsabilités par domaine fonctionnel (SOLID)

#### Couche Présentation

- Application.java : Interface console interactive
- Classes de test démontrant les fonctionnalités des DAO et services
- Affichage formaté des données et gestion des interactions utilisateur

## Patterns de conception utilisés

### Stratégie d'héritage (TABLE_PER_CLASS)

- Utilisé pour la hiérarchie Evaluation/BasicEvaluation/CompleteEvaluation
- Chaque classe concrète possède sa propre table en base de données
- BasicEvaluation → table LIKES
- CompleteEvaluation → table COMMENTAIRES

### Service Layer Pattern

- Encapsulation de la logique métier dans des services dédiés
- CityService, RestaurantTypeService, EvaluationCriteriaService pour les entités de référence
- RestaurantService pour la gestion des restaurants
- EvaluationService pour les likes/dislikes et évaluations complètes
- Validation centralisée et gestion cohérente des transactions

### Data Access Object (DAO)

- Interface IDao générique définissant le contrat CRUD
- AbstractDao implémentant les opérations communes via génériques
- DAO spécifiques héritant de AbstractDao avec méthodes métier additionnelles

### Named Queries

- Requêtes JPQL nommées définies au niveau des entités
- Amélioration de la maintenabilité et de la performance
- Validation des requêtes au démarrage de l'application

### Embedded Objects

- Classe Localisation embarquée dans Restaurant
- Regroupement logique de l'adresse (rue + ville)

### Verrouillage Optimiste

- Annotation @Version sur l'entité Restaurant
- Détection automatique des conflits de modification concurrente
- Gestion propre des OptimisticLockException dans les services

## Structure de la base de données

### Tables principales

| Table | Description |
|-------|-------------|
| RESTAURANTS | Informations sur les restaurants |
| VILLES | Liste des villes |
| TYPES_GASTRONOMIQUES | Types de cuisine |
| CRITERES_EVALUATION | Critères d'évaluation standardisés |
| LIKES | Évaluations simples (like/dislike) |
| COMMENTAIRES | Évaluations détaillées avec commentaires |
| NOTES | Notes par critère pour les évaluations complètes |

### Relations

```
VILLES (1) ──────────< (N) RESTAURANTS
TYPES_GASTRONOMIQUES (1) ──< (N) RESTAURANTS

RESTAURANTS (1) ──< (N) LIKES
RESTAURANTS (1) ──< (N) COMMENTAIRES

COMMENTAIRES (1) ──────< (N) NOTES
CRITERES_EVALUATION (1) ──< (N) NOTES
```

## Services disponibles

### CityService

- Gestion des villes (création, modification, suppression)
- Recherche par code postal ou nom de ville
- Validation de l'unicité des codes postaux
- Statistiques sur les villes (avec/sans restaurants)

### RestaurantTypeService

- Gestion des types gastronomiques (CRUD)
- Recherche par libellé
- Validation de l'unicité des libellés
- Statistiques (type le plus/moins populaire)

### EvaluationCriteriaService

- Gestion des critères d'évaluation (Service, Cuisine, Cadre)
- Validation de l'unicité des noms de critères
- Initialisation automatique des critères standards
- Validation de l'existence des critères

### RestaurantService

- Gestion complète des restaurants (CRUD)
- Recherches multicritères (nom, ville, type)
- Validation d'unicité (nom + ville)
- Création de restaurant avec nouvelle ville en transaction unique
- Modification de l'adresse et du type
- Gestion des conflits de concurrence (verrouillage optimiste)

### EvaluationService

- Création d'évaluations simples (likes/dislikes)
- Création d'évaluations complètes avec notes multiples
- Calcul de statistiques (moyennes par critère, moyenne générale)
- Comptage des évaluations par type
- Transactions atomiques pour les évaluations complètes

## Gestion des transactions

Le projet utilise `JpaUtils.inTransaction()` pour garantir l'atomicité des opérations. Les transactions principales incluent :

- Création de restaurant avec nouvelle ville (transaction unique)
- Création d'évaluation complète avec toutes ses notes (transaction unique)
- Si une opération échoue, toute la transaction est annulée (rollback)

## Validations métier

Le projet implémente plusieurs niveaux de validation :

### Au niveau des entités

- Annotations JPA (@Column avec contraintes)
- Annotations de validation (@Min, @Max sur Grade)
- Contraintes d'unicité en base de données

### Au niveau des services

- Validation d'unicité (codes postaux, noms de critères, libellés de types)
- Validation de l'existence des entités référencées
- Validation des plages de valeurs (notes entre 1 et 5)
- Validation de l'intégrité référentielle avant suppression

### Gestion des erreurs

- Logging détaillé des erreurs avec Log4j2
- Messages d'erreur explicites retournés aux appelants
- Rollback automatique en cas d'erreur dans une transaction
- Gestion des OptimisticLockException pour les conflits de concurrence

## Classes de test

Le projet inclut quatre classes de test démontrant les fonctionnalités :

### DaoTest

- Tests des opérations CRUD de base
- Tests des Named Queries
- Tests des recherches par critères

### ServiceTest

- Tests de la couche service
- Validation des règles métier
- Tests des statistiques et calculs de moyennes

### ServiceTransactionTest

- Démonstration des transactions atomiques
- Tests de rollback en cas d'erreur
- Validation de l'intégrité des données

### VerrouillageTest

- Test du verrouillage optimiste
- Simulation de conflit de modification concurrente
- Validation de la détection des conflits

## Configuration

### Prérequis

- JDK 21
- Maven 3.x
- Accès à une base de données Oracle

### Installation

1. Cloner le repository
2. Copier `src/main/resources/hibernate.properties.template` vers `src/main/resources/hibernate.properties`
3. Configurer les paramètres de connexion à la base de données
4. Exécuter le script `GuideResto_CREATE_TABLES.sql` sur la base de données
5. Compiler avec Maven : `mvn clean compile`

### Exécution

```bash
mvn exec:java -Dexec.mainClass="ch.hearc.ig.guideresto.presentation.Application"
```

## Utilisation

L'application propose un menu interactif permettant de :

1. Consulter tous les restaurants
2. Rechercher des restaurants par différents critères
3. Créer de nouveaux restaurants
4. Évaluer des restaurants (simple ou complet)
5. Modifier les informations d'un restaurant
6. Supprimer des restaurants

Les évaluations simples permettent un like ou dislike rapide, tandis que les évaluations complètes offrent la possibilité d'ajouter un commentaire textuel et des notes détaillées selon différents critères (Service, Cuisine, Cadre).

## Auteurs

- Code de base : Cédric Baudet, Alain Matile, Arnaud Geiser
- Migration ORM et architecture service : Jérémie Bressoud & Loïc Barthoulot

---

**Dernière mise à jour** : Janvier 2025  
**Cours** : Intégration de la couche logicielle - HE-Arc