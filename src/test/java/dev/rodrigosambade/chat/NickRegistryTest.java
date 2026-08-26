package dev.rodrigosambade.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NickRegistryTest {

    @Test
    void nicknameIsUniqueIgnoringCase() {
        NickRegistry<Object> registry = new NickRegistry<>();
        Object firstClient = new Object();
        Object secondClient = new Object();

        assertTrue(registry.register("Rodrigo", firstClient));
        assertFalse(registry.register("rodrigo", secondClient));
        assertEquals(1, registry.nicks().size());
    }
}
