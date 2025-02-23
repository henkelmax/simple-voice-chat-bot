package de.maxhenkel.voicechatbot;

import java.util.function.Consumer;

public class ExceptionHandler implements Consumer<Throwable> {
    @Override
    public void accept(Throwable e) {
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().contains("CompletableFuture")) {
                continue;
            }
            Main.LOGGER.error("{}:{}: {}", element.getClassName(), element.getLineNumber(), e.getMessage());
            break;
        }
    }

}
