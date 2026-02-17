package com.marcportabella.immichuploader.domain

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

data class LocalIntakeFile(
    val name: String,
    val type: String,
    val size: Long,
    val lastModifiedEpochMillis: Long,
    val sourceFile: PlatformFile? = null,
    val previewUrl: String?,
    val captureDateTime: String? = null,
    val timeZone: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val exifMetadata: Map<String, String> = emptyMap(),
    val exifSummary: String? = null
)

data class ParsedExifMetadata(
    val captureDateTime: String?,
    val timeZone: String?,
    val cameraMake: String?,
    val cameraModel: String?,
    val metadata: Map<String, String>
)

fun mapLocalIntakeFilesToAssets(files: List<LocalIntakeFile>): List<LocalAsset> =
    files.mapIndexed { index, file ->
        val normalizedType = file.type.ifBlank { "application/octet-stream" }
        LocalAsset(
            id = LocalAssetId(
                "local-${file.name}-${file.size}-${file.lastModifiedEpochMillis}-$index"
            ),
            fileName = file.name,
            mimeType = normalizedType,
            fileSizeBytes = file.size,
            previewUrl = file.previewUrl,
            sourceFile = file.sourceFile,
            captureDateTime = file.captureDateTime,
            timeZone = file.timeZone,
            cameraMake = file.cameraMake,
            cameraModel = file.cameraModel,
            exifMetadata = file.exifMetadata,
            exifSummary = file.exifSummary
        )
    }

fun parseJpegExifMetadata(bytes: ByteArray): ParsedExifMetadata? {
    if (bytes.size < 4 || bytes[0].u8() != 0xFF || bytes[1].u8() != 0xD8) return null

    var offset = 2
    while (offset + 4 <= bytes.size) {
        if (bytes[offset].u8() != 0xFF) {
            offset += 1
            continue
        }

        val marker = bytes[offset + 1].u8()
        if (marker == 0xD9 || marker == 0xDA) break
        if (offset + 4 > bytes.size) break

        val segmentLength = ((bytes[offset + 2].u8() shl 8) or bytes[offset + 3].u8())
        if (segmentLength < 2 || offset + 2 + segmentLength > bytes.size) break

        if (marker == 0xE1 && segmentLength >= 8) {
            val exifStart = offset + 4
            val signatureMatches =
                bytes[exifStart].u8() == 0x45 &&
                    bytes[exifStart + 1].u8() == 0x78 &&
                    bytes[exifStart + 2].u8() == 0x69 &&
                    bytes[exifStart + 3].u8() == 0x66 &&
                    bytes[exifStart + 4].u8() == 0x00 &&
                    bytes[exifStart + 5].u8() == 0x00
            if (signatureMatches) {
                return parseTiffExif(
                    bytes = bytes,
                    tiffStart = exifStart + 6,
                    tiffLength = segmentLength - 8
                )
            }
        }

        offset += 2 + segmentLength
    }

    return null
}

