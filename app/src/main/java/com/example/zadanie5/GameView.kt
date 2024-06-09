package com.example.zadanie5

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameView(context: Context?) : SurfaceView(context), Runnable{

    private lateinit var gameThread : Thread
    private lateinit var canvas : Canvas

    private var soundPool : SoundPool? = null

    private var running = false
    private var ready = false

    private val balls = mutableListOf<Circle>()
    private val blocks = mutableListOf<Rectangle>()
    private val particles = mutableListOf<Particle>()
    private lateinit var paddle : Rectangle
    private var playerLives = 3

    private var lastFrame = SystemClock.uptimeMillis()
    private var deltaTime = 0f
    enum class BallStates{
        UNSET,
        INITIALIZED,
        FOLLOWING_PADDLE,
        RELEASED,
        PLAYING,
    }
    enum class DIRECTION{
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }
    private var ballState = BallStates.UNSET

    init {
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if(!ready){
                    initializeGame()
                }
            }
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) { }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                pause()
            }
        })
    }
    private fun initializeGame(){
        paddle = Rectangle(
            (width - 200f) /2f,
            height - (400f),
            200f,
            20f,
            Vec2(0f,0f))

        balls.clear()
        initializeBall()

        val botOffset = 1200
        val topOffset = 200f
        val spacingInColumnDefault = 20f
        val spacingInRowDefault = 20f
        val numBlocksInRow = (width/(spacingInRowDefault*2)).toInt()
        val numBlocksInColumn = ((height-botOffset-topOffset)/(spacingInColumnDefault*2)).toInt()

        blocks.clear()
        var spacingInColumn = 0f
        for (i in 0..<numBlocksInColumn){
            var spacingInRow = 0f
            for (j in 0 ..<numBlocksInRow){
                val block = Rectangle(
                    (j*Rectangle.BLOCK_WIDTH)+spacingInRow,
                    (i*Rectangle.BLOCK_HEIGHT)+spacingInColumn + spacingInColumnDefault + topOffset
                    ,Rectangle.BLOCK_WIDTH,Rectangle.BLOCK_HEIGHT,Vec2(0f,0f))
                spacingInRow += spacingInRowDefault
                blocks.add(block)
            }
            spacingInColumn += spacingInRowDefault
        }
        soundPool = createSoundPool()
        soundPool!!.load(context,R.raw.destroy,1)
        resume()
        ready = true
    }

    private fun getDeltaTimeInSeconds() : Float{
        val now = SystemClock.uptimeMillis()
        val deltaTimeInMillis = now - lastFrame
        lastFrame = now
        println(deltaTimeInMillis)
        return deltaTimeInMillis / 1000f
    }

    override fun run() {
        while(running){
            if(!ready){
                continue
            }
            if(playerLives <= 0){
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(context,MainActivity::class.java)
                    context.startActivity(intent)
                },3000)
                canvas = holder.lockCanvas()
                drawTextGameLost()
                holder.unlockCanvasAndPost(canvas)
                break

            }else if(blocks.isEmpty()){
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(context,MainActivity::class.java)
                    context.startActivity(intent)
                },3000)
                canvas = holder.lockCanvas()
                drawTextGameWon()
                holder.unlockCanvasAndPost(canvas)
                break
            }
            deltaTime = getDeltaTimeInSeconds()

            iterateBalls()
            if(balls.isEmpty()){
                playerLives -= 1
                initializeBall()
            }

            iterateParticles()

            if(holder.surface.isValid){
                canvas = holder.lockCanvas()
                draw()
                holder.unlockCanvasAndPost(canvas)
            }

        }
    }

    private fun initializeBall(){
        val ball = Circle(paddle.x + (paddle.width/2),
            paddle.y-Circle.CIRCLE_RADIUS - 10f,
            Circle.CIRCLE_RADIUS,
            Vec2(0f,0f))
        ballState = BallStates.INITIALIZED
        balls.add(ball)
    }

    private fun iterateBalls(){
        for (ball in balls){
            when(ballState){
                BallStates.FOLLOWING_PADDLE -> {
                    ball.x = paddle.x + (paddle.width/2)
                    ball.y = paddle.y - ball.radius - 10f
                }
                BallStates.UNSET -> throw Exception("Game not initialized")
                BallStates.INITIALIZED -> {
                    continue
                }
                BallStates.RELEASED -> {
                    ballState = BallStates.PLAYING

                    ball.speed = Vec2(Circle.CIRCLE_SPEED,-Circle.CIRCLE_SPEED)

                }
                BallStates.PLAYING -> {
                }
            }

            updateBallPosition(ball)
            if(ball.died){
                continue
            }
            val blocksToDelete = mutableListOf<Rectangle>()
            for (block in blocks){
                if(isCircleHitRectangle(ball,block)){
                    onBallHitBlock(ball,block)
                    blocksToDelete.add(block)
                    createParticles(block)
                    playSoundBlockBreak()
                    break
                }
            }
            blocks.removeAll(blocksToDelete)
        }
        balls.removeIf { b -> b.died }
    }

    private fun createParticles(block: Rectangle) {
        val center = block.getCenterPosition()
        for (i in 0..3){
            particles.add(createRandomParticle(center.x,center.y))
        }
    }

    private fun createRandomParticle(x: Float, y: Float): Particle {
        val color = RGBArray[Random.nextInt(0, RGBArray.size)]

        val pi2 = 314*2
        val radian = Random.nextInt(0,pi2)/100f
        val speedX = cos(radian) * Particle.PARTICLE_SPEED
        val speedY = sin(radian) * Particle.PARTICLE_SPEED


        return Particle(x, y, Particle.PARTICLE_RADIUS_DEFAULT, Vec2(speedX,speedY), color)
    }

    private fun iterateParticles(){
        for (particle in particles){
            particle.x += particle.speed.x * deltaTime
            particle.y += particle.speed.y * deltaTime
            particle.life -= deltaTime * 1f
        }
        particles.removeIf { p -> p.life <= 0 }
    }


    private fun getCollisionSide(rect1: Rectangle, rect2:Rectangle) : DIRECTION{
        // Minkowski Sum
        val rect1Center = rect1.getCenterPosition()
        val rect2Center = rect2.getCenterPosition()
        val wy = (rect1.width + rect2.width) * (rect1Center.y - rect2Center.y)
        val hx = (rect1.height + rect2.height) * (rect1Center.x - rect2Center.x)
        return if( wy > hx){
            if(wy > -hx){
                DIRECTION.TOP
            }else{
                DIRECTION.RIGHT
            }
        } else if(wy > -hx){
            DIRECTION.LEFT
        }else{
            DIRECTION.BOTTOM
        }
    }

    private fun onBallHitBlock(ball:Circle, block: Rectangle){
        ball.x -= ball.speed.x * deltaTime
        ball.y -= ball.speed.y * deltaTime
        val rectFromBall = Rectangle(ball.x-ball.radius,ball.y-ball.radius,ball.radius*2,ball.radius*2,Vec2(0f,0f))
        when(getCollisionSide(block,rectFromBall)){
            DIRECTION.TOP ->{
                ball.speed.y *= -1f
            }
            DIRECTION.BOTTOM ->{
                ball.speed.y *= -1f
            }
            DIRECTION.RIGHT -> {
                ball.speed.x *= -1f
            }
            DIRECTION.LEFT -> {
                ball.speed.x *= -1f
            }
        }
    }

    private fun isBallHitWallLeft(ball: Circle) : Boolean{
        return ball.x - ball.radius < 0
    }
    private fun isBallHitWallRight(ball: Circle) : Boolean{
        return ball.x + ball.radius > width
    }
    private fun isBallHitWallTop(ball: Circle) : Boolean{
        return ball.y - ball.radius < 0
    }
    private fun isBallHitWallBottom(ball: Circle) : Boolean{
        return ball.y + ball.radius > height
    }

    private fun isCircleHitRectangle(circle: Circle, rectangle: Rectangle) : Boolean{
        // https://www.jeffreythompson.org/collision-detection/circle-rect.php
        var testX = circle.x
        var testY = circle.x

        // circle is to the left
        if (circle.x < rectangle.x)                         testX = rectangle.x
        // circle is to the right
        else if(circle.x > rectangle.x+rectangle.width)     testX = rectangle.x + rectangle.width

        // circle is above
        if(circle.y < rectangle.y)                          testY = rectangle.y
        // circle is below
        else if(circle.y > rectangle.y+rectangle.height)    testY = rectangle.y + rectangle.height

        val distX = circle.x-testX
        val distY = circle.y-testY
        val distance = sqrt((distX*distX) + (distY*distY))

        if(distance <= circle.radius)  return true
        return false
    }

    private fun onBallHitPaddle(ball:Circle){
        ball.x -= ball.speed.x * deltaTime
        ball.y -= ball.speed.y * deltaTime
        ball.speed.x *= -1
        ball.speed.y *= -1
    }

    private fun updateBallPosition(ball: Circle){
        ball.x += ball.speed.x * deltaTime
        ball.y += ball.speed.y * deltaTime

        if(isBallHitWallLeft(ball)){
            ball.x -= ball.speed.x * deltaTime
            ball.y -= ball.speed.y * deltaTime
            ball.speed.x *= -1
        }
        if(isBallHitWallRight(ball)){
            ball.x -= ball.speed.x * deltaTime
            ball.y -= ball.speed.y * deltaTime
            ball.x += ball.speed.x * deltaTime
        }
        if(isCircleHitRectangle(ball,paddle)){
            onBallHitPaddle(ball)
        }
        if(isBallHitWallTop(ball)){
            ball.x -= ball.speed.x * deltaTime
            ball.y -= ball.speed.y * deltaTime
        }
        if(isBallHitWallBottom(ball)){
            ball.died = true
        }
    }

    private fun draw(){
        canvas.drawColor(Color.BLACK)
        val paint = Paint()
        paint.setColor(ContextCompat.getColor(context,R.color.white))
        canvas.drawRect(paddle.x,paddle.y, paddle.x + paddle.width, paddle.y + paddle.height, paint)

        for (ball in balls){
            paint.setColor(ContextCompat.getColor(context,R.color.white))
            canvas.drawCircle(ball.x,ball.y,ball.radius,paint)
        }

        val copied = mutableListOf<Rectangle>()
        copied.addAll(blocks)
        for (block in copied){
            paint.setColor(ContextCompat.getColor(context,R.color.white))
            canvas.drawRect(block.x, block.y,block.x+block.width,block.y+block.height,paint)
        }

        for(particle in particles){
            val color = Color.argb(particle.lifePercentage(),particle.rgb.r,particle.rgb.g,particle.rgb.b)
            paint.setColor(color)
            canvas.drawCircle(particle.x,particle.y,particle.radius,paint)
        }
        drawTextLivesRemaining()
    }

    private fun drawTextLivesRemaining(){
        val paintText = Paint()
        paintText.setColor(ContextCompat.getColor(this.context,R.color.white))
        paintText.textSize = 50f
        canvas.drawText("Lives remaining: $playerLives",40f,height-200f,paintText)
    }
    private fun drawTextGameLost(){
        val paintText = Paint()
        val paintColor = Color.rgb(255,0,0)
        paintText.setColor(paintColor)
        paintText.textSize = 150f
        paintText.textAlign = Paint.Align.CENTER
        val xPos = width/2f
        val yPos = height/2f
        canvas.drawText("GAME OVER",xPos,yPos,paintText)
    }
    private fun drawTextGameWon(){
        val paintText = Paint()
        val paintColor = Color.rgb(0,255,0)
        paintText.setColor(paintColor)
        paintText.textSize = 150f
        paintText.textAlign = Paint.Align.CENTER
        val xPos = width/2f
        val yPos = height/2f
        canvas.drawText("VICTORY",xPos,yPos,paintText)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event!!.x
        when(event.action){
            MotionEvent.ACTION_MOVE -> {
                if(ballState == BallStates.INITIALIZED){
                    ballState = BallStates.FOLLOWING_PADDLE
                }
                paddle.x = touchX - paddle.width/2
                if(paddle.x < 0)
                    paddle.x = 0f
                if(paddle.x > width - paddle.width)
                    paddle.x = width - paddle.width
            }
            MotionEvent.ACTION_UP -> {
                if(ballState == BallStates.FOLLOWING_PADDLE){
                    ballState = BallStates.RELEASED
                }
            }
        }
        return true
    }

    fun resume(){
        running = true
        gameThread = Thread(this)
        gameThread.start()
    }
    fun pause(){
        running = false
        try{
            gameThread.join()
        } catch (e : InterruptedException){
            e.printStackTrace()
        }
    }

    private fun playSoundBlockBreak() {
        soundPool!!.play(1,0.5f,0.5f,0,0,1f)
    }

    private fun createSoundPool(): SoundPool{
        val builder = SoundPool.Builder()
        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        return builder.setAudioAttributes(attr)
            .setMaxStreams(6)
            .build()
    }
}


