package com.marcportabella.immichuploader

import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.parseJpegExifMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UploadExifSliceWebTest {

    @Test
    fun localIntakeMapperBuildsAssetsWithPreviewMetadata() {
        val previewBytes = byteArrayOf(1, 2, 3)
        val assets = mapLocalIntakeFilesToAssets(
            listOf(
                LocalIntakeFile(
                    name = "photo.jpg",
                    type = "image/jpeg",
                    size = 123,
                    lastModifiedEpochMillis = 1_700_000_000_000,
                    previewUrl = "blob:preview-1",
                    previewBytes = previewBytes,
                    captureDateTime = "2026-01-02T03:04:05",
                    timeZone = "+01:00",
                    cameraMake = "Canon",
                    cameraModel = "R6",
                    exifMetadata = mapOf("iso" to "200")
                ),
                LocalIntakeFile(
                    name = "unknown.bin",
                    type = "",
                    size = 99,
                    lastModifiedEpochMillis = 1_700_000_000_100,
                    previewUrl = null
                )
            )
        )

        assertEquals(2, assets.size)
        assertEquals("photo.jpg", assets[0].fileName)
        assertEquals("image/jpeg", assets[0].mimeType)
        assertEquals("blob:preview-1", assets[0].previewUrl)
        assertEquals(previewBytes.toList(), assets[0].previewBytes?.toList())
        assertEquals("2026-01-02T03:04:05", assets[0].captureDateTime)
        assertEquals("+01:00", assets[0].timeZone)
        assertEquals("Canon", assets[0].cameraMake)
        assertEquals("R6", assets[0].cameraModel)
        assertEquals("200", assets[0].exifMetadata["iso"])
        assertEquals("application/octet-stream", assets[1].mimeType)
        assertNull(assets[1].previewUrl)
        assertTrue(assets[0].id.value.startsWith("local-photo.jpg-123-"))
    }

    @Test
    fun jpegExifParserExtractsDateTimeTimezoneAndCameraFields() {
        val jpegBytes = buildJpegWithExif(
            entries = listOf(
                exifAsciiEntry(MAKE_TAG, "Canon"),
                exifAsciiEntry(MODEL_TAG, "EOS R6"),
                exifLongEntry(EXIF_IFD_POINTER_TAG, 50)
            ),
            exifEntries = listOf(
                exifAsciiEntry(DATE_TIME_ORIGINAL_TAG, "2026:01:02 03:04:05"),
                exifAsciiEntry(OFFSET_TIME_ORIGINAL_TAG, "+0100"),
                exifShortEntry(ISO_SPEED_TAG, 320),
                exifRationalEntry(F_NUMBER_TAG, 28, 10)
            )
        )

        val parsed = parseJpegExifMetadata(jpegBytes)

        assertNotNull(parsed)
        assertEquals("2026-01-02T03:04:05", parsed.captureDateTime)
        assertEquals("+01:00", parsed.timeZone)
        assertEquals("Canon", parsed.cameraMake)
        assertEquals("EOS R6", parsed.cameraModel)
        assertEquals("320", parsed.metadata["iso"])
        assertEquals("2.8", parsed.metadata["fNumber"])
    }

    @Test
    fun jpegExifParserExtractsTimezoneFromDateTimeOriginalOffsetSuffix() {
        val jpegBytes = buildJpegWithExif(
            entries = listOf(
                exifAsciiEntry(MAKE_TAG, "Sony"),
                exifLongEntry(EXIF_IFD_POINTER_TAG, 50)
            ),
            exifEntries = listOf(
                exifAsciiEntry(DATE_TIME_ORIGINAL_TAG, "2026:01:02 03:04:05+0530")
            )
        )

        val parsed = parseJpegExifMetadata(jpegBytes)

        assertNotNull(parsed)
        assertEquals("2026-01-02T03:04:05", parsed.captureDateTime)
        assertEquals("+05:30", parsed.timeZone)
    }

    @Test
    fun jpegExifParserExtractsTimezoneFromTimeZoneOffsetTag() {
        val jpegBytes = buildJpegWithExif(
            entries = listOf(
                exifLongEntry(EXIF_IFD_POINTER_TAG, 50)
            ),
            exifEntries = listOf(
                exifAsciiEntry(DATE_TIME_ORIGINAL_TAG, "2026:01:02 03:04:05"),
                exifSignedShortEntry(TIME_ZONE_OFFSET_TAG, -5)
            )
        )

        val parsed = parseJpegExifMetadata(jpegBytes)

        assertNotNull(parsed)
        assertEquals("2026-01-02T03:04:05", parsed.captureDateTime)
        assertEquals("-05:00", parsed.timeZone)
    }
}

private data class ExifEntry(
    val tag: Int,
    val type: Int,
    val count: Int,
    val valueBytes: ByteArray
)

private fun exifAsciiEntry(tag: Int, value: String): ExifEntry {
    val bytes = (value + '\u0000').encodeToByteArray()
    return ExifEntry(tag = tag, type = 2, count = bytes.size, valueBytes = bytes)
}

