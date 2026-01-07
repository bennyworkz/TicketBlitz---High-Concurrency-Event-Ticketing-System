package com.ticketblitz.inventory.service;

import com.ticketblitz.common.util.RedisKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Seat Lock Service
 * Handles distributed locking of seats using Redis
 * 
 * Key Features:
 * - Atomic lock acquisition (SET NX EX)
 * - 10-minute lock expiry (TTL)
 * - Lock ownership verification
 * - Automatic cleanup on expiry
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;
    
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    /**
     * Try to lock a seat for a user
     * 
     * @param eventId Event ID
     * @param seatId Seat identifier (e.g., "A1", "B5")
     * @param userId User ID attempting to lock
     * @return true if lock acquired, false if already locked
     */
    public boolean tryLockSeat(Long eventId, String seatId, String userId) {
        String lockKey = RedisKeyGenerator.seatLockKey(eventId, seatId);
        
        // SET NX EX: Set if Not eXists with EXpiry
        // This is atomic - no race condition possible
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, userId, LOCK_DURATION);
        
        if (Boolean.TRUE.equals(locked)) {
            log.info("Seat locked: event={}, seat={}, user={}", eventId, seatId, userId);
            return true;
        }
        
        // Check if already locked by same user
        String currentOwner = redisTemplate.opsForValue().get(lockKey);
        if (userId.equals(currentOwner)) {
            log.info("Seat already locked by same user: event={}, seat={}, user={}", 
                    eventId, seatId, userId);
            // Refresh TTL
            redisTemplate.expire(lockKey, LOCK_DURATION);
            return true;
        }
        
        log.warn("Seat already locked by another user: event={}, seat={}, owner={}", 
                eventId, seatId, currentOwner);
        return false;
    }

    /**
     * Try to lock multiple seats atomically
     * Either all seats are locked or none
     * 
     * @param eventId Event ID
     * @param seatIds List of seat identifiers
     * @param userId User ID
     * @return true if all seats locked, false otherwise
     */
    public boolean tryLockSeats(Long eventId, List<String> seatIds, String userId) {
        log.info("Attempting to lock {} seats for user {}", seatIds.size(), userId);
        
        // Try to lock each seat
        for (String seatId : seatIds) {
            if (!tryLockSeat(eventId, seatId, userId)) {
                // Failed to lock this seat - rollback all previous locks
                log.warn("Failed to lock seat {}, rolling back", seatId);
                rollbackLocks(eventId, seatIds, userId);
                return false;
            }
        }
        
        log.info("Successfully locked all {} seats for user {}", seatIds.size(), userId);
        return true;
    }

    /**
     * Release a seat lock
     * Only the lock owner can release
     * 
     * @param eventId Event ID
     * @param seatId Seat identifier
     * @param userId User ID attempting to release
     * @return true if released, false if not owner or not locked
     */
    public boolean releaseSeat(Long eventId, String seatId, String userId) {
        String lockKey = RedisKeyGenerator.seatLockKey(eventId, seatId);
        String currentOwner = redisTemplate.opsForValue().get(lockKey);
        
        if (currentOwner == null) {
            log.warn("Seat not locked: event={}, seat={}", eventId, seatId);
            return false;
        }
        
        if (!userId.equals(currentOwner)) {
            log.warn("User {} cannot release seat {} owned by {}", 
                    userId, seatId, currentOwner);
            return false;
        }
        
        redisTemplate.delete(lockKey);
        log.info("Seat released: event={}, seat={}, user={}", eventId, seatId, userId);
        return true;
    }

    /**
     * Release multiple seats
     * 
     * @param eventId Event ID
     * @param seatIds List of seat identifiers
     * @param userId User ID
     */
    public void releaseSeats(Long eventId, List<String> seatIds, String userId) {
        log.info("Releasing {} seats for user {}", seatIds.size(), userId);
        
        for (String seatId : seatIds) {
            releaseSeat(eventId, seatId, userId);
        }
    }

    /**
     * Check if a seat is locked
     * 
     * @param eventId Event ID
     * @param seatId Seat identifier
     * @return true if locked, false otherwise
     */
    public boolean isSeatLocked(Long eventId, String seatId) {
        String lockKey = RedisKeyGenerator.seatLockKey(eventId, seatId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * Get the owner of a seat lock
     * 
     * @param eventId Event ID
     * @param seatId Seat identifier
     * @return User ID of lock owner, or null if not locked
     */
    public String getSeatLockOwner(Long eventId, String seatId) {
        String lockKey = RedisKeyGenerator.seatLockKey(eventId, seatId);
        return redisTemplate.opsForValue().get(lockKey);
    }

    /**
     * Get remaining TTL for a seat lock
     * 
     * @param eventId Event ID
     * @param seatId Seat identifier
     * @return Remaining seconds, or -1 if not locked
     */
    public long getSeatLockTTL(Long eventId, String seatId) {
        String lockKey = RedisKeyGenerator.seatLockKey(eventId, seatId);
        Long ttl = redisTemplate.getExpire(lockKey);
        return ttl != null ? ttl : -1;
    }

    /**
     * Get all locked seats for an event
     * 
     * @param eventId Event ID
     * @return Set of locked seat identifiers
     */
    public Set<String> getLockedSeats(Long eventId) {
        String pattern = RedisKeyGenerator.seatLockKey(eventId, "*");
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }
        
        // Extract seat IDs from keys
        return keys.stream()
                .map(key -> key.substring(key.lastIndexOf(":") + 1))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Rollback locks (used when multi-seat lock fails)
     * 
     * @param eventId Event ID
     * @param seatIds Seats to unlock
     * @param userId User ID
     */
    private void rollbackLocks(Long eventId, List<String> seatIds, String userId) {
        log.info("Rolling back locks for user {}", userId);
        
        for (String seatId : seatIds) {
            releaseSeat(eventId, seatId, userId);
        }
    }

    /**
     * Force release all locks for an event (admin operation)
     * 
     * @param eventId Event ID
     * @return Number of locks released
     */
    public long forceReleaseAllLocks(Long eventId) {
        String pattern = RedisKeyGenerator.seatLockKey(eventId, "*");
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        Long deleted = redisTemplate.delete(keys);
        log.warn("Force released {} locks for event {}", deleted, eventId);
        return deleted != null ? deleted : 0;
    }
}
