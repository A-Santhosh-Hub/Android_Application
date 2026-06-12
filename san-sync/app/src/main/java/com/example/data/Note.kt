package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val isChecklist: Boolean = false,
    val colorIndex: Int = 0, // Maps to Keep's light/dark background colors
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderTime: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val labels: String = "", // Comma-separated labels applied to this note, eg "Work,College"
    val drawingData: String? = null // JSON string representation of drawing/sketch vector strokes
) {
    // Helper to extract checklist items from contents column
    fun getChecklistItems(): List<ChecklistItem> {
        if (!isChecklist || content.isBlank()) return emptyList()
        return try {
            val array = JSONArray(content)
            val list = mutableListOf<ChecklistItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChecklistItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isChecked = obj.optBoolean("isChecked", false),
                        category = obj.optString("category", "General"),
                        time = obj.optString("time", ""),
                        reminderOffsetMinutes = obj.optInt("reminderOffsetMinutes", 0),
                        isRoutine = obj.optBoolean("isRoutine", false),
                        routineDays = obj.optString("routineDays", ""),
                        lastCheckedDate = obj.optString("lastCheckedDate", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            // Fallback for simple multiline text converted to list
            content.split("\n")
                .filter { it.isNotBlank() }
                .map { line ->
                    ChecklistItem(
                        id = UUID.randomUUID().toString(),
                        text = line,
                        isChecked = false,
                        category = "General",
                        time = "",
                        reminderOffsetMinutes = 0
                    )
                }
        }
    }

    // Helper to extract drawing strokes
    fun getDrawingStrokes(): List<DrawingStroke> {
        if (drawingData.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(drawingData)
            val strokes = mutableListOf<DrawingStroke>()
            for (i in 0 until array.length()) {
                val strokeObj = array.getJSONObject(i)
                val colorHex = strokeObj.optString("colorHex", "#000000")
                val width = strokeObj.optDouble("width", 5.0).toFloat()
                val pointsArray = strokeObj.optJSONArray("points") ?: JSONArray()
                val points = mutableListOf<DrawingPoint>()
                for (j in 0 until pointsArray.length()) {
                    val ptObj = pointsArray.getJSONObject(j)
                    points.add(
                        DrawingPoint(
                            x = ptObj.optDouble("x", 0.0).toFloat(),
                            y = ptObj.optDouble("y", 0.0).toFloat()
                        )
                    )
                }
                strokes.add(DrawingStroke(colorHex, width, points))
            }
            strokes
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false,
    val category: String = "General",
    val time: String = "", // e.g. "06:00 AM" or "18:00"
    val reminderOffsetMinutes: Int = 0, // 0 for none, 3, 5, 10
    val isRoutine: Boolean = false,
    val routineDays: String = "", // empty = Mon-Sun (or select days like "Mon,Tue")
    val lastCheckedDate: String = "" // "YYYY-MM-DD" of when it was last checked
) {
    companion object {
        fun listToJson(items: List<ChecklistItem>): String {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("text", item.text)
                obj.put("isChecked", item.isChecked)
                obj.put("category", item.category)
                obj.put("time", item.time)
                obj.put("reminderOffsetMinutes", item.reminderOffsetMinutes)
                obj.put("isRoutine", item.isRoutine)
                obj.put("routineDays", item.routineDays)
                obj.put("lastCheckedDate", item.lastCheckedDate)
                array.put(obj)
            }
            return array.toString()
        }
    }
}

data class DrawingPoint(val x: Float, val y: Float)

data class DrawingStroke(
    val colorHex: String = "#3F51B5",
    val width: Float = 5f,
    val points: List<DrawingPoint> = emptyList()
) {
    companion object {
        fun listToJson(strokes: List<DrawingStroke>): String {
            val array = JSONArray()
            for (stroke in strokes) {
                val strokeObj = JSONObject()
                strokeObj.put("colorHex", stroke.colorHex)
                strokeObj.put("width", stroke.width)
                val pointsArray = JSONArray()
                for (pt in stroke.points) {
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x)
                    ptObj.put("y", pt.y)
                    pointsArray.put(ptObj)
                }
                strokeObj.put("points", pointsArray)
                array.put(strokeObj)
            }
            return array.toString()
        }
    }
}
