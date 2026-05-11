package github.dluckycompany.clawkins.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.audio.DialogueSoundManager;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.PlayerAnimation;
import github.dluckycompany.clawkins.component.PlayerProfile;
import github.dluckycompany.clawkins.component.Tiled;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.encounter.EncounterZone;
import github.dluckycompany.clawkins.input.InputConventions;

public class InteractionSystem extends EntitySystem {
    private static final float INTERACT_RANGE = 1.2f;
    private static final float PLAYER_INTERACT_ORIGIN_Y_FACTOR = 0.22f;
    private static final float FACING_DOT_THRESHOLD = 0.6f;
    private static final float TYPEWRITER_CHARS_PER_SECOND = 45f;
    private static final float TRIPPABLE_REARM_DELAY_SECONDS = 0.25f;
    private static final String DIALOGUE_FLOW_KEY = "DialogueFlow";
    private static final String INTERACTIONS_KEY = "Interactions";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)}");

    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> interactibles;
    private ImmutableArray<Entity> encounters;
    private List<DialogueEntry> dialogueFlow = List.of();
    private int dialogueFlowIndex = 0;
    private String dialogueName = "";
    private String dialogueText = "";
    private String dialogueFullText = "";
    private Interactible.DialoguePosition dialoguePosition = Interactible.DialoguePosition.BOTTOM;
    private boolean dialogueVisible = false;
    private int dialogueVisibleChars = 0;
    private float typewriterCarry = 0f;
    private final DialogueSoundManager dialogueSoundManager = new DialogueSoundManager();
    
    // Merchant detection
    private Runnable onMerchantInteraction = () -> {};
    private boolean isMerchantMode = false;
    private Supplier<List<Clawkin>> clawkinPartySupplier = List::of;
    private final Map<String, Predicate<SpecialInteractionContext>> preDialogueCheckByObjectId = new HashMap<>();
    private final Map<String, Predicate<SpecialInteractionContext>> preDialogueCheckByGroupId = new HashMap<>();
    private final Map<String, Consumer<SpecialInteractionContext>> specialInteractionByObjectId = new HashMap<>();
    private final Map<String, Consumer<SpecialInteractionContext>> specialInteractionByGroupId = new HashMap<>();
    /** ObjectIds that run special handlers immediately with no dialogue box (normalized keys). */
    private final Set<String> skipDialogueObjectIds = new HashSet<>();
    private final Map<String, Integer> persistedInteractionCountsByObjectId = new HashMap<>();
    private Consumer<SpecialInteractionContext> pendingSpecialInteraction;
    private SpecialInteractionContext pendingSpecialInteractionContext;
    private final Set<Entity> activeTrippableTargets = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, Float> trippableRearmTimers = new IdentityHashMap<>();

    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(Player.class, Transform.class, PlayerAnimation.class).get());
        interactibles = engine.getEntitiesFor(Family.all(Interactible.class, Transform.class).get());
        encounters = engine.getEntitiesFor(Family.all(EncounterZone.class, Transform.class).get());
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
        activeTrippableTargets.clear();
        trippableRearmTimers.clear();
        dialogueSoundManager.dispose();
    }

    @Override
    public void update(float deltaTime) {
        if (dialogueVisible) {
            tickTypewriter(deltaTime);
            if (!isInteractionPressed()) {
                return;
            }
            if (!isDialogueFullyRevealed()) {
                revealDialogueImmediately();
                return;
            }
            if (showNextDialogueLine()) {
                return;
            }
            hideDialogue();
            return;
        }

        if (processTrippableInteraction(deltaTime)) {
            return;
        }

        if (!isInteractionPressed()) {
            return;
        }

        Entity target = findFacingTarget();
        if (target == null) {
            return;
        }

        Entity playerEntity = players.first();
        startInteraction(playerEntity, target);
    }

    public boolean isDialogueVisible() {
        return dialogueVisible;
    }

    public String getDialogueName() {
        return dialogueName;
    }

    public String getDialogueText() {
        return dialogueText;
    }

    public String getDialogueFullText() {
        return dialogueFullText;
    }

    public Interactible.DialoguePosition getDialoguePosition() {
        return dialoguePosition;
    }

    public void setOnMerchantInteraction(Runnable callback) {
        this.onMerchantInteraction = callback != null ? callback : () -> {};
    }

    public boolean isMerchantMode() {
        return isMerchantMode;
    }

    public void setClawkinPartySupplier(Supplier<List<Clawkin>> supplier) {
        this.clawkinPartySupplier = supplier != null ? supplier : List::of;
    }

    public void closeMerchant() {
        isMerchantMode = false;
    }

    public Map<String, Integer> snapshotPersistedInteractionCountsByObjectId() {
        return Map.copyOf(persistedInteractionCountsByObjectId);
    }

    public void loadPersistedInteractionCountsByObjectId(Map<String, Integer> countsByObjectId) {
        persistedInteractionCountsByObjectId.clear();
        if (countsByObjectId == null || countsByObjectId.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : countsByObjectId.entrySet()) {
            String normalizedObjectId = normalizeObjectId(entry.getKey());
            if (normalizedObjectId.isEmpty()) {
                continue;
            }
            int count = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (count > 0) {
                persistedInteractionCountsByObjectId.put(normalizedObjectId, count);
            }
        }
    }

    public void registerSpecialInteraction(String objectId, Consumer<SpecialInteractionContext> specialInteraction) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty() || specialInteraction == null) {
            return;
        }
        specialInteractionByObjectId.put(normalizedObjectId, specialInteraction);
    }

    /**
     * For this ObjectId, skip the dialogue overlay and run the registered special interaction immediately.
     */
    public void registerSkipDialogueForObjectId(String objectId) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty()) {
            return;
        }
        skipDialogueObjectIds.add(normalizedObjectId);
    }

    public void registerPreDialogueCheck(String objectId, Predicate<SpecialInteractionContext> preDialogueCheck) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty() || preDialogueCheck == null) {
            return;
        }
        preDialogueCheckByObjectId.put(normalizedObjectId, preDialogueCheck);
    }

    public void registerSpecialInteractionByGroupId(String groupId, Consumer<SpecialInteractionContext> specialInteraction) {
        String normalizedGroupId = normalizeGroupId(groupId);
        if (normalizedGroupId.isEmpty() || specialInteraction == null) {
            return;
        }
        specialInteractionByGroupId.put(normalizedGroupId, specialInteraction);
    }

    public void registerPreDialogueCheckByGroupId(String groupId, Predicate<SpecialInteractionContext> preDialogueCheck) {
        String normalizedGroupId = normalizeGroupId(groupId);
        if (normalizedGroupId.isEmpty() || preDialogueCheck == null) {
            return;
        }
        preDialogueCheckByGroupId.put(normalizedGroupId, preDialogueCheck);
    }

    public void unregisterPreDialogueCheck(String objectId) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty()) {
            return;
        }
        preDialogueCheckByObjectId.remove(normalizedObjectId);
    }

    public void unregisterPreDialogueCheckByGroupId(String groupId) {
        String normalizedGroupId = normalizeGroupId(groupId);
        if (normalizedGroupId.isEmpty()) {
            return;
        }
        preDialogueCheckByGroupId.remove(normalizedGroupId);
    }

    public void unregisterSpecialInteraction(String objectId) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty()) {
            return;
        }
        specialInteractionByObjectId.remove(normalizedObjectId);
    }

    public void unregisterSpecialInteractionByGroupId(String groupId) {
        String normalizedGroupId = normalizeGroupId(groupId);
        if (normalizedGroupId.isEmpty()) {
            return;
        }
        specialInteractionByGroupId.remove(normalizedGroupId);
    }

    public void clearSpecialInteractions() {
        specialInteractionByObjectId.clear();
        specialInteractionByGroupId.clear();
    }

    public void clearPreDialogueChecks() {
        preDialogueCheckByObjectId.clear();
        preDialogueCheckByGroupId.clear();
    }

    public void rearmTrippableByObjectId(String objectId) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty() || interactibles == null || interactibles.size() == 0) {
            return;
        }
        for (Entity entity : interactibles) {
            Interactible interactible = Interactible.MAPPER.get(entity);
            if (interactible == null || !interactible.isTrippable()) {
                continue;
            }
            if (!normalizedObjectId.equals(normalizeObjectId(interactible.getObjectId()))) {
                continue;
            }
            activeTrippableTargets.remove(entity);
            trippableRearmTimers.remove(entity);
        }
    }

    public void triggerInteractionByObjectId(String objectId) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty()
                || players == null
                || players.size() == 0
                || interactibles == null
                || interactibles.size() == 0) {
            return;
        }

        Entity playerEntity = players.first();
        for (Entity entity : interactibles) {
            Interactible interactible = Interactible.MAPPER.get(entity);
            if (interactible == null) {
                continue;
            }
            if (!normalizedObjectId.equals(normalizeObjectId(interactible.getObjectId()))) {
                continue;
            }

            if (interactible.isTrippable()) {
                activeTrippableTargets.add(entity);
                trippableRearmTimers.remove(entity);
            }
            startInteraction(playerEntity, entity);
            return;
        }
    }

    private Entity findFacingTarget() {
        if (players == null || players.size() == 0 || interactibles == null || interactibles.size() == 0) {
            return null;
        }

        Entity playerEntity = players.first();
        Transform playerTransform = Transform.MAPPER.get(playerEntity);
        PlayerAnimation playerAnimation = PlayerAnimation.MAPPER.get(playerEntity);
        Vector2 playerCenter = interactionOriginOf(playerTransform);
        Vector2 facing = directionVector(playerAnimation.getDirection());

        Entity bestTarget = null;
        float bestDistance = Float.MAX_VALUE;

        for (Entity entity : interactibles) {
            Interactible interactible = Interactible.MAPPER.get(entity);
            if (interactible != null && interactible.isTrippable() && activeTrippableTargets.contains(entity)) {
                continue;
            }
            Transform t = Transform.MAPPER.get(entity);
            if (t == null) {
                continue;
            }
            boolean insideInfluenceArea = isWithinInfluenceArea(entity, playerCenter);
            Vector2 targetPoint = insideInfluenceArea
                    ? new Vector2(playerCenter)
                    : closestPointOnInfluenceArea(entity, playerCenter);
            Vector2 toTarget = targetPoint.sub(playerCenter);
            float distance = toTarget.len();
            if (!insideInfluenceArea && distance > INTERACT_RANGE) {
                continue;
            }

            if (!insideInfluenceArea && distance == 0f) {
                toTarget = centerOf(t).sub(playerCenter);
                distance = toTarget.len();
                if (distance == 0f) {
                    continue;
                }
            }

            if (!insideInfluenceArea) {
                float dot = toTarget.nor().dot(facing);
                if (dot < FACING_DOT_THRESHOLD) {
                    continue;
                }
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                bestTarget = entity;
            }
        }

        return bestTarget;
    }

    private static Vector2 centerOf(Transform transform) {
        return new Vector2(
                transform.getPosition().x + transform.getSize().x * 0.5f,
                transform.getPosition().y + transform.getSize().y * 0.5f
        );
    }

    private static Vector2 interactionOriginOf(Transform transform) {
        return new Vector2(
                transform.getPosition().x + transform.getSize().x * 0.5f,
                transform.getPosition().y + transform.getSize().y * PLAYER_INTERACT_ORIGIN_Y_FACTOR
        );
    }

    private static Vector2 closestPointOnRect(Vector2 point, Rectangle rect) {
        float x = Math.max(rect.x, Math.min(point.x, rect.x + rect.width));
        float y = Math.max(rect.y, Math.min(point.y, rect.y + rect.height));
        return new Vector2(x, y);
    }

    private static Vector2 closestPointOnInfluenceArea(Entity entity, Vector2 point) {
        return closestPointOnRect(point, influenceBounds(entity));
    }

    private static boolean isWithinInfluenceArea(Entity entity, Vector2 point) {
        Tiled tiled = Tiled.MAPPER.get(entity);
        if (tiled != null && tiled.getMapObjectRef() != null) {
            float tiledX = point.x / Main.UNIT_SCALE;
            float tiledY = point.y / Main.UNIT_SCALE;
            if (containsPoint(tiled.getMapObjectRef(), tiledX, tiledY)) {
                return true;
            }
        }
        return influenceBounds(entity).contains(point);
    }

    private static Rectangle influenceBounds(Entity entity) {
        Transform transform = Transform.MAPPER.get(entity);
        Rectangle fallback = new Rectangle(
                transform.getPosition().x,
                transform.getPosition().y,
                transform.getSize().x,
                transform.getSize().y
        );
        Tiled tiled = Tiled.MAPPER.get(entity);
        if (tiled == null || tiled.getMapObjectRef() == null) {
            return fallback;
        }

        Rectangle tiledBounds = mapObjectBounds(tiled.getMapObjectRef());
        if (tiledBounds == null) {
            return fallback;
        }
        return new Rectangle(
                tiledBounds.x * Main.UNIT_SCALE,
                tiledBounds.y * Main.UNIT_SCALE,
                tiledBounds.width * Main.UNIT_SCALE,
                tiledBounds.height * Main.UNIT_SCALE
        );
    }

    private static Rectangle mapObjectBounds(MapObject mapObject) {
        if (mapObject instanceof RectangleMapObject rectangleMapObject) {
            return rectangleMapObject.getRectangle();
        }
        if (mapObject instanceof PolygonMapObject polygonMapObject) {
            return polygonMapObject.getPolygon().getBoundingRectangle();
        }
        if (mapObject instanceof CircleMapObject circleMapObject) {
            float radius = circleMapObject.getCircle().radius;
            return new Rectangle(
                    circleMapObject.getCircle().x - radius,
                    circleMapObject.getCircle().y - radius,
                    radius * 2f,
                    radius * 2f
            );
        }
        if (mapObject instanceof EllipseMapObject ellipseMapObject) {
            return new Rectangle(
                    ellipseMapObject.getEllipse().x,
                    ellipseMapObject.getEllipse().y,
                    ellipseMapObject.getEllipse().width,
                    ellipseMapObject.getEllipse().height
            );
        }
        return null;
    }

    private static boolean containsPoint(MapObject mapObject, float x, float y) {
        if (mapObject instanceof PolygonMapObject polygonMapObject) {
            return polygonMapObject.getPolygon().contains(x, y);
        }
        if (mapObject instanceof RectangleMapObject rectangleMapObject) {
            return rectangleMapObject.getRectangle().contains(x, y);
        }
        if (mapObject instanceof CircleMapObject circleMapObject) {
            return circleMapObject.getCircle().contains(x, y);
        }
        if (mapObject instanceof EllipseMapObject ellipseMapObject) {
            return ellipseMapObject.getEllipse().contains(x, y);
        }
        return false;
    }

    private static Vector2 directionVector(PlayerAnimation.Direction direction) {
        return switch (direction) {
            case NORTH -> new Vector2(0f, 1f);
            case SOUTH -> new Vector2(0f, -1f);
            case EAST -> new Vector2(1f, 0f);
            case WEST -> new Vector2(-1f, 0f);
        };
    }

    private static boolean isInteractionPressed() {
        return InputConventions.isInteractJustPressed();
    }

    private boolean processTrippableInteraction(float deltaTime) {
        if (players == null || players.size() == 0 || interactibles == null || interactibles.size() == 0) {
            activeTrippableTargets.clear();
            trippableRearmTimers.clear();
            return false;
        }

        Entity playerEntity = players.first();
        Transform playerTransform = Transform.MAPPER.get(playerEntity);
        if (playerTransform == null) {
            activeTrippableTargets.clear();
            trippableRearmTimers.clear();
            return false;
        }

        Vector2 playerPoint = interactionOriginOf(playerTransform);
        Set<Entity> currentlyInsideTrippableTargets = Collections.newSetFromMap(new IdentityHashMap<>());
        Entity enteredTarget = null;

        for (Entity entity : interactibles) {
            Interactible interactible = Interactible.MAPPER.get(entity);
            Transform targetTransform = Transform.MAPPER.get(entity);
            if (interactible == null || targetTransform == null || !interactible.isTrippable()) {
                continue;
            }

            if (!isWithinInfluenceArea(entity, playerPoint)) {
                continue;
            }

            currentlyInsideTrippableTargets.add(entity);
            if (!activeTrippableTargets.contains(entity) && enteredTarget == null) {
                enteredTarget = entity;
            }
        }

        if (!activeTrippableTargets.isEmpty()) {
            List<Entity> rearmedTargets = new ArrayList<>();
            for (Entity activeTarget : activeTrippableTargets) {
                if (currentlyInsideTrippableTargets.contains(activeTarget)) {
                    trippableRearmTimers.remove(activeTarget);
                    continue;
                }

                float elapsedOutsideSeconds = trippableRearmTimers.getOrDefault(activeTarget, 0f)
                        + Math.max(0f, deltaTime);
                if (elapsedOutsideSeconds >= TRIPPABLE_REARM_DELAY_SECONDS) {
                    rearmedTargets.add(activeTarget);
                } else {
                    trippableRearmTimers.put(activeTarget, elapsedOutsideSeconds);
                }
            }

            for (Entity rearmedTarget : rearmedTargets) {
                activeTrippableTargets.remove(rearmedTarget);
                trippableRearmTimers.remove(rearmedTarget);
            }
        }

        if (enteredTarget == null) {
            return false;
        }

        activeTrippableTargets.add(enteredTarget);
        trippableRearmTimers.remove(enteredTarget);
        startInteraction(playerEntity, enteredTarget);
        return true;
    }

    private void startInteraction(Entity playerEntity, Entity targetEntity) {
        if (playerEntity == null || targetEntity == null) {
            return;
        }

        Interactible interactible = Interactible.MAPPER.get(targetEntity);
        if (interactible == null) {
            return;
        }
        restorePersistedInteractionCount(interactible);

        String playerName = resolvePlayerName(playerEntity);
        int nextInteractionCount = Math.max(1, interactible.getInteractionCount() + 1);

        Predicate<SpecialInteractionContext> preDialogueCheck = resolvePreDialogueCheck(interactible);
        Consumer<SpecialInteractionContext> specialInteraction = resolveSpecialInteraction(interactible);
        SpecialInteractionContext specialContext = new SpecialInteractionContext(
                playerEntity,
                targetEntity,
                safeText(interactible.getObjectId(), ""),
                safeText(interactible.getObjectName(), "Object"),
                nextInteractionCount
        );
        if (!runPreDialogueCheck(preDialogueCheck, specialContext)) {
            return;
        }
        interactible.incrementInteractionCount();
        persistInteractionCount(interactible);

        String dialogueSource = resolveDialogueSource(interactible);
        List<DialogueEntry> resolvedFlow = resolveDialogueFlow(interactible, playerName, dialogueSource);
        if (skipDialogueObjectIds.contains(normalizeObjectId(interactible.getObjectId()))) {
            resolvedFlow = List.of();
        }

        // All interactions follow the same pattern: dialogue first, then special interaction
        if (resolvedFlow.isEmpty()) {
            runSpecialInteraction(specialInteraction, specialContext);
            return;
        }

        queueSpecialInteractionAfterDialogue(specialInteraction, specialContext);
        showDialogue(resolvedFlow, interactible.getDialoguePosition());
    }

    private void showDialogue(List<DialogueEntry> flow, Interactible.DialoguePosition position) {
        this.dialogueFlow = flow;
        this.dialogueFlowIndex = 0;
        this.dialoguePosition = position == null ? Interactible.DialoguePosition.BOTTOM : position;
        this.dialogueVisible = true;
        setActiveDialogueLine(flow.get(0));
    }

    private void hideDialogue() {
        Consumer<SpecialInteractionContext> specialInteractionToRun = pendingSpecialInteraction;
        SpecialInteractionContext specialInteractionContextToRun = pendingSpecialInteractionContext;

        this.dialogueVisible = false;
        this.dialogueFlow = List.of();
        this.dialogueFlowIndex = 0;
        this.dialogueName = "";
        this.dialogueText = "";
        this.dialogueFullText = "";
        this.dialogueVisibleChars = 0;
        this.typewriterCarry = 0f;
        dialogueSoundManager.stop();

        pendingSpecialInteraction = null;
        pendingSpecialInteractionContext = null;

        runSpecialInteraction(specialInteractionToRun, specialInteractionContextToRun);
    }

    private void tickTypewriter(float deltaTime) {
        if (isDialogueFullyRevealed()) {
            dialogueSoundManager.stop();
            return;
        }

        int previousVisible = dialogueVisibleChars;
        float charProgress = TYPEWRITER_CHARS_PER_SECOND * Math.max(0f, deltaTime) + typewriterCarry;
        int charsToReveal = (int) charProgress;
        typewriterCarry = charProgress - charsToReveal;
        if (charsToReveal <= 0) {
            return;
        }

        dialogueVisibleChars = Math.min(dialogueFullText.length(), dialogueVisibleChars + charsToReveal);
        dialogueText = dialogueFullText.substring(0, dialogueVisibleChars);

        if (dialogueVisibleChars > previousVisible) {
            playDialogueSounds(previousVisible, dialogueVisibleChars);
        }
    }

    private boolean isDialogueFullyRevealed() {
        return dialogueVisibleChars >= dialogueFullText.length();
    }

    private void revealDialogueImmediately() {
        dialogueVisibleChars = dialogueFullText.length();
        dialogueText = dialogueFullText;
        dialogueSoundManager.stop();
    }

    private boolean showNextDialogueLine() {
        if (dialogueFlowIndex + 1 >= dialogueFlow.size()) {
            return false;
        }
        dialogueFlowIndex++;
        setActiveDialogueLine(dialogueFlow.get(dialogueFlowIndex));
        return true;
    }

    private void setActiveDialogueLine(DialogueEntry entry) {
        this.dialogueName = entry.name();
        this.dialogueFullText = entry.text();
        this.dialogueText = "";
        this.dialogueVisibleChars = 0;
        this.typewriterCarry = 0f;
        dialogueSoundManager.stop();
    }

    private void playDialogueSounds(int startIndex, int endIndex) {
        if (dialogueFullText == null || dialogueFullText.isEmpty()) {
            return;
        }

        int safeStart = Math.max(0, startIndex);
        int safeEnd = Math.min(dialogueFullText.length(), endIndex);
        for (int i = safeStart; i < safeEnd; i++) {
            char c = dialogueFullText.charAt(i);
            dialogueSoundManager.onCharacterRevealed(c, i);
        }
    }

    private static String resolveDialogueSource(Interactible interactible) {
        String dialogueDirectory = interactible.getDialogueDirectory();
        if (dialogueDirectory == null || dialogueDirectory.isBlank()) {
            return dialogueDirectory;
        }

        String[] rawEntries = dialogueDirectory.split("\\|");
        if (rawEntries.length <= 1) {
            return dialogueDirectory.trim();
        }

        List<String> entries = new ArrayList<>();
        for (String rawEntry : rawEntries) {
            if (rawEntry == null) {
                continue;
            }
            String entry = rawEntry.trim();
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }

        if (entries.isEmpty()) {
            return dialogueDirectory.trim();
        }

        int interactionIndex = Math.max(0, interactible.getInteractionCount() - 1);
        int sourceIndex = Math.min(interactionIndex, entries.size() - 1);
        return entries.get(sourceIndex);
    }

    private List<DialogueEntry> resolveDialogueFlow(Interactible interactible, String playerName, String source) {
        String trimmedSource = source == null ? "" : source.trim();
        boolean isFileBackedDialogue = isJsonDialoguePath(trimmedSource);
        String fallbackObjectName = isFileBackedDialogue ? "Object" : "";
        Map<String, String> objectNamesById = buildObjectNameLookup();
        DialogueContext context = new DialogueContext(
                safeText(interactible.getObjectName(), fallbackObjectName),
                safeText(interactible.getObjectId(), safeText(interactible.getObjectName(), "Object")),
                safeText(playerName, "Player"),
                objectNamesById
        );

        if (isFileBackedDialogue) {
            List<DialogueEntry> fileFlow = loadDialogueFlowFromFile(trimmedSource, context, interactible.getInteractionCount());
            if (!fileFlow.isEmpty()) {
                return fileFlow;
            }
        }

        String speaker = replacePlaceholders(context.thisName(), context);
        String text = replacePlaceholders(safeText(source, "..."), context);
        return List.of(new DialogueEntry(speaker, text));
    }

    private static boolean isJsonDialoguePath(String source) {
        return source != null && source.toLowerCase().endsWith(".json");
    }

    private List<DialogueEntry> loadDialogueFlowFromFile(String path, DialogueContext context, int interactionCount) {
        FileHandle fileHandle = Gdx.files.internal(path);
        if (!fileHandle.exists()) {
            Gdx.app.error(InteractionSystem.class.getSimpleName(), "Dialogue file not found: " + path);
            return List.of();
        }

        try {
            JsonValue root = new JsonReader().parse(fileHandle);
            JsonValue interactionsArray = root.get(INTERACTIONS_KEY);
            if (interactionsArray != null && interactionsArray.isArray()) {
                JsonValue interactionNode = selectInteractionNode(root, interactionsArray, interactionCount);
                if (interactionNode != null) {
                    JsonValue flowArray = interactionNode.get(DIALOGUE_FLOW_KEY);
                    if (flowArray == null) {
                        flowArray = interactionNode.get("dialogueFlow");
                    }
                    return parseDialogueFlowEntries(flowArray, context, path);
                }
            }

            JsonValue flowArray = root.get(DIALOGUE_FLOW_KEY);
            if (flowArray == null) {
                flowArray = root.get("dialogueFlow");
            }
            return parseDialogueFlowEntries(flowArray, context, path);
        } catch (Exception ex) {
            Gdx.app.error(InteractionSystem.class.getSimpleName(), "Failed to parse dialogue file: " + path, ex);
            return List.of();
        }
    }

    private JsonValue selectInteractionNode(JsonValue root, JsonValue interactionsArray, int interactionCount) {
        int totalInteractions = interactionsArray.size;
        if (totalInteractions <= 0) {
            return null;
        }

        int interactionIndex = Math.max(0, interactionCount - 1);
        if (interactionIndex < totalInteractions) {
            return interactionsArray.get(interactionIndex);
        }

        return interactionsArray.get(totalInteractions - 1);
    }

    private List<DialogueEntry> parseDialogueFlowEntries(JsonValue flowArray, DialogueContext context, String path) {
        if (flowArray == null || !flowArray.isArray()) {
            Gdx.app.error(InteractionSystem.class.getSimpleName(), "DialogueFlow array missing in: " + path);
            return List.of();
        }

        List<DialogueEntry> entries = new ArrayList<>();
        for (JsonValue line = flowArray.child; line != null; line = line.next) {
            String nameTemplate = line.getString("Name", "{this}");
            String textTemplate = line.getString("Text", "");
            String resolvedName = replacePlaceholders(nameTemplate, context);
            String resolvedText = replacePlaceholders(textTemplate, context);
            entries.add(new DialogueEntry(resolvedName, resolvedText));
        }
        return entries;
    }

    private static String replacePlaceholders(String input, DialogueContext context) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String tokenRaw = matcher.group(1).trim();
            String token = tokenRaw.toLowerCase(Locale.ROOT);
            String replacement = switch (token) {
                case "this" -> context.thisName();
                case "objectid", "objectidname", "object_id" -> context.objectId();
                case "player" -> context.playerName();
                default -> context.lookupObjectNameById(tokenRaw);
            };
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private Map<String, String> buildObjectNameLookup() {
        boolean noInteractibles = interactibles == null || interactibles.size() == 0;
        boolean noEncounters = encounters == null || encounters.size() == 0;
        if (noInteractibles && noEncounters) {
            return Map.of();
        }

        Map<String, String> lookup = new HashMap<>();
        if (interactibles != null) {
            for (Entity entity : interactibles) {
                Interactible interactible = Interactible.MAPPER.get(entity);
                if (interactible == null) {
                    continue;
                }

                String objectId = interactible.getObjectId();
                if (objectId == null || objectId.isBlank()) {
                    continue;
                }
                String objectName = safeText(interactible.getObjectName(), "Object");
                lookup.put(objectId.toLowerCase(Locale.ROOT), objectName);
            }
        }

        if (encounters != null) {
            for (Entity entity : encounters) {
                EncounterZone encounterZone = EncounterZone.MAPPER.get(entity);
                if (encounterZone == null) {
                    continue;
                }

                String encounterId = encounterZone.getEncounterId();
                if (encounterId == null || encounterId.isBlank()) {
                    continue;
                }
                String enemyName = safeText(encounterZone.getEnemyName(), encounterId);
                lookup.put(encounterId.toLowerCase(Locale.ROOT), enemyName);
            }
        }

        List<Clawkin> party = clawkinPartySupplier.get();
        if (party != null) {
            for (Clawkin clawkin : party) {
                if (clawkin == null) {
                    continue;
                }

                String clawkinId = clawkin.getId();
                if (clawkinId == null || clawkinId.isBlank()) {
                    continue;
                }
                String clawkinName = safeText(clawkin.getName(), clawkinId);
                lookup.put(clawkinId.toLowerCase(Locale.ROOT), clawkinName);
            }
        }

        return Map.copyOf(lookup);
    }

    private static String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String resolvePlayerName(Entity playerEntity) {
        PlayerProfile profile = PlayerProfile.MAPPER.get(playerEntity);
        if (profile == null || profile.getPlayerName() == null || profile.getPlayerName().isBlank()) {
            return "Player";
        }
        return profile.getPlayerName();
    }

    private Consumer<SpecialInteractionContext> resolveSpecialInteraction(Interactible interactible) {
        String objectId = interactible == null ? null : interactible.getObjectId();
        String normalizedObjectId = normalizeObjectId(objectId);
        if (!normalizedObjectId.isEmpty()) {
            Consumer<SpecialInteractionContext> byObjectId = specialInteractionByObjectId.get(normalizedObjectId);
            if (byObjectId != null) {
                return byObjectId;
            }
        }

        String groupId = interactible == null ? null : interactible.getGroupId();
        String normalizedGroupId = normalizeGroupId(groupId);
        if (normalizedGroupId.isEmpty()) {
            return null;
        }
        return specialInteractionByGroupId.get(normalizedGroupId);
    }

    private Predicate<SpecialInteractionContext> resolvePreDialogueCheck(Interactible interactible) {
        String objectId = interactible == null ? null : interactible.getObjectId();
        String normalizedObjectId = normalizeObjectId(objectId);
        if (!normalizedObjectId.isEmpty()) {
            Predicate<SpecialInteractionContext> byObjectId = preDialogueCheckByObjectId.get(normalizedObjectId);
            if (byObjectId != null) {
                return byObjectId;
            }
        }

        String groupId = interactible == null ? null : interactible.getGroupId();
        String normalizedGroupId = normalizeGroupId(groupId);
        if (normalizedGroupId.isEmpty()) {
            return null;
        }
        return preDialogueCheckByGroupId.get(normalizedGroupId);
    }

    private static String normalizeObjectId(String objectId) {
        if (objectId == null) {
            return "";
        }
        return objectId.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeGroupId(String groupId) {
        if (groupId == null) {
            return "";
        }
        return groupId.trim().toLowerCase(Locale.ROOT);
    }

    private void restorePersistedInteractionCount(Interactible interactible) {
        String normalizedObjectId = normalizeObjectId(interactible.getObjectId());
        if (normalizedObjectId.isEmpty()) {
            return;
        }
        Integer persistedCount = persistedInteractionCountsByObjectId.get(normalizedObjectId);
        if (persistedCount == null) {
            return;
        }
        if (persistedCount > interactible.getInteractionCount()) {
            interactible.setInteractionCount(persistedCount);
        }
    }

    private void persistInteractionCount(Interactible interactible) {
        String normalizedObjectId = normalizeObjectId(interactible.getObjectId());
        if (normalizedObjectId.isEmpty()) {
            return;
        }
        persistedInteractionCountsByObjectId.put(normalizedObjectId, interactible.getInteractionCount());
    }

    private void queueSpecialInteractionAfterDialogue(
            Consumer<SpecialInteractionContext> specialInteraction,
            SpecialInteractionContext context) {
        pendingSpecialInteraction = specialInteraction;
        pendingSpecialInteractionContext = context;
    }

    private void runSpecialInteraction(
            Consumer<SpecialInteractionContext> specialInteraction,
            SpecialInteractionContext context) {
        if (specialInteraction == null || context == null) {
            return;
        }

        try {
            specialInteraction.accept(context);
        } catch (Exception ex) {
            Gdx.app.error(
                    InteractionSystem.class.getSimpleName(),
                    "Special interaction failed for ObjectId=" + context.objectId(),
                    ex
            );
        }
    }

    private boolean runPreDialogueCheck(
            Predicate<SpecialInteractionContext> preDialogueCheck,
            SpecialInteractionContext context) {
        if (preDialogueCheck == null || context == null) {
            return true;
        }
        try {
            return preDialogueCheck.test(context);
        } catch (Exception ex) {
            Gdx.app.error(
                    InteractionSystem.class.getSimpleName(),
                    "Pre-dialogue check failed for ObjectId=" + context.objectId(),
                    ex
            );
            return true;
        }
    }

    private record DialogueEntry(String name, String text) {
    }

    @SuppressWarnings("all")
    private record DialogueContext(String thisName, String objectId, String playerName, Map<String, String> objectNamesById) {
        private String lookupObjectNameById(String token) {
            if (token == null || token.isBlank()) {
                return "";
            }
            String resolved = objectNamesById.get(token.toLowerCase(Locale.ROOT));
            return resolved == null ? "{" + token + "}" : resolved;
        }
    }

    public record SpecialInteractionContext(
            Entity playerEntity,
            Entity targetEntity,
            String objectId,
            String objectName,
            int interactionCount) {
        public SpecialInteractionContext {
            Objects.requireNonNull(playerEntity, "playerEntity");
            Objects.requireNonNull(targetEntity, "targetEntity");
            objectId = objectId == null ? "" : objectId;
            objectName = objectName == null || objectName.isBlank() ? "Object" : objectName;
            interactionCount = Math.max(0, interactionCount);
        }
    }
}
