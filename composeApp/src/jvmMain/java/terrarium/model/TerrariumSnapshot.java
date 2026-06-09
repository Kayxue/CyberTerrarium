package terrarium.model;

import java.time.Instant;
import java.util.List;

public final class TerrariumSnapshot {
    private final TerrariumEnvironmentState environment;
    private final List<TerrariumFishState> fish;
    private final List<TerrariumSourceSnapshot> sources;
    private final Instant sampledAt;

    public TerrariumSnapshot(
        TerrariumEnvironmentState environment,
        List<TerrariumFishState> fish,
        List<TerrariumSourceSnapshot> sources,
        Instant sampledAt
    ) {
        this.environment = environment == null ? TerrariumEnvironmentState.healthy() : environment;
        this.fish = List.copyOf(fish == null ? List.of() : fish);
        this.sources = List.copyOf(sources == null ? List.of() : sources);
        this.sampledAt = sampledAt == null ? Instant.now() : sampledAt;
    }

    public static TerrariumSnapshot empty() {
        return new TerrariumSnapshot(TerrariumEnvironmentState.healthy(), List.of(), List.of(), Instant.now());
    }

    public TerrariumEnvironmentState getEnvironment() { return environment; }
    public List<TerrariumFishState> getFish() { return fish; }
    public List<TerrariumSourceSnapshot> getSources() { return sources; }
    public Instant getSampledAt() { return sampledAt; }
}
