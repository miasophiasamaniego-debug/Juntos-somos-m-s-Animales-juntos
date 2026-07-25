package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvasView(
    engine: GameEngine,
    onTapJump: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 60 FPS / VSYNC Frame Ticker for butter-smooth Canvas redraws
    var frameTimeNanos by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameTimeNanos = it }
        }
    }

    // Reusable Path objects to prevent GC allocations per frame
    val reusablePath1 = remember { Path() }
    val reusablePath2 = remember { Path() }

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
            // Bind to VSYNC frame tick
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTimeNanos

            engine.updateDimensions(size.width, size.height)

            // 1. Draw Parallax Background
            drawSkyAndMountains(engine, reusablePath1, reusablePath2)

            // 2. Draw Road
            drawRoad(engine)

            // 3. Draw Finish Line Eco House (if approaching)
            if (engine.houseX > -500f) {
                drawEcoHouseFinish(engine, reusablePath1)
            }

            // 4. Draw Collectibles & Powerups
            drawCollectiblesAndPowerups(engine, reusablePath1)

            // 5. Draw Realistic Cars / Obstacles
            drawRealisticCars(engine, reusablePath1, reusablePath2)

            // 6. Draw Player (Animal with Active Powerup Visual Effects)
            drawPlayerAnimal(engine, reusablePath1, reusablePath2)

            // 7. Draw Particles
            drawParticles(engine)
        }
    }
}

