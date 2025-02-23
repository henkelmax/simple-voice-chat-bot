package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ButtonRegistry {

    private static final Map<String, Button> buttons = new HashMap<>();

    public static void init() {
        Main.LOGGER.info("Initializing button registry");
        Main.API.addEventListener(new ListenerAdapter() {
            @Override
            public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
                Button button = buttons.get(event.getComponentId());
                if (button == null) {
                    Main.LOGGER.info("Could not find registered button with ID '{}'", event.getComponentId());
                    return;
                }
                button.listener.accept(event);
            }
        });
    }

    public static void registerButton(String id, Consumer<ButtonInteractionEvent> listener) {
        if (buttons.containsKey(id)) {
            throw new IllegalStateException("Button with ID '%s' already registered".formatted(id));
        }
        buttons.put(id, new Button(id, listener));
    }

    private record Button(String id, Consumer<ButtonInteractionEvent> listener) {
    }

}
