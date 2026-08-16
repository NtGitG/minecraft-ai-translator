package fr.ntgitg.mineglot.core.update;

import java.util.concurrent.CompletableFuture;

public interface UpdateCheckAccess {
    CompletableFuture<UpdateCheckResult> checkOnceAsync();

    void dismissForSession();

    boolean isDismissedForSession();
}
