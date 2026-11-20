package com.example

import com.example.audio.VSTracksAudioEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioRoutingTest {

    @Test
    fun `standard split routes left source to left and right source to right`() {
        val samples = shortArrayOf(
            10000, 2000,
            -8000, 4000
        )

        VSTracksAudioEngine.applyStereoRoutingInPlace(
            interleavedStereo = samples,
            frameCount = 2,
            leftVol = 0.8f,
            rightVol = 0.5f,
            masterVol = 1.0f,
            isStereoSplitEnabled = true,
            isLRSwapped = false
        )

        assertEquals(8000.toShort(), samples[0])
        assertEquals(1000.toShort(), samples[1])
        assertEquals((-6400).toShort(), samples[2])
        assertEquals(2000.toShort(), samples[3])
    }

    @Test
    fun `inverted LR swaps left and right PCM samples cleanly`() {
        // Simulates 1 kHz Click on logical Left (16000) and silent logical Right (0)
        val clickOnlySamples = shortArrayOf(16000, 0, -16000, 0)

        VSTracksAudioEngine.applyStereoRoutingInPlace(
            interleavedStereo = clickOnlySamples,
            frameCount = 2,
            leftVol = 1.0f,
            rightVol = 0.9f,
            masterVol = 1.0f,
            isStereoSplitEnabled = true,
            isLRSwapped = true
        )

        // Click must now appear ONLY on Physical Right, Physical Left must be 0
        assertEquals(0.toShort(), clickOnlySamples[0])
        assertEquals(14400.toShort(), clickOnlySamples[1])
        assertEquals(0.toShort(), clickOnlySamples[2])
        assertEquals((-14400).toShort(), clickOnlySamples[3])
    }

    @Test
    fun `inverted LR swaps asymmetric stereo program material`() {
        val stereoSamples = shortArrayOf(12000, 4000)

        VSTracksAudioEngine.applyStereoRoutingInPlace(
            interleavedStereo = stereoSamples,
            frameCount = 1,
            leftVol = 1.0f,
            rightVol = 0.5f,
            masterVol = 1.0f,
            isStereoSplitEnabled = true,
            isLRSwapped = true
        )

        // Physical Left gets srcR (4000) * leftVol (1.0) = 4000
        // Physical Right gets srcL (12000) * rightVol (0.5) = 6000
        assertEquals(4000.toShort(), stereoSamples[0])
        assertEquals(6000.toShort(), stereoSamples[1])
    }

    @Test
    fun `mono mode sums left and right into both channels`() {
        val samples = shortArrayOf(10000, 2000)

        VSTracksAudioEngine.applyStereoRoutingInPlace(
            interleavedStereo = samples,
            frameCount = 1,
            leftVol = 1.0f,
            rightVol = 0.5f,
            masterVol = 1.0f,
            isStereoSplitEnabled = false,
            isLRSwapped = true
        )

        // Mono average = (10000 + 2000) / 2 = 6000
        assertEquals(6000.toShort(), samples[0])
        assertEquals(3000.toShort(), samples[1])
    }
}
