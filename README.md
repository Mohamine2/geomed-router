# Projet de fin d'année - Ing1 Cy Tech

Ce projet consiste en la création d'une application interactive développée en **JavaFX** dédiée à l'optimisation spatiale des services d'urgence. L'objectif est de modéliser la couverture territoriale de sites critiques (hôpitaux) pour minimiser les temps d'intervention via l'analyse de diagrammes de Voronoï et de triangulations de Delaunay.

L'application est construite sans l'utilisation de bibliothèques mathématiques tierces, afin de garantir une maîtrise totale sur la logique algorithmique.

## Statut Actuel : MVP (Minimum Viable Product)

La version actuelle (MVP) se concentre sur les fondations de l'architecture et du moteur géométrique nécessaire à la prise de décision spatiale.

**Fonctionnalités (MVP):**
- [ ] Ajout manuel de sites de secours (Sites).
- [ ] Ajout manuel de zones d'intervention (points utilisateurs).
- [ ] Calcul de la Triangulation de Delaunay (maillage du territoire).
- [ ] Algorithme de recherche du plus proche voisin (liaison sinistre -> centre de secours le plus proche).
- [ ] Affichage terminal

**Fonctionnalités à venir (Projet complet) :**
- [ ] Interface JavaFX basée sur un modèle MVC hiérarchique.
- [ ] Calcul du diagramme de Voronoï par dualité (délimitation précise des zones de couverture).
- [ ] Drag & Drop des entités avec rafraîchissement en temps réel (simulation de déplacement des moyens).
- [ ] Importation (CSV) et exportation binaire de la cartographie.
- [ ] Panneau d'analyse statistique : évaluation des surfaces de couverture, détection des zones blanches et calcul du temps moyen d'intervention.

## Architecture Technique

Le projet utilise **Maven** pour la gestion des dépendances et du cycle de vie.
- **Modèle :** Gestion des entités spatiales (`Site`, `UserPoint`, `Triangle`) avec propriétés observables.
- **Vue :** Fichiers FXML séparés (`main.fxml`, `map.fxml`, `sidebar.fxml`) pour une collaboration sans conflit.
- **Contrôleur :** Contrôleurs dédiés injectés via JavaFX pour séparer la logique de rendu et d'interaction.
- **Algorithmique :** Implémentation de l'algorithme de Bowyer-Watson pour le maillage du territoire.

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
