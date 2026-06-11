package org.waste.of.time

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.phys.Vec3
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

object Utils {
    // Why cant I use the std lib?
    fun Boolean.toByte(): Byte = if (this) 1 else 0
    fun Vec3.asString() = "(%.2f, %.2f, %.2f)".format(x, y, z)

    /** 1.21.6+ serializes entities via ValueOutput; this collects the result into a CompoundTag. */
    fun Entity.saveToCompound(): CompoundTag {
        val out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess())
        saveWithoutId(out)
        return out.buildResult()
    }

    fun getTime(): String {
        val localDateTime = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault()

        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME

        return zonedDateTime.format(formatter)
    }

    fun Vec3.manhattanDistance2d(other: Vec3) =
        abs(this.x - other.x) + abs(this.z - other.z)

    fun Long.toReadableByteCount(si: Boolean = true): String {
        val unit = if (si) 1000 else 1024
        if (this < unit) return "$this B"
        val exp = (ln(toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", this / unit.toDouble().pow(exp.toDouble()), pre)
    }

    val BlockEntity.typeName: String
        get() = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)?.path ?: "unknown"
}
