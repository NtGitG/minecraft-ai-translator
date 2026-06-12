package fr.ntgitg.mineglot.events.gui;

import fr.ntgitg.mineglot.ui.gui.core.ScreenManager;
import fr.ntgitg.mineglot.ui.gui.screens.main.MainGui;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonListener;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonType;
import fr.ntgitg.mineglot.ui.gui.utils.button.CustomButtonFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiEventHandler {
    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiIngameMenu) {
            int x;
            int y;
            if (!event.buttonList.isEmpty()) {
                GuiButton first = event.buttonList.get(0);
                x = first.xPosition; // aligné sur le premier bouton
                y = first.yPosition + 24; // sous le bouton déconnexion
            } else {
                x = event.gui.width / 2 - 100;
                y = event.gui.height / 4 + 48;
            }
            GuiButton gptButton = CustomButtonFactory.create(ButtonType.MINEGLOT_GPT, x, y, "",
                    new ButtonListener() {
                        @Override
                        public void onButtonClick(
                                fr.ntgitg.mineglot.ui.gui.utils.button.CustomButton button) {
                            Minecraft mc = Minecraft.getMinecraft();
                            if (!(mc.currentScreen instanceof MainGui)) {
                                ScreenManager.openMainMenu();
                            }
                        }
                    });
            event.buttonList.add(gptButton);
        }
    }

    @SubscribeEvent
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.gui instanceof GuiIngameMenu
                && event.button.id == ButtonType.MINEGLOT_GPT.getId()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof MainGui)) {
                ScreenManager.openMainMenu();
            }
        }
    }
}
