package com.example.zadanie5

import kotlin.math.cos
import kotlin.math.sin

private fun multiplyMatrices(matrix1: Array<Array<Float>>, matrix2: Array<Array<Float>>): Array<Array<Float>> {
    val row1 = matrix1.size
    val col1 = matrix1[0].size
    val col2 = matrix2[0].size
    val product = Array(row1) { Array(col2) { 0f } }

    for (i in 0 until row1) {
        for (j in 0 until col2) {
            for (k in 0 until col1) {
                product[i][j] += matrix1[i][k] * matrix2[k][j]
            }
        }
    }

    return product
}

private fun rotateDirectionVector(vec: Array<Array<Float>>,radian: Float): Array<Array<Float>>{
    val rotationMatrix = arrayOf(
        arrayOf(cos(radian), sin(radian)),
        arrayOf(-sin(radian), cos(radian)),
    )
    val directionMatrix = arrayOf(
        arrayOf(vec[0][0]),
        arrayOf(vec[1][0]),
    )
    return multiplyMatrices(rotationMatrix,directionMatrix)
}