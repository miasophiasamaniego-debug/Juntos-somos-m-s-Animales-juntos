package com.example.game

data class GameLevel(
    val id: Int, // 1, 2, 3 or 0 for Endless
    val name: String,
    val description: String,
    val targetDistance: Float, // Distance in meters to reach the Eco House
    val baseCarSpeed: Float,
    val obstacleFrequency: Float,
    val bottleSpawnRate: Float,
    val cardboardSpawnRate: Float,
    val skyTopColorHex: Long = 0xFF81D4FA,
    val skyBottomColorHex: Long = 0xFFE0F7FA,
    val roadColorHex: Long = 0xFF546E7A
) {
    companion object {
        val ALL_LEVELS = listOf(
            GameLevel(
                id = 1,
                name = "Calle del Barrio",
                description = "Esquiva los primeros autos y junta botellas para tu casita.",
                targetDistance = 1500f,
                baseCarSpeed = 6f,
                obstacleFrequency = 1.0f,
                bottleSpawnRate = 1.2f,
                cardboardSpawnRate = 1.0f,
                skyTopColorHex = 0xFF81D4FA,
                skyBottomColorHex = 0xFFE0F7FA,
                roadColorHex = 0xFF546E7A
            ),
            GameLevel(
                id = 2,
                name = "Avenida Central",
                description = "Tráfico más fluido con taxis y camiones de cartón.",
                targetDistance = 2500f,
                baseCarSpeed = 8.5f,
                obstacleFrequency = 1.3f,
                bottleSpawnRate = 1.3f,
                cardboardSpawnRate = 1.3f,
                skyTopColorHex = 0xFF80CBC4,
                skyBottomColorHex = 0xFFE0F2F1,
                roadColorHex = 0xFF455A64
            ),
            GameLevel(
                id = 3,
                name = "Zona Industrial",
                description = "Gran desafío de salto con autos veloces y doble carril.",
                targetDistance = 3500f,
                baseCarSpeed = 11f,
                obstacleFrequency = 1.6f,
                bottleSpawnRate = 1.5f,
                cardboardSpawnRate = 1.5f,
                skyTopColorHex = 0xFFFFCC80,
                skyBottomColorHex = 0xFFFFF3E0,
                roadColorHex = 0xFF37474F
            ),
            GameLevel(
                id = 0,
                name = "Modo Infinito Reciclaje",
                description = "Corre sin fin para romper récords de puntuación y materiales.",
                targetDistance = Float.MAX_VALUE,
                baseCarSpeed = 7f,
                obstacleFrequency = 1.2f,
                bottleSpawnRate = 1.4f,
                cardboardSpawnRate = 1.4f,
                skyTopColorHex = 0xFFCE93D8,
                skyBottomColorHex = 0xFFF3E5F5,
                roadColorHex = 0xFF263238
            )
        )

        fun getById(id: Int): GameLevel {
            return ALL_LEVELS.firstOrNull { it.id == id } ?: ALL_LEVELS.first()
        }
    }
}
