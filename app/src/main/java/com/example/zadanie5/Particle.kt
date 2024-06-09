package com.example.zadanie5

class Particle(var x:Float, var y:Float, var radius: Float, var speed: Vec2, val rgb:RGB){
    var life: Float = PARTICLE_LIFE_INITIAL
    fun lifePercentage(): Float{
        return life / PARTICLE_LIFE_INITIAL
    }
    companion object{
        const val PARTICLE_LIFE_INITIAL = 3f
        const val PARTICLE_RADIUS_DEFAULT = 20f
        const val PARTICLE_SPEED = 100f
    }
}
