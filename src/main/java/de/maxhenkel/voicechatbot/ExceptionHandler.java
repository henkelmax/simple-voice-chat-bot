package de.maxhenkel.voicechatbot;

import java.util.function.Function;

public class ExceptionHandler<T> implements Function<Throwable, T> {

    @Override
    public T apply(Throwable e) {
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().contains("CompletableFuture")) {
                continue;
            }
            Main.LOGGER.error("{}:{}: {}", element.getClassName(), element.getLineNumber(), e.getMessage());
            break;
        }

        return null;
    }

}