private fun parseTiffExif(
    bytes: ByteArray,
    tiffStart: Int,
    tiffLength: Int
): ParsedExifMetadata? {
    if (tiffLength < 8 || tiffStart < 0 || tiffStart + tiffLength > bytes.size) return null

    val littleEndian = when {
        bytes[tiffStart].u8() == 0x49 && bytes[tiffStart + 1].u8() == 0x49 -> true
        bytes[tiffStart].u8() == 0x4D && bytes[tiffStart + 1].u8() == 0x4D -> false
        else -> return null
    }
    val reader = ExifReader(bytes, littleEndian)
    if (reader.u16(tiffStart + 2) != 42) return null
    val ifd0Offset = reader.u32(tiffStart + 4) ?: return null

    val ifd0 = parseIfd(
        bytes = bytes,
        reader = reader,
        tiffStart = tiffStart,
        tiffLength = tiffLength,
        ifdOffset = ifd0Offset
    )
    val exifIfdOffset = ifd0.pointers[EXIF_IFD_POINTER_TAG]
    val exifIfd = exifIfdOffset?.let {
        parseIfd(
            bytes = bytes,
            reader = reader,
            tiffStart = tiffStart,
            tiffLength = tiffLength,
            ifdOffset = it
        )
    }
    val gpsIfdOffset = ifd0.pointers[GPS_INFO_POINTER_TAG]
    val gpsIfd = gpsIfdOffset?.let {
        parseIfd(
            bytes = bytes,
            reader = reader,
            tiffStart = tiffStart,
            tiffLength = tiffLength,
            ifdOffset = it
        )
    }

    val dateTimeOriginalRaw = exifIfd?.values?.get(DATE_TIME_ORIGINAL_TAG) ?: ifd0.values[DATE_TIME_TAG]
    val timezoneRawFromOffsetTags =
        exifIfd?.values?.get(OFFSET_TIME_ORIGINAL_TAG) ?: exifIfd?.values?.get(OFFSET_TIME_TAG)
    val timezoneRawFromTimeZoneOffsetTag =
        exifIfd?.values?.get(TIME_ZONE_OFFSET_TAG) ?: ifd0.values[TIME_ZONE_OFFSET_TAG]
    val timezoneRawFromGps =
        inferTimeZoneOffsetFromGps(
            dateTimeOriginal = dateTimeOriginalRaw,
            gpsDateStamp = gpsIfd?.values?.get(GPS_DATE_STAMP_TAG),
            gpsTimeStamp = gpsIfd?.values?.get(GPS_TIME_STAMP_TAG)
        )
    val cameraMake = ifd0.values[MAKE_TAG]
    val cameraModel = ifd0.values[MODEL_TAG]
    val parsedDateTimeOriginal = dateTimeOriginalRaw?.let(::parseExifDateTimeAndOffset)

    val metadata = linkedMapOf<String, String>()
    exifIfd?.values?.get(DATE_TIME_DIGITIZED_TAG)?.let { metadata["dateTimeDigitized"] = normalizeExifDateTime(it) ?: it }
    exifIfd?.values?.get(LENS_MODEL_TAG)?.let { metadata["lensModel"] = it }
    exifIfd?.values?.get(ISO_SPEED_TAG)?.let { metadata["iso"] = it }
    exifIfd?.values?.get(EXPOSURE_TIME_TAG)?.let { metadata["exposureTime"] = it }
    exifIfd?.values?.get(F_NUMBER_TAG)?.let { metadata["fNumber"] = it }
    exifIfd?.values?.get(FOCAL_LENGTH_TAG)?.let { metadata["focalLengthMm"] = it }
    ifd0.values[SOFTWARE_TAG]?.let { metadata["software"] = it }
    gpsIfd?.values?.get(GPS_DATE_STAMP_TAG)?.let { metadata["gpsDateStamp"] = it }
    gpsIfd?.values?.get(GPS_TIME_STAMP_TAG)?.let { metadata["gpsTimeStamp"] = it }

    val hasUsefulData =
        dateTimeOriginalRaw != null ||
            timezoneRawFromOffsetTags != null ||
            timezoneRawFromTimeZoneOffsetTag != null ||
            timezoneRawFromGps != null ||
            parsedDateTimeOriginal?.second != null ||
            cameraMake != null ||
            cameraModel != null ||
            metadata.isNotEmpty()

    if (!hasUsefulData) return null

    return ParsedExifMetadata(
        captureDateTime = parsedDateTimeOriginal?.first ?: dateTimeOriginalRaw?.let(::normalizeExifDateTime) ?: dateTimeOriginalRaw,
        timeZone = timezoneRawFromOffsetTags?.let(::normalizeTimeZoneOffset)
            ?: timezoneRawFromTimeZoneOffsetTag?.let(::normalizeTimeZoneOffsetHours)
            ?: parsedDateTimeOriginal?.second
            ?: timezoneRawFromGps
            ?: timezoneRawFromOffsetTags
            ?: timezoneRawFromTimeZoneOffsetTag,
        cameraMake = cameraMake,
        cameraModel = cameraModel,
        metadata = metadata
    )
}

