package com.xstar.notebook.data.todo

/**
 * 内置分类系统与调色板。迁移 / 首次建库时据此预置数据（使用固定主键）。
 */
object TodoPresets {

    const val KEY_QUADRANT = "quadrant"
    const val KEY_STATE = "state"
    const val KEY_TYPE = "type"

    /** 固定系统 id（迁移 SQL 与代码保持一致）。 */
    const val SYSTEM_ID_QUADRANT = 1L
    const val SYSTEM_ID_STATE = 2L
    const val SYSTEM_ID_TYPE = 3L

    /** 固定分类 id 基准，避免与自增冲突：象限 101..，状态 201..，类型 301.. */
    const val CATEGORY_ID_BASE = 100L

    /** 内置类型分类 id。 */
    const val TYPE_STORY = 301L
    const val TYPE_REQUIREMENT = 302L
    const val TYPE_TASK = 303L

    val QUADRANT = SystemPreset(
        key = KEY_QUADRANT,
        name = "重要 × 紧急",
        categories = listOf(
            CategoryPreset("重要且紧急", 0xFFE5484D),
            CategoryPreset("重要不紧急", 0xFFF5A623),
            CategoryPreset("紧急不重要", 0xFF3E8ED0),
            CategoryPreset("不重要不紧急", 0xFF8A93A6),
        ),
    )

    val STATE = SystemPreset(
        key = KEY_STATE,
        name = "状态",
        categories = listOf(
            CategoryPreset("待办", 0xFF8A93A6),
            CategoryPreset("进行中", 0xFF3E8ED0),
            CategoryPreset("已阻塞", 0xFFE5484D),
            CategoryPreset("已完成", 0xFF00A896),
        ),
    )

    /** 类型系统：随层级概念使用的类型标签（故事 → 需求 → 任务）。 */
    val TYPE = SystemPreset(
        key = KEY_TYPE,
        name = "类型",
        categories = listOf(
            CategoryPreset("故事", 0xFF6B4CE0),
            CategoryPreset("需求", 0xFF3E8ED0),
            CategoryPreset("任务", 0xFF00A896),
        ),
    )

    val BUILT_INS: List<SystemPreset> = listOf(QUADRANT, STATE, TYPE)

    data class CategoryPreset(val name: String, val colorArgb: Long)

    data class SystemPreset(val key: String, val name: String, val categories: List<CategoryPreset>)

    /** 新建自定义分类时轮换使用的调色板。 */
    val PALETTE: List<Long> = listOf(
        0xFFE5484D,
        0xFFF5A623,
        0xFF3E8ED0,
        0xFF00A896,
        0xFF6B4CE0,
        0xFFEF5DA8,
        0xFF7C8B3F,
        0xFF8A93A6,
        0xFFF08C00,
        0xFFC242C2,
        0xFF2F9E44,
        0xFF1971C2,
    )
}
