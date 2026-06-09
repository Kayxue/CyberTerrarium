package terrarium.core;

import terrarium.model.TerrariumSourceSnapshot;

public interface TerrariumResourceAdapter {
    String getSourceId();

    String getDisplayName();

    TerrariumSourceSnapshot readSnapshot();
}
