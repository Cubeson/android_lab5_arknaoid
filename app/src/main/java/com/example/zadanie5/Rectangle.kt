package com.example.zadanie5

class Rectangle(var x:Float, var y:Float, var width:Float, var height:Float, var speed: Vec2){
    fun getCenterPosition(): Vec2{
        val xc = x + (width/2)
        val yc = y + (height/2)
        return  Vec2(xc,yc)
    }
    companion object{
        const val BLOCK_WIDTH = 80f
        const val BLOCK_HEIGHT = 40f
    }
}
