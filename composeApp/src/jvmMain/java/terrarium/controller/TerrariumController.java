package terrarium.controller;

import terrarium.core.TerrariumResourceAdapter;
import terrarium.core.TerrariumSnapshotComposer;
import terrarium.model.TerrariumSnapshot;
import terrarium.model.TerrariumSourceSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class TerrariumController implements ITerrariumController {
    private final TerrariumSnapshotComposer composer;

    public TerrariumController() {
        this(new TerrariumSnapshotComposer());
    }

    public TerrariumController(TerrariumSnapshotComposer composer) {
        this.composer = composer == null ? new TerrariumSnapshotComposer() : composer;
    }

    @Override
    public TerrariumSnapshot getSnapshot(List<TerrariumResourceAdapter> adapters) {
        List<TerrariumSourceSnapshot> snapshots = new ArrayList<>();
        if (adapters != null) {
            for (TerrariumResourceAdapter adapter : adapters) {
                if (adapter == null) {
                    continue;
                }
                try {
                    snapshots.add(adapter.readSnapshot());
                } catch (RuntimeException e) {
                    snapshots.add(TerrariumSourceSnapshot.unavailable(
                        adapter.getSourceId(),
                        adapter.getDisplayName(),
                        e.getMessage() == null ? "Terrarium resource adapter failed." : e.getMessage()
                    ));
                }
            }
        }
        return composer.composeSnapshots(snapshots);
    }

    @Override
    public TerrariumSnapshot composeSnapshots(List<TerrariumSourceSnapshot> sourceSnapshots) {
        return composer.composeSnapshots(sourceSnapshots);
    }
}