private data class ParsedIfd(
    val values: Map<Int, String>,
    val pointers: Map<Int, Int>
)

private fun parseIfd(
    bytes: ByteArray,
    reader: ExifReader,
    tiffStart: Int,
    tiffLength: Int,
    ifdOffset: Int
): ParsedIfd {
    val ifdStart = tiffStart + ifdOffset
    if (ifdOffset < 0 || ifdStart + 2 > tiffStart + tiffLength || ifdStart + 2 > bytes.size) {
        return ParsedIfd(emptyMap(), emptyMap())
    }

    val entryCount = reader.u16(ifdStart) ?: return ParsedIfd(emptyMap(), emptyMap())
    val values = mutableMapOf<Int, String>()
    val pointers = mutableMapOf<Int, Int>()
    val dataEnd = tiffStart + tiffLength

    for (entryIndex in 0 until entryCount) {
        val entryOffset = ifdStart + 2 + (entryIndex * 12)
        if (entryOffset + 12 > dataEnd || entryOffset + 12 > bytes.size) break

        val tag = reader.u16(entryOffset) ?: continue
        val type = reader.u16(entryOffset + 2) ?: continue
        val count = reader.u32(entryOffset + 4) ?: continue
        val typeSize = exifTypeSize(type) ?: continue
        val byteCount = count.toLong() * typeSize.toLong()
        if (byteCount <= 0L || byteCount > Int.MAX_VALUE) continue

        val valueOffset = if (byteCount <= 4L) {
            entryOffset + 8
        } else {
            val relative = reader.u32(entryOffset + 8) ?: continue
            tiffStart + relative
        }
        val valueEnd = valueOffset + byteCount.toInt()
        if (valueOffset < tiffStart || valueEnd > dataEnd || valueEnd > bytes.size) continue

        val parsedValue = readExifValueAsString(
            bytes = bytes,
            reader = reader,
            type = type,
            count = count,
            valueOffset = valueOffset
        )
        if (!parsedValue.isNullOrBlank()) {
            values[tag] = parsedValue
        }

        if (tag == EXIF_IFD_POINTER_TAG || tag == GPS_INFO_POINTER_TAG) {
            val pointer = readExifPointerValue(reader, type, valueOffset)
            if (pointer != null) {
                pointers[tag] = pointer
            }
        }
    }

    return ParsedIfd(values = values, pointers = pointers)
}

private fun readExifValueAsString(
    bytes: ByteArray,
    reader: ExifReader,
    type: Int,
    count: Int,
    valueOffset: Int
): String? =
    when (type) {
        EXIF_TYPE_ASCII -> {
            if (count <= 0) return null
            val raw = bytes.decodeToString(
                startIndex = valueOffset,
                endIndex = valueOffset + count
            ).substringBefore('\u0000').trim()
            raw.ifEmpty { null }
        }

        EXIF_TYPE_SHORT -> {
            val value = reader.u16(valueOffset) ?: return null
            value.toString()
        }

        EXIF_TYPE_SSHORT -> {
            val value = reader.s16(valueOffset) ?: return null
            value.toString()
        }

        EXIF_TYPE_LONG -> {
            val value = reader.u32(valueOffset) ?: return null
            value.toString()
        }

        EXIF_TYPE_RATIONAL -> {
            if (count <= 0) return null
            val values = (0 until count).mapNotNull { index ->
                val componentOffset = valueOffset + (index * 8)
                val numerator = reader.u32(componentOffset) ?: return@mapNotNull null
                val denominator = reader.u32(componentOffset + 4) ?: return@mapNotNull null
                if (denominator == 0) return@mapNotNull null
                formatRationalValue(numerator, denominator)
            }
            values.takeIf { it.isNotEmpty() }?.joinToString(":")
        }

        else -> null
    }

