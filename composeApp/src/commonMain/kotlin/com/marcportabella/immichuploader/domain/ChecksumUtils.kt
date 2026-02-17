package com.marcportabella.immichuploader.domain

fun sha1Hex(bytes: ByteArray): String {
    val digest = sha1(bytes)
    val builder = StringBuilder(digest.size * 2)
    digest.forEach { byte ->
        builder.append(((byte.toInt() ushr 4) and 0xF).toString(16))
        builder.append((byte.toInt() and 0xF).toString(16))
    }
    return builder.toString()
}

private fun sha1(input: ByteArray): ByteArray {
    val padded = padSha1(input)
    var h0 = 0x67452301
    var h1 = 0xEFCDAB89.toInt()
    var h2 = 0x98BADCFE.toInt()
    var h3 = 0x10325476
    var h4 = 0xC3D2E1F0.toInt()

    val w = IntArray(80)
    var offset = 0
    while (offset < padded.size) {
        for (i in 0 until 16) {
            val index = offset + i * 4
            w[i] = ((padded[index].toInt() and 0xFF) shl 24) or
                ((padded[index + 1].toInt() and 0xFF) shl 16) or
                ((padded[index + 2].toInt() and 0xFF) shl 8) or
                (padded[index + 3].toInt() and 0xFF)
        }
        for (i in 16 until 80) {
            w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4

        for (i in 0 until 80) {
            val (f, k) = when (i) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) to 0xCA62C1D6.toInt()
            }

            val temp = a.rotateLeft(5) + f + e + k + w[i]
            e = d
            d = c
            c = b.rotateLeft(30)
            b = a
            a = temp
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
        offset += 64
    }

    return byteArrayOf(
        (h0 ushr 24).toByte(), (h0 ushr 16).toByte(), (h0 ushr 8).toByte(), h0.toByte(),
        (h1 ushr 24).toByte(), (h1 ushr 16).toByte(), (h1 ushr 8).toByte(), h1.toByte(),
        (h2 ushr 24).toByte(), (h2 ushr 16).toByte(), (h2 ushr 8).toByte(), h2.toByte(),
        (h3 ushr 24).toByte(), (h3 ushr 16).toByte(), (h3 ushr 8).toByte(), h3.toByte(),
        (h4 ushr 24).toByte(), (h4 ushr 16).toByte(), (h4 ushr 8).toByte(), h4.toByte()
    )
}

private fun padSha1(input: ByteArray): ByteArray {
    val bitLength = input.size.toLong() * 8L
    val withOne = input.size + 1
    val mod = withOne % 64
    val padZeros = if (mod <= 56) 56 - mod else 56 + (64 - mod)
    val result = ByteArray(input.size + 1 + padZeros + 8)
    input.copyInto(result, endIndex = input.size)
    result[input.size] = 0x80.toByte()
    for (i in 0 until 8) {
        result[result.size - 1 - i] = (bitLength ushr (i * 8)).toByte()
    }
    return result
}
