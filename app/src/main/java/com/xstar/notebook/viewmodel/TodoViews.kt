package com.xstar.notebook.viewmodel

import com.xstar.notebook.data.db.entity.CategoryEntity
import com.xstar.notebook.data.db.entity.TodoNodeEntity
import com.xstar.notebook.data.repo.TodoHubData
import com.xstar.notebook.data.repo.TodoHubRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class TodoDoneFilter(val label: String) {
    ALL("全部"),
    OPEN("待办"),
    DONE("已完成"),
    OVERDUE("已过期"),
}

data class TodoRow(
    val node: TodoNodeEntity,
    val tags: List<CategoryEntity>,
)

data class TodoGroup(
    val category: CategoryEntity,
    val rows: List<TodoRow>,
)

class TodoTagIndex private constructor(
    private val tagsByNode: Map<Long, List<CategoryEntity>>,
) {
    fun tagsOf(nodeId: Long): List<CategoryEntity> = tagsByNode[nodeId].orEmpty()

    companion object {
        fun build(data: TodoHubData): TodoTagIndex {
            val byId = data.categories.associateBy { it.id }
            val grouped = HashMap<Long, MutableList<CategoryEntity>>()
            data.links.forEach { link ->
                byId[link.categoryId]?.let { category ->
                    grouped.getOrPut(link.nodeId) { mutableListOf() }.add(category)
                }
            }
            grouped.values.forEach { it.sortWith(compareBy({ c -> c.systemId }, { c -> c.position }, { c -> c.id })) }
            return TodoTagIndex(grouped)
        }
    }
}

object TodoViews {
    private val zone: ZoneId
        get() = ZoneId.systemDefault()

    fun summary(data: TodoHubData): Pair<Int, Int> =
        data.nodes.count { !it.done } to data.nodes.count { it.done }

    fun groups(
        data: TodoHubData,
        systemId: Long?,
        categoryId: Long?,
        filter: TodoDoneFilter,
        now: Long = System.currentTimeMillis(),
    ): List<TodoGroup> {
        if (systemId == null) return emptyList()
        val index = TodoTagIndex.build(data)
        val categories = data.categoriesOf(systemId)
            .filter { categoryId == null || it.id == categoryId }
        val categoryIds = data.categoriesOf(systemId).mapTo(HashSet()) { it.id }
        val result = categories.map { category ->
            TodoGroup(
                category,
                data.nodes
                    .filter { node -> index.tagsOf(node.id).any { it.id == category.id } }
                    .filter { matchesFilter(it, filter, now) }
                    .sortedWith(TodoHubRepository.todoOrder)
                    .map { TodoRow(it, index.tagsOf(it.id)) },
            )
        }.toMutableList()

        if (categoryId == null) {
            val uncategorized = data.nodes
                .filter { node -> index.tagsOf(node.id).none { it.id in categoryIds } }
                .filter { matchesFilter(it, filter, now) }
                .sortedWith(TodoHubRepository.todoOrder)
                .map { TodoRow(it, index.tagsOf(it.id)) }
            result += TodoGroup(
                CategoryEntity(
                    id = -systemId,
                    systemId = systemId,
                    name = "未分类",
                    colorArgb = 0xFF8A93A6,
                    position = Int.MAX_VALUE,
                ),
                uncategorized,
            )
        }
        return result
    }

    fun rowsForDate(
        data: TodoHubData,
        date: LocalDate,
        systemId: Long?,
        categoryId: Long?,
        filter: TodoDoneFilter,
    ): List<TodoRow> = rows(data) { node ->
        coversDate(node, date) && matchesSelection(data, node, systemId, categoryId, filter)
    }

    fun overdueRows(
        data: TodoHubData,
        systemId: Long?,
        categoryId: Long?,
        now: Long = System.currentTimeMillis(),
    ): List<TodoRow> = rows(data) { node ->
        isOverdue(node, now) && matchesCategory(data, node, systemId, categoryId)
    }

    fun undatedRows(
        data: TodoHubData,
        systemId: Long?,
        categoryId: Long?,
        filter: TodoDoneFilter,
    ): List<TodoRow> = rows(data) { node ->
        node.startAt == null && node.dueAt == null &&
            matchesSelection(data, node, systemId, categoryId, filter)
    }

    fun dateColors(
        data: TodoHubData,
        month: YearMonth,
        systemId: Long?,
        categoryId: Long?,
        filter: TodoDoneFilter,
    ): Map<LocalDate, List<Long>> {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        val index = TodoTagIndex.build(data)
        val colors = HashMap<LocalDate, LinkedHashSet<Long>>()
        data.nodes.filter { matchesSelection(data, it, systemId, categoryId, filter) }
            .forEach { node ->
                val start = node.startAt?.let(::toDate) ?: node.dueAt?.let(::toDate) ?: return@forEach
                val end = node.dueAt?.let(::toDate) ?: start
                val nodeColors = if (systemId == null) {
                    emptyList()
                } else {
                    index.tagsOf(node.id)
                        .filter { it.systemId == systemId && (categoryId == null || it.id == categoryId) }
                        .map { it.colorArgb }
                }.ifEmpty { listOf(0xFF8A93A6) }
                var date = maxOf(minOf(start, end), first)
                val rangeEnd = minOf(maxOf(start, end), last)
                while (!date.isAfter(rangeEnd)) {
                    colors.getOrPut(date) { linkedSetOf() }.addAll(nodeColors)
                    date = date.plusDays(1)
                }
            }
        return colors.mapValues { (_, value) -> value.take(4) }
    }

    fun isOverdue(node: TodoNodeEntity, now: Long = System.currentTimeMillis()): Boolean =
        !node.done && node.dueAt != null && node.dueAt < now

    private fun rows(data: TodoHubData, predicate: (TodoNodeEntity) -> Boolean): List<TodoRow> {
        val index = TodoTagIndex.build(data)
        return data.nodes.filter(predicate)
            .sortedWith(TodoHubRepository.todoOrder)
            .map { TodoRow(it, index.tagsOf(it.id)) }
    }

    private fun coversDate(node: TodoNodeEntity, date: LocalDate): Boolean {
        val start = node.startAt?.let(::toDate) ?: node.dueAt?.let(::toDate) ?: return false
        val end = node.dueAt?.let(::toDate) ?: start
        val low = minOf(start, end)
        val high = maxOf(start, end)
        return !date.isBefore(low) && !date.isAfter(high)
    }

    private fun matchesSelection(
        data: TodoHubData,
        node: TodoNodeEntity,
        systemId: Long?,
        categoryId: Long?,
        filter: TodoDoneFilter,
    ): Boolean = matchesFilter(node, filter, System.currentTimeMillis()) &&
        matchesCategory(data, node, systemId, categoryId)

    private fun matchesCategory(
        data: TodoHubData,
        node: TodoNodeEntity,
        systemId: Long?,
        categoryId: Long?,
    ): Boolean {
        if (systemId == null || categoryId == null) return true
        return data.links.any { it.nodeId == node.id && it.categoryId == categoryId }
    }

    private fun matchesFilter(node: TodoNodeEntity, filter: TodoDoneFilter, now: Long): Boolean = when (filter) {
        TodoDoneFilter.ALL -> true
        TodoDoneFilter.OPEN -> !node.done
        TodoDoneFilter.DONE -> node.done
        TodoDoneFilter.OVERDUE -> isOverdue(node, now)
    }

    private fun toDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
}
