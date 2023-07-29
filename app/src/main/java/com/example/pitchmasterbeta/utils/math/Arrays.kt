package com.example.pitchmasterbeta.utils.math

fun shiftArrayRight(array: IntArray, newInt: Int) {
    if (array.size <= 1) {
        return  // No need to shift if the array has 0 or 1 element
    }

    // Shift the elements to the right
    System.arraycopy(array, 0, array, 1, array.size - 1)
    array[0] = newInt // Assign the last element to the first position
}
