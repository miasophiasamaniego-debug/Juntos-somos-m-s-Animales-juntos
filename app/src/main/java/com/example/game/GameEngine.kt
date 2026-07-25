package com.example.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

// Represents an obstacle (Vehicle, Car, Truck)
data class CarObstacle(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val type: CarType,
    val color: Color,
    val secondaryColor: Color = Color(0xFF212121)
)

enum class CarType(val emoji: String, val label: String) {
    RED_CAR("🚗", "Auto Rojo"),
    TAXI("🚕", "Taxi"),
    SPORTS("🏎️", "Deportivo"),
    TRUCK("🚚", "Camión de Carga"),
    BUS("🚌", "Autobús")
}

// Represents collectibles and power-ups in the game
data class ItemCollectible(
    var x: Float,
    var y: Float,
    val radius: Float = 28f,
    val type: CollectibleType,
    var isCollected: Boolean = false
)

enum class CollectibleType(val emoji: String, val label: String, val points: Int) {
    BOTTLE("🍾", "Botella", 10),
    CARDBOARD("📦", "Cartón", 15),
    SUPER_JUMP("🦘", "Súper Salto", 30),
    LEAF_SHIELD("🍃", "Escudo Hojas", 40),
    SPEED_BOOTS("⚡", "Botas Veloces", 35),
    STAR("⭐", "Estrella Extra", 50)
}

// Visual particle for jump, collection, crash, boost, or victory
data class GameParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var size: Float,
    var alpha: Float = 1.0f,
    var lifetime: Int = 30
)

