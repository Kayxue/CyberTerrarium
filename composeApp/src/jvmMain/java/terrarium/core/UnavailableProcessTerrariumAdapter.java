package terrarium.core;

import terrarium.model.TerrariumSourceSnapshot;

public final class UnavailableProcessTerrariumAdapter implements TerrariumResourceAdapter {
    public static final String SOURCE_ID = "processes";

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public String getDisplayName() {
        return "Processes";
    }

    @Override
    public TerrariumSourceSnapshot readSnapshot() {
        return TerrariumSourceSnapshot.unavailable(
            SOURCE_ID,
            getDisplayName(),
            "Process reader is not implemented yet, so no process fish are emitted."
        );
    }
}
