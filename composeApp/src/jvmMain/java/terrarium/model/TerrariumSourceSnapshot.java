package terrarium.model;

import java.time.Instant;
import java.util.List;

public final class TerrariumSourceSnapshot {
    private final String sourceId;
    private final String displayName;
    private final TerrariumSourceStatus sourceStatus;
    private final List<TerrariumEnvironmentSignal> environmentSignals;
    private final List<TerrariumCreatureSignal> creatureSignals;
    private final Instant sampledAt;
    private final String message;

    public TerrariumSourceSnapshot(
        String sourceId,
        String displayName,
        TerrariumSourceStatus sourceStatus,
        List<TerrariumEnvironmentSignal> environmentSignals,
        List<TerrariumCreatureSignal> creatureSignals,
        Instant sampledAt,
        String message
    ) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.displayName = displayName == null ? this.sourceId : displayName;
        this.sourceStatus = sourceStatus == null ? TerrariumSourceStatus.UNAVAILABLE : sourceStatus;
        this.environmentSignals = List.copyOf(environmentSignals == null ? List.of() : environmentSignals);
        this.creatureSignals = List.copyOf(creatureSignals == null ? List.of() : creatureSignals);
        this.sampledAt = sampledAt == null ? Instant.now() : sampledAt;
        this.message = message == null ? "" : message;
    }

    public static TerrariumSourceSnapshot unavailable(String sourceId, String displayName, String message) {
        return new TerrariumSourceSnapshot(
            sourceId,
            displayName,
            TerrariumSourceStatus.UNAVAILABLE,
            List.of(),
            List.of(),
            Instant.now(),
            message
        );
    }

    public String getSourceId() { return sourceId; }
    public String getDisplayName() { return displayName; }
    public TerrariumSourceStatus getSourceStatus() { return sourceStatus; }
    public List<TerrariumEnvironmentSignal> getEnvironmentSignals() { return environmentSignals; }
    public List<TerrariumCreatureSignal> getCreatureSignals() { return creatureSignals; }
    public Instant getSampledAt() { return sampledAt; }
    public String getMessage() { return message; }
}
