package dev.rodrigosambade.chat;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NickRegistry<T> {

    private final ConcurrentMap<String, T> clients = new ConcurrentHashMap<>();

    public boolean register(String nickname, T value) {
        Objects.requireNonNull(value, "value");
        String key = normalize(nickname);
        return clients.putIfAbsent(key, value) == null;
    }

    public void remove(String nickname, T value) {
        clients.remove(normalize(nickname), value);
    }

    public Collection<T> values() {
        return List.copyOf(clients.values());
    }

    public List<String> nicks() {
        return clients.keySet()
                .stream()
                .sorted()
                .toList();
    }

    private static String normalize(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname must not be blank");
        }
        return nickname.trim().toLowerCase(Locale.ROOT);
    }
}