class GameEngine(
    val character: AnimalCharacter,
    val level: GameLevel
) {
    var width = 1080f
    var height = 1920f
    var groundY = 1400f

    // Animal Physics State
    var playerX = 220f
    var playerY = groundY
    var playerVelocityY = 0f
    var playerRadius = 45f
    var isGrounded = true
    var jumpCount = 0
    var maxJumps = if (character.doubleJumpAllowed) 2 else 1
    var isDucking = false
    var rotationAngle = 0f

    // Active Powerups & Timers (in frames ~60fps)
    var superJumpFrames = 0
    var speedBootsFrames = 0
    var leafShieldActive = character.hasShield // Start with shield if animal has inherent shield

    var invincibilityFrames = 0
    var lives = 3

    // Progress & Stats
    var distanceTraveled = 0f
    var score = 0
    var bottlesCollected = 0
    var cardboardCollected = 0
    var isFinished = false
    var isVictory = false
    var houseX = -1f // Set when finish line approaches

    // Lists of Entities
    val cars = mutableListOf<CarObstacle>()
    val items = mutableListOf<ItemCollectible>()
    val particles = mutableListOf<GameParticle>()

    // Internal timers & counters
    private var frameCount = 0
    private var nextCarTimer = 0
    private var nextItemTimer = 0

    fun updateDimensions(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        width = w
        height = h
        groundY = h * 0.72f
        if (playerY > groundY) {
            playerY = groundY
        }
    }

    fun jump() {
        if (isFinished) return

        val jumpMult = if (superJumpFrames > 0) 1.65f else 1.0f

        if (isGrounded) {
            playerVelocityY = -22f * character.jumpPowerMultiplier * jumpMult
            isGrounded = false
            jumpCount = 1
            createJumpParticles(if (superJumpFrames > 0) Color(0xFFFFD54F) else Color.White)
        } else if (jumpCount < maxJumps) {
            playerVelocityY = -18f * character.jumpPowerMultiplier * jumpMult
            jumpCount++
            rotationAngle = 0f
            createJumpParticles(if (superJumpFrames > 0) Color(0xFFFFD54F) else Color.White)
        }
    }

    fun updateGameStep(): GameEvent {
        if (isFinished) return GameEvent.NONE

        frameCount++

        // Decrement Active Power-up Timers
        if (superJumpFrames > 0) superJumpFrames--
        if (speedBootsFrames > 0) speedBootsFrames--
        if (invincibilityFrames > 0) invincibilityFrames--

        // Calculate active speed
        val bootsMultiplier = if (speedBootsFrames > 0) 1.55f else 1.0f
        val currentSpeedMult = character.speedMultiplier * bootsMultiplier

        distanceTraveled += 2.8f * currentSpeedMult
        score += (1 * currentSpeedMult).toInt()

        // Speed boots trail particles
        if (speedBootsFrames > 0 && frameCount % 3 == 0) {
            particles.add(
                GameParticle(
                    x = playerX - playerRadius * 0.5f,
                    y = playerY - Random.nextFloat() * playerRadius,
                    vx = -Random.nextFloat() * 6f - 2f,
                    vy = (Random.nextFloat() - 0.5f) * 2f,
                    color = Color(0xFFFF9800).copy(alpha = 0.8f),
                    size = Random.nextFloat() * 12f + 6f,
                    lifetime = 18
                )
            )
        }

        // Check Victory Condition
        if (distanceTraveled >= level.targetDistance && !isVictory) {
            if (houseX < 0) {
                houseX = width + 200f
            }
            houseX -= 4.5f * currentSpeedMult
            if (houseX <= width * 0.5f) {
                isVictory = true
                isFinished = true
                createVictoryParticles()
                return GameEvent.VICTORY
            }
        }

        // Apply Player Gravity
        playerY += playerVelocityY
        playerVelocityY += 0.95f // Gravity

        // Rotation during double jump
        if (!isGrounded && jumpCount > 1) {
            rotationAngle = (rotationAngle + 20f) % 360f
        } else {
            rotationAngle = 0f
        }

        // Ground Collision
        val currentGround = groundY - if (isDucking) 15f else 0f
        if (playerY >= currentGround) {
            if (!isGrounded) {
                // Landed
                createLandingParticles()
            }
            playerY = currentGround
            playerVelocityY = 0f
            isGrounded = true
            jumpCount = 0
        }

        // Magnet Effect (if Capybara or Magnet active)
        if (character.isMagnet) {
            for (item in items) {
                if (!item.isCollected) {
                    val dx = playerX - item.x
                    val dy = playerY - item.y
                    val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (dist < 340f) {
                        item.x += dx * 0.14f
                        item.y += dy * 0.14f
                    }
                }
            }
        }

        // Spawn Vehicles / Realistic Cars
        nextCarTimer--
        if (nextCarTimer <= 0 && (houseX < 0)) {
            spawnCar()
            nextCarTimer = (Random.nextInt(55, 95) / level.obstacleFrequency).toInt()
        }

        // Move Cars & Check Collisions
        val carIterator = cars.iterator()
        var collisionOccurred = false

        while (carIterator.hasNext()) {
            val car = carIterator.next()
            car.x -= car.speed * currentSpeedMult

            // Exhaust smoke behind car
            if (frameCount % 4 == 0) {
                particles.add(
                    GameParticle(
                        x = car.x + car.width + 5f,
                        y = car.y - 12f,
                        vx = Random.nextFloat() * 3f + 1f,
                        vy = -Random.nextFloat() * 2f,
                        color = Color(0xFF757575).copy(alpha = 0.4f),
                        size = Random.nextFloat() * 8f + 4f,
                        lifetime = 22
                    )
                )
            }

            // Check collision with player
            if (invincibilityFrames <= 0) {
                val playerCurrentRadius = if (isDucking) playerRadius * 0.5f else playerRadius
                val playerRect = Rect(
                    playerX - playerCurrentRadius,
                    playerY - playerCurrentRadius * 2,
                    playerX + playerCurrentRadius,
                    playerY
                )
                val carRect = Rect(
                    car.x,
                    car.y - car.height,
                    car.x + car.width,
                    car.y
                )

                if (playerRect.overlaps(carRect)) {
                    collisionOccurred = true
                    carIterator.remove()
                    createCrashParticles(car.x, car.y)
                    break
                }
            }

            if (car.x + car.width < -120) {
                carIterator.remove()
            }
        }

        // Handle Crash Event
        if (collisionOccurred) {
            if (leafShieldActive) {
                leafShieldActive = false
                invincibilityFrames = 60 // 1 sec invincibility
                createShieldShatterParticles()
                return GameEvent.SHIELD_BROKEN
            } else {
                lives--
                invincibilityFrames = 75 // 1.25 sec invincibility
                if (lives <= 0) {
                    isFinished = true
                    return GameEvent.GAME_OVER
                } else {
                    return GameEvent.CAR_CRASH
                }
            }
        }

        // Spawn Collectibles (Bottles / Cardboard / Powerups)
        nextItemTimer--
        if (nextItemTimer <= 0 && (houseX < 0)) {
            spawnItem()
            nextItemTimer = Random.nextInt(35, 75)
        }

        // Move Collectibles & Check Pickup
        val itemIterator = items.iterator()
        var itemPicked: CollectibleType? = null

        while (itemIterator.hasNext()) {
            val item = itemIterator.next()
            item.x -= level.baseCarSpeed * 0.85f

            val dx = playerX - item.x
            val dy = (playerY - playerRadius) - item.y
            val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

            if (dist < (playerRadius + item.radius) && !item.isCollected) {
                item.isCollected = true
                itemPicked = item.type
                score += item.type.points

                when (item.type) {
                    CollectibleType.BOTTLE -> bottlesCollected++
                    CollectibleType.CARDBOARD -> cardboardCollected++
                    CollectibleType.SUPER_JUMP -> {
                        superJumpFrames = 360 // 6 seconds super jump power
                    }
                    CollectibleType.LEAF_SHIELD -> {
                        leafShieldActive = true // Leaf shield protects against 1 crash
                    }
                    CollectibleType.SPEED_BOOTS -> {
                        speedBootsFrames = 360 // 6 seconds fast run
                    }
                    CollectibleType.STAR -> score += 120
                }

                createCollectParticles(item.x, item.y, getItemColor(item.type))
                itemIterator.remove()
            } else if (item.x < -60) {
                itemIterator.remove()
            }
        }

        // Update Particles
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.2f // particle gravity
            p.alpha -= 0.03f
            p.lifetime--
            if (p.alpha <= 0f || p.lifetime <= 0) {
                particleIterator.remove()
            }
        }

        return when (itemPicked) {
            CollectibleType.BOTTLE -> GameEvent.PICKUP_BOTTLE
            CollectibleType.CARDBOARD -> GameEvent.PICKUP_CARDBOARD
            CollectibleType.SUPER_JUMP -> GameEvent.PICKUP_SUPER_JUMP
            CollectibleType.LEAF_SHIELD -> GameEvent.PICKUP_LEAF_SHIELD
            CollectibleType.SPEED_BOOTS -> GameEvent.PICKUP_SPEED_BOOTS
            CollectibleType.STAR -> GameEvent.PICKUP_STAR
            null -> GameEvent.NONE
        }
    }

    private fun spawnCar() {
        val types = CarType.entries.toTypedArray()
        val type = types[Random.nextInt(types.size)]
        val carSpeed = level.baseCarSpeed + Random.nextFloat() * 2.5f

        val (w, h, color, secColor) = when (type) {
            CarType.RED_CAR -> Quadruple(140f, 70f, Color(0xFFD32F2F), Color(0xFFB71C1C))
            CarType.TAXI -> Quadruple(145f, 72f, Color(0xFFFFB300), Color(0xFF212121))
            CarType.SPORTS -> Quadruple(160f, 58f, Color(0xFF0288D1), Color(0xFF01579B))
            CarType.TRUCK -> Quadruple(210f, 115f, Color(0xFF8D6E63), Color(0xFF4E342E))
            CarType.BUS -> Quadruple(250f, 125f, Color(0xFF2E7D32), Color(0xFF1B5E20))
        }

        cars.add(
            CarObstacle(
                x = width + Random.nextInt(50, 160),
                y = groundY,
                width = w,
                height = h,
                speed = carSpeed,
                type = type,
                color = color,
                secondaryColor = secColor
            )
        )
    }

    private fun spawnItem() {
        val rand = Random.nextFloat()
        val type = when {
            rand < 0.35f -> CollectibleType.BOTTLE
            rand < 0.65f -> CollectibleType.CARDBOARD
            rand < 0.77f -> CollectibleType.SUPER_JUMP
            rand < 0.88f -> CollectibleType.LEAF_SHIELD
            rand < 0.96f -> CollectibleType.SPEED_BOOTS
            else -> CollectibleType.STAR
        }

        // Height variation
        val heightOffsets = listOf(60f, 140f, 230f)
        val hOffset = heightOffsets[Random.nextInt(heightOffsets.size)]

        items.add(
            ItemCollectible(
                x = width + 60f,
                y = groundY - hOffset,
                type = type
            )
        )
    }

    private fun getItemColor(type: CollectibleType): Color {
        return when (type) {
            CollectibleType.BOTTLE -> Color(0xFF29B6F6)
            CollectibleType.CARDBOARD -> Color(0xFFD7CCC8)
            CollectibleType.SUPER_JUMP -> Color(0xFFFFB300)
            CollectibleType.LEAF_SHIELD -> Color(0xFF66BB6A)
            CollectibleType.SPEED_BOOTS -> Color(0xFFFF3D00)
            CollectibleType.STAR -> Color(0xFFFFD54F)
        }
    }

    private fun createJumpParticles(color: Color = Color.White) {
        for (i in 0..10) {
            particles.add(
                GameParticle(
                    x = playerX,
                    y = playerY,
                    vx = (Random.nextFloat() - 0.5f) * 7f,
                    vy = Random.nextFloat() * 4f + 1f,
                    color = color.copy(alpha = 0.85f),
                    size = Random.nextFloat() * 10f + 5f,
                    lifetime = 22
                )
            )
        }
    }

    private fun createLandingParticles() {
        for (i in 0..10) {
            particles.add(
                GameParticle(
                    x = playerX,
                    y = groundY,
                    vx = (Random.nextFloat() - 0.5f) * 8f,
                    vy = -Random.nextFloat() * 3f,
                    color = Color(0xFFB0BEC5),
                    size = Random.nextFloat() * 8f + 4f,
                    lifetime = 18
                )
            )
        }
    }

    private fun createCollectParticles(x: Float, y: Float, color: Color) {
        for (i in 0..16) {
            particles.add(
                GameParticle(
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 12f,
                    vy = (Random.nextFloat() - 0.5f) * 12f,
                    color = color,
                    size = Random.nextFloat() * 12f + 5f,
                    lifetime = 28
                )
            )
        }
    }

    private fun createCrashParticles(x: Float, y: Float) {
        for (i in 0..30) {
            particles.add(
                GameParticle(
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 18f,
                    vy = (Random.nextFloat() - 0.5f) * 18f,
                    color = Color(0xFFFF5252),
                    size = Random.nextFloat() * 14f + 6f,
                    lifetime = 35
                )
            )
        }
    }

    private fun createShieldShatterParticles() {
        for (i in 0..30) {
            particles.add(
                GameParticle(
                    x = playerX,
                    y = playerY - playerRadius,
                    vx = (Random.nextFloat() - 0.5f) * 15f,
                    vy = (Random.nextFloat() - 0.5f) * 15f,
                    color = Color(0xFF81C784),
                    size = Random.nextFloat() * 14f + 5f,
                    lifetime = 30
                )
            )
        }
    }

    private fun createVictoryParticles() {
        val colors = listOf(Color(0xFFFF4081), Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF00B0FF))
        for (i in 0..90) {
            particles.add(
                GameParticle(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * (height * 0.5f),
                    vx = (Random.nextFloat() - 0.5f) * 9f,
                    vy = Random.nextFloat() * 7f + 2f,
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 16f + 8f,
                    lifetime = 100
                )
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

enum class GameEvent {
    NONE,
    PICKUP_BOTTLE,
    PICKUP_CARDBOARD,
    PICKUP_SUPER_JUMP,
    PICKUP_LEAF_SHIELD,
    PICKUP_SPEED_BOOTS,
    PICKUP_STAR,
    CAR_CRASH,
    SHIELD_BROKEN,
    GAME_OVER,
    VICTORY
}
