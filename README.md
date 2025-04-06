
~~# 🎓 Sorbonne Université – M2 STL – Année 2024–2025
**Groupe :** Mélissa LATEB & Darko DJORDJEVIC  
**Encadrant :** Pietro Ferrara 
**Module :** Techniques d’Analyse Statique (TAS)  
**Projet :** Développement de domaines abstraits dans le framework LiSA

Ce document fournit une explication détaillée des modules **`RoundingInterval`** et **`PairwiseInequalityDomain`**, développés dans le cadre du projet LiSA Tutorial. Ces deux domaines d’interprétation abstraite ont été conçus pour améliorer la précision et la pertinence des analyses statiques en tenant compte à la fois des valeurs numériques arrondies et des relations linéaires entre variables.



---
## RoundingInterval

Le **`RoundingInterval`** est un domaine non relationnel basé sur les intervalles numériques, enrichi d’une logique d’arrondi. Il vise à représenter avec précision les plages de valeurs possibles d’une variable, tout en modélisant les effets des erreurs d’arrondi sur les opérations arithmétiques.

### Caractéristiques principales

- **Plage numérique avec arrondi contrôlé :**  
  Chaque variable est représentée par un intervalle `[a, b]` dont les bornes sont affectées par :
  - une **précision configurable** (nombre de décimales),
  - un **mode d’arrondi** (`UP`, `DOWN`, `HALF_EVEN`, etc.).

- **Règles d’évaluation précises :**  
  Les opérations arithmétiques sont effectuées sur les bornes des intervalles, suivies d’un arrondi appliqué avec précision selon les paramètres définis.

- **Gestion des cas limites :**
  - Division par zéro
  - Infinités positives et négatives
  - Inclusion de zéro dans un intervalle

- **Support des boucles via widening/narrowing :**  
  L’opérateur de widening permet de détecter les tendances de croissance et d'assurer la terminaison de l’analyse.

### Exemple d’utilisation

```scala
def x = 2.4;
def y = x + 3.1;
// Résultat : y ∈ [5.5, 5.5] (précision = 1, mode = HALF_EVEN)

def a = 0.0;
while (a < 10.0) {
    a = a + 0.1;
}
// Résultat après widening : a ∈ [0.0, +∞]
```

### Avantages

- Très utile pour les programmes manipulant des **valeurs flottantes**.
- Permet de **quantifier l’imprécision numérique** due aux erreurs d’arrondi.
- Adapté à l’analyse de logiciels embarqués, bancaires ou scientifiques où la précision est critique.

---

## PairwiseInequalityDomain

Le **`PairwiseInequalityDomain`** est un domaine relationnel basé sur un sous-ensemble restreint mais expressif des contraintes linéaires. Il modélise des relations du type **`a * x + b * y ≤ c`**, où `x` et `y` sont des variables du programme.

### Fondements théoriques

Basé sur l’article :
> Axel Simon, Andy King, Jacob M. Howe — *Two Variables per Linear Inequality as an Abstract Domain* (2002)

Ce domaine offre un compromis puissant entre **expressivité relationnelle** et **efficacité computationnelle**, sans atteindre la complexité des polyèdres convexes.

### Fonctionnalités clés

- **Représentation compacte :**  
  Chaque contrainte est une inégalité linéaire impliquant **au plus deux variables**.

- **Propagation des contraintes :**
  - Lors d’une affectation, le domaine génère ou met à jour les relations linéaires existantes.
  - Lors d’une condition, les hypothèses sont ajoutées et simplifiées pour affiner l’état abstrait.

- **Structure de treillis complète :**
  - **Top** : état sans information (absence de contraintes)
  - **Bottom** : conflit ou ensemble vide (contradiction)
  - **Join (⊔)** : union des contraintes compatibles
  - **Widening** : perte contrôlée de précision pour garantir la convergence

### Exemple d’analyse

```scala
def x = 5;
def y = 3;
if (x <= y + 2) {
    def z = x + 1;
}
// Le domaine infère : x - y ≤ 2, donc z - y ≤ 3
```

### Cas de boucle

```scala
def i = 0;
def j = 10;
while (i < j) {
    i = i + 1;
    j = j - 1;
}
// Après stabilisation : i + j = 10
```

### Difficultés techniques

- **Représentation canonique des contraintes**
- **Détection de redondances et de contradictions**
- **Précision perdue dans les joins multiples**

---

## Comparaison et Complémentarité

| Critère                     | `RoundingInterval`                         | `PairwiseInequalityDomain`                   |
|----------------------------|--------------------------------------------|----------------------------------------------|
| Type de domaine            | Non relationnel                            | Relationnel (binaire)                        |
| Représentation             | Intervalle `[a, b]` avec précision & arrondi | Inégalité linéaire `a * x + b * y ≤ c`       |
| Suivi des relations        | Aucun lien entre variables                 | Relations entre paires de variables          |
| Avantage principal         | Haute précision sur les valeurs            | Détection fine des dépendances linéaires     |
| Limite                    | Aucun lien inter-variable                  | Pas de suivi des valeurs absolues            |

Ces deux domaines sont **hautement complémentaires** :

- `RoundingInterval` fournit une **approximation des valeurs** possibles pour chaque variable.
- `PairwiseInequalityDomain` modélise les **relations entre variables**, ce qui permet une analyse plus fine lorsque les deux sont combinés.

### Produit cartésien

Nous avons débuté l’implémentation d’un **produit cartésien personnalisé** nommé `RoundingInequalityCartesianProduct`, combinant les deux domaines `RoundingInterval` et `PairwiseInequalityDomain` dans un environnement unique.

Ce produit a pour objectif de cumuler :
- la **précision numérique** du domaine `RoundingInterval`,
- avec la **richesse relationnelle** du domaine `PairwiseInequalityDomain`.

Il s’appuie sur l’infrastructure `CartesianProduct` du framework LiSA et respecte l’interface `ValueDomain`.

#### État actuel

> ⚠️ Le produit cartésien est **encore en développement** et **n’est pas pleinement fonctionnel à ce stade**.

- La classe Java est implémentée et compilable.
- Les tests d’analyse se lancent sans erreur, mais **les résultats produits sont incomplets ou absents**.
- Les contraintes issues de `PairwiseInequalityDomain` ne s’affichent pas encore dans les graphes générés.
- Des investigations sont en cours pour comprendre et corriger ce comportement.

#### Prochaines étapes

- Élargir les cas de test `.imp` pour mieux stimuler les deux sous-domaines.
- Valider que les contraintes des deux domaines sont bien conservées et combinées.
- Vérifier l’impact du produit sur la précision et la terminaison des analyses dans LiSA.

Ce travail reste une **preuve de concept prometteuse**, posant les fondations pour des analyses hybrides plus puissantes dans le futur.
