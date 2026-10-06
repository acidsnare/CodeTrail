package codetrail.core.progress

/**
 * Everything the player has earned. Stars are the single currency: they unlock characters.
 * A level pays out once, the first time it is solved, so replaying does not farm stars.
 */
data class Progress(
    val totalStars: Int = 0,
    val levelsWon: Int = 0,
    /** Levels won per difficulty tier. */
    val wonPerTier: Map<Int, Int> = emptyMap(),
    /** Levels won per world id. */
    val wonPerWorld: Map<String, Int> = emptyMap(),
    /** Seeds of levels already paid out, keyed as "tier:seed". Bounded, oldest dropped. */
    val rewarded: Set<String> = emptySet(),
) {
    fun key(tier: Int, seed: Long) = "$tier:$seed"

    fun isRewarded(tier: Int, seed: Long) = key(tier, seed) in rewarded

    fun reward(tier: Int, seed: Long, stars: Int, worldId: String? = null): Progress {
        if (isRewarded(tier, seed) || stars <= 0) return this
        val keys = (rewarded + key(tier, seed)).let { if (it.size > MAX_REWARDED) it.drop(it.size - MAX_REWARDED).toSet() else it }
        return copy(
            totalStars = totalStars + stars,
            levelsWon = levelsWon + 1,
            wonPerTier = wonPerTier + (tier to (wonPerTier[tier] ?: 0) + 1),
            wonPerWorld = if (worldId == null) wonPerWorld else wonPerWorld + (worldId to (wonPerWorld[worldId] ?: 0) + 1),
            rewarded = keys,
        )
    }

    companion object {
        const val MAX_REWARDED = 500
    }
}
