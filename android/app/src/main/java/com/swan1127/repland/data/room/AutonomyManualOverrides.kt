package com.swan1127.repland.data.room

import com.swan1127.repland.domain.model.TaskInputSource
import com.swan1127.repland.domain.model.TaskInputSources
import java.time.LocalDate
import org.json.JSONArray

/** Called inside the user's existing Room transaction, so manual intent wins over old capture hints. */
internal class AutonomyManualOverrides(private val database: ReplandDatabase) {
    private val workspace = database.planningWorkspaceDao()

    suspend fun placed(taskId: String, date: LocalDate) {
        val hadWindow = removeWindow(taskId)
        val held = heldTasks()
        if (taskId in held) saveHeld(held - taskId)
        val task = requireNotNull(database.taskDao().getById(taskId))
        if (hadWindow || task.scheduledForEpochDay?.let { it > date.toEpochDay() } == true) {
            val sources = TaskInputSources.decode(task.inputSources).copy(scheduledDate = TaskInputSource.USER_INPUT)
            database.taskDao().update(task.copy(scheduledForEpochDay = date.toEpochDay(), inputSources = sources.encode(),
                updatedAtEpochMillis = maxOf(System.currentTimeMillis(), task.updatedAtEpochMillis + 1)))
        }
    }

    suspend fun dateEdited(taskId: String) { removeWindow(taskId) }

    suspend fun removed(taskIds: Set<String>) {
        if (taskIds.isEmpty() || !hasAutonomyHistory()) return
        saveHeld(heldTasks() + taskIds)
    }

    private suspend fun hasAutonomyHistory(): Boolean = workspace.get(RoomAutonomyRepository.SETTINGS)?.payload == "true" ||
        workspace.get(RoomAutonomyRepository.LAST_REVISION) != null || workspace.get(RoomAutonomyRepository.WINDOWS) != null ||
        workspace.get(RoomAutonomyRepository.UNDO) != null || workspace.get(RoomAutonomyRepository.RECEIPT) != null

    private suspend fun heldTasks(): Set<String> = workspace.get(RoomAutonomyRepository.HELD)?.let { row ->
        JSONArray(row.payload).let { array -> (0 until array.length()).map(array::getString).toSet() }
    }.orEmpty()

    private suspend fun saveHeld(ids: Set<String>) {
        if (ids.isEmpty()) workspace.remove(RoomAutonomyRepository.HELD)
        else workspace.put(PlanningWorkspaceEntity(RoomAutonomyRepository.HELD, JSONArray(ids.sorted()).toString()))
    }

    private suspend fun removeWindow(taskId: String): Boolean {
        val row = workspace.get(RoomAutonomyRepository.WINDOWS) ?: return false
        val windows = JSONArray(row.payload)
        val retained = (0 until windows.length()).map(windows::getJSONObject).filterNot { it.getString("task") == taskId }
        if (retained.size == windows.length()) return false
        if (retained.isEmpty()) workspace.remove(RoomAutonomyRepository.WINDOWS)
        else workspace.put(PlanningWorkspaceEntity(RoomAutonomyRepository.WINDOWS, JSONArray(retained).toString()))
        return true
    }
}
