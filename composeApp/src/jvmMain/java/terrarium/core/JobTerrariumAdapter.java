package terrarium.core;

import job.controller.IJobController;
import job.model.Job;
import job.model.result.FlowRun;
import job.model.result.FlowRunJob;
import job.model.result.JobStatus;
import job.repository.IJobConfigRepository;
import terrarium.model.TerrariumCreatureKind;
import terrarium.model.TerrariumCreatureSignal;
import terrarium.model.TerrariumCreatureStatus;
import terrarium.model.TerrariumMotionStyle;
import terrarium.model.TerrariumSourceSnapshot;
import terrarium.model.TerrariumSourceStatus;
import terrarium.model.TerrariumVisualHint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JobTerrariumAdapter implements TerrariumResourceAdapter {
    public static final String SOURCE_ID = "jobs";

    private final IJobController controller;
    private final IJobConfigRepository jobConfigRepository;

    public JobTerrariumAdapter(IJobController controller) {
        this(controller, null);
    }

    public JobTerrariumAdapter(
        IJobController controller,
        IJobConfigRepository jobConfigRepository
    ) {
        this.controller = controller;
        this.jobConfigRepository = jobConfigRepository;
    }

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public String getDisplayName() {
        return "Jobs";
    }

    @Override
    public TerrariumSourceSnapshot readSnapshot() {
        if (controller == null) {
            return TerrariumSourceSnapshot.unavailable(SOURCE_ID, getDisplayName(), "Job controller is not available.");
        }

        List<Job> jobs = controller.listJobs();
        List<FlowRun> runs = controller.listFlowRuns();
        List<Long> runIds = runs.stream().map(FlowRun::getId).toList();
        List<FlowRunJob> runJobs = runIds.isEmpty() ? List.of() : controller.listFlowRunJobs(runIds);
        Map<String, FlowRunJob> latestByJobId = latestRunJobByJobId(runJobs);

        List<TerrariumCreatureSignal> fish = new ArrayList<>();
        for (Job job : jobs) {
            fish.add(toFishSignal(job, latestByJobId.get(job.getId())));
        }

        return new TerrariumSourceSnapshot(
            SOURCE_ID,
            getDisplayName(),
            TerrariumSourceStatus.AVAILABLE,
            List.of(),
            fish,
            Instant.now(),
            ""
        );
    }

    private TerrariumCreatureSignal toFishSignal(Job job, FlowRunJob latest) {
        String jobId = job.getId() == null ? "" : job.getId();
        String label = job.getTitle() == null || job.getTitle().isBlank() ? jobId : job.getTitle();
        boolean enabled = job.isEnabled();
        JobStatus latestStatus = latest == null ? null : latest.getStatus();
        int health = healthFor(enabled, latestStatus);
        int stress = stressFor(enabled, latestStatus);
        int activity = activityFor(enabled, latestStatus);
        int risk = riskFor(enabled, latestStatus);
        TerrariumCreatureStatus status = statusFor(enabled, latestStatus);
        TerrariumMotionStyle motionStyle = motionFor(enabled, latestStatus);
        int priority = priorityFor(job);
        int importance = TerrariumMath.clampInt(50 + priority * 5, 0, 100);
        double sizeWeight = latest == null
            ? 1.0d
            : TerrariumMath.clampDouble(0.8d + Math.log1p(Math.max(0L, latest.getDurationMs())) / 12.0d, 0.7d, 1.6d);

        return new TerrariumCreatureSignal(
            "job:" + jobId,
            label,
            TerrariumCreatureKind.JOB,
            SOURCE_ID,
            jobId,
            health,
            stress,
            activity,
            risk,
            status,
            TerrariumVisualHint.stable(jobId, sizeWeight, motionStyle, importance)
        );
    }

    private int priorityFor(Job job) {
        if (jobConfigRepository != null && job.getId() != null) {
            return jobConfigRepository.findOneById(job.getId())
                .map(config -> config.getPriority())
                .orElse(0);
        }
        return job.getConfig() == null ? 0 : job.getConfig().getPriority();
    }

    private static Map<String, FlowRunJob> latestRunJobByJobId(List<FlowRunJob> runJobs) {
        List<FlowRunJob> sorted = new ArrayList<>(runJobs);
        sorted.sort(
            Comparator.comparingLong(FlowRunJob::getRunId)
                .thenComparing(job -> job.getEndedAt() == null ? Instant.EPOCH : job.getEndedAt())
                .reversed()
        );
        Map<String, FlowRunJob> latest = new HashMap<>();
        for (FlowRunJob runJob : sorted) {
            latest.putIfAbsent(runJob.getJobId(), runJob);
        }
        return latest;
    }

    private static int healthFor(boolean enabled, JobStatus status) {
        if (!enabled) {
            return 35;
        }
        if (status == null) {
            return 70;
        }
        return switch (status) {
            case SUCCESS -> 88;
            case IDLE -> 72;
            case INITIALIZING, QUEUED, RUNNING -> 64;
            case FAILED -> 34;
            case CANCELLED -> 30;
            case TIMEOUT -> 24;
        };
    }

    private static int stressFor(boolean enabled, JobStatus status) {
        if (!enabled) {
            return 30;
        }
        if (status == null) {
            return 18;
        }
        return switch (status) {
            case SUCCESS -> 10;
            case IDLE -> 20;
            case INITIALIZING, QUEUED, RUNNING -> 55;
            case FAILED -> 78;
            case CANCELLED -> 72;
            case TIMEOUT -> 86;
        };
    }

    private static int activityFor(boolean enabled, JobStatus status) {
        if (!enabled) {
            return 5;
        }
        if (status == null) {
            return 38;
        }
        return switch (status) {
            case SUCCESS -> 58;
            case IDLE -> 28;
            case INITIALIZING, QUEUED -> 65;
            case RUNNING -> 90;
            case FAILED -> 30;
            case CANCELLED, TIMEOUT -> 18;
        };
    }

    private static int riskFor(boolean enabled, JobStatus status) {
        if (!enabled) {
            return 40;
        }
        if (status == null) {
            return 25;
        }
        return switch (status) {
            case SUCCESS -> 8;
            case IDLE -> 22;
            case INITIALIZING, QUEUED, RUNNING -> 45;
            case FAILED -> 82;
            case CANCELLED -> 72;
            case TIMEOUT -> 92;
        };
    }

    private static TerrariumCreatureStatus statusFor(boolean enabled, JobStatus status) {
        if (!enabled) {
            return TerrariumCreatureStatus.INACTIVE;
        }
        if (status == null) {
            return TerrariumCreatureStatus.UNKNOWN;
        }
        return switch (status) {
            case SUCCESS, IDLE -> TerrariumCreatureStatus.HEALTHY;
            case INITIALIZING, QUEUED, RUNNING -> TerrariumCreatureStatus.STRESSED;
            case FAILED, CANCELLED, TIMEOUT -> TerrariumCreatureStatus.SICK;
        };
    }

    private static TerrariumMotionStyle motionFor(boolean enabled, JobStatus status) {
        if (!enabled) {
            return TerrariumMotionStyle.STILL;
        }
        if (status == null) {
            return TerrariumMotionStyle.CALM;
        }
        return switch (status) {
            case SUCCESS -> TerrariumMotionStyle.ACTIVE;
            case IDLE -> TerrariumMotionStyle.CALM;
            case INITIALIZING, QUEUED, RUNNING -> TerrariumMotionStyle.ERRATIC;
            case FAILED, CANCELLED, TIMEOUT -> TerrariumMotionStyle.DRIFTING;
        };
    }
}
