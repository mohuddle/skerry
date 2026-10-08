@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.mohuddle.skerry.allowlist

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingPreferenceTest {

    @Test
    fun defaultsOffAndSurvivesANewStore() = runTest {
        val file = File.createTempFile("skerry-charging", ".preferences_pb").apply { delete() }
        val firstJob = SupervisorJob()
        val first = preference(file, firstJob)
        assertFalse(first.get())
        first.setEnabled(true)
        firstJob.cancelAndJoin()

        val secondJob = SupervisorJob()
        val second = preference(file, secondJob)
        assertTrue(second.get())
        second.setEnabled(false)
        assertFalse(second.get())
        secondJob.cancelAndJoin()
        file.delete()
    }

    private fun preference(file: File, job: CompletableJob): ChargingPreference {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + UnconfinedTestDispatcher()),
        ) { file }
        return ChargingPreference(dataStore)
    }
}
