package CFBsimPack

import kotlinx.serialization.Serializable

@Serializable
data class FbsSeedFile(
    val season: Int = 2026,
    val teams: List<FbsTeamSeed> = emptyList(),
)

@Serializable
data class FbsTeamSeed(
    val conference: String,
    val name: String,
    val abbr: String,
    val tradition: Int,
    val fanbase: Int,
    val donors: Int,
    val footprint: Int,
    val pipeline: Int,
    val momentum: Int,
    val rivals: List<FbsRivalSeed> = emptyList(),
)

@Serializable
data class FbsRivalSeed(
    val abbr: String,
    val strength: Int,
)
