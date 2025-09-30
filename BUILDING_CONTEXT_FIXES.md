# Corrections du filtrage par immeuble (Building Context)

## Problème identifié
Les utilisateurs voyaient les données (channels, messages, votes) de TOUS les immeubles au lieu de voir uniquement les données de l'immeuble sélectionné.

## Corrections apportées

### 1. JwtAuthenticationFilter
**Fichier:** `src/main/java/be/delomid/oneapp/mschat/mschat/config/JwtAuthenticationFilter.java`

**Modification:** Le filtre extrait maintenant le `buildingId`, `userId` et `role` du JWT et les stocke dans les détails de l'authentification.

```java
// Extract building context from JWT
String buildingId = jwtConfig.extractBuildingId(jwt);
String userId = jwtConfig.extractUserId(jwt);
String role = jwtConfig.extractRole(jwt);

// Store building context in authentication details
Map<String, Object> details = new HashMap<>();
details.put("buildingId", buildingId);
details.put("userId", userId);
details.put("role", role);
authToken.setDetails(details);
```

### 2. ChannelService
**Fichier:** `src/main/java/be/delomid/oneapp/mschat/mschat/service/ChannelService.java`

**Modifications:**
- `getCurrentBuildingFromContext()` extrait maintenant le buildingId depuis les détails HTTP
- `getUserChannels()` vérifie que buildingId n'est jamais null
- Ajout de logs détaillés pour le debugging

### 3. MessageService
**Fichier:** `src/main/java/be/delomid/oneapp/mschat/mschat/service/MessageService.java`

**Modifications:**
- `getCurrentBuildingFromContext()` mis à jour
- `validateChannelBuildingAccess()` vérifie que le canal appartient au bâtiment actuel

### 4. VoteService
**Fichier:** `src/main/java/be/delomid/oneapp/mschat/mschat/service/VoteService.java`

**Modifications:**
- Ajout de `validateChannelBuildingAccess()` pour tous les votes
- `createVote()`, `getChannelVotes()`, `getVoteById()`, `submitVote()` vérifient le contexte

### 5. ChannelRepository
**Fichier:** `src/main/java/be/delomid/oneapp/mschat/mschat/repository/ChannelRepository.java`

**Modification:** La requête `findChannelsByUserIdAndBuilding` filtre mieux:
```sql
AND (c.buildingId = :buildingId OR (c.buildingId IS NULL AND c.type = 'PUBLIC'))
```

### 6. SecurityContextUtil (nouveau)
**Fichier:** `src/main/java/be/delomid/oneapp/mschat/mschat/util/SecurityContextUtil.java`

Classe utilitaire pour extraire le contexte de sécurité de manière centralisée.

## Flux d'authentification

1. **Connexion:** L'utilisateur se connecte
2. **Sélection immeuble:** L'utilisateur choisit un immeuble
3. **JWT généré:** Le backend génère un JWT avec `buildingId`
4. **Requêtes filtrées:** Toutes les requêtes sont filtrées par ce `buildingId`
5. **Changement d'immeuble:** Génération d'un nouveau JWT avec le nouveau `buildingId`

## Logs de débogage

Pour diagnostiquer les problèmes, vérifier les logs:
- `Current building from JWT: {buildingId}`
- `Fetching channels for userId: {userId} and buildingId: {buildingId}`
- `Found {count} channels for user {userId} in building {buildingId}`

## Tests recommandés

1. Se connecter avec un utilisateur ayant plusieurs immeubles
2. Vérifier que seuls les channels du premier immeuble sont visibles
3. Changer d'immeuble
4. Vérifier que SEULS les channels du nouvel immeuble sont visibles
5. Vérifier les messages, votes, fichiers suivent le même comportement