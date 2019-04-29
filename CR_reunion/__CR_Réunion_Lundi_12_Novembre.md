# CR Réunion Lundi 12 Novembre

## quelques remarques de Barbara sur les maquettes :
- conseil : changer l'ordre entre bottom-up et optim
- conseil : le champ pour renseigner le budget global pourrait se situer sur la fenêtre de dialogue (affichée lorsqu'on demande une optimisation)

## piste de réfléxion lancée par Barbara : quand mettre à jour les optims
- a réfléchir : que se passe-t-il si une fois une optimisation lancée, l'utilisateur revient sur l'arbre et le modifie ? Dégrisement du bouton d'opti / pop-up / MAJ auto ? A quelle moment calcule t-on l'optimisation ? 
- Comment parser les arbres de façon à optimiser les calculs, et ainsi racourcir le temps de calcul ? Utilisation d'un genre de 'DOM' qu'on modifierait à la volée pour éviter de reparser tout l'arbre ? Utilisation d'un format se servant des redondances pour accélérer les calculs ?
- Mettre un petit historique des optimisations ? (afin de pouvoir comparer avec les calculs précédents sans les refaire) - pourrait se faire en stockant le chemin opti dans l'XML de l'arbre
- Possibilité de paralléliser les calculs d'arbre ?
- Fonctionnalité front-end : ne pas proposer de recalculer l'optim dans les cas où les modifications n'entraineraient pas de changement sur le résultat.
- Dans tout les cas, il faudra probablement partir sur un cycle en W : d'abord fournir une version simple mais fonctionnelle, et ensuite repartir sur un second développement pour incorporer des fonctionnalités plus avancées.

Note :
- C'est le calcul de la sémantique qui est le plus couteux
- Pour certains scénarios , le moindre changement dans une branche peut conduire à une modif de toute l'optimisation. Impossible donc d'élaguer l'espace de recherche.

## répartition des parties du rapport :
1. Objectif / fonction d'usage (Mathilde)
2. Rappel fonctionnement ADTOOL (JL)
3. Description des nouvelles fonctionnalités à ajouter (Severine - Augustin)
4. Architecture : UML / Use case / Emboitement (Julien - Lucas)
5. Tests / Prise en main (RV & Martin)
6. Conclusion (RV & Martin)