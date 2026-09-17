package com.example

import android.content.Context
import android.location.Location
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.local.entity.PendingLocationEntity
import com.example.data.repository.GroupTrackRepository
import com.example.service.LocationUpdateStrategy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun read_string_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GroupTrack", appName)
  }

  @Test
  fun location_strategy_validates_coordinates_and_accuracy() {
    val validLoc = Location("gps").apply {
      latitude = 13.0827
      longitude = 80.2707
      accuracy = 10.0f
      time = System.currentTimeMillis()
    }
    assertTrue(LocationUpdateStrategy.isValidLocation(validLoc))

    // Reject out-of-bounds latitude
    val invalidLat = Location("gps").apply {
      latitude = 95.0
      longitude = 80.2707
      accuracy = 10.0f
      time = System.currentTimeMillis()
    }
    assertFalse(LocationUpdateStrategy.isValidLocation(invalidLat))

    // Reject impossible accuracy (> 150m)
    val badAccuracy = Location("gps").apply {
      latitude = 13.0827
      longitude = 80.2707
      accuracy = 350.0f
      time = System.currentTimeMillis()
    }
    assertFalse(LocationUpdateStrategy.isValidLocation(badAccuracy))

    // Reject stale timestamp (> 5 mins old)
    val staleLoc = Location("gps").apply {
      latitude = 13.0827
      longitude = 80.2707
      accuracy = 10.0f
      time = System.currentTimeMillis() - (10 * 60 * 1000L)
    }
    assertFalse(LocationUpdateStrategy.isValidLocation(staleLoc))
  }

  @Test
  fun room_pending_location_queue_offline_sync() = runBlocking {
    val dao = db.pendingLocationDao()

    val pending = PendingLocationEntity(
      groupId = "group-1",
      latitude = 13.0827,
      longitude = 80.2707,
      accuracy = 12f,
      altitude = 15.0,
      speed = 0f,
      bearing = 0f,
      recordedAt = "2026-09-16T12:00:00.000Z"
    )

    val id = dao.insertLocation(pending)
    assertTrue(id > 0)

    val batch = dao.getPendingBatch(10)
    assertEquals(1, batch.size)
    assertEquals("group-1", batch[0].groupId)
    assertEquals(13.0827, batch[0].latitude, 0.0001)

    // Delete once synced
    dao.deleteLocationsByIds(listOf(batch[0].id))
    val remaining = dao.getPendingBatch(10)
    assertEquals(0, remaining.size)
  }

  @Test
  fun manikandan_login_credentials_test() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessionManager = SessionManager(context)
    val repository = GroupTrackRepository(context, sessionManager, db)

    val result = repository.login("manikandan30122k2@gmail.com", "Mani@3012")
    assertTrue(result.isSuccess)
    val auth = result.getOrNull()
    assertNotNull(auth)
    assertEquals("manikandan30122k2@gmail.com", auth?.user?.email)
    assertEquals("Manikandan", auth?.user?.name)
  }

  @Test
  fun error_boundary_controller_captures_and_clears_exception() {
    val controller = com.example.ui.components.DefaultErrorBoundaryController()
    assertNull(controller.currentError)

    val runtimeException = IllegalStateException("Simulated crash in map rendering")
    controller.reportError(runtimeException)

    assertNotNull(controller.currentError)
    assertEquals("Simulated crash in map rendering", controller.currentError?.message)

    controller.clearError()
    assertNull(controller.currentError)
  }
}
