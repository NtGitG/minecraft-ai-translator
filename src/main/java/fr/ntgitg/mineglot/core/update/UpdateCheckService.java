package fr.ntgitg.mineglot.core.update;

import fr.ntgitg.mineglot.MineGlot;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateCheckService implements UpdateCheckAccess {
    private final UpdateChecker checker;
    private final Executor executor;
    private final AtomicBoolean dismissedForSession = new AtomicBoolean(false);
    private final Object checkLock = new Object();

    private volatile CompletableFuture<UpdateCheckResult> checkFuture;

    private UpdateCheckService() {
        this(new UpdateChecker(new GitHubLatestReleaseClient(), MineGlot.VERSION),
                ThreadManager.getHttpExecutor());
    }

    UpdateCheckService(UpdateChecker checker, Executor executor) {
        if (checker == null) {
            throw new IllegalArgumentException("checker cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor cannot be null");
        }
        this.checker = checker;
        this.executor = executor;
    }

    public static UpdateCheckService getInstance() {
        return SingletonManager.getInstance(UpdateCheckService.class, UpdateCheckService::new);
    }

    @Override
    public CompletableFuture<UpdateCheckResult> checkOnceAsync() {
        CompletableFuture<UpdateCheckResult> current = checkFuture;
        if (current != null) {
            return current;
        }

        synchronized (checkLock) {
            if (checkFuture == null) {
                try {
                    checkFuture = CompletableFuture.supplyAsync(checker::check, executor)
                            .handle((result, error) -> {
                                UpdateCheckResult safeResult = result;
                                if (error != null || safeResult == null) {
                                    String reason = error == null
                                            ? "empty update-check result"
                                            : error.getClass().getSimpleName();
                                    safeResult = UpdateCheckResult.unavailable(reason);
                                }
                                logResult(safeResult);
                                return safeResult;
                            });
                } catch (RuntimeException e) {
                    ModLogger.debug("Verification des mises a jour non planifiee: {}",
                            e.getMessage());
                    checkFuture = CompletableFuture.completedFuture(
                            UpdateCheckResult.unavailable("update executor unavailable"));
                }
            }
            return checkFuture;
        }
    }

    @Override
    public void dismissForSession() {
        dismissedForSession.set(true);
    }

    @Override
    public boolean isDismissedForSession() {
        return dismissedForSession.get();
    }

    private void logResult(UpdateCheckResult result) {
        if (result.getStatus() == UpdateCheckResult.Status.UPDATE_AVAILABLE) {
            ModLogger.info("Mise a jour MineGlot disponible: {} (version actuelle: {})",
                    result.getReleaseInfo().getDisplayVersion(), MineGlot.VERSION);
        } else if (result.getStatus() == UpdateCheckResult.Status.UP_TO_DATE) {
            ModLogger.debug("MineGlot est a jour ({})", MineGlot.VERSION);
        } else {
            ModLogger.debug("Verification des mises a jour indisponible: {}",
                    result.getFailureReason());
        }
    }
}
