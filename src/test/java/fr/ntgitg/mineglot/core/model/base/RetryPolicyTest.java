package fr.ntgitg.mineglot.core.model.base;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RetryPolicyTest {

    @After
    public void tearDownThreadManager() {
        ThreadManager.shutdown();
        SingletonManager.removeInstance(ThreadManager.class);
    }

    @Test
    public void doesNotRetryClientHttpErrors() throws Exception {
        AtomicInteger attempts = new AtomicInteger();

        CompletableFuture<String> result = RetryPolicy.withRetryAsync(() -> {
            attempts.incrementAndGet();
            throw new BaseHttpClient.ApiHttpException(401, "bad key");
        });

        try {
            result.get(3, TimeUnit.SECONDS);
            fail("Expected the operation to fail");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertTrue(e.getCause().getCause() instanceof BaseHttpClient.ApiHttpException);
        }

        assertEquals(1, attempts.get());
    }

    @Test
    public void retriesServerHttpErrorsAndEventuallySucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger();

        CompletableFuture<String> result = RetryPolicy.withRetryAsync(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                throw new BaseHttpClient.ApiHttpException(503, "temporary");
            }
            return "ok";
        });

        assertEquals("ok", result.get(6, TimeUnit.SECONDS));
        assertEquals(2, attempts.get());
    }

    @Test
    public void retryBackoffDoesNotOccupyHttpWorkers() throws Exception {
        CountDownLatch firstAttempts = new CountDownLatch(4);
        List<CompletableFuture<String>> retryingOperations = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            AtomicInteger attempts = new AtomicInteger();
            retryingOperations.add(RetryPolicy.withRetryAsync(() -> {
                int attempt = attempts.incrementAndGet();
                if (attempt == 1) {
                    firstAttempts.countDown();
                    throw new BaseHttpClient.ApiHttpException(503, "temporary");
                }
                return "ok";
            }));
        }

        assertTrue(firstAttempts.await(2, TimeUnit.SECONDS));

        List<Future<?>> probes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            probes.add(ThreadManager.getHttpExecutor().submit(() -> {
            }));
        }

        for (Future<?> probe : probes) {
            probe.get(500, TimeUnit.MILLISECONDS);
        }

        for (CompletableFuture<String> operation : retryingOperations) {
            assertEquals("ok", operation.get(6, TimeUnit.SECONDS));
        }
    }

    @Test
    public void timeoutCompletesFutureExceptionallyAndCancelsRunningAttempt() throws Exception {
        AtomicInteger attempts = new AtomicInteger();

        CompletableFuture<String> result = RetryPolicy.withRetryAsync(() -> {
            attempts.incrementAndGet();
            try {
                Thread.sleep(5_000L);
                return "late";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted", e);
            }
        }, 100, TimeUnit.MILLISECONDS);

        try {
            result.get(2, TimeUnit.SECONDS);
            fail("Expected timeout");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
        }

        assertEquals(1, attempts.get());
    }
}
