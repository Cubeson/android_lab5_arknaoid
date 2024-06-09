package com.example.zadanie5

// From 0.0f to 1.0f
class RGB(val r:Float,val g:Float,val b:Float){
    constructor(r:Int,g:Int,b:Int) : this(r/100f,g/100f,b/100f)
}
val RGBArray = arrayOf(
    RGB(255,0,0),
    RGB(0,255,0),
    RGB(0,0,255),

    RGB(255,255,0),
    RGB(0,255,255),
    RGB(255,0,255),
)