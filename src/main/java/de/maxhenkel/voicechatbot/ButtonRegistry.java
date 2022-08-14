package de.maxhenkel.voicechatbot;

import org.javacord.api.listener.interaction.ButtonClickListener;

import java.util.HashMap;
import java.util.Map;

public class ButtonRegistry {

    private static final Map<String, Button> buttons = new HashMap<>();

    public static void init() {
        Main.LOGGER.info("Initializing button registry");
        Main.API.addButtonClickListener(buttonClickEvent -> {
            String customId = buttonClickEvent.getButtonInteraction().getCustomId();
            Button button = buttons.get(customId);
            if (button == null) {
                Main.LOGGER.info("Could not find registered button with ID '{}'", customId);
                return;
            }
            button.listener.onButtonClick(buttonClickEvent);
        });
    }

    public static void registerButton(String id, ButtonClickListener listener) {
        if (buttons.containsKey(id)) {
            throw new IllegalStateException("Button with ID '%s' already registered".formatted(id));
        }
        buttons.put(id, new Button(id, listener));
    }

    private record Button(String id, ButtonClickListener listener) {
    }

}
