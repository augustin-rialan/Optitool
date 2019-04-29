# Utiliser Git en SSH

Git peut être utiliser en HTTP et en SSH. L'avantage du deuxième est qu'il est possible de mettre en place des clés SSH, et de ne pas avoir à retaper son user/pw à chaque push

## configurer git en ssh

Il suffit de modifier le fichier .git/config du projet et de remplacer dans [remote "origin"] l'url par ssh://git@gitlab.insa-rennes.fr:16022/jsoule/OptiTool.git

## Mettre en place une paire de clé ssh

Sur gitlab, aller sur settings > ssh keys > add an SSH key si aucune n'est déjà en place

# Charte sur comment utiliser Git

1. Petit récapitulatif de l'organisation des branches:
    - **master** : cette branche correspond à la version **stable** et **actuelle** du projet. Elle doit contenir exclusivement les fonctionnalités de cette version. (Donc par exemple si l'on developpe la v2 et que la dernière version terminée est la v1, toutes les fonctionnalités propres à la v2 doivent être à part).
    - **v0/v1/...** : cette branche correspond à la version **en cours de dev** mais **stable** (c'est à dire testée et qui fonctionne parfaitement). Une fois que toutes les fonctionnalités nécessaire ont été ajoutées, la branche sera fusionnée avec master
    - Ainsi, tout developpement en cours qui n'est pas pleinement fonctionnel doit être sur une **branche a part**
    - Une bonne pratique : créer une nouvelle branche pour le developement de chaque nouvelle fonctionnalité. Il vaut mieux créer des branches pour rien (il suffit de merge) que l'inverse (il est très pénible d'extraire une fonctionnalité d'une branche).

2. **Comment merge** sur master et les branches de version :
    - ces branches doivent toujours rester stables, donc il ne faut pas que votre merge induise des conflits. Pour éviter cela, on suivra le workflow suivant :
        1. Assurez-vous d'être sur la bonne branche
        2. git pull origin < la branche vers laquelle je souhaites merge >
        3. (depuis la branche à merge) : git merge < la branche vers laquelle je souhaites merge >
        4. je résous les conflits
        5. je publie une merge request de ma branche vers la branche cible
        6. une fois fait, je supprime ma branche si besoin