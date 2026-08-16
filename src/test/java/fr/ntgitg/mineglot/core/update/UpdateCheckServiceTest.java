package fr.ntgitg.mineglot.core.update;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UpdateCheckServiceTest {

    @Test
    public void checksOnlyOncePerSession() {
        AtomicInteger calls = new AtomicInteger();
        UpdateChecker checker = new UpdateChecker(() -> {
            calls.incrementAndGet();
            return null;
        }, "1.0.2");
        UpdateCheckService service = new UpdateCheckService(checker, Runnable::run);

        CompletableFuture<UpdateCheckResult> first = service.checkOnceAsync();
        CompletableFuture<UpdateCheckResult> second = service.checkOnceAsync();

        assertSame(first, second);
        assertTrue(first.isDone());
        assertTrue(calls.get() == 1);
    }

    @Test
    public void remembersLaterChoiceForCurrentSession() {
        UpdateCheckService service = new UpdateCheckService(
                new UpdateChecker(() -> null, "1.0.2"), Runnable::run);

        assertFalse(service.isDismissedForSession());
        service.dismissForSession();
        assertTrue(service.isDismissedForSession());
    }
}
