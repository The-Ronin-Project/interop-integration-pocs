package com.projectronin.interop.completeness

fun main() {
    val dag = buildDag()

    println("Processing Graph:")
    dag.forEach { (vertex, edges) ->
        println("${vertex.name} -> ${edges.joinToString(", ") { it.destination.name }}")
    }
    println()

    val run = loadRun()
    val loadData = getLoads()

    val initialLoads =
        run.initialResources.map { initial -> loadData.single { it.sourceResource == initial && it.targetResourceType == initial.type } }
    initialLoads.forEach { initialLoad ->
        val initial = initialLoad.successes.single()
        val status = checkResourcesAgainstDag(dag, loadData, initial, initialLoad.targetResourceType)
        println("$initial = $status")
    }
}

fun buildDag(): Map<Vertex, List<Edge>> {
    val dagData = loadDagData()

    val dag = mutableMapOf<Vertex, MutableList<Edge>>()
    dagData.forEach { (resource, parents) ->
        val resourceVertex = Vertex(resource)

        parents.forEach { parent ->
            val parentVertex = Vertex(parent)
            val edge = Edge(parentVertex, resourceVertex)
            dag.computeIfAbsent(parentVertex) { mutableListOf() }.add(edge)
        }
    }
    return dag
}

data class Vertex(val name: String)

data class Edge(val source: Vertex, val destination: Vertex)

fun loadDagData(): Map<String, List<String>> {
    val dagData = mutableMapOf<String, List<String>>()
    dagData["Condition"] = listOf("Patient")
    dagData["Observation"] = listOf("Patient", "Condition")
    dagData["Patient"] = listOf()
    return dagData
}

fun loadRun(): Run {
    return Run(
        id = "run1",
        initialResources = listOf(
            Resource("Patient", "1234"),
            Resource("Patient", "5678")
        )
    )
}

fun getLoads(): List<Load> {
    val patient1 = Load(
        runId = "run1",
        sourceResource = Resource("Patient", "1234"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Patient",
        successes = listOf(Resource("Patient", "1234"))
    )
    val patient2 = Load(
        runId = "run1",
        sourceResource = Resource("Patient", "5678"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Patient",
        successes = listOf(Resource("Patient", "5678"))
    )

    val condition1 = Load(
        runId = "run1",
        sourceResource = Resource("Patient", "1234"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Condition",
        successes = listOf(Resource("Condition", "1234"))
    )
    val condition2 = Load(
        runId = "run1",
        sourceResource = Resource("Patient", "1234"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Condition",
        successes = listOf(Resource("Condition", "5678"))
    )
    val condition3 = Load(
        runId = "run1",
        sourceResource = Resource("Patient", "5678"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Condition",
        successes = listOf()
    )

    val observation1 = Load(
        runId = "run1",
        sourceResource = Resource("Patient", "1234"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Observation",
        successes = listOf(Resource("Observation", "1234"))
    )
    val observation2 = Load(
        runId = "run1",
        sourceResource = Resource("Patient", "5678"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Observation",
        successes = listOf(Resource("Observation", "5678"))
    )
    val observation3 = Load(
        runId = "run1",
        sourceResource = Resource("Condition", "1234"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Observation",
        successes = listOf(Resource("Observation", "1357"))
    )
    val observation4 = Load(
        runId = "run1",
        sourceResource = Resource("Condition", "5678"),
        resultStatus = ResultStatus.SUCCESS,
        targetResourceType = "Observation",
        successes = listOf()
    )

    return listOf(
        patient1,
        patient2,
        condition1,
        condition2,
        condition3,
        observation1,
        observation2,
        observation3,
        observation4
    )
}

fun getLoadsForResource(loads: List<Load>, resource: Resource): List<Load> =
    loads.filter { it.sourceResource == resource }

fun checkResourcesAgainstDag(
    dag: Map<Vertex, List<Edge>>,
    loads: List<Load>,
    resource: Resource,
    runInitialLoadType: String
): RunStatus {
    val edges = dag[Vertex(resource.type)] ?: emptyList()
    if (edges.isEmpty()) {
        return RunStatus.SUCCESS
    }

    val resourceLoads = getLoadsForResource(loads, resource).filter { it.targetResourceType != runInitialLoadType }
    val unencounteredEdges = edges.map { it.destination.name }.toMutableList()
    resourceLoads.forEach { load ->
        unencounteredEdges.remove(load.targetResourceType)

        if (load.resultStatus != ResultStatus.SUCCESS) {
            println("$load was not success")
            return when (load.resultStatus) {
                ResultStatus.FAILURE -> RunStatus.FAILED
                else -> RunStatus.ERROR
            }
        }

        load.successes.forEach { childResource ->
            val dagStatus = checkResourcesAgainstDag(dag, loads, childResource, runInitialLoadType)
            if (dagStatus != RunStatus.SUCCESS) {
                println("DAG failed for $childResource")
                return dagStatus
            }
        }
    }

    if (unencounteredEdges.isNotEmpty()) {
        println("$resource contained no loads for the following expected edges: ${unencounteredEdges.joinToString(", ")}")
        return RunStatus.INCOMPLETE
    }

    // Nothing failed, so we're good.
    return RunStatus.SUCCESS
}

data class Run(
    val id: String,
    val initialResources: List<Resource>
)

data class Resource(val type: String, val id: String)

data class Load(
    val runId: String,
    val sourceResource: Resource,
    val resultStatus: ResultStatus,
    val targetResourceType: String,
    val successes: List<Resource> = emptyList(),
    val failures: List<Resource> = emptyList()
)

enum class ResultStatus {
    SUCCESS,
    FAILURE,
    ERROR
}

enum class RunStatus {
    SUCCESS,
    FAILED,
    ERROR,
    INCOMPLETE;
}
