package com.android.marker.utils

fun Double.formatLatLong(digits: Int) = java.lang.String.format("%.${digits}f", this)

