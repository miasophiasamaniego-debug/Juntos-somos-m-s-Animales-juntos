package com.example.game

import androidx.compose.ui.graphics.Color

data class AnimalCharacter(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val specialAbility: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val speedMultiplier: Float = 1.0f,
    val jumpPowerMultiplier: Float = 1.0f,
    val doubleJumpAllowed: Boolean = true,
    val hasShield: Boolean = false,
    val isMagnet: Boolean = false,
    val bottleCost: Int = 0,
    val cardboardCost: Int = 0
) {
    companion object {
        val ALL_ANIMALS = listOf(
            AnimalCharacter(
                id = "bunny",
                name = "Conejito Eco",
                emoji = "🐰",
                description = "Un conejito alegre que salta alto sobre los autos.",
                specialAbility = "Salto Doble Balanceado",
                primaryColor = Color(0xFFFFB74D),
                secondaryColor = Color(0xFFFFF3E0),
                speedMultiplier = 1.0f,
                jumpPowerMultiplier = 1.1f,
                doubleJumpAllowed = true,
                bottleCost = 0,
                cardboardCost = 0
            ),
            AnimalCharacter(
                id = "puppy",
                name = "Perrito Veloz",
                emoji = "🐶",
                description = "El mas rapido para correr y esquivar el trafico.",
                specialAbility = "Velocidad +20%",
                primaryColor = Color(0xFF8D6E63),
                secondaryColor = Color(0xFFD7CCC8),
                speedMultiplier = 1.25f,
                jumpPowerMultiplier = 1.0f,
                doubleJumpAllowed = true,
                bottleCost = 15,
                cardboardCost = 10
            ),
            AnimalCharacter(
                id = "kitty",
                name = "Gatito Acróbata",
                emoji = "🐱",
                description = "Acróbata felino que alcanza plataformas elevadas.",
                specialAbility = "Super Salto Doble (+25%)",
                primaryColor = Color(0xFFFF8A65),
                secondaryColor = Color(0xFFFFCCBC),
                speedMultiplier = 1.05f,
                jumpPowerMultiplier = 1.35f,
                doubleJumpAllowed = true,
                bottleCost = 25,
                cardboardCost = 15
            ),
            AnimalCharacter(
                id = "bear",
                name = "Oso Guardián",
                emoji = "🐻",
                description = "Protegido con un casco de cartón que absorbe 1 choque.",
                specialAbility = "Escudo Antichoque Inicial",
                primaryColor = Color(0xFF795548),
                secondaryColor = Color(0xFFA1887F),
                speedMultiplier = 0.95f,
                jumpPowerMultiplier = 1.0f,
                doubleJumpAllowed = true,
                hasShield = true,
                bottleCost = 40,
                cardboardCost = 30
            ),
            AnimalCharacter(
                id = "capybara",
                name = "Capibara Eco-Imán",
                emoji = "🦫",
                description = "Capibara sabio que atrae botellas y cartón automáticamente.",
                specialAbility = "Imán de Reciclaje Mágico",
                primaryColor = Color(0xFF4CAF50),
                secondaryColor = Color(0xFFC8E6C9),
                speedMultiplier = 1.1f,
                jumpPowerMultiplier = 1.15f,
                doubleJumpAllowed = true,
                isMagnet = true,
                bottleCost = 60,
                cardboardCost = 50
            )
        )

        fun getById(id: String): AnimalCharacter {
            return ALL_ANIMALS.firstOrNull { it.id == id } ?: ALL_ANIMALS.first()
        }
    }
}
