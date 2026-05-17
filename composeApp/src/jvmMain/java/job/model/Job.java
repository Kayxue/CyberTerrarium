package job.model;

import job.model.script.JobScript;
import job.model.trigger.JobTrigger;
import job.model.trigger.ManualTrigger;

import java.util.ArrayList;
import java.util.List;

public class Job {
    private String id;
    private String stageId;
    private String title;
    private String description;
    private JobScript script;
    private JobConfig config;
    private JobTrigger trigger;
    private List<JobDependency> dependencies;
    private boolean enabled;

    public Job() {
        this.dependencies = new ArrayList<>();
        this.config = new JobConfig();
        this.trigger = new ManualTrigger();
        this.script = new JobScript();
    }

    public Job(
        String id,
        String stageId,
        String title,
        String description,
        JobScript script,
        JobConfig config,
        JobTrigger trigger,
        List<JobDependency> dependencies,
        boolean enabled
    ) {
        this.id = id;
        this.stageId = stageId;
        this.title = title;
        this.description = description;
        this.script = script;
        this.config = config;
        this.trigger = trigger;
        this.dependencies = dependencies;
        this.enabled = enabled;
    }

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getStageId() { return stageId; }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobScript getScript() { return script; }

    public void setScript(JobScript script) {
        this.script = script;
    }

    public JobConfig getConfig() {
        return config;
    }

    public void setConfig(JobConfig config) {
        this.config = config;
    }

    public JobTrigger getTrigger() { return trigger; }

    public void setTrigger(JobTrigger trigger) {
        this.trigger = trigger;
    }

    public List<JobDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<JobDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
