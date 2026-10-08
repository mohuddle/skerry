@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.mohuddle.skerry.allowlist

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowlistStoreTest {

    @Test
    fun startsEmpty() = runTest {
        val harness = harness()
        assertEquals(emptySet<String>(), harness.store.list())
        assertFalse(harness.store.contains("com.spotify.music"))
        harness.close()
    }

    @Test
    fun addContainsListAndRemove() = runTest {
        val harness = harness()
        harness.store.add("com.spotify.music")
        harness.store.add("com.spotify.music")
        harness.store.add("io.github.mohuddle.aftercast")

        assertTrue(harness.store.contains("com.spotify.music"))
        assertEquals(
            setOf("com.spotify.music", "io.github.mohuddle.aftercast"),
            harness.store.list(),
        )

        harness.store.remove("com.spotify.music")
        assertFalse(harness.store.contains("com.spotify.music"))
        assertEquals(setOf("io.github.mohuddle.aftercast"), harness.store.list())
        harness.close()
    }

    @Test
    fun persistsAcrossANewStore() = runTest {
        val file = tempFile()
        val firstJob = SupervisorJob()
        val first = store(file, firstJob)
        first.add("com.google.android.apps.messaging")
        firstJob.cancelAndJoin()

        val secondJob = SupervisorJob()
        val second = store(file, secondJob)
        assertTrue(second.contains("com.google.android.apps.messaging"))
        assertEquals(setOf("com.google.android.apps.messaging"), second.list())
        secondJob.cancelAndJoin()
        file.delete()
    }

    private fun harness(): Harness {
        val file = tempFile()
        val job = SupervisorJob()
        return Harness(store(file, job), job, file)
    }

    private class Harness(
        val store: AllowlistStore,
        private val job: Job,
        private val file: File,
    ) {
        suspend fun close() {
            job.cancelAndJoin()
            file.delete()
        }
    }

    private fun store(file: File, job: Job): AllowlistStore {
        val scope = CoroutineScope(job + UnconfinedTestDispatcher())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        return AllowlistStore(dataStore)
    }

    private fun tempFile(): File {
        return File.createTempFile("skerry-allowlist", ".preferences_pb").apply { delete() }
    }
}
