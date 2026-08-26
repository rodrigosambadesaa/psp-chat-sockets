package dev.rodrigosambade.chat;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class NickRegistryTest{@Test void nickIsUniqueIgnoringCase(){var r=new NickRegistry<Object>();Object a=new Object(),b=new Object();assertTrue(r.register("Rodrigo",a));assertFalse(r.register("rodrigo",b));assertEquals(1,r.nicks().size());}}
