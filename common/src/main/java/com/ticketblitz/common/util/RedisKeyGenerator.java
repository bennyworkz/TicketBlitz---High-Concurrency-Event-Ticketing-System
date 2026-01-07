package com.ticketblitz.common.util;

/**
 * Utility class for generating consistent Redis keys across services
 */
public class RedisKeyGenerator {
    
    private static final String LOCK_PREFIX = "lock";
    private static final String INVENTORY_PREFIX = "inventory";
    private static final String CACHE_PREFIX = "cache";
    
    /**
     * Generate lock key for seat selection
     * Format: lock:event:{eventId}:seat:{seatId}
     */
    public static String seatLockKey(Long eventId, String seatId) {
        return String.format("%s:event:%d:seat:%s", LOCK_PREFIX, eventId, seatId);
    }
    
    /**
     * Generate inventory key for Tatkal booking
     * Format: inventory:event:{eventId}
     */
    public static String inventoryKey(Long eventId) {
        return String.format("%s:event:%d", INVENTORY_PREFIX, eventId);
    }
    
    /**
     * Alias for inventoryKey (for clarity in Tatkal context)
     * Format: inventory:event:{eventId}
     */
    public static String tatkalInventoryKey(Long eventId) {
        return inventoryKey(eventId);
    }
    
    /**
     * Generate cache key for event
     * Format: cache:event:{eventId}
     */
    public static String eventCacheKey(Long eventId) {
        return String.format("%s:event:%d", CACHE_PREFIX, eventId);
    }
    
    /**
     * Generate cache key for seat availability
     * Format: cache:seats:event:{eventId}
     */
    public static String seatsCacheKey(Long eventId) {
        return String.format("%s:seats:event:%d", CACHE_PREFIX, eventId);
    }
}
