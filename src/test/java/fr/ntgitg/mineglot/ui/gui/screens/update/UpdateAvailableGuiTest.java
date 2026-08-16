package fr.ntgitg.mineglot.ui.gui.screens.update;

import fr.ntgitg.mineglot.core.update.ReleaseInfo;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class UpdateAvailableGuiTest {

    @Test
    public void downloadShowsMinecraftLinkConfirmationWithoutDismissing() throws Exception {
        PromptFixture fixture = new PromptFixture(uri -> true);

        fixture.gui.actionPerformed(button(UpdateAvailableGui.DOWNLOAD_BUTTON_ID));

        assertEquals(1, fixture.openedScreens.size());
        assertSame(fixture.confirmationScreen, fixture.openedScreens.get(0));
        assertEquals(fixture.releaseInfo.getReleasePageUrl(), fixture.confirmationUrl.get());
        assertEquals(UpdateAvailableGui.LINK_CONFIRMATION_ID,
                fixture.confirmationId.get());
        assertEquals(0, fixture.dismissCalls.get());
    }

    @Test
    public void laterDismissesAndReturnsToParent() throws Exception {
        PromptFixture fixture = new PromptFixture(uri -> true);

        fixture.gui.actionPerformed(button(UpdateAvailableGui.LATER_BUTTON_ID));

        assertEquals(1, fixture.dismissCalls.get());
        assertSame(fixture.parent, fixture.onlyOpenedScreen());
    }

    @Test
    public void escapePathBehavesLikeLater() {
        PromptFixture fixture = new PromptFixture(uri -> true);

        fixture.gui.handleBackButton();

        assertEquals(1, fixture.dismissCalls.get());
        assertSame(fixture.parent, fixture.onlyOpenedScreen());
    }

    @Test
    public void cancellingLinkConfirmationReturnsToPrompt() {
        PromptFixture fixture = new PromptFixture(uri -> true);

        fixture.gui.confirmClicked(false, UpdateAvailableGui.LINK_CONFIRMATION_ID);

        assertEquals(0, fixture.dismissCalls.get());
        assertSame(fixture.gui, fixture.onlyOpenedScreen());
    }

    @Test
    public void successfulBrowserOpenDismissesAndReturnsToParent() {
        AtomicReference<URI> openedUri = new AtomicReference<>();
        PromptFixture fixture = new PromptFixture(uri -> {
            openedUri.set(uri);
            return true;
        });

        fixture.gui.confirmClicked(true, UpdateAvailableGui.LINK_CONFIRMATION_ID);

        assertEquals(fixture.releaseInfo.getReleasePageUri(), openedUri.get());
        assertEquals(1, fixture.dismissCalls.get());
        assertSame(fixture.parent, fixture.onlyOpenedScreen());
    }

    @Test
    public void browserFailureKeepsPromptOpen() {
        PromptFixture fixture = new PromptFixture(uri -> false);

        fixture.gui.confirmClicked(true, UpdateAvailableGui.LINK_CONFIRMATION_ID);

        assertEquals(0, fixture.dismissCalls.get());
        assertSame(fixture.gui, fixture.onlyOpenedScreen());
    }

    private static GuiButton button(int id) {
        return new GuiButton(id, 0, 0, "test");
    }

    private static final class PromptFixture {
        private final GuiScreen parent = new GuiScreen() {
        };
        private final ReleaseInfo releaseInfo = new ReleaseInfo("v1.0.3",
                "https://github.com/NtGitG/minecraft-ai-translator/releases/tag/v1.0.3");
        private final AtomicInteger dismissCalls = new AtomicInteger();
        private final List<GuiScreen> openedScreens = new ArrayList<>();
        private final GuiScreen confirmationScreen = new GuiScreen() {
        };
        private final AtomicReference<String> confirmationUrl = new AtomicReference<>();
        private final AtomicInteger confirmationId = new AtomicInteger(-1);
        private final UpdateAvailableGui gui;

        private PromptFixture(ReleaseLinkOpener linkOpener) {
            gui = new UpdateAvailableGui(parent, releaseInfo, "1.0.2",
                    dismissCalls::incrementAndGet, linkOpener, openedScreens::add,
                    (callback, url, id) -> {
                        confirmationUrl.set(url);
                        confirmationId.set(id);
                        return confirmationScreen;
                    });
        }

        private GuiScreen onlyOpenedScreen() {
            assertEquals(1, openedScreens.size());
            return openedScreens.get(0);
        }
    }
}
