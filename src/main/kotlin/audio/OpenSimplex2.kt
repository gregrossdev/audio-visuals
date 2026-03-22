package audio

/**
 * Pure Kotlin port of OpenSimplex2 noise (KdotJPG).
 * Only the noise3_ImproveXY variant is exposed — optimised for
 * 2-D slices through 3-D noise (z = time for animated flow fields).
 *
 * Reference: https://github.com/KdotJPG/OpenSimplex2
 */
object OpenSimplex2 {

    // --- constants ---
    private const val PRIME_X = 0x5205402B9270C86FL
    private const val PRIME_Y = 0x598CD327003817B5L
    private const val PRIME_Z = 0x5BCC226E9FA0BACBL
    private const val HASH_MULTIPLIER = 0x53A3F72DEEC546F5L
    private const val SEED_FLIP_3D = -0x52D547B2E96ED629L

    private const val N_GRADS_3D_EXPONENT = 8
    private const val N_GRADS_3D = 1 shl N_GRADS_3D_EXPONENT // 256

    private const val ROOT3OVER3 = 0.577350269189626
    private const val ROTATE_3D_ORTHOGONALIZER = -0.21132486540518713
    private const val NORMALIZER_3D = 0.07969837668935331
    private const val RSQUARED_3D = 0.6f

    // --- gradient table (built once) ---
    private val GRADIENTS_3D: FloatArray = buildGradients3D()

    private fun buildGradients3D(): FloatArray {
        val grad3 = floatArrayOf(
             2.22474487139f,       2.22474487139f,      -1.0f,                 0.0f,
             2.22474487139f,       2.22474487139f,       1.0f,                 0.0f,
             3.0862664687972017f,  1.1721513422464978f,  0.0f,                 0.0f,
             1.1721513422464978f,  3.0862664687972017f,  0.0f,                 0.0f,
            -2.22474487139f,       2.22474487139f,      -1.0f,                 0.0f,
            -2.22474487139f,       2.22474487139f,       1.0f,                 0.0f,
            -1.1721513422464978f,  3.0862664687972017f,  0.0f,                 0.0f,
            -3.0862664687972017f,  1.1721513422464978f,  0.0f,                 0.0f,
            -1.0f,                -2.22474487139f,      -2.22474487139f,       0.0f,
             1.0f,                -2.22474487139f,      -2.22474487139f,       0.0f,
             0.0f,                -3.0862664687972017f, -1.1721513422464978f,  0.0f,
             0.0f,                -1.1721513422464978f, -3.0862664687972017f,  0.0f,
            -1.0f,                -2.22474487139f,       2.22474487139f,       0.0f,
             1.0f,                -2.22474487139f,       2.22474487139f,       0.0f,
             0.0f,                -1.1721513422464978f,  3.0862664687972017f,  0.0f,
             0.0f,                -3.0862664687972017f,  1.1721513422464978f,  0.0f,
            -2.22474487139f,      -2.22474487139f,      -1.0f,                 0.0f,
            -2.22474487139f,      -2.22474487139f,       1.0f,                 0.0f,
            -3.0862664687972017f, -1.1721513422464978f,  0.0f,                 0.0f,
            -1.1721513422464978f, -3.0862664687972017f,  0.0f,                 0.0f,
            -2.22474487139f,      -1.0f,                -2.22474487139f,       0.0f,
            -2.22474487139f,       1.0f,                -2.22474487139f,       0.0f,
            -1.1721513422464978f,  0.0f,                -3.0862664687972017f,  0.0f,
            -3.0862664687972017f,  0.0f,                -1.1721513422464978f,  0.0f,
            -2.22474487139f,      -1.0f,                 2.22474487139f,       0.0f,
            -2.22474487139f,       1.0f,                 2.22474487139f,       0.0f,
            -3.0862664687972017f,  0.0f,                 1.1721513422464978f,  0.0f,
            -1.1721513422464978f,  0.0f,                 3.0862664687972017f,  0.0f,
            -1.0f,                 2.22474487139f,      -2.22474487139f,       0.0f,
             1.0f,                 2.22474487139f,      -2.22474487139f,       0.0f,
             0.0f,                 1.1721513422464978f, -3.0862664687972017f,  0.0f,
             0.0f,                 3.0862664687972017f, -1.1721513422464978f,  0.0f,
            -1.0f,                 2.22474487139f,       2.22474487139f,       0.0f,
             1.0f,                 2.22474487139f,       2.22474487139f,       0.0f,
             0.0f,                 3.0862664687972017f,  1.1721513422464978f,  0.0f,
             0.0f,                 1.1721513422464978f,  3.0862664687972017f,  0.0f,
             2.22474487139f,      -2.22474487139f,      -1.0f,                 0.0f,
             2.22474487139f,      -2.22474487139f,       1.0f,                 0.0f,
             1.1721513422464978f, -3.0862664687972017f,  0.0f,                 0.0f,
             3.0862664687972017f, -1.1721513422464978f,  0.0f,                 0.0f,
             2.22474487139f,      -1.0f,                -2.22474487139f,       0.0f,
             2.22474487139f,       1.0f,                -2.22474487139f,       0.0f,
             3.0862664687972017f,  0.0f,                -1.1721513422464978f,  0.0f,
             1.1721513422464978f,  0.0f,                -3.0862664687972017f,  0.0f,
             2.22474487139f,      -1.0f,                 2.22474487139f,       0.0f,
             2.22474487139f,       1.0f,                 2.22474487139f,       0.0f,
             1.1721513422464978f,  0.0f,                 3.0862664687972017f,  0.0f,
             3.0862664687972017f,  0.0f,                 1.1721513422464978f,  0.0f,
        )
        // Normalise gradients
        for (i in grad3.indices) {
            grad3[i] = (grad3[i] / NORMALIZER_3D).toFloat()
        }
        // Tile into full table
        val gradients = FloatArray(N_GRADS_3D * 4)
        var j = 0
        for (i in gradients.indices) {
            gradients[i] = grad3[j]
            j++
            if (j == grad3.size) j = 0
        }
        return gradients
    }

