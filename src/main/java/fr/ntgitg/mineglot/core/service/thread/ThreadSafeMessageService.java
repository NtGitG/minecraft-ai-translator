package fr.ntgitg.mineglot.core.service.thread;

import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;

public final class ThreadSafeMessageService {

    private ThreadSafeMessageService() {
    }

    public static boolean scheduleOnMainThread(Runnable task) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                ModLogger.debug("Thread principal Minecraft indisponible, tache ignoree");
                return false;
            }

            if (mc.isCallingFromMinecraftThread()) {
                task.run();
            } else {
                mc.addScheduledTask(task);
            }
            return true;
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la planification sur le thread principal", e);
            return false;
        }
    }

    public static void sendError(String errorKey, Object... args) {
        scheduleOnMainThread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null) {
                    MessageService.sendError(mc.thePlayer, errorKey, args);
                }
            } catch (Exception e) {
                ModLogger.error("Erreur lors de l'affichage du message d'erreur: {}", errorKey, e);
            }
        });
    }

    public static void sendSuccess(String successKey, Object... args) {
        scheduleOnMainThread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null) {
                    MessageService.sendSuccess(mc.thePlayer, successKey, args);
                }
            } catch (Exception e) {
                ModLogger.error("Erreur lors de l'affichage du message de succes: {}", successKey, e);
            }
        });
    }

    public static void sendInfo(String infoKey, Object... args) {
        scheduleOnMainThread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null) {
                    MessageService.sendInfo(mc.thePlayer, infoKey, args);
                }
            } catch (Exception e) {
                ModLogger.error("Erreur lors de l'affichage du message d'information: {}", infoKey, e);
            }
        });
    }
}
