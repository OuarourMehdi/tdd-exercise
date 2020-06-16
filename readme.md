# Merge baskets feature
Les utilisateurs non authentifiés peuvent faire des courses sur le site. Les offres ajoutées au panier sont stockés 
dans un panier identifié par l'id de session web.

Au passage en caisse, l'utilisateur anonyme est invité à se connecter. 
Si le système détecte que l'utilisateur authentifié dispose d'un ancien panier (identifié par l'id utilisateur), 
une pop-in invite l’utilisateur à choisir entre garder son nouveau panier (panier créé en mode anonyme) ou son son 
ancien panier connecté, une case à cocher est présente permettant d’activer la fusion des offres des deux 
paniers (les offres similaires se verront appliquer la quantité du panier gardé).

Les paniers anonymes doivent être supprimés du système après l’opération de fusion.

Ecrire le service permettant de réaliser cette opération de fusion et de retourner le panier résultant.