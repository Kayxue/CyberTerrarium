package com.kay.cyberterrarium.jobmanagement.components

sealed interface GraphSelection {
    data class StageSelection(val stageId: String) : GraphSelection
    data class JobSelection(val jobId: String) : GraphSelection
    data class DependencySelection(val jobId: String, val upstreamJobId: String) : GraphSelection
}
