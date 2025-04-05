# Rapport TAS

Ce document fournit une brève explication des modules **`RoundingInterval`** et **`PairwiseInequalityDomain`**, qui font partie d'une bibliothèque ou d'un projet relatif aux domaines d'interprétation abstraite.

---

## RoundingInterval

Le **`RoundingInterval`** représente une interface ou une implémentation utilisée dans le cadre de l'interprétation abstraite pour modéliser des plages de valeurs continues.

### Caractéristiques principales :
- **Concept de plage arrondie :** Ce modèle est utilisé pour représenter des intervalles de valeurs numériques, où les limites inférieure et supérieure de l'intervalle peuvent être arrondies à des valeurs proches, selon certaines restrictions imposées.
- **Cas d'utilisation principal :** Utile pour des analyses statiques ou des calculs nécessitant de tenir compte de l'approximation dans les calculs numériques (par exemple, gestion des erreurs d'arrondi induites par des opérations sur des floats ou doubles).
- **Opérations prises en charge :**
    - Comparaisons entre intervalles (chevauchement, inclusion).
    - Ajustement dynamique des limites pour incorporer une précision accrue ou des intervalles élargis.

Ce type d'intervalle simplifie la manipulation et l'analyse de contraintes linéaires ou faiblement délimitées.

---

## PairwiseInequalityDomain

Le **`PairwiseInequalityDomain`** modèle un domaine basé sur des inégalités linéaires entre paires de variables. Ce domaine est conçu spécifiquement pour l’interprétation abstraite dans les analyses statiques.

### Points clés :
- **Inégalités linéaires :** Ce domaine suit un ensemble de contraintes définissant des relations du type :
    - Inférieur à (`<`), supérieur à (`>`), égalité (`=`), etc.
    - Ces relations sont appliquées entre des identifiants (variables) et des constantes.
- **Structure de treillis :**
    - Les entités principales sont **Top** (le cas le plus général, aucune contrainte) et **Bottom** (conflit, contraintes incompatibles).
    - Fournit des opérations pour calculer la **plus petite borne supérieure (LUB)** ou la **plus grande borne inférieure (GLB)** entre deux domaines.
- **Fonctionnalités clés :**
    - **Affectation de valeurs :** Ajoute des contraintes lors de l’attribution de valeurs (identifiants, expressions binaires, constantes).
    - **Assomptions et simplifications de contraintes :** Permet de traiter efficacement des relations complexes pour optimiser l'analyse.
- **Application :** Utilisé pour analyser les relations entre variables dans un programme et détecter des incohérences ou des optimisations potentielles.

---

## Comparaison et Complémentarité

- **`RoundingInterval`** et **`PairwiseInequalityDomain`** sont souvent utilisés conjointement dans des systèmes d’analyse statique.
    - Le premier gère les intervalles approchés pour modéliser les **valeurs possibles d’une variable**, tandis que le second formalise les **relations relatives entre variables**.
- Ensemble, ils permettent de raisonner efficacement sur les propriétés numériques d’un programme tout en tenant compte des contraintes imposées par ses instructions.

---

Pour plus de détails, veuillez consulter la documentation associée ou explorer l'implémentation des classes concernées dans le code source.