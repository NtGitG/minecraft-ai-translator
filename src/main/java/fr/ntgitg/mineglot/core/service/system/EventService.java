package fr.ntgitg.mineglot.core.service.system;

import fr.ntgitg.mineglot.core.service.AbstractService;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.events.chat.ChatMessageInterceptor;
import fr.ntgitg.mineglot.events.chat.ChatSelectionEventHandler;
import fr.ntgitg.mineglot.events.config.ConfigEventListener;
import fr.ntgitg.mineglot.events.gui.GuiEventHandler;
import fr.ntgitg.mineglot.events.hud.HUDRenderEventListener;
import fr.ntgitg.mineglot.events.server.ServerChangeEventListener;
import fr.ntgitg.mineglot.events.signs.SignInteractionEventListener;
import fr.ntgitg.mineglot.events.ui.UIManagerEventHandler;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public class EventService extends AbstractService {
    private final List<Object> registeredListeners = new ArrayList<>();

    private final ChatMessageInterceptor chatMessageInterceptor;
    private final ConfigEventListener configEventListener;
    private final ChatSelectionEventHandler chatSelectionEventHandler;
    private final ServerChangeEventListener serverChangeEventListener;
    private final SignInteractionEventListener signInteractionEventListener;
    private final GuiEventHandler guiEventHandler;
    private final HUDRenderEventListener hudRenderEventListener;
    private final UIManagerEventHandler uiManagerEventHandler;

    EventService() {
        super("Events");

        this.chatMessageInterceptor = new ChatMessageInterceptor();
        this.configEventListener = new ConfigEventListener();
        this.chatSelectionEventHandler = new ChatSelectionEventHandler();
        this.serverChangeEventListener = new ServerChangeEventListener();
        this.signInteractionEventListener = new SignInteractionEventListener();
        this.guiEventHandler = new GuiEventHandler();
        this.hudRenderEventListener = new HUDRenderEventListener();
        this.uiManagerEventHandler = new UIManagerEventHandler();
    }

    public static EventService getInstance() {
        return SingletonManager.getInstance(EventService.class, EventService::new);
    }

    @Override
    protected void doStart() throws Exception {
        try {
            registerListener(guiEventHandler, "Gui Event Handler");
            registerListener(hudRenderEventListener, "HUD Render Event Listener");
            registerListener(uiManagerEventHandler, "UI Manager Event Handler");

            registerListener(chatMessageInterceptor, "Chat Message Interceptor");
            registerListener(chatSelectionEventHandler, "Chat Selection Handler");
            registerListener(configEventListener, "Config Event Listener");
            registerListener(serverChangeEventListener, "Server Change Listener");
            registerListener(signInteractionEventListener, "Sign Interaction Listener");
        } catch (Exception e) {
            ModLogger.error("Erreur critique lors de l'enregistrement des evenements", e);
            doStop();
            throw new RuntimeException("Impossible d'enregistrer les evenements", e);
        }
    }

    @Override
    protected void doStop() throws Exception {
        for (Object listener : registeredListeners) {
            try {
                MinecraftForge.EVENT_BUS.unregister(listener);
            } catch (Exception e) {
                ModLogger.warn("Erreur lors du desenregistrement de : {}",
                        listener.getClass().getSimpleName(), e);
            }
        }

        registeredListeners.clear();
    }

    private void registerListener(Object listener, String description) {
        try {
            MinecraftForge.EVENT_BUS.register(listener);
            registeredListeners.add(listener);
        } catch (Exception e) {
            ModLogger.error(
                    "Echec enregistrement evenement '{}' - Listeners existants: {}, Type: {}, Erreur: {}",
                    description, registeredListeners.size(), e.getClass().getSimpleName(),
                    e.getMessage(), e);
            throw new RuntimeException("Impossible d'enregistrer l'evenement " + description, e);
        }
    }

}
