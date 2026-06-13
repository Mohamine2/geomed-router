# Projet Génie Logiciel (PGL) - Système de Répartition et de Routage Médical
## Projet de fin d'année - Ing1 Cy Tech

## Description
Ce projet est une application Java d'aide à la décision avec interface graphique JavaFX. Elle permet de coupler la triangulation de Delaunay et les diagrammes de Voronoï avec une implémentation Dijkstra pour optimiser la répartition et le routage de victimes d'incidents vers des structures hospitalières adaptées.

## Fonctionnalités Principales
* **Cartographie et Réseaux Routiers** : Chargement de cartes depuis OpenStreetMap (OSM) ou via des fichiers binaires optimisés (`RoadNetwork`, `Edge`).
* **Moteurs Algorithmiques Avancés** : 
  * Calcul de trajets optimisés via l'algorithme de Dijkstra (`RoutingEngine`).
  * Triangulation de Delaunay (`DelaunayEngine`) et Diagrammes de Voronoï (`VoronoiEngine`) pour modéliser les zones de couverture des hôpitaux.
  * Moteur de répartition automatique (`DispatchEngine`) selon les spécialités médicales et les capacités.
* **Importation de Données (I/O)** : Chargement facile d'hôpitaux et d'incidents via des fichiers CSV (`CsvIncidentImporter`, `CsvSiteImporter`).
* **Interface Utilisateur Interactive** : Visualisation dynamique de la carte et panneau de contrôle latéral (`MapController`, `SidebarController`).
* **Explicabilité et Conformité RGPD** : Traçabilité des décisions de répartition (`DispatchDecision`) et génération de rapports conformes au RGPD (`GDPRReportingService`).
* **Sécurité** : Contrôle des accès selon les rôles des utilisateurs (`UserRole`, `SecurityContext`).

## Architecture du Projet

Le code source est organisé selon les packages suivants :
* `pgl.app.model` : Entités du domaine (ex: `Hospital`, `VictimIncident`, `VoronoiCell`, `MedicalSpecialty`).
* `pgl.app.algo` : Logique algorithmique complexe et géométrie.
* `pgl.app.controller` : Contrôleurs pour l'interface JavaFX.
* `pgl.app.io` : Gestion des entrées/sorties, sérialisation de la carte et parsing CSV.
* `pgl.app.explainability` : Modules de transparence algorithmique et RGPD.
* `pgl.app.security` : Gestion des droits et des sessions.
* `pgl.app.test` : Outils de génération de données de simulation.

## Prérequis et Lancement

### Prérequis
- **Java JDK** : Version 17 ou supérieure.
- **Maven** : Installé et configuré dans le PATH.

### Installation et Exécution

1. Clonez le dépôt sur votre machine locale :
   ```bash
   git clone <URL_DU_DEPOT>
   cd Projet-Genie-Logiciel
   ```
2. Exécuter l'application JavaFX
    ```bash
    javafx:run
    ```
3. Générer Javadoc
   ```bash
    mvn javadoc:javadoc
    ```
