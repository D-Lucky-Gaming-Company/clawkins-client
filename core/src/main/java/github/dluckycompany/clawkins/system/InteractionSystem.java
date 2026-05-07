package github.dluckycompany.clawkins.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import github.dluckycompany.clawkins.audio.DialogueSoundManager;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.PlayerAnimation;
import github.dluckycompany.clawkins.component.PlayerProfile;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.encounter.EncounterZone;

public class InteractionSystem extends EntitySystem {
    private static final float INTERACT_RANGE = 1.2f;
    private static final float PLAYER_INTERACT_ORIGIN_Y_FACTOR = 0.22f;
    private static final float FACING_DOT_THRESHOLD = 0.6f;
    private static final float TYPEWRITER_CHARS_PER_SECOND = 45f;
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
    private final Map<String, Consumer<SpecialInteractionContext>> specialInteractionByObjectId = new HashMap<>();
    private Consumer<SpecialInteractionContext> pendingSpecialInteraction;
    private SpecialInteractionContext pendingSpecialInteractionContext;
    private final Set<Entity> activeTrippableTargets = Collections.newSetFromMap(new IdentityHashMap<>());

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

        if (processTrippableInteraction()) {
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

    public void registerSpecialInteraction(String objectId, Consumer<SpecialInteractionContext> specialInteraction) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty() || specialInteraction == null) {
            return;
        }
        specialInteractionByObjectId.put(normalizedObjectId, specialInteraction);
    }

    public void unregisterSpecialInteraction(String objectId) {
        String normalizedObjectId = normalizeObjectId(objectId);
        if (normalizedObjectId.isEmpty()) {
            return;
        }
        specialInteractionByObjectId.remove(normalizedObjectId);
    }

    public void clearSpecialInteractions() {
        specialInteractionByObjectId.clear();
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
            Transform t = Transform.MAPPER.get(entity);
            Rectangle targetBounds = new Rectangle(
                    t.getPosition().x,
                    t.getPosition().y,
                    t.getSize().x,
                    t.getSize().y
            );
            Vector2 targetPoint = closestPointOnRect(playerCenter, targetBounds);
            Vector2 toTarget = targetPoint.sub(playerCenter);
            float distance = toTarget.len();
            if (distance > INTERACT_RANGE) {
                continue;
            }

            if (distance == 0f) {
                toTarget = centerOf(t).sub(playerCenter);
                distance = toTarget.len();
                if (distance == 0f) {
                    continue;
                }
            }

            float dot = toTarget.nor().dot(facing);
            if (dot < FACING_DOT_THRESHOLD) {
                continue;
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

    private static Vector2 directionVector(PlayerAnimation.Direction direction) {
        return switch (direction) {
            case NORTH -> new Vector2(0f, 1f);
            case SOUTH -> new Vector2(0f, -1f);
            case EAST -> new Vector2(1f, 0f);
            case WEST -> new Vector2(-1f, 0f);
        };
    }

    private static boolean isInteractionPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.Z)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
    }

    private boolean processTrippableInteraction() {
        if (players == null || players.size() == 0 || interactibles == null || interactibles.size() == 0) {
            activeTrippableTargets.clear();
            return false;
        }

        Entity playerEntity = players.first();
        Transform playerTransform = Transform.MAPPER.get(playerEntity);
        if (playerTransform == null) {
            activeTrippableTargets.clear();
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

            Rectangle targetBounds = new Rectangle(
                    targetTransform.getPosition().x,
                    targetTransform.getPosition().y,
                    targetTransform.getSize().x,
                    targetTransform.getSize().y
            );
            if (!targetBounds.contains(playerPoint)) {
                continue;
            }

            currentlyInsideTrippableTargets.add(entity);
            if (!activeTrippableTargets.contains(entity) && enteredTarget == null) {
                enteredTarget = entity;
            }
        }

        activeTrippableTargets.retainAll(currentlyInsideTrippableTargets);
        activeTrippableTargets.addAll(currentlyInsideTrippableTargets);

        if (enteredTarget == null) {
            return false;
        }

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

        String playerName = resolvePlayerName(playerEntity);
        interactible.incrementInteractionCount();

        Consumer<SpecialInteractionContext> specialInteraction = resolveSpecialInteraction(interactible);
        SpecialInteractionContext specialContext = new SpecialInteractionContext(
                playerEntity,
                targetEntity,
                safeText(interactible.getObjectId(), ""),
                safeText(interactible.getObjectName(), "Object"),
                interactible.getInteractionCount()
        );

        // Merchant interactions stay key-compatible with existing UI flow.
        if (interactible.isMerchant()) {
            isMerchantMode = true;
            onMerchantInteraction.run();
            return;
        }

        String dialogueSource = resolveDialogueSource(interactible);
        List<DialogueEntry> resolvedFlow = resolveDialogueFlow(interactible, playerName, dialogueSource);
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
        Map<String, String> objectNamesById = buildObjectNameLookup();
        DialogueContext context = new DialogueContext(
                safeText(interactible.getObjectName(), "Object"),
                safeText(interactible.getObjectId(), safeText(interactible.getObjectName(), "Object")),
                safeText(playerName, "Player"),
                objectNamesById
        );

        String trimmedSource = source == null ? "" : source.trim();
        if (isJsonDialoguePath(trimmedSource)) {
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
        if (normalizedObjectId.isEmpty()) {
            return null;
        }
        return specialInteractionByObjectId.get(normalizedObjectId);
    }

    private static String normalizeObjectId(String objectId) {
        if (objectId == null) {
            return "";
        }
        return objectId.trim().toLowerCase(Locale.ROOT);
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
