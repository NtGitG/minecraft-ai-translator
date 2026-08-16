package fr.ntgitg.mineglot.ui.gui.screens.update;

import fr.ntgitg.mineglot.core.update.ReleaseInfo;
import fr.ntgitg.mineglot.core.update.UpdateCheckAccess;
import fr.ntgitg.mineglot.core.update.UpdateCheckResult;
import net.minecraft.client.gui.GuiScreen;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class UpdateNotificationControllerTest {

    @Test
    public void usesMostRecentlyOpenedMainMenuWhenCheckCompletes() {
        FakeUpdateAccess updateAccess = new FakeUpdateAccess();
        FakeClientBridge client = new FakeClientBridge();
        UpdateNotificationController controller = new UpdateNotificationController(
                updateAccess, Runnable::run, client);
        GuiScreen firstMenu = screen();
        GuiScreen latestMenu = screen();

        client.currentScreen = firstMenu;
        controller.onMainMenuOpened(firstMenu);
        client.currentScreen = latestMenu;
        controller.onMainMenuOpened(latestMenu);
        updateAccess.completeWithUpdate();

        assertEquals(1, client.promptCount);
        assertSame(latestMenu, client.promptParent);
    }

    @Test
    public void retriesCachedPromptWhenPlayerReturnsToMainMenu() {
        FakeUpdateAccess updateAccess = new FakeUpdateAccess();
        FakeClientBridge client = new FakeClientBridge();
        QueuedExecutor executor = new QueuedExecutor();
        UpdateNotificationController controller = new UpdateNotificationController(
                updateAccess, executor, client);
        GuiScreen oldMenu = screen();
        GuiScreen otherScreen = screen();

        client.currentScreen = oldMenu;
        controller.onMainMenuOpened(oldMenu);
        client.currentScreen = otherScreen;
        updateAccess.completeWithUpdate();
        executor.runAll();
        assertEquals(0, client.promptCount);

        GuiScreen newMenu = screen();
        client.currentScreen = newMenu;
        controller.onMainMenuOpened(newMenu);
        assertEquals(0, client.promptCount);
        executor.runAll();

        assertEquals(1, client.promptCount);
        assertSame(newMenu, client.promptParent);
    }

    @Test
    public void schedulesOnlyOnePromptPerSession() {
        FakeUpdateAccess updateAccess = new FakeUpdateAccess();
        FakeClientBridge client = new FakeClientBridge();
        UpdateNotificationController controller = new UpdateNotificationController(
                updateAccess, Runnable::run, client);
        GuiScreen menu = screen();

        client.currentScreen = menu;
        controller.onMainMenuOpened(menu);
        updateAccess.completeWithUpdate();
        controller.onMainMenuOpened(menu);
        controller.onMainMenuOpened(menu);

        assertEquals(1, client.promptCount);
    }

    @Test
    public void dismissedSessionNeverShowsPrompt() {
        FakeUpdateAccess updateAccess = new FakeUpdateAccess();
        updateAccess.dismissForSession();
        FakeClientBridge client = new FakeClientBridge();
        UpdateNotificationController controller = new UpdateNotificationController(
                updateAccess, Runnable::run, client);
        GuiScreen menu = screen();

        client.currentScreen = menu;
        controller.onMainMenuOpened(menu);
        updateAccess.completeWithUpdate();

        assertEquals(0, client.promptCount);
    }

    private static GuiScreen screen() {
        return new GuiScreen() {
        };
    }

    private static ReleaseInfo release() {
        return new ReleaseInfo("v1.0.3",
                "https://github.com/NtGitG/minecraft-ai-translator/releases/tag/v1.0.3");
    }

    private static final class FakeUpdateAccess implements UpdateCheckAccess {
        private final CompletableFuture<UpdateCheckResult> result = new CompletableFuture<>();
        private boolean dismissed;

        @Override
        public CompletableFuture<UpdateCheckResult> checkOnceAsync() {
            return result;
        }

        @Override
        public void dismissForSession() {
            dismissed = true;
        }

        @Override
        public boolean isDismissedForSession() {
            return dismissed;
        }

        private void completeWithUpdate() {
            result.complete(UpdateCheckResult.updateAvailable(release()));
        }
    }

    private static final class FakeClientBridge implements UpdateClientBridge {
        private GuiScreen currentScreen;
        private int promptCount;
        private GuiScreen promptParent;

        @Override
        public void runOnClientThread(Runnable task) {
            task.run();
        }

        @Override
        public GuiScreen getCurrentScreen() {
            return currentScreen;
        }

        @Override
        public void showPrompt(GuiScreen parentScreen, ReleaseInfo releaseInfo,
                               String currentVersion, Runnable dismissAction) {
            promptCount++;
            promptParent = parentScreen;
        }
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private void runAll() {
            Runnable task;
            while ((task = tasks.poll()) != null) {
                task.run();
            }
        }
    }
}
