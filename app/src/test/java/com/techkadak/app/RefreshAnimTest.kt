package com.techkadak.app

import com.techkadak.app.data.UserSettings
import com.techkadak.app.ui.PullGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshAnimTest {
    @Test fun refreshAnimDefaultsOn() { assertTrue(UserSettings().refreshAnim) }
    @Test fun gapIsLinearThenRubberBands() {
        assertEquals(0f, PullGeometry.gap(0f, 100f), 0.001f)
        assertEquals(50f, PullGeometry.gap(0.5f, 100f), 0.001f)
        assertEquals(100f, PullGeometry.gap(1f, 100f), 0.001f)
        assertEquals(125f, PullGeometry.gap(2f, 100f), 0.001f)
        assertTrue(PullGeometry.gap(10f, 100f) <= 150f)
    }
    @Test fun badValuesAreSafe() {
        assertEquals(0f, PullGeometry.gap(Float.NaN, 100f), 0.001f)
        assertEquals(0f, PullGeometry.gap(-3f, 100f), 0.001f)
        assertEquals(0f, PullGeometry.grip(Float.NaN), 0.001f)
        assertEquals(1f, PullGeometry.grip(5f), 0.001f)
    }
}
