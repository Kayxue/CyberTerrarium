package terrarium

import job.controller.IJobController
import job.model.Job
import job.model.JobConfig
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
import job.repository.IJobConfigRepository
import process.ProcessManager
import process.ProcessTreeNode
import terrarium.controller.TerrariumController
import terrarium.core.JobTerrariumAdapter
import terrarium.core.ProcessTerrariumAdapter
import terrarium.core.SystemUsageTerrariumAdapter
import terrarium.model.TerrariumCreatureKind
import terrarium.model.TerrariumCreatureStatus
import terrarium.model.TerrariumSourceStatus
import terrarium.model.TerrariumSystemMetrics
import java.util.Optional
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
    fun processAdapterEmitsEveryProcessAsUniqueFish() {
        val manager = RecordingProcessManager()
        val adapter = ProcessTerrariumAdapter(manager)

        val firstSource = adapter.readSnapshot()
        val firstSnapshot = TerrariumController().composeSnapshots(listOf(firstSource))

        assertEquals(TerrariumSourceStatus.AVAILABLE, firstSource.sourceStatus)
        assertEquals(0, firstSource.environmentSignals.size)
        assertEquals(manager.lastProcessCount, firstSnapshot.fish.size)
        assertTrue(firstSnapshot.fish.isNotEmpty())
        assertTrue(firstSnapshot.fish.all { it.kind == TerrariumCreatureKind.PROCESS })
        assertTrue(firstSnapshot.fish.all { it.id == "process:${it.sourceRef}" })
        assertEquals(
            firstSnapshot.fish.size,
            firstSnapshot.fish.map { it.sourceRef }.toSet().size
        )

        val secondSource = adapter.readSnapshot()

        assertEquals(TerrariumSourceStatus.AVAILABLE, secondSource.sourceStatus)
        assertEquals(2, manager.readCount)
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

    @Test
    fun jobPriorityControlsFishDisplayImportance() {
        val lowPriorityJob = job("low-priority", "Low priority", enabled = true)
        val highPriorityJob = job("high-priority", "High priority", enabled = true)
        val controller = FakeJobController(
            jobs = listOf(lowPriorityJob, highPriorityJob),
            runs = emptyList(),
            runJobs = emptyList()
        )
        val repository = FakeJobConfigRepository(
            mapOf(
                "low-priority" to JobConfig().apply { priority = -5 },
                "high-priority" to JobConfig().apply { priority = 8 }
            )
        )

        val snapshot = TerrariumController().getSnapshot(
            listOf(JobTerrariumAdapter(controller, repository))
        )
        val fishByRef = snapshot.fish.associateBy { it.sourceRef }

        assertTrue(
            fishByRef.getValue("high-priority").visualHint.importance >
                fishByRef.getValue("low-priority").visualHint.importance
        )
    }

    @Test
    fun enabledJobWithoutRunHistoryIsHealthyAndIdle() {
        val idleJob = job("idle-job", "Idle job", enabled = true)
        val controller = FakeJobController(
            jobs = listOf(idleJob),
            runs = emptyList(),
            runJobs = emptyList()
        )

        val snapshot = TerrariumController().getSnapshot(listOf(JobTerrariumAdapter(controller)))
        val fish = snapshot.fish.single()

        assertEquals(TerrariumCreatureStatus.HEALTHY, fish.status)
        assertTrue(fish.health >= 70)
        assertTrue(fish.stress < 30)
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

private class FakeJobConfigRepository(
    private val configs: Map<String, JobConfig>
) : IJobConfigRepository {
    override fun findOneById(jobId: String): Optional<JobConfig> =
        Optional.ofNullable(configs[jobId])

    override fun findAll(): List<JobConfig> = configs.values.toList()
    override fun save(jobId: String, config: JobConfig): Unit = unsupported()
    override fun updateOneById(jobId: String, config: JobConfig): Unit = unsupported()
    override fun deleteOneById(jobId: String): Unit = unsupported()

    private fun unsupported(): Nothing {
        throw UnsupportedOperationException("Fake repository only supports read methods.")
    }
}

private class RecordingProcessManager : ProcessManager() {
    var readCount: Int = 0
        private set
    var lastProcessCount: Int = 0
        private set

    override fun getProcessTrees(): List<ProcessTreeNode> {
        readCount += 1
        return super.getProcessTrees().also { trees ->
            lastProcessCount = trees.sumOf(::countProcessTree)
        }
    }

    private fun countProcessTree(process: ProcessTreeNode): Int {
        return 1 + process.children.sumOf(::countProcessTree)
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