private fun DrawScope.drawSkyAndMountains(
    engine: GameEngine,
    path1: Path,
    path2: Path
) {
    val level = engine.level

    // Sky Background
    drawRect(
        color = Color(level.skyTopColorHex),
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
    path1.reset()
    path1.moveTo(-scrollOffset, engine.groundY)
    path1.cubicTo(
        -scrollOffset + size.width * 0.25f, engine.groundY - 140f,
        -scrollOffset + size.width * 0.5f, engine.groundY - 60f,
        -scrollOffset + size.width * 0.75f, engine.groundY - 180f
    )
    path1.lineTo(-scrollOffset + size.width, engine.groundY)
    path1.close()
    drawPath(path = path1, color = Color(0xFFC8E6C9))

    path2.reset()
    val o2 = (scrollOffset + size.width * 0.5f) % size.width
    path2.moveTo(-o2, engine.groundY)
    path2.cubicTo(
        -o2 + size.width * 0.3f, engine.groundY - 120f,
        -o2 + size.width * 0.6f, engine.groundY - 190f,
        -o2 + size.width, engine.groundY - 80f
    )
    path2.lineTo(-o2 + size.width * 1.2f, engine.groundY)
    path2.close()
    drawPath(path = path2, color = Color(0xFFA5D6A7))
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

private fun DrawScope.drawRealisticCars(
    engine: GameEngine,
    beamPath: Path,
    glassPath: Path
) {
    for (car in engine.cars) {
        val carLeft = car.x
        val carTop = car.y - car.height
        val cWidth = car.width
        val cHeight = car.height

        // 1. Headlight Cone Beam on Road
        beamPath.reset()
        beamPath.moveTo(carLeft, car.y - 15f)
        beamPath.lineTo(carLeft - 180f, car.y + 10f)
        beamPath.lineTo(carLeft - 180f, car.y - 45f)
        beamPath.close()
        drawPath(
            path = beamPath,
            color = Color(0xFFFFF59D).copy(alpha = 0.2f)
        )

        // 2. Ground Shadow under Car
        drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(carLeft - 10f, car.y - 12f),
            size = Size(cWidth + 20f, 20f)
        )

        when (car.type) {
            CarType.RED_CAR, CarType.TAXI, CarType.SPORTS -> {
                val mainColor = car.color

                // Lower Body
                drawRoundRect(
                    color = mainColor,
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
                    color = mainColor,
                    topLeft = Offset(cabinLeft, cabinTop),
                    size = Size(cabinWidth, cabinHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )

                // Glass Windshield
                glassPath.reset()
                glassPath.moveTo(cabinLeft + 8f, cabinTop + 6f)
                glassPath.lineTo(cabinLeft + cabinWidth - 8f, cabinTop + 6f)
                glassPath.lineTo(cabinLeft + cabinWidth - 2f, cabinTop + cabinHeight - 4f)
                glassPath.lineTo(cabinLeft + 2f, cabinTop + cabinHeight - 4f)
                glassPath.close()
                drawPath(path = glassPath, color = Color(0xFF81D4FA))

                // TAXI Sign
                if (car.type == CarType.TAXI) {
                    drawRoundRect(
                        color = Color(0xFFFF6F00),
                        topLeft = Offset(cabinLeft + cabinWidth * 0.3f, cabinTop - 14f),
                        size = Size(cabinWidth * 0.4f, 14f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

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
            }
            CarType.TRUCK -> {
                // Truck Body
                drawRoundRect(
                    color = Color(0xFFE53935),
                    topLeft = Offset(carLeft, carTop + cHeight * 0.2f),
                    size = Size(cWidth * 0.35f, cHeight * 0.7f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = Color(0xFF8D6E63),
                    topLeft = Offset(carLeft + cWidth * 0.32f, carTop),
                    size = Size(cWidth * 0.68f, cHeight * 0.88f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
            CarType.BUS -> {
                // City Bus
                drawRoundRect(
                    color = car.color,
                    topLeft = Offset(carLeft, carTop),
                    size = Size(cWidth, cHeight * 0.88f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }

        // Wheels
        val wheelRadius = cHeight * 0.2f
        val wheelsY = car.y - 4f
        val wheelOffsets = when (car.type) {
            CarType.BUS, CarType.TRUCK -> listOf(0.18f, 0.65f, 0.85f)
            else -> listOf(0.22f, 0.78f)
        }

        for (wf in wheelOffsets) {
            val wx = carLeft + cWidth * wf
            drawCircle(
                color = Color(0xFF212121),
                radius = wheelRadius,
                center = Offset(wx, wheelsY)
            )
            drawCircle(
                color = Color(0xFFCFD8DC),
                radius = wheelRadius * 0.55f,
                center = Offset(wx, wheelsY)
            )
        }
    }
}

private fun DrawScope.drawCollectiblesAndPowerups(
    engine: GameEngine,
    path: Path
) {
    for (item in engine.items) {
        if (item.isCollected) continue

        val floatOffset = sin(engine.distanceTraveled * 0.08f + item.x) * 9f
        val itemCenter = Offset(item.x, item.y + floatOffset)

        when (item.type) {
            CollectibleType.BOTTLE -> {
                drawRoundRect(
                    color = Color(0xFF29B6F6).copy(alpha = 0.85f),
                    topLeft = Offset(itemCenter.x - 14f, itemCenter.y - 24f),
                    size = Size(28f, 48f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRect(
                    color = Color(0xFFFF5252),
                    topLeft = Offset(itemCenter.x - 8f, itemCenter.y - 32f),
                    size = Size(16f, 8f)
                )
            }
            CollectibleType.CARDBOARD -> {
                drawRoundRect(
                    color = Color(0xFFBCAAA4),
                    topLeft = Offset(itemCenter.x - 22f, itemCenter.y - 22f),
                    size = Size(44f, 44f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRect(
                    color = Color(0xFF8D6E63),
                    topLeft = Offset(itemCenter.x - 6f, itemCenter.y - 22f),
                    size = Size(12f, 44f)
                )
            }
            CollectibleType.SUPER_JUMP -> {
                drawCircle(
                    color = Color(0xFFFFB300),
                    radius = 24f,
                    center = itemCenter
                )
                drawCircle(
                    color = Color(0xFFFF6F00),
                    radius = 24f,
                    center = itemCenter,
                    style = Stroke(width = 4f)
                )
            }
            CollectibleType.LEAF_SHIELD -> {
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 24f,
                    center = itemCenter
                )
                drawCircle(
                    color = Color(0xFF1B5E20),
                    radius = 24f,
                    center = itemCenter,
                    style = Stroke(width = 4f)
                )
            }
            CollectibleType.SPEED_BOOTS -> {
                drawCircle(
                    color = Color(0xFFFF3D00),
                    radius = 24f,
                    center = itemCenter
                )
                drawCircle(
                    color = Color(0xFFBF360C),
                    radius = 24f,
                    center = itemCenter,
                    style = Stroke(width = 4f)
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

private fun DrawScope.drawPlayerAnimal(
    engine: GameEngine,
    earPath1: Path,
    earPath2: Path
) {
    val pX = engine.playerX
    val pY = engine.playerY
    val r = engine.playerRadius
    val animal = engine.character

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

    // POWER-UP VISUAL FX
    if (engine.leafShieldActive) {
        drawCircle(
            color = Color(0xFF2E7D32),
            radius = r * 1.55f,
            center = Offset(pX, pY - r),
            style = Stroke(width = 5f)
        )
    }

    if (engine.superJumpFrames > 0) {
        drawCircle(
            color = Color(0xFFFF8F00),
            radius = r * 1.6f,
            center = Offset(pX, pY - r),
            style = Stroke(width = 4f)
        )
    }

    rotate(degrees = engine.rotationAngle, pivot = Offset(pX, pY - r)) {
        val bodyColor = animal.primaryColor
        val bellyColor = animal.secondaryColor

        val bodyHeight = if (engine.isDucking) r * 1.1f else r * 1.8f
        val bodyTop = pY - bodyHeight

        when (animal.id) {
            "bunny" -> {
                drawOval(
                    color = bodyColor,
                    topLeft = Offset(pX - 18f, bodyTop - 38f),
                    size = Size(14f, 42f)
                )
                drawOval(
                    color = bodyColor,
                    topLeft = Offset(pX + 4f, bodyTop - 38f),
                    size = Size(14f, 42f)
                )
            }
            "puppy" -> {
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
                earPath1.reset()
                earPath1.moveTo(pX - 20f, bodyTop)
                earPath1.lineTo(pX - 10f, bodyTop - 25f)
                earPath1.lineTo(pX, bodyTop)
                earPath1.close()
                drawPath(earPath1, bodyColor)

                earPath2.reset()
                earPath2.moveTo(pX, bodyTop)
                earPath2.lineTo(pX + 10f, bodyTop - 25f)
                earPath2.lineTo(pX + 20f, bodyTop)
                earPath2.close()
                drawPath(earPath2, bodyColor)
            }
            "bear", "capybara" -> {
                drawCircle(color = bodyColor, radius = 14f, center = Offset(pX - 18f, bodyTop))
                drawCircle(color = bodyColor, radius = 14f, center = Offset(pX + 18f, bodyTop))
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

        // Eyes & Nose
        val eyeY = bodyTop + bodyHeight * 0.3f
        drawCircle(color = Color.Black, radius = 5f, center = Offset(pX - 10f, eyeY))
        drawCircle(color = Color.Black, radius = 5f, center = Offset(pX + 10f, eyeY))
        drawCircle(color = Color(0xFFFF80AB), radius = 4f, center = Offset(pX, eyeY + 8f))

        if (engine.speedBootsFrames > 0 || engine.superJumpFrames > 0) {
            val bootColor = if (engine.speedBootsFrames > 0) Color(0xFFFF3D00) else Color(0xFFFFB300)
            drawRoundRect(
                color = bootColor,
                topLeft = Offset(pX - 22f, pY - 14f),
                size = Size(18f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = bootColor,
                topLeft = Offset(pX + 4f, pY - 14f),
                size = Size(18f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

private fun DrawScope.drawEcoHouseFinish(
    engine: GameEngine,
    roofPath: Path
) {
    val hX = engine.houseX
    val groundY = engine.groundY

    val houseWidth = 380f
    val houseHeight = 320f
    val houseTop = groundY - houseHeight

    // Main Cardboard Structure
    drawRoundRect(
        color = Color(0xFFD7CCC8),
        topLeft = Offset(hX, houseTop),
        size = Size(houseWidth, houseHeight),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // Roof
    roofPath.reset()
    roofPath.moveTo(hX - 30f, houseTop)
    roofPath.lineTo(hX + houseWidth / 2f, houseTop - 120f)
    roofPath.lineTo(hX + houseWidth + 30f, houseTop)
    roofPath.close()
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
        }
    }

    // Door
    drawRoundRect(
        color = Color(0xFFA1887F),
        topLeft = Offset(hX + houseWidth / 2f - 40f, groundY - 110f),
        size = Size(80f, 110f),
        cornerRadius = CornerRadius(10f, 10f)
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
