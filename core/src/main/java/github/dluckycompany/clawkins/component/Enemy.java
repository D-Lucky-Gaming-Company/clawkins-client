package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

public class Enemy implements Component {
    public static final ComponentMapper<Enemy> MAPPER = ComponentMapper.getFor(Enemy.class);

    public enum State {
        IDLE,
        ROAMING,
        ALERTED,
        CHASING
    }

    private boolean canRoam;
    private boolean canChase;
    private State state;
    
    private final float roamingSpeed;
    private final float chasingSpeed;
    private final float sightRange;
    private final float sightConeDotThreshold;

    private final Vector2 facingDirection;
    private final Vector2 homePosition;
    private float roamTimer;
    private float roamInterval;
    private float idleTimer;
    private float chaseMemoryTimer;
    private boolean idlingBetweenRoams;
    private final float roamDecisionDistance;
    private final float chaseProbeDistance;

    public Enemy(boolean canRoam, boolean canChase, float roamingSpeed, float chasingSpeed, float sightRange, float sightConeDotThreshold) {
        this.canRoam = canRoam;
        this.canChase = canChase;
        this.state = State.IDLE;
        
        this.roamingSpeed = roamingSpeed;
        this.chasingSpeed = chasingSpeed;
        this.sightRange = sightRange;
        this.sightConeDotThreshold = sightConeDotThreshold;
        
        this.facingDirection = new Vector2(0, -1); // default facing down
        this.homePosition = new Vector2();
        this.roamTimer = 0f;
        this.roamInterval = 2.0f; // Change roam direction every 2 seconds
        this.idleTimer = 0f;
        this.chaseMemoryTimer = 0f;
        this.idlingBetweenRoams = false;
        this.roamDecisionDistance = 0.75f;
        this.chaseProbeDistance = 0.45f;
    }

    public boolean canRoam() { return canRoam; }
    public void setCanRoam(boolean canRoam) { this.canRoam = canRoam; }

    public boolean canChase() { return canChase; }
    public void setCanChase(boolean canChase) { this.canChase = canChase; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public float getRoamingSpeed() { return roamingSpeed; }
    public float getChasingSpeed() { return chasingSpeed; }
    public float getSightRange() { return sightRange; }
    public float getSightConeDotThreshold() { return sightConeDotThreshold; }

    public Vector2 getFacingDirection() { return facingDirection; }
    public void setFacingDirection(Vector2 direction) {
        if (direction != null && !direction.isZero()) {
            this.facingDirection.set(direction).nor();
        }
    }
    public Vector2 getHomePosition() { return homePosition; }

    public float getRoamTimer() { return roamTimer; }
    public void setRoamTimer(float roamTimer) { this.roamTimer = roamTimer; }

    public float getRoamInterval() { return roamInterval; }
    public void setRoamInterval(float roamInterval) { this.roamInterval = roamInterval; }

    public float getIdleTimer() { return idleTimer; }
    public void setIdleTimer(float idleTimer) { this.idleTimer = idleTimer; }

    public float getChaseMemoryTimer() { return chaseMemoryTimer; }
    public void setChaseMemoryTimer(float chaseMemoryTimer) { this.chaseMemoryTimer = chaseMemoryTimer; }

    public boolean isIdlingBetweenRoams() { return idlingBetweenRoams; }
    public void setIdlingBetweenRoams(boolean idlingBetweenRoams) { this.idlingBetweenRoams = idlingBetweenRoams; }

    public float getRoamDecisionDistance() { return roamDecisionDistance; }
    public float getChaseProbeDistance() { return chaseProbeDistance; }
}
