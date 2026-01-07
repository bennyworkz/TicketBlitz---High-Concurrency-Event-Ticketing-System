package com.ticketblitz.inventory.service;

import com.ticketblitz.common.util.RedisKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Tatkal Inventory Service
 * Handles first-come-first-served inventory for Tatkal events
 * 
 * Key Features:
 * - Atomic decrement (DECR)
 * - No seat selection
 * - Fast booking (no locking overhead)
 * - Inventory tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TatkalInventoryService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Initialize inventory for a Tatkal event
     * 
     * @param eventId Event ID
     * @param totalSeats Total available seats
     */
    public void initializeInventory(Long eventId, int totalSeats) {
        String inventoryKey = RedisKeyGenerator.tatkalInventoryKey(eventId);
        redisTemplate.opsForValue().set(inventoryKey, String.valueOf(totalSeats));
        log.info("Initialized Tatkal inventory: event={}, seats={}", eventId, totalSeats);
    }

    /**
     * Try to reserve a seat (atomic decrement)
     * 
     * @param eventId Event ID
     * @return true if seat reserved, false if sold out
     */
    public boolean tryReserveSeat(Long eventId) {
        String inventoryKey = RedisKeyGenerator.tatkalInventoryKey(eventId);
        
        // DECR is atomic - no race condition
        Long remaining = redisTemplate.opsForValue().decrement(inventoryKey);
        
        if (remaining == null || remaining < 0) {
            // Sold out - increment back
            if (remaining != null && remaining < 0) {
                redisTemplate.opsForValue().increment(inventoryKey);
            }
            log.warn("Tatkal event sold out: event={}", eventId);
            return false;
        }
        
        log.info("Tatkal seat reserved: event={}, remaining={}", eventId, remaining);
        return true;
    }

    /**
     * Release a reserved seat (increment inventory)
     * Used when booking fails or is cancelled
     * 
     * @param eventId Event ID
     */
    public void releaseSeat(Long eventId) {
        String inventoryKey = RedisKeyGenerator.tatkalInventoryKey(eventId);
        Long remaining = redisTemplate.opsForValue().increment(inventoryKey);
        log.info("Tatkal seat released: event={}, remaining={}", eventId, remaining);
    }

    /**
     * Get remaining inventory
     * 
     * @param eventId Event ID
     * @return Number of available seats, or -1 if not initialized
     */
    public int getRemainingInventory(Long eventId) {
        String inventoryKey = RedisKeyGenerator.tatkalInventoryKey(eventId);
        String value = redisTemplate.opsForValue().get(inventoryKey);
        
        if (value == null) {
            log.warn("Tatkal inventory not initialized: event={}", eventId);
            return -1;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Invalid inventory value: event={}, value={}", eventId, value);
            return -1;
        }
    }

    /**
     * Check if event is sold out
     * 
     * @param eventId Event ID
     * @return true if sold out, false otherwise
     */
    public boolean isSoldOut(Long eventId) {
        int remaining = getRemainingInventory(eventId);
        return remaining <= 0;
    }

    /**
     * Reset inventory (admin operation)
     * 
     * @param eventId Event ID
     * @param totalSeats New total seats
     */
    public void resetInventory(Long eventId, int totalSeats) {
        String inventoryKey = RedisKeyGenerator.tatkalInventoryKey(eventId);
        redisTemplate.opsForValue().set(inventoryKey, String.valueOf(totalSeats));
        log.warn("Tatkal inventory reset: event={}, seats={}", eventId, totalSeats);
    }

    /**
     * Delete inventory (when event is deleted)
     * 
     * @param eventId Event ID
     */
    public void deleteInventory(Long eventId) {
        String inventoryKey = RedisKeyGenerator.tatkalInventoryKey(eventId);
        redisTemplate.delete(inventoryKey);
        log.info("Tatkal inventory deleted: event={}", eventId);
    }
}
