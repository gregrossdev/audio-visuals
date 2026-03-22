package audio

data class AudioFeatures(
    val bandEnergies: BandEnergies,
    val spectralCentroid: Float,
    val onsets: OnsetEvents,
    val energy: Float
)
