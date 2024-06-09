package com.example.zadanie5

class Circle(var x:Float, var y:Float, var radius: Float, var speed: Vec2) {
    var died = false
    companion object{
        const val CIRCLE_RADIUS = 30f
        const val CIRCLE_SPEED = 500f
    }
}

