# CR Lundi 19 Novembre

## Réunion préalable :
- Que fait la partie amélioration à la fin ? ne devrait-on pas la déplacer au niveau des fonctionnalitées ?
Pour rappel :
- Il faut qu'une première version des parties respectives soient prêtes pour ce soir
- A la suite de ça, trouver vous un collègue pour faire une relecture croisée : deadline mercredi pour pouvoir rendre jeudi ou vendredi

## Réunion avec Barbara

### Rappel des élements à implémenter
au cas où que certains ne les connaissaient toujours pas ...
- mettre un bouton /entrée menu pour que l'utilisateur demande une optimisation
- Faire une pop-up lorsque l'utilisateur demande une optimisation contenant - Les domaines / le budget / un bouton confirmer
- Créer autant d'onglets d'arbre que de domaines
- afficher un signal lorsque l'optimisation est en cours ("Loading - Plz wait")
- Mettre un maximum d'ergonomie dans tout ça : Choix du budget en plusieurs devises, vérification de l'entrée sous forme numérique, ...
si possible : afficher la sémantique dans les logs DAG Solver - elle doit être stockée sous forme de matrice, et en théorie doit être l'entrée passée a lp_solve

### En ce qui concerne l'update d'arbre
Pour info :
- La sémantique peut-être calculée à partir d'un XML dont les valeurs n'ont pas été encore choisies
- Le calcul de la sémantique est la partie la plus longue de l'optimisation

J'ai mis en annexe des schéma pour les différentes solutions et que faire en cas d'update de l'arbre. Il faudrait en plus rajouter certaines features sur l'UI :
- Bouton pour reporter les valeurs d'un onglet domaine à un autre ? cadenas pour bloquer les changements ? ...
- Gestion du bouton optimize : quand est-il disable/unable ? ...

L'optimisation du calcul de la sémantique est encore floue. Mais quelques approches ont été présentées :
- factoriser le code de la sémantique : celui-ci possède beaucoup de redondances; ainsi factoriser, changer les valeurs dans les noeuds est plus simple (mais est-ce moins couteux en terme de complexité ..?)
- utiliser du calcul en parallèle pour les parties indépendantes