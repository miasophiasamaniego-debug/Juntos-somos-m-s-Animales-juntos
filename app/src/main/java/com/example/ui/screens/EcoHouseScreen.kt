package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameProgressEntity
import com.example.data.HousePart

@Composable
fun EcoHouseScreen(
    progress: GameProgressEntity,
    onUpgrade: (HousePart, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFC8E6C9),
                        Color(0xFFA5D6A7)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "🏠 Casita de Botellas y Cartón",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B5E20)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Eco House Banner Image
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_eco_house_banner),
                        contentDescription = "Casita de Botellas",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Animal Guests floating tag
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🐾 Refugio de Animalitos Felices",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resource Balance Display
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🍾", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${progress.totalBottles} Botellas",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0288D1)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📦", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${progress.totalCardboard} Cartón",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Mejoras de la Casita Ecológica",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Upgrade Item 1: Roof
            UpgradeCard(
                title = "Techo de Cartón Reforzado",
                level = progress.houseRoofLevel,
                maxLevel = 5,
                emoji = "🏠",
                description = "Protege a los animales de la lluvia con capas de cartón reciclado.",
                bottleCost = progress.houseRoofLevel * 15,
                cardboardCost = progress.houseRoofLevel * 20,
                currentBottles = progress.totalBottles,
                currentCardboard = progress.totalCardboard,
                onUpgrade = {
                    onUpgrade(
                        HousePart.ROOF,
                        progress.houseRoofLevel * 15,
                        progress.houseRoofLevel * 20
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Upgrade Item 2: Walls
            UpgradeCard(
                title = "Paredes de Botellas de Plástico",
                level = progress.houseWallLevel,
                maxLevel = 5,
                emoji = "🍾",
                description = "Muros aislantes hechos con botellas llenas de luz.",
                bottleCost = progress.houseWallLevel * 20,
                cardboardCost = progress.houseWallLevel * 15,
                currentBottles = progress.totalBottles,
                currentCardboard = progress.totalCardboard,
                onUpgrade = {
                    onUpgrade(
                        HousePart.WALLS,
                        progress.houseWallLevel * 20,
                        progress.houseWallLevel * 15
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Upgrade Item 3: Garden
            UpgradeCard(
                title = "Jardín Ecológico",
                level = progress.houseGardenLevel,
                maxLevel = 5,
                emoji = "🌻",
                description = "Maceteros de botellas con flores y arbustos coloridos.",
                bottleCost = (progress.houseGardenLevel + 1) * 25,
                cardboardCost = (progress.houseGardenLevel + 1) * 20,
                currentBottles = progress.totalBottles,
                currentCardboard = progress.totalCardboard,
                onUpgrade = {
                    onUpgrade(
                        HousePart.GARDEN,
                        (progress.houseGardenLevel + 1) * 25,
                        (progress.houseGardenLevel + 1) * 20
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Upgrade Item 4: Solar
            UpgradeCard(
                title = "Paneles Solares y Luz",
                level = progress.houseSolarLevel,
                maxLevel = 5,
                emoji = "☀️",
                description = "Energía limpia para iluminar la casita de noche.",
                bottleCost = (progress.houseSolarLevel + 1) * 35,
                cardboardCost = (progress.houseSolarLevel + 1) * 30,
                currentBottles = progress.totalBottles,
                currentCardboard = progress.totalCardboard,
                onUpgrade = {
                    onUpgrade(
                        HousePart.SOLAR,
                        (progress.houseSolarLevel + 1) * 35,
                        (progress.houseSolarLevel + 1) * 30
                    )
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    level: Int,
    maxLevel: Int,
    emoji: String,
    description: String,
    bottleCost: Int,
    cardboardCost: Int,
    currentBottles: Int,
    currentCardboard: Int,
    onUpgrade: () -> Unit
) {
    val canAfford = currentBottles >= bottleCost && currentCardboard >= cardboardCost && level < maxLevel

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (level >= maxLevel) "¡Nivel Máximo!" else "Nivel $level / $maxLevel",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))

            if (level < maxLevel) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "🍾 $bottleCost",
                            color = if (currentBottles >= bottleCost) Color(0xFF0288D1) else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "📦 $cardboardCost",
                            color = if (currentCardboard >= cardboardCost) Color(0xFF795548) else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onUpgrade,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Mejorar")
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completado",
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¡Casita en Estado Perfecto!",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
