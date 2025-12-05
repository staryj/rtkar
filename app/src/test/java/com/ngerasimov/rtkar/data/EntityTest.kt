package com.ngerasimov.rtkar.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Entity data class
 */
class EntityTest {
    
    @Test
    fun `entity equality works correctly`() {
        // Given
        val entity1 = Entity(EntityType.BARRIER, "Test Device", "device1")
        val entity2 = Entity(EntityType.BARRIER, "Test Device", "device1")
        val entity3 = Entity(EntityType.DOOR, "Test Device", "device1")
        
        // Then
        assertEquals(entity1, entity2)
        assertNotEquals(entity1, entity3)
    }
    
    @Test
    fun `entity hashCode is consistent`() {
        // Given
        val entity1 = Entity(EntityType.BARRIER, "Test Device", "device1")
        val entity2 = Entity(EntityType.BARRIER, "Test Device", "device1")
        
        // Then
        assertEquals(entity1.hashCode(), entity2.hashCode())
    }
    
    @Test
    fun `entity toString contains all fields`() {
        // Given
        val entity = Entity(EntityType.BARRIER, "Test Device", "device1")
        
        // When
        val toString = entity.toString()
        
        // Then
        assertTrue(toString.contains("BARRIER"))
        assertTrue(toString.contains("Test Device"))
        assertTrue(toString.contains("device1"))
    }
}
