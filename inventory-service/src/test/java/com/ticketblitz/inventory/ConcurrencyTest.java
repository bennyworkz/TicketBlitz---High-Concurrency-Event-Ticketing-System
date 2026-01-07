package com.ticketblitz.inventory;

import com.ticketblitz.inventory.service.SeatLockService;
import com.ticketblitz.inventory.service.TatkalInventoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Tests for Inventory Service
 * 
 * These tests simulate high-concurrency scenarios to verify:
 * 1. No race conditions in seat locking
 * 2. Only ONE user can lock a seat
 * 3. Atomic operations work correctly
 * 4. System handles 10,000+ concurrent requests
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConcurrencyTest {

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private TatkalInventoryService tatkalInventoryService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        // Clean Redis before each test
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // Create thread pool for concurrent operations
        executorService = Executors.newFixedThreadPool(100);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    // ==================== SEAT LOCKING CONCURRENCY TESTS ====================

    @Test
    @Order(1)
    @DisplayName("100 users try to lock same seat - only 1 should succeed")
    void testConcurrentSeatLocking_OnlyOneWins() throws InterruptedException {
        // ARRANGE
        Long eventId = 1L;
        String seatId = "A1";
        int numberOfUsers = 100;
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        
        // ACT: 100 users try to lock the same seat simultaneously
        for (int i = 0; i < numberOfUsers; i++) {
            final String userId = "user-" + i;
            
            executorService.submit(() -> {
                try {
                    boolean locked = seatLockService.tryLockSeat(eventId, seatId, userId);
                    if (locked) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(10, TimeUnit.SECONDS);
        
        // ASSERT: Only ONE user should have successfully locked the seat
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(99);
        
        // Verify in Redis
        String owner = redisTemplate.opsForValue().get("lock:event:1:seat:A1");
        assertThat(owner).isNotNull();
        assertThat(owner).startsWith("user-");
        
        System.out.println("✅ Concurrency Test 1: SUCCESS");
        System.out.println("   - 100 users tried to lock seat A1");
        System.out.println("   - Only 1 succeeded: " + owner);
        System.out.println("   - 99 failed (as expected)");
    }

    @Test
    @Order(2)
    @DisplayName("1000 users try to lock 10 different seats - each seat locked by 1 user")
    void testConcurrentMultipleSeatLocking() throws InterruptedException {
        // ARRANGE
        Long eventId = 1L;
        int numberOfSeats = 10;
        int usersPerSeat = 100;
        int totalUsers = numberOfSeats * usersPerSeat;
        
        Map<String, AtomicInteger> successPerSeat = new ConcurrentHashMap<>();
        for (int i = 1; i <= numberOfSeats; i++) {
            successPerSeat.put("A" + i, new AtomicInteger(0));
        }
        
        CountDownLatch latch = new CountDownLatch(totalUsers);
        
        // ACT: 100 users try to lock each of 10 seats
        for (int seatNum = 1; seatNum <= numberOfSeats; seatNum++) {
            final String seatId = "A" + seatNum;
            
            for (int userNum = 0; userNum < usersPerSeat; userNum++) {
                final String userId = "user-" + seatNum + "-" + userNum;
                
                executorService.submit(() -> {
                    try {
                        boolean locked = seatLockService.tryLockSeat(eventId, seatId, userId);
                        if (locked) {
                            successPerSeat.get(seatId).incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        // Wait for all threads to complete
        latch.await(15, TimeUnit.SECONDS);
        
        // ASSERT: Each seat should be locked by exactly ONE user
        for (int i = 1; i <= numberOfSeats; i++) {
            String seatId = "A" + i;
            int successCount = successPerSeat.get(seatId).get();
            assertThat(successCount).isEqualTo(1);
            
            // Verify in Redis
            String owner = redisTemplate.opsForValue().get("lock:event:1:seat:" + seatId);
            assertThat(owner).isNotNull();
        }
        
        System.out.println("✅ Concurrency Test 2: SUCCESS");
        System.out.println("   - 1000 users tried to lock 10 seats");
        System.out.println("   - Each seat locked by exactly 1 user");
        System.out.println("   - No race conditions detected");
    }

    @Test
    @Order(3)
    @DisplayName("10,000 users try to lock same seat - stress test")
    void testHighConcurrencySeatLocking_10000Users() throws InterruptedException {
        // ARRANGE
        Long eventId = 1L;
        String seatId = "VIP1";
        int numberOfUsers = 10_000;
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        
        long startTime = System.currentTimeMillis();
        
        // ACT: 10,000 users try to lock the same seat
        for (int i = 0; i < numberOfUsers; i++) {
            final String userId = "user-" + i;
            
            executorService.submit(() -> {
                try {
                    boolean locked = seatLockService.tryLockSeat(eventId, seatId, userId);
                    if (locked) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        // ASSERT
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9_999);
        
        // Verify in Redis
        String owner = redisTemplate.opsForValue().get("lock:event:1:seat:VIP1");
        assertThat(owner).isNotNull();
        
        System.out.println("✅ Concurrency Test 3: SUCCESS");
        System.out.println("   - 10,000 users tried to lock seat VIP1");
        System.out.println("   - Only 1 succeeded: " + owner);
        System.out.println("   - 9,999 failed (as expected)");
        System.out.println("   - Duration: " + duration + "ms");
        System.out.println("   - Throughput: " + (numberOfUsers * 1000 / duration) + " requests/sec");
    }

    // ==================== TATKAL INVENTORY CONCURRENCY TESTS ====================

    @Test
    @Order(4)
    @DisplayName("1000 users try to book 100 Tatkal seats - exactly 100 should succeed")
    void testConcurrentTatkalBooking() throws InterruptedException {
        // ARRANGE
        Long eventId = 100L;
        int totalSeats = 100;
        int numberOfUsers = 1000;
        
        tatkalInventoryService.initializeInventory(eventId, totalSeats);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        
        // ACT: 1000 users try to book 100 seats
        for (int i = 0; i < numberOfUsers; i++) {
            executorService.submit(() -> {
                try {
                    boolean reserved = tatkalInventoryService.tryReserveSeat(eventId);
                    if (reserved) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(15, TimeUnit.SECONDS);
        
        // ASSERT: Exactly 100 should succeed, 900 should fail
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failureCount.get()).isEqualTo(900);
        
        // Verify inventory is 0
        int remaining = tatkalInventoryService.getRemainingInventory(eventId);
        assertThat(remaining).isEqualTo(0);
        
        // Verify sold out
        boolean soldOut = tatkalInventoryService.isSoldOut(eventId);
        assertThat(soldOut).isTrue();
        
        System.out.println("✅ Concurrency Test 4: SUCCESS");
        System.out.println("   - 1000 users tried to book 100 Tatkal seats");
        System.out.println("   - Exactly 100 succeeded");
        System.out.println("   - 900 failed (sold out)");
        System.out.println("   - No overbooking detected");
    }

    @Test
    @Order(5)
    @DisplayName("10,000 users try to book 500 Tatkal seats - stress test")
    void testHighConcurrencyTatkalBooking_10000Users() throws InterruptedException {
        // ARRANGE
        Long eventId = 200L;
        int totalSeats = 500;
        int numberOfUsers = 10_000;
        
        tatkalInventoryService.initializeInventory(eventId, totalSeats);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        
        long startTime = System.currentTimeMillis();
        
        // ACT: 10,000 users try to book 500 seats
        for (int i = 0; i < numberOfUsers; i++) {
            executorService.submit(() -> {
                try {
                    boolean reserved = tatkalInventoryService.tryReserveSeat(eventId);
                    if (reserved) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        // ASSERT
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(500);
        assertThat(failureCount.get()).isEqualTo(9_500);
        
        // Verify inventory is 0
        int remaining = tatkalInventoryService.getRemainingInventory(eventId);
        assertThat(remaining).isEqualTo(0);
        
        System.out.println("✅ Concurrency Test 5: SUCCESS");
        System.out.println("   - 10,000 users tried to book 500 Tatkal seats");
        System.out.println("   - Exactly 500 succeeded");
        System.out.println("   - 9,500 failed (sold out)");
        System.out.println("   - Duration: " + duration + "ms");
        System.out.println("   - Throughput: " + (numberOfUsers * 1000 / duration) + " requests/sec");
    }

    // ==================== MULTI-SEAT LOCKING CONCURRENCY TESTS ====================

    @Test
    @Order(6)
    @DisplayName("100 users try to lock 3 seats each - test atomic multi-seat locking")
    void testConcurrentMultiSeatLocking() throws InterruptedException {
        // ARRANGE
        Long eventId = 1L;
        int numberOfUsers = 100;
        List<String> seats = Arrays.asList("B1", "B2", "B3");
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        
        // ACT: 100 users try to lock the same 3 seats
        for (int i = 0; i < numberOfUsers; i++) {
            final String userId = "user-" + i;
            
            executorService.submit(() -> {
                try {
                    boolean locked = seatLockService.tryLockSeats(eventId, seats, userId);
                    if (locked) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(15, TimeUnit.SECONDS);
        
        // ASSERT: Only ONE user should have locked all 3 seats
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(99);
        
        // Verify all 3 seats are locked by same user
        String owner1 = redisTemplate.opsForValue().get("lock:event:1:seat:B1");
        String owner2 = redisTemplate.opsForValue().get("lock:event:1:seat:B2");
        String owner3 = redisTemplate.opsForValue().get("lock:event:1:seat:B3");
        
        assertThat(owner1).isNotNull();
        assertThat(owner1).isEqualTo(owner2);
        assertThat(owner2).isEqualTo(owner3);
        
        System.out.println("✅ Concurrency Test 6: SUCCESS");
        System.out.println("   - 100 users tried to lock 3 seats atomically");
        System.out.println("   - Only 1 succeeded: " + owner1);
        System.out.println("   - All 3 seats locked by same user");
        System.out.println("   - Atomic operation verified");
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @Order(7)
    @DisplayName("Measure lock acquisition latency under load")
    void testLockAcquisitionLatency() throws InterruptedException {
        // ARRANGE
        Long eventId = 1L;
        int numberOfOperations = 1000;
        
        List<Long> latencies = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(numberOfOperations);
        
        // ACT: Measure latency for 1000 lock operations
        for (int i = 0; i < numberOfOperations; i++) {
            final String seatId = "C" + i;
            final String userId = "user-" + i;
            
            executorService.submit(() -> {
                try {
                    long start = System.nanoTime();
                    seatLockService.tryLockSeat(eventId, seatId, userId);
                    long end = System.nanoTime();
                    
                    latencies.add((end - start) / 1_000_000); // Convert to milliseconds
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all operations to complete
        latch.await(15, TimeUnit.SECONDS);
        
        // ASSERT: Calculate statistics
        Collections.sort(latencies);
        long min = latencies.get(0);
        long max = latencies.get(latencies.size() - 1);
        long avg = latencies.stream().mapToLong(Long::longValue).sum() / latencies.size();
        long p50 = latencies.get(latencies.size() / 2);
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));
        
        // All latencies should be reasonable (< 100ms)
        assertThat(p99).isLessThan(100);
        
        System.out.println("✅ Concurrency Test 7: PERFORMANCE METRICS");
        System.out.println("   - Operations: " + numberOfOperations);
        System.out.println("   - Min latency: " + min + "ms");
        System.out.println("   - Avg latency: " + avg + "ms");
        System.out.println("   - P50 latency: " + p50 + "ms");
        System.out.println("   - P95 latency: " + p95 + "ms");
        System.out.println("   - P99 latency: " + p99 + "ms");
        System.out.println("   - Max latency: " + max + "ms");
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @Order(8)
    @DisplayName("Same user tries to lock same seat multiple times - should succeed")
    void testSameUserMultipleLockAttempts() throws InterruptedException {
        // ARRANGE
        Long eventId = 1L;
        String seatId = "D1";
        String userId = "user-123";
        int numberOfAttempts = 100;
        
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfAttempts);
        
        // ACT: Same user tries to lock same seat 100 times
        for (int i = 0; i < numberOfAttempts; i++) {
            executorService.submit(() -> {
                try {
                    boolean locked = seatLockService.tryLockSeat(eventId, seatId, userId);
                    if (locked) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(10, TimeUnit.SECONDS);
        
        // ASSERT: All attempts should succeed (idempotent)
        assertThat(successCount.get()).isEqualTo(100);
        
        System.out.println("✅ Concurrency Test 8: SUCCESS");
        System.out.println("   - Same user tried to lock same seat 100 times");
        System.out.println("   - All 100 attempts succeeded (idempotent)");
    }
}
