package terrarium.controller;

import terrarium.core.TerrariumResourceAdapter;
import terrarium.model.TerrariumSnapshot;
import terrarium.model.TerrariumSourceSnapshot;

import java.util.List;

public interface ITerrariumController {
    TerrariumSnapshot getSnapshot(List<TerrariumResourceAdapter> adapters);

    TerrariumSnapshot composeSnapshots(List<TerrariumSourceSnapshot> sourceSnapshots);
}
