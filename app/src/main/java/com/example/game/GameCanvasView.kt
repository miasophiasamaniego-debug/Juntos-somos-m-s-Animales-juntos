package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvasView(
    engine: GameEngine,
    onTapJump: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTapJump() }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            engine.updateDimensions(size.width, size.height)

            // 1. Draw Parallax Background
            drawSkyAndMountains(engine)

            // 2. Draw Road
            drawRoad(engine)

            // 3. Draw Finish Line Eco House (if approaching)
            if (engine.houseX > -500f) {
                drawEcoHouseFinish(engine)
            }

            // 4. Draw Collectibles & Powerups
            drawCollectiblesAndPowerups(engine)

            // 5. Draw Realistic Cars / Obstacles
            drawRealisticCars(engine)

            // 6. Draw Player (Animal with Active Powerup Visual Effects)
            drawPlayerAnimal(engine)

            // 7. Draw Particles
            drawParticles(engine)
        }
    }
}

private fun DrawScope.drawSkyAndMountains(engine: GameEngine) {
    val level = engine.level
    val skyBrush = Brush.verticalGradient(
        colors = listOf(
            Color(level.skyTopColorHex),
            Color(level.skyBottomColorHex)
        ),
        startY = 0f,
        endY = engine.groundY
    )

    // Sky Background
    drawRect(
        brush = skyBrush,
        size = Size(size.width, engine.groundY)
    )

    // Sun / Moon with soft aura
    drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = 0.3f),
        radius = 75f,
        center = Offset(size.width * 0.82f, size.height * 0.12f)
    )
    drawCircle(
        color = Color(0xFFFFF176),
        radius = 50f,
        center = Offset(size.width * 0.82f, size.height * 0.12f)
    )

    // Parallax Eco Hills (Cardboard & Forest style)
    val scrollOffset = (engine.distanceTraveled * 0.3f) % size.width
    val hillPath = Path().apply {
        moveTo(-scrollOffset, engine.groundY)
        cubicTo(
            -scrollOffset + size.width * 0.25f, engine.groundY - 140f,
            -scrollOffset + size.width * 0.5f, engine.groundY - 60f,
            -scrollOffset + size.width * 0.75f, engine.groundY - 180f
        )
        lineTo(-scrollOffset + size.width, engine.groundY)
        close()
    }
    drawPath(path = hillPath, color = Color(0xFFC8E6C9))

    val hillPath2 = Path().apply {
        val o2 = (scrollOffset + size.width * 0.5f) % size.width
        moveTo(-o2, engine.groundY)
        cubicTo(
            -o2 + size.width * 0.3f, engine.groundY - 120f,
            -o2 + size.width * 0.6f, engine.groundY - 190f,
            -o2 + size.width, engine.groundY - 80f
        )
        lineTo(-o2 + size.width * 1.2f, engine.groundY)
        close()
    }
    drawPath(path = hillPath2, color = Color(0xFFA5D6A7))
}