private fun readExifPointerValue(
    reader: ExifReader,
    type: Int,
    valueOffset: Int
): Int? =
    when (type) {
        EXIF_TYPE_LONG -> reader.u32(valueOffset)
        EXIF_TYPE_SHORT -> reader.u16(valueOffset)
        else -> null
    }

private fun exifTypeSize(type: Int): Int? =
    when (type) {
        EXIF_TYPE_ASCII -> 1
        EXIF_TYPE_SHORT -> 2
        EXIF_TYPE_SSHORT -> 2
        EXIF_TYPE_LONG -> 4
        EXIF_TYPE_RATIONAL -> 8
        else -> null
    }

private fun formatRationalValue(numerator: Int, denominator: Int): String {
    val value = numerator.toDouble() / denominator.toDouble()
    val rounded = (value * 10.0).toInt() / 10.0
    return if (value < 1.0 && numerator != 0) {
        "1/${(1.0 / value).toInt().coerceAtLeast(1)}"
    } else if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun normalizeExifDateTime(value: String): String? {
    val trimmed = value.trim()
    val match = Regex("""^(\d{4})[:\-](\d{2})[:\-](\d{2})[ T](\d{2}):(\d{2}):(\d{2})$""")
        .matchEntire(trimmed)
        ?: return null

    val normalized = "${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]}T" +
        "${match.groupValues[4]}:${match.groupValues[5]}:${match.groupValues[6]}"
    return runCatching { LocalDateTime.parse(normalized) }.getOrNull()?.toString()
}

private fun parseExifDateTimeAndOffset(value: String): Pair<String, String?>? {
    val trimmed = value.trim()
    val match = Regex(
        """^(\d{4})[:\-](\d{2})[:\-](\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\s*(Z|[+-]\d{2}:?\d{2}))?$"""
    ).matchEntire(trimmed) ?: return null

    val normalizedDateTime = "${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]}T" +
        "${match.groupValues[4]}:${match.groupValues[5]}:${match.groupValues[6]}"
    val parsedDateTime = runCatching { LocalDateTime.parse(normalizedDateTime) }.getOrNull() ?: return null
    val rawOffset = match.groupValues[7].ifBlank { null }
    return parsedDateTime.toString() to rawOffset?.let(::normalizeTimeZoneOffset)
}

private fun normalizeTimeZoneOffsetHours(value: String): String? {
    val hours = value.trim().toIntOrNull() ?: return null
    if (hours !in -23..23) return null
    val sign = if (hours >= 0) "+" else "-"
    val hh = kotlin.math.abs(hours).toString().padStart(2, '0')
    return "$sign$hh:00"
}

private fun inferTimeZoneOffsetFromGps(
    dateTimeOriginal: String?,
    gpsDateStamp: String?,
    gpsTimeStamp: String?
): String? {
    val localDateTime = parseExifDateTimeAndOffset(dateTimeOriginal ?: return null)
        ?.first
        ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        ?: return null
    val gpsUtcDateTime = parseGpsUtcDateTime(gpsDateStamp ?: return null, gpsTimeStamp ?: return null) ?: return null

    val localEpochMinutes = localDateTime.toInstant(TimeZone.UTC).epochSeconds / 60
    val gpsEpochMinutes = gpsUtcDateTime.toInstant(TimeZone.UTC).epochSeconds / 60
    var diff = (localEpochMinutes - gpsEpochMinutes).toInt()

    while (diff < -14 * 60) diff += 24 * 60
    while (diff > 14 * 60) diff -= 24 * 60
    if (diff !in (-14 * 60)..(14 * 60)) return null

    val sign = if (diff >= 0) "+" else "-"
    val absMinutes = kotlin.math.abs(diff)
    val hours = (absMinutes / 60).toString().padStart(2, '0')
    val minutes = (absMinutes % 60).toString().padStart(2, '0')
    return "$sign$hours:$minutes"
}

private fun parseGpsUtcDateTime(dateStamp: String, timeStamp: String): LocalDateTime? {
    val date = Regex("""^(\d{4})[:\-](\d{2})[:\-](\d{2})$""").matchEntire(dateStamp.trim()) ?: return null
    val parts = timeStamp.split(':')
    if (parts.size < 3) return null
    val hour = parts[0].substringBefore('/').toIntOrNull() ?: return null
    val minute = parts[1].substringBefore('/').toIntOrNull() ?: return null
    val second = parts[2].substringBefore('/').toIntOrNull() ?: return null
    val localDateTime = buildString {
        append(date.groupValues[1])
        append('-')
        append(date.groupValues[2])
        append('-')
        append(date.groupValues[3])
        append('T')
        append(hour.toString().padStart(2, '0'))
        append(':')
        append(minute.toString().padStart(2, '0'))
        append(':')
        append(second.toString().padStart(2, '0'))
    }
    return runCatching { LocalDateTime.parse(localDateTime) }.getOrNull()
}

internal fun normalizeTimeZoneOffset(value: String): String? {
    val trimmed = value.trim()
    if (trimmed == "Z") return "Z"
    val normalizedInput = Regex("""^([+-])(\d{2})(\d{2})$""")
        .matchEntire(trimmed)
        ?.let { "${it.groupValues[1]}${it.groupValues[2]}:${it.groupValues[3]}" }
        ?: trimmed
    val offset = runCatching { UtcOffset.parse(normalizedInput) }.getOrNull() ?: return null
    return offset.toString()
}

private class ExifReader(
    private val bytes: ByteArray,
    private val littleEndian: Boolean
) {
    fun u16(offset: Int): Int? {
        if (offset < 0 || offset + 1 >= bytes.size) return null
        val a = bytes[offset].u8()
        val b = bytes[offset + 1].u8()
        return if (littleEndian) {
            a or (b shl 8)
        } else {
            (a shl 8) or b
        }
    }

    fun u32(offset: Int): Int? {
        if (offset < 0 || offset + 3 >= bytes.size) return null
        val a = bytes[offset].u8()
        val b = bytes[offset + 1].u8()
        val c = bytes[offset + 2].u8()
        val d = bytes[offset + 3].u8()
        return if (littleEndian) {
            a or (b shl 8) or (c shl 16) or (d shl 24)
        } else {
            (a shl 24) or (b shl 16) or (c shl 8) or d
        }
    }

    fun s16(offset: Int): Int? {
        val unsigned = u16(offset) ?: return null
        return if (unsigned and 0x8000 != 0) unsigned - 0x10000 else unsigned
    }
}

private fun Byte.u8(): Int = toInt() and 0xFF

private const val MAKE_TAG = 0x010F
private const val MODEL_TAG = 0x0110
private const val SOFTWARE_TAG = 0x0131
private const val DATE_TIME_TAG = 0x0132
private const val EXIF_IFD_POINTER_TAG = 0x8769
private const val GPS_INFO_POINTER_TAG = 0x8825

private const val DATE_TIME_ORIGINAL_TAG = 0x9003
private const val DATE_TIME_DIGITIZED_TAG = 0x9004
private const val OFFSET_TIME_TAG = 0x9010
private const val OFFSET_TIME_ORIGINAL_TAG = 0x9011
private const val TIME_ZONE_OFFSET_TAG = 0x882A
private const val GPS_TIME_STAMP_TAG = 0x0007
private const val GPS_DATE_STAMP_TAG = 0x001D
private const val ISO_SPEED_TAG = 0x8827
private const val EXPOSURE_TIME_TAG = 0x829A
private const val F_NUMBER_TAG = 0x829D
private const val FOCAL_LENGTH_TAG = 0x920A
private const val LENS_MODEL_TAG = 0xA434

private const val EXIF_TYPE_ASCII = 2
private const val EXIF_TYPE_SHORT = 3
private const val EXIF_TYPE_SSHORT = 8
private const val EXIF_TYPE_LONG = 4
private const val EXIF_TYPE_RATIONAL = 5
