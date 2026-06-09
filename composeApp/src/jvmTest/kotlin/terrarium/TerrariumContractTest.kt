package terrarium

import job.controller.IJobController
import job.model.Job
import job.model.JobDependency
import job.model.flow.FlowJobLink
import job.model.result.FlowRun
import job.model.result.FlowRunJob
import job.model.result.FlowStatus
import job.model.result.JobStatus
import job.model.script.ScriptLanguage
import job.model.stage.BarrierMode
import job.model.stage.FlowStage
import job.model.stage.StageFailMode
import terrarium.controller.TerrariumController
import terrarium.core.JobTerrariumAdapter
import terrarium.core.SystemUsageTerrariumAdapter
import terrarium.core.UnavailableProcessTerrariumAdapter
import terrarium.model.TerrariumCreatureStatus
import terrarium.model.TerrariumSourceStatus
import terrarium.model.TerrariumSystemMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerrariumContractTest {
    @Test
    fun systemPressureDegradesEnvironmentWithoutEmittingFish() {
        val adapter = SystemUsageTerrariumAdapter(
            TerrariumSystemMetrics(
                96.0,
                3_200_000_000L,
                15_000_000_000L,
                16_000_000_000L,
                94.0,
                25_000_000L,
                8_000_000L,
                88.0
            )
        )

        val snapshot = TerrariumController().getSnapshot(listOf(adapter))

        assertTrue(snapshot.environment.health < 55)
        assertTrue(snapshot.environment.clarity < 75)
        assertTrue(snapshot.environment.stress > 50)
        assertEquals(0, snapshot.fish.size)
    }

    @Test
    fun unavailableProcessAdapterDoesNotEmitProcessFish() {
        val source = UnavailableProcessTerrariumAdapter().readSnapshot()
        val snapshot = TerrariumController().composeSnapshots(listOf(source))

        assertEquals(TerrariumSourceStatus.UNAVAILABLE, source.sourceStatus)
        assertEquals(0, snapshot.fish.size)
        assertTrue(source.message.contains("not implemented"))
    }

    @Test
    fun jobAdapterMapsLatestResultsToFishHealth() {
        val successJob = job("success-job", "Healthy job", enabled = true)
        val failedJob = job("failed-job", "Sick job", enabled = true)
        val disabledJob = job("disabled-job", "Inactive job", enabled = false)
        val controller = FakeJobController(
            jobs = listOf(successJob, failedJob, disabledJob),
            runs = listOf(flowRun(1), flowRun(2)),
            runJobs = listOf(
                runJob(1, "success-job", JobStatus.FAILED),
                runJob(2, "success-job", JobStatus.SUCCESS),
                runJob(2, "failed-job", JobStatus.TIMEOUT),
                runJob(2, "disabled-job", JobStatus.SUCCESS)
            )
        )

        val snapshot = TerrariumController().getSnapshot(listOf(JobTerrariumAdapter(controller)))
        val fishByRef = snapshot.fish.associateBy { it.sourceRef }

        assertEquals(TerrariumCreatureStatus.HEALTHY, fishByRef.getValue("success-job").status)
        assertEquals(TerrariumCreatureStatus.SICK, fishByRef.getValue("failed-job").status)
        assertEquals(TerrariumCreatureStatus.INACTIVE, fishByRef.getValue("disabled-job").status)
        assertTrue(fishByRef.getValue("success-job").health > fishByRef.getValue("failed-job").health)
    }

    private fun job(id: String, title: String, enabled: Boolean): Job {
        return Job().apply {
            this.id = id
            this.title = title
            this.stageId = "stage"
            this.isEnabled = enabled
        }
    }

    private fun flowRun(id: Long): FlowRun {
        return FlowRun().apply {
            this.id = id
            this.flowId = "flow"
            this.status = FlowStatus.SUCCESS
        }
    }

    private fun runJob(runId: Long, jobId: String, status: JobStatus): FlowRunJob {
        return FlowRunJob().apply {
            this.runId = runId
            this.jobId = jobId
            this.status = status
            this.durationMs = 1000L
        }
    }
}

private class FakeJobController(
    private val jobs: List<Job>,
    private val runs: List<FlowRun>,
    private val runJobs: List<FlowRunJob>
) : IJobController {
    override fun listJobs(): List<Job> = jobs
    override fun listFlowRuns(): List<FlowRun> = runs
    override fun listFlowRunJobs(runIds: List<Long>): List<FlowRunJob> = runJobs.filter { it.runId in runIds }
    override fun listFlowJobs(flowId: String): List<FlowJobLink> = emptyList()
    override fun listAllFlowJobs(): List<FlowJobLink> = emptyList()
    override fun listAllFlowStages(): List<FlowStage> = emptyList()
    override fun listJobDependenciesByJobIds(jobIds: List<String>): List<JobDependency> = emptyList()
    override fun listFlowIds(): List<String> = emptyList()
    override fun listFlowStages(flowId: String): List<FlowStage> = emptyList()
    override fun createJobForFlow(
        flowId: String,
        stageId: String,
        title: String,
        description: String,
        language: ScriptLanguage,
        scriptContent: String,
        position: Int
    ): String = unsupported()

    override fun createOrUpdateFlowStage(
        flowId: String,
        stageId: String,
        displayName: String,
        order: Int,
        barrierMode: BarrierMode,
        failMode: StageFailMode
    ): Unit = unsupported()

    override fun createFlow(flowName: String): String = unsupported()
    override fun deleteFlow(flowId: String): Unit = unsupported()
    override fun deleteJob(jobId: String): Unit = unsupported()
    override fun updateJob(jobId: String, title: String, description: String, stageId: String, order: Int, enabled: Boolean): Unit = unsupported()
    override fun updateJobScript(jobId: String, language: ScriptLanguage, scriptContent: String): Unit = unsupported()
    override fun updateFlowJobStageRelativePosition(flowId: String, jobId: String, stageRelativeX: Double, stageRelativeY: Double): Unit = unsupported()
    override fun updateFlowStageWidth(stageId: String, stageWidth: Double): Unit = unsupported()
    override fun updateJobDependencyControlPoint(jobId: String, upstreamJobId: String, bendX: Double, bendY: Double): Unit = unsupported()
    override fun saveJobDependency(jobId: String, upstreamJobId: String): Unit = unsupported()
    override fun deleteJobDependency(jobId: String, upstreamJobId: String): Unit = unsupported()
    override fun deleteFlowStage(stageId: String): Unit = unsupported()
    override fun runFlow(flowId: String, maxWorkers: Int): FlowRun = unsupported()
    override fun suggestNextPosition(flowId: String): Int = unsupported()

    private fun unsupported(): Nothing {
        throw UnsupportedOperationException("Fake controller only supports read methods.")
    }
}
