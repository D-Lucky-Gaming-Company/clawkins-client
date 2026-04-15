package github.dluckycompany.clawkins.encounter;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Minimal event queue for world encounter events.
 */
public class EncounterEventBus {
    private final Queue<EncounterEvent> events = new ArrayDeque<>();

    public void publish(EncounterEvent event) {
        events.offer(event);
    }

    public EncounterEvent poll() {
        return events.poll();
    }

    public boolean hasEvents() {
        return !events.isEmpty();
    }
}
