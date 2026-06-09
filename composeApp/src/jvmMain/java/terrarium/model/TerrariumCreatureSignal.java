package terrarium.model;

import terrarium.core.TerrariumMath;

public final class TerrariumCreatureSignal {
    private final String id;
    private final String label;
    private final TerrariumCreatureKind kind;
    private final String sourceId;
    private final String sourceRef;
    private final int health;
    private final int stress;
    private final int activity;
    private final int risk;
    private final TerrariumCreatureStatus status;
    private final TerrariumVisualHint visualHint;

    public TerrariumCreatureSignal(
        String id,
        String label,
        TerrariumCreatureKind kind,
        String sourceId,
        String sourceRef,
        int health,
        int stress,
        int activity,
        int risk,
        TerrariumCreatureStatus status,
        TerrariumVisualHint visualHint
    ) {
        this.id = id == null ? "" : id;
        this.label = label == null ? "" : label;
        this.kind = kind == null ? TerrariumCreatureKind.JOB : kind;
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceRef = sourceRef == null ? "" : sourceRef;
        this.health = TerrariumMath.clampInt(health, 0, 100);
        this.stress = TerrariumMath.clampInt(stress, 0, 100);
        this.activity = TerrariumMath.clampInt(activity, 0, 100);
        this.risk = TerrariumMath.clampInt(risk, 0, 100);
        this.status = status == null ? TerrariumCreatureStatus.UNKNOWN : status;
        this.visualHint = visualHint == null
            ? TerrariumVisualHint.stable(this.id, 1.0d, TerrariumMotionStyle.CALM)
            : visualHint;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public TerrariumCreatureKind getKind() { return kind; }
    public String getSourceId() { return sourceId; }
    public String getSourceRef() { return sourceRef; }
    public int getHealth() { return health; }
    public int getStress() { return stress; }
    public int getActivity() { return activity; }
    public int getRisk() { return risk; }
    public TerrariumCreatureStatus getStatus() { return status; }
    public TerrariumVisualHint getVisualHint() { return visualHint; }
}
