package com.ngerasimov.rtkar.data.repository

import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.EntityType
import com.ngerasimov.rtkar.data.api.Device
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.StorageManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for DeviceRepository
 */
class DeviceRepositoryTest {
    
    private lateinit var mockApiWrapper: RtkApiWrapper
    private lateinit var mockStorageManager: StorageManager
    private lateinit var repository: DeviceRepository
    
    @Before
    fun setup() {
        mockApiWrapper = mock()
        mockStorageManager = mock()
        repository = DeviceRepository(mockApiWrapper, mockStorageManager)
    }
    
    @Test
    fun `getBarrierDevices delegates to apiWrapper`() = runTest {
        // Given
        val authToken = "test-token"
        val expectedDevices = listOf<Device>()
        whenever(mockApiWrapper.getBarrierDevices(authToken, false))
            .thenReturn(Result.success(expectedDevices))
        
        // When
        val result = repository.getBarrierDevices(authToken)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedDevices, result.getOrNull())
        verify(mockApiWrapper).getBarrierDevices(authToken, false)
    }
    
    @Test
    fun `authToken flow delegates to storageManager`() = runTest {
        // Given
        val expectedToken = "test-token"
        whenever(mockStorageManager.authToken).thenReturn(flowOf(expectedToken))
        
        // When
        val token = repository.authToken.first()
        
        // Then
        assertEquals(expectedToken, token)
    }
    
    @Test
    fun `entities flow delegates to storageManager`() = runTest {
        // Given
        val expectedEntities = listOf(
            Entity(EntityType.BARRIER, "Test", "device1")
        )
        whenever(mockStorageManager.entities).thenReturn(flowOf(expectedEntities))
        
        // When
        val entities = repository.entities.first()
        
        // Then
        assertEquals(expectedEntities, entities)
    }
    
    @Test
    fun `saveAuthToken delegates to storageManager`() = runTest {
        // Given
        val token = "test-token"
        coDoNothing().whenever(mockStorageManager).saveAuthToken(any())
        
        // When
        repository.saveAuthToken(token)
        
        // Then
        coVerify(mockStorageManager).saveAuthToken(token)
    }
}