    // --- public API ---

    /**
     * Evaluate 3-D OpenSimplex2 noise with the **ImproveXY** re-orientation.
     *
     * The lattice is rotated so that horizontal slices (constant z) produce
     * the best-looking 2-D noise — ideal for flow fields animated over time.
     *
     * @return a value in the range `[-1, 1]`.
     */
    fun noise3_ImproveXY(seed: Long, x: Double, y: Double, z: Double): Float {
        val xy = x + y
        val s2 = xy * ROTATE_3D_ORTHOGONALIZER
        val zz = z * ROOT3OVER3
        val xr = x + s2 + zz
        val yr = y + s2 + zz
        val zr = xy * -ROOT3OVER3 + zz
        return noise3_UnrotatedBase(seed, xr, yr, zr)
    }

    // --- core evaluation (overlapping BCC lattice) ---

    private fun noise3_UnrotatedBase(seed: Long, xr: Double, yr: Double, zr: Double): Float {
        val xrb = fastRound(xr)
        val yrb = fastRound(yr)
        val zrb = fastRound(zr)
        var xri = (xr - xrb).toFloat()
        var yri = (yr - yrb).toFloat()
        var zri = (zr - zrb).toFloat()

        var xNSign = ((-1.0f - xri).toInt()) or 1
        var yNSign = ((-1.0f - yri).toInt()) or 1
        var zNSign = ((-1.0f - zri).toInt()) or 1

        var ax0 = xNSign * -xri
        var ay0 = yNSign * -yri
        var az0 = zNSign * -zri

        var xrbp = xrb.toLong() * PRIME_X
        var yrbp = yrb.toLong() * PRIME_Y
        var zrbp = zrb.toLong() * PRIME_Z

        var s = seed
        var value = 0f
        var a = (RSQUARED_3D - xri * xri) - (yri * yri + zri * zri)

        for (l in 0..1) {
            if (a > 0) {
                value += (a * a) * (a * a) * grad(s, xrbp, yrbp, zrbp, xri, yri, zri)
            }

            if (ax0 >= ay0 && ax0 >= az0) {
                var b = a + ax0 + ax0
                if (b > 1) {
                    b -= 1
                    value += (b * b) * (b * b) * grad(
                        s, xrbp - xNSign * PRIME_X, yrbp, zrbp,
                        xri + xNSign, yri, zri
                    )
                }
            } else if (ay0 > ax0 && ay0 >= az0) {
                var b = a + ay0 + ay0
                if (b > 1) {
                    b -= 1
                    value += (b * b) * (b * b) * grad(
                        s, xrbp, yrbp - yNSign * PRIME_Y, zrbp,
                        xri, yri + yNSign, zri
                    )
                }
            } else {
                var b = a + az0 + az0
                if (b > 1) {
                    b -= 1
                    value += (b * b) * (b * b) * grad(
                        s, xrbp, yrbp, zrbp - zNSign * PRIME_Z,
                        xri, yri, zri + zNSign
                    )
                }
            }

            if (l == 1) break

            ax0 = 0.5f - ax0
            ay0 = 0.5f - ay0
            az0 = 0.5f - az0

            xri = xNSign * ax0
            yri = yNSign * ay0
            zri = zNSign * az0

            a += (0.75f - ax0) - (ay0 + az0)

            xrbp += (xNSign shr 1).toLong() and PRIME_X
            yrbp += (yNSign shr 1).toLong() and PRIME_Y
            zrbp += (zNSign shr 1).toLong() and PRIME_Z

            xNSign = -xNSign
            yNSign = -yNSign
            zNSign = -zNSign

            s = s xor SEED_FLIP_3D
        }

        return value
    }

    // --- gradient lookup via hash ---

    private fun grad(
        seed: Long, xrvp: Long, yrvp: Long, zrvp: Long,
        dx: Float, dy: Float, dz: Float
    ): Float {
        var hash = (seed xor xrvp) xor (yrvp xor zrvp)
        hash *= HASH_MULTIPLIER
        hash = hash xor (hash ushr (64 - N_GRADS_3D_EXPONENT + 2))
        val gi = (hash.toInt()) and ((N_GRADS_3D - 1) shl 2)
        return GRADIENTS_3D[gi or 0] * dx +
               GRADIENTS_3D[gi or 1] * dy +
               GRADIENTS_3D[gi or 2] * dz
    }

    // --- util ---

    private fun fastRound(x: Double): Int =
        if (x < 0) (x - 0.5).toInt() else (x + 0.5).toInt()
}