private fun exifShortEntry(tag: Int, value: Int): ExifEntry =
    ExifEntry(
        tag = tag,
        type = 3,
        count = 1,
        valueBytes = byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())
    )

private fun exifSignedShortEntry(tag: Int, value: Int): ExifEntry {
    val raw = value and 0xFFFF
    return ExifEntry(
        tag = tag,
        type = 8,
        count = 1,
        valueBytes = byteArrayOf((raw and 0xFF).toByte(), ((raw shr 8) and 0xFF).toByte())
    )
}

private fun exifLongEntry(tag: Int, value: Int): ExifEntry =
    ExifEntry(
        tag = tag,
        type = 4,
        count = 1,
        valueBytes = byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    )

private fun exifRationalEntry(tag: Int, numerator: Int, denominator: Int): ExifEntry =
    ExifEntry(
        tag = tag,
        type = 5,
        count = 1,
        valueBytes = byteArrayOf(
            (numerator and 0xFF).toByte(),
            ((numerator shr 8) and 0xFF).toByte(),
            ((numerator shr 16) and 0xFF).toByte(),
            ((numerator shr 24) and 0xFF).toByte(),
            (denominator and 0xFF).toByte(),
            ((denominator shr 8) and 0xFF).toByte(),
            ((denominator shr 16) and 0xFF).toByte(),
            ((denominator shr 24) and 0xFF).toByte()
        )
    )

private fun buildJpegWithExif(
    entries: List<ExifEntry>,
    exifEntries: List<ExifEntry>
): ByteArray {
    val tiff = buildLittleEndianTiff(entries, exifEntries)
    val app1Length = tiff.size + 8
    return byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(),
        0xFF.toByte(), 0xE1.toByte(),
        ((app1Length shr 8) and 0xFF).toByte(), (app1Length and 0xFF).toByte(),
        0x45, 0x78, 0x69, 0x66, 0x00, 0x00
    ) + tiff + byteArrayOf(0xFF.toByte(), 0xD9.toByte())
}

private fun buildLittleEndianTiff(entries: List<ExifEntry>, exifEntries: List<ExifEntry>): ByteArray {
    val ifd0Offset = 8
    val ifd0Size = 2 + entries.size * 12 + 4
    val exifIfdOffset = ifd0Offset + ifd0Size
    val exifIfdSize = 2 + exifEntries.size * 12 + 4
    val dataStart = exifIfdOffset + exifIfdSize

    val total = ByteArray(dataStart + entries.sumOf { paddedSize(it.valueBytes.size) } + exifEntries.sumOf { paddedSize(it.valueBytes.size) })
    total[0] = 0x49
    total[1] = 0x49
    total[2] = 0x2A
    total[3] = 0x00
    writeInt32LE(total, 4, ifd0Offset)

    var dataCursor = dataStart
    writeIfd(total, ifd0Offset, entries, dataCursor).also { dataCursor = it }
    writeIfd(total, exifIfdOffset, exifEntries, dataCursor)

    return total
}

private fun writeIfd(buffer: ByteArray, ifdOffset: Int, entries: List<ExifEntry>, dataCursorStart: Int): Int {
    writeInt16LE(buffer, ifdOffset, entries.size)
    var dataCursor = dataCursorStart
    entries.forEachIndexed { index, entry ->
        val entryOffset = ifdOffset + 2 + (index * 12)
        writeInt16LE(buffer, entryOffset, entry.tag)
        writeInt16LE(buffer, entryOffset + 2, entry.type)
        writeInt32LE(buffer, entryOffset + 4, entry.count)

        if (entry.valueBytes.size <= 4) {
            entry.valueBytes.forEachIndexed { byteIndex, value ->
                buffer[entryOffset + 8 + byteIndex] = value
            }
        } else {
            writeInt32LE(buffer, entryOffset + 8, dataCursor)
            entry.valueBytes.copyInto(buffer, destinationOffset = dataCursor)
            dataCursor += paddedSize(entry.valueBytes.size)
        }
    }
    writeInt32LE(buffer, ifdOffset + 2 + (entries.size * 12), 0)
    return dataCursor
}

private fun paddedSize(size: Int): Int = if (size % 2 == 0) size else size + 1

private fun writeInt16LE(buffer: ByteArray, offset: Int, value: Int) {
    buffer[offset] = (value and 0xFF).toByte()
    buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
}

private fun writeInt32LE(buffer: ByteArray, offset: Int, value: Int) {
    buffer[offset] = (value and 0xFF).toByte()
    buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
    buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
    buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
}

private const val MAKE_TAG = 0x010F
private const val MODEL_TAG = 0x0110
private const val EXIF_IFD_POINTER_TAG = 0x8769
private const val DATE_TIME_ORIGINAL_TAG = 0x9003
private const val OFFSET_TIME_ORIGINAL_TAG = 0x9011
private const val TIME_ZONE_OFFSET_TAG = 0x882A
private const val ISO_SPEED_TAG = 0x8827
private const val F_NUMBER_TAG = 0x829D