private fun DrawScope.drawRoad(engine: GameEngine) {
    val groundY = engine.groundY
    val roadHeight = size.height - groundY

    // Asphalt Texture
    drawRect(
        color = Color(engine.level.roadColorHex),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, roadHeight)
    )

    // Sidewalk Grass & Curb
    drawRect(
        color = Color(0xFF7CB342),
        topLeft = Offset(0f, groundY - 14f),
        size = Size(size.width, 14f)
    )
    drawRect(
        color = Color(0xFFE0E0E0),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, 10f)
    )

    // Dashed Road Lines
    val dashWidth = 85f
    val gap = 55f
    val roadScroll = (engine.distanceTraveled * 2.8f) % (dashWidth + gap)

    var x = -roadScroll
    while (x < size.width + dashWidth) {
        drawRoundRect(
            color = Color(0xFFFFEB3B),
            topLeft = Offset(x, groundY + roadHeight * 0.42f),
            size = Size(dashWidth, 12f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        x += dashWidth + gap
    }
}

private fun DrawScope.drawRealisticCars(engine: GameEngine) {
    for (car in engine.cars) {
        val carLeft = car.x
        val carTop = car.y - car.height
        val cWidth = car.width
        val cHeight = car.height

        // 1. Headlight Cone Beam on Road (Extends forward)
        val beamPath = Path().apply {
            moveTo(carLeft, car.y - 15f)
            lineTo(carLeft - 180f, car.y + 10f)
            lineTo(carLeft - 180f, car.y - 45f)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFFF9C4).copy(alpha = 0.02f),
                    Color(0xFFFFF59D).copy(alpha = 0.35f)
                ),
                startX = carLeft - 180f,
                endX = carLeft
            )
        )

        // 2. Ground Shadow under Car
        drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(carLeft - 10f, car.y - 12f),
            size = Size(cWidth + 20f, 20f)
        )

        when (car.type) {
            CarType.RED_CAR, CarType.TAXI, CarType.SPORTS -> {
                // Sleek Sedan / Taxi / Sports Car
                val mainColor = car.color
                val darkShade = car.secondaryColor

                // Lower Body Metallic
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(mainColor, darkShade)
                    ),
                    topLeft = Offset(carLeft, carTop + cHeight * 0.35f),
                    size = Size(cWidth, cHeight * 0.55f),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Aerodynamic Roof Cabin
                val cabinWidth = cWidth * 0.55f
                val cabinLeft = carLeft + cWidth * 0.22f
                val cabinTop = carTop
                val cabinHeight = cHeight * 0.45f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(mainColor, darkShade)
                    ),
                    topLeft = Offset(cabinLeft, cabinTop),
                    size = Size(cabinWidth, cabinHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )

                // Glass Windshields with Light Reflection Glare
                val glassPath = Path().apply {
                    moveTo(cabinLeft + 8f, cabinTop + 6f)
                    lineTo(cabinLeft + cabinWidth - 8f, cabinTop + 6f)
                    lineTo(cabinLeft + cabinWidth - 2f, cabinTop + cabinHeight - 4f)
                    lineTo(cabinLeft + 2f, cabinTop + cabinHeight - 4f)
                    close()
                }
                drawPath(
                    path = glassPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFB3E5FC), Color(0xFF0288D1))
                    )
                )

                // Diagonal Windshield Reflection
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(cabinLeft + 15f, cabinTop + 8f),
                    end = Offset(cabinLeft + 35f, cabinTop + cabinHeight - 6f),
                    strokeWidth = 4f
                )

                // TAXI Sign
                if (car.type == CarType.TAXI) {
                    drawRoundRect(
                        color = Color(0xFFFF6F00),
                        topLeft = Offset(cabinLeft + cabinWidth * 0.3f, cabinTop - 14f),
                        size = Size(cabinWidth * 0.4f, 14f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // Sports Car Rear Spoiler
                if (car.type == CarType.SPORTS) {
                    drawRect(
                        color = Color(0xFF212121),
                        topLeft = Offset(carLeft + cWidth - 20f, carTop + 10f),
                        size = Size(22f, 8f)
                    )
                }

                // Front Grill & Chrome Bumper
                drawRoundRect(
                    color = Color(0xFF424242),
                    topLeft = Offset(carLeft, carTop + cHeight * 0.55f),
                    size = Size(10f, cHeight * 0.3f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRect(
                    color = Color(0xFFECEFF1), // Chrome
                    topLeft = Offset(carLeft, carTop + cHeight * 0.8f),
                    size = Size(8f, 10f)
                )

                // License Plate ("ECO")
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(carLeft, carTop + cHeight * 0.65f),
                    size = Size(6f, 12f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Headlight LED & Tail Light
                drawCircle(
                    color = Color(0xFFFFF59D),
                    radius = 9f,
                    center = Offset(carLeft + 4f, carTop + cHeight * 0.45f)
                )
                drawCircle(
                    color = Color(0xFFFF1744),
                    radius = 8f,
                    center = Offset(carLeft + cWidth - 4f, carTop + cHeight * 0.45f)
                )

                // Side Mirror
                drawRoundRect(
                    color = mainColor,
                    topLeft = Offset(carLeft + cWidth * 0.28f, cabinTop + 12f),
                    size = Size(10f, 8f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
            CarType.TRUCK -> {
                // Heavy Cardboard Cargo Truck
                // Cab Section
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                    ),
                    topLeft = Offset(carLeft, carTop + cHeight * 0.2f),
                    size = Size(cWidth * 0.35f, cHeight * 0.7f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Truck Windshield
                drawRoundRect(
                    color = Color(0xFF81D4FA),
                    topLeft = Offset(carLeft + 8f, carTop + cHeight * 0.28f),
                    size = Size(cWidth * 0.25f, cHeight * 0.25f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Large Eco Cardboard Container Back
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E))
                    ),
                    topLeft = Offset(carLeft + cWidth * 0.32f, carTop),
                    size = Size(cWidth * 0.68f, cHeight * 0.88f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Eco Leaf Logo on Truck
                drawCircle(
                    color = Color(0xFF81C784),
                    radius = 16f,
                    center = Offset(carLeft + cWidth * 0.65f, carTop + cHeight * 0.45f)
                )
            }
            CarType.BUS -> {
                // City Bus with many windows
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(car.color, car.secondaryColor)
                    ),
                    topLeft = Offset(carLeft, carTop),
                    size = Size(cWidth, cHeight * 0.88f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Long Row of Passenger Windows
                val winWidth = cWidth * 0.15f
                for (i in 0..4) {
                    drawRoundRect(
                        color = Color(0xFFB3E5FC),
                        topLeft = Offset(carLeft + 15f + i * (winWidth + 12f), carTop + 15f),
                        size = Size(winWidth, cHeight * 0.35f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
                // Front Headlight & Destination Sign
                drawRoundRect(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(carLeft + 10f, carTop + 6f),
                    size = Size(60f, 10f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }

        // Realistic Rubber Wheels with Silver Alloy Spokes
        val wheelRadius = cHeight * 0.2f
        val wheelsY = car.y - 4f

        val wheelOffsets = when (car.type) {
            CarType.BUS, CarType.TRUCK -> listOf(0.18f, 0.65f, 0.85f)
            else -> listOf(0.22f, 0.78f)
        }

        for (wf in wheelOffsets) {
            val wx = carLeft + cWidth * wf
            // Outer Rubber Tire
            drawCircle(
                color = Color(0xFF212121),
                radius = wheelRadius,
                center = Offset(wx, wheelsY)
            )
            // Silver Alloy Rim
            drawCircle(
                color = Color(0xFFCFD8DC),
                radius = wheelRadius * 0.55f,
                center = Offset(wx, wheelsY)
            )
            // Center Cap
            drawCircle(
                color = Color(0xFF37474F),
                radius = wheelRadius * 0.22f,
                center = Offset(wx, wheelsY)
            )
        }
    }
}

private fun DrawScope.drawCollectiblesAndPowerups(engine: GameEngine) {
    for (item in engine.items) {
        if (item.isCollected) continue

        // Floating bounce animation
        val floatOffset = sin(engine.distanceTraveled * 0.08f + item.x) * 9f
        val itemCenter = Offset(item.x, item.y + floatOffset)

        when (item.type) {
            CollectibleType.BOTTLE -> {
                // Plastic Bottle
                drawRoundRect(
                    color = Color(0xFF29B6F6).copy(alpha = 0.85f),
                    topLeft = Offset(itemCenter.x - 14f, itemCenter.y - 24f),
                    size = Size(28f, 48f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Bottle Cap
                drawRect(
                    color = Color(0xFFFF5252),
                    topLeft = Offset(itemCenter.x - 8f, itemCenter.y - 32f),
                    size = Size(16f, 8f)
                )
                // Label
                drawRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(itemCenter.x - 14f, itemCenter.y - 8f),
                    size = Size(28f, 14f)
                )
            }
            CollectibleType.CARDBOARD -> {
                // Cardboard Box
                drawRoundRect(
                    color = Color(0xFFBCAAA4),
                    topLeft = Offset(itemCenter.x - 22f, itemCenter.y - 22f),
                    size = Size(44f, 44f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Tape line
                drawRect(
                    color = Color(0xFF8D6E63),
                    topLeft = Offset(itemCenter.x - 6f, itemCenter.y - 22f),
                    size = Size(12f, 44f)
                )
                // Recycling symbol / box flap
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 8f,
                    center = itemCenter
                )
            }
            CollectibleType.SUPER_JUMP -> {
                // Súper Salto Badge (Golden Spring / Kangaroo)
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = 0.35f),
                    radius = 32f,
                    center = itemCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFECB3), Color(0xFFFFB300))
                    ),
                    radius = 24f,
                    center = itemCenter
                )
                drawCircle(
                    color = Color(0xFFFF6F00),
                    radius = 24f,
                    center = itemCenter,
                    style = Stroke(width = 4f)
                )
                // Spring Coil graphic
                for (i in -1..1) {
                    drawOval(
                        color = Color(0xFF3E2723),
                        topLeft = Offset(itemCenter.x - 12f, itemCenter.y + i * 8f - 4f),
                        size = Size(24f, 8f),
                        style = Stroke(width = 3f)
                    )
                }
            }
            CollectibleType.LEAF_SHIELD -> {
                // Escudo de Hojas (Leaf Shield)
                drawCircle(
                    color = Color(0xFFA5D6A7).copy(alpha = 0.45f),
                    radius = 32f,
                    center = itemCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC8E6C9), Color(0xFF4CAF50))
                    ),
                    radius = 24f,
                    center = itemCenter
                )
                drawCircle(
                    color = Color(0xFF1B5E20),
                    radius = 24f,
                    center = itemCenter,
                    style = Stroke(width = 4f)
                )
                // Leaf Emblem
                val leafPath = Path().apply {
                    moveTo(itemCenter.x - 12f, itemCenter.y + 12f)
                    quadraticTo(itemCenter.x - 14f, itemCenter.y - 12f, itemCenter.x + 12f, itemCenter.y - 12f)
                    quadraticTo(itemCenter.x + 14f, itemCenter.y + 12f, itemCenter.x - 12f, itemCenter.y + 12f)
                    close()
                }
                drawPath(leafPath, color = Color.White)
            }
            CollectibleType.SPEED_BOOTS -> {
                // Botas Veloces (Speedy Boots)
                drawCircle(
                    color = Color(0xFFFFAB91).copy(alpha = 0.45f),
                    radius = 32f,
                    center = itemCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFCCBC), Color(0xFFFF3D00))
                    ),
                    radius = 24f,
                    center = itemCenter
                )
                drawCircle(
                    color = Color(0xFFBF360C),
                    radius = 24f,
                    center = itemCenter,
                    style = Stroke(width = 4f)
                )
                // Winged Boot Emblem
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(itemCenter.x - 14f, itemCenter.y - 10f),
                    size = Size(22f, 20f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(itemCenter.x - 14f, itemCenter.y + 4f),
                    size = Size(28f, 10f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
            CollectibleType.STAR -> {
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 24f,
                    center = itemCenter
                )
            }
        }
    }
}

private fun DrawScope.drawPlayerAnimal(engine: GameEngine) {
    val pX = engine.playerX
    val pY = engine.playerY
    val r = engine.playerRadius
    val animal = engine.character

    // Invincibility flicker
    if (engine.invincibilityFrames > 0 && (engine.invincibilityFrames / 6) % 2 == 0) {
        return
    }

    // Shadow on ground
    val shadowWidth = if (engine.isGrounded) r * 1.8f else r * 1.2f
    val shadowAlpha = if (engine.isGrounded) 0.3f else 0.15f
    drawOval(
        color = Color.Black.copy(alpha = shadowAlpha),
        topLeft = Offset(pX - shadowWidth * 0.5f, engine.groundY - 8f),
        size = Size(shadowWidth, 16f)
    )

    // POWER-UP VISUAL FX 1: Leaf Shield Aura 🍃
    if (engine.leafShieldActive) {
        drawCircle(
            color = Color(0xFF81C784).copy(alpha = 0.35f),
            radius = r * 1.55f,
            center = Offset(pX, pY - r)
        )
        drawCircle(
            color = Color(0xFF2E7D32),
            radius = r * 1.55f,
            center = Offset(pX, pY - r),
            style = Stroke(width = 5f)
        )

        // Orbiting Leaf Badges around shield
        val orbitAngle = (engine.distanceTraveled * 0.1f)
        for (i in 0..2) {
            val ang = orbitAngle + i * (2f * Math.PI / 3f)
            val lx = pX + cos(ang).toFloat() * (r * 1.55f)
            val ly = (pY - r) + sin(ang).toFloat() * (r * 1.55f)
            drawCircle(color = Color(0xFF4CAF50), radius = 10f, center = Offset(lx, ly))
        }
    }

    // POWER-UP VISUAL FX 2: Super Jump Golden Glow 🦘
    if (engine.superJumpFrames > 0) {
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.35f),
            radius = r * 1.6f,
            center = Offset(pX, pY - r)
        )
        drawCircle(
            color = Color(0xFFFF8F00),
            radius = r * 1.6f,
            center = Offset(pX, pY - r),
            style = Stroke(width = 4f)
        )
    }

    rotate(degrees = engine.rotationAngle, pivot = Offset(pX, pY - r)) {
        // Animal Body
        val bodyColor = animal.primaryColor
        val bellyColor = animal.secondaryColor

        val bodyHeight = if (engine.isDucking) r * 1.1f else r * 1.8f
        val bodyTop = pY - bodyHeight

        // Ears (depending on animal)
        when (animal.id) {
            "bunny" -> {
                // Long Bunny Ears
                drawOval(
                    color = bodyColor,
                    topLeft = Offset(pX - 18f, bodyTop - 38f),
                    size = Size(14f, 42f)
                )
                drawOval(
                    color = bellyColor,
                    topLeft = Offset(pX - 15f, bodyTop - 34f),
                    size = Size(8f, 32f)
                )

                drawOval(
                    color = bodyColor,
                    topLeft = Offset(pX + 4f, bodyTop - 38f),
                    size = Size(14f, 42f)
                )
                drawOval(
                    color = bellyColor,
                    topLeft = Offset(pX + 7f, bodyTop - 34f),
                    size = Size(8f, 32f)
                )
            }
            "puppy" -> {
                // Floppy Puppy Ears
                drawOval(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(pX - 26f, bodyTop - 5f),
                    size = Size(18f, 32f)
                )
                drawOval(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(pX + 8f, bodyTop - 5f),
                    size = Size(18f, 32f)
                )
            }
            "kitty" -> {
                // Pointy Cat Ears
                val earPathLeft = Path().apply {
                    moveTo(pX - 20f, bodyTop)
                    lineTo(pX - 10f, bodyTop - 25f)
                    lineTo(pX, bodyTop)
                    close()
                }
                drawPath(earPathLeft, bodyColor)

                val earPathRight = Path().apply {
                    moveTo(pX, bodyTop)
                    lineTo(pX + 10f, bodyTop - 25f)
                    lineTo(pX + 20f, bodyTop)
                    close()
                }
                drawPath(earPathRight, bodyColor)
            }
            "bear" -> {
                // Round Bear Ears
                drawCircle(color = bodyColor, radius = 14f, center = Offset(pX - 18f, bodyTop))
                drawCircle(color = bodyColor, radius = 14f, center = Offset(pX + 18f, bodyTop))
            }
            "capybara" -> {
                // Cute Capybara Snout & small ears
                drawCircle(color = bodyColor, radius = 10f, center = Offset(pX - 20f, bodyTop + 5f))
                drawCircle(color = bodyColor, radius = 10f, center = Offset(pX + 20f, bodyTop + 5f))
            }
        }

        // Main Body Oval
        drawOval(
            color = bodyColor,
            topLeft = Offset(pX - r, bodyTop),
            size = Size(r * 2f, bodyHeight)
        )

        // Belly Patch
        drawOval(
            color = bellyColor,
            topLeft = Offset(pX - r * 0.6f, bodyTop + bodyHeight * 0.35f),
            size = Size(r * 1.2f, bodyHeight * 0.55f)
        )

        // Eyes & Cute Nose
        val eyeY = bodyTop + bodyHeight * 0.3f
        drawCircle(color = Color.Black, radius = 5f, center = Offset(pX - 10f, eyeY))
        drawCircle(color = Color.White, radius = 2f, center = Offset(pX - 11f, eyeY - 2f))

        drawCircle(color = Color.Black, radius = 5f, center = Offset(pX + 10f, eyeY))
        drawCircle(color = Color.White, radius = 2f, center = Offset(pX + 9f, eyeY - 2f))

        // Cute Pink Nose
        drawCircle(color = Color(0xFFFF80AB), radius = 4f, center = Offset(pX, eyeY + 8f))

        // POWER-UP VISUAL FX 3: Speed Boots or Super Jump Spring Boots on feet ⚡
        if (engine.speedBootsFrames > 0) {
            // Fiery Red Speed Boots on feet
            drawRoundRect(
                color = Color(0xFFFF3D00),
                topLeft = Offset(pX - 22f, pY - 14f),
                size = Size(18f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFFFF3D00),
                topLeft = Offset(pX + 4f, pY - 14f),
                size = Size(18f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        } else if (engine.superJumpFrames > 0) {
            // Golden Spring Boots on feet
            drawRoundRect(
                color = Color(0xFFFFB300),
                topLeft = Offset(pX - 22f, pY - 14f),
                size = Size(18f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFFFFB300),
                topLeft = Offset(pX + 4f, pY - 14f),
                size = Size(18f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

private fun DrawScope.drawEcoHouseFinish(engine: GameEngine) {
    val hX = engine.houseX
    val groundY = engine.groundY

    val houseWidth = 380f
    val houseHeight = 320f
    val houseTop = groundY - houseHeight

    // Main Cardboard Structure
    drawRoundRect(
        color = Color(0xFFD7CCC8), // Cardboard brown
        topLeft = Offset(hX, houseTop),
        size = Size(houseWidth, houseHeight),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // Roof (Green recycled bottle roof)
    val roofPath = Path().apply {
        moveTo(hX - 30f, houseTop)
        lineTo(hX + houseWidth / 2f, houseTop - 120f)
        lineTo(hX + houseWidth + 30f, houseTop)
        close()
    }
    drawPath(roofPath, color = Color(0xFF4CAF50))

    // Plastic Bottle Windows
    for (row in 0..1) {
        for (col in 0..2) {
            val wx = hX + 45f + col * 105f
            val wy = houseTop + 60f + row * 90f
            drawRoundRect(
                color = Color(0xFF81D4FA).copy(alpha = 0.9f),
                topLeft = Offset(wx, wy),
                size = Size(75f, 65f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Bottle pattern grid
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(wx + 8f, wy + 8f),
                size = Size(59f, 49f),
                style = Stroke(width = 3f),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }

    // Door made of cardboard with heart handle
    drawRoundRect(
        color = Color(0xFFA1887F),
        topLeft = Offset(hX + houseWidth / 2f - 40f, groundY - 110f),
        size = Size(80f, 110f),
        cornerRadius = CornerRadius(10f, 10f)
    )

    // Solar Panel on roof
    drawRoundRect(
        color = Color(0xFF1565C0),
        topLeft = Offset(hX + 60f, houseTop - 90f),
        size = Size(100f, 40f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Welcome Sign Banner
    drawRoundRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(hX + 30f, houseTop - 150f),
        size = Size(houseWidth - 60f, 45f),
        cornerRadius = CornerRadius(8f, 8f)
    )
}

private fun DrawScope.drawParticles(engine: GameEngine) {
    for (p in engine.particles) {
        drawCircle(
            color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
            radius = p.size,
            center = Offset(p.x, p.y)
        )
    }
}
