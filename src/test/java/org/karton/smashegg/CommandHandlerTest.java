package org.karton.smashegg.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.karton.smashegg.SmashEgg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandHandlerTest {
    private SmashEgg plugin;
    private CommandSender sender;
    private CommandHandler handler;

    @BeforeEach
    void setup() {
        plugin = mock(SmashEgg.class);
        sender = mock(CommandSender.class);
        when(sender.hasPermission("smashegg.use")).thenReturn(true);
        handler = new CommandHandler(plugin);
    }

    @Test
    void unauthorizedReloadDoesNotTouchConfiguration() {
        handler.onCommand(sender, null, "smashegg", new String[]{"reload"});
        verify(plugin).message(sender, "no-permission");
        verify(plugin, never()).reloadSettings();
        assertEquals(List.of(), handler.onTabComplete(sender, null, "smashegg", new String[]{""}));
    }

    @Test
    void missingUsePermissionIsDenied() {
        when(sender.hasPermission("smashegg.use")).thenReturn(false);
        when(sender.hasPermission("smashegg.reload")).thenReturn(true);
        handler.onCommand(sender, null, "smashegg", new String[]{"reload"});
        verify(plugin).message(sender, "no-permission");
        verify(plugin, never()).reloadSettings();
        assertTrue(handler.onTabComplete(sender, null, "smashegg", new String[]{""}).isEmpty());
    }

    @Test
    void reloadIsCaseInsensitiveAndReportsActualResult() {
        when(sender.hasPermission("smashegg.reload")).thenReturn(true);
        when(plugin.reloadSettings()).thenReturn(true, false);
        handler.onCommand(sender, null, "smashegg", new String[]{"RELOAD"});
        handler.onCommand(sender, null, "smashegg", new String[]{"reload"});
        verify(plugin).message(sender, "reload-success");
        verify(plugin).message(sender, "reload-failure");
        assertEquals(List.of("reload"), handler.onTabComplete(sender, null, "smashegg", new String[]{"RE"}));
    }

    @Test
    void rejectsMissingUnknownAndExtraArguments() {
        for (String[] args : List.of(new String[0], new String[]{"help"}, new String[]{"reload", "extra"})) {
            handler.onCommand(sender, null, "smashegg", args);
        }
        verify(plugin, times(3)).message(sender, "usage");
        verify(plugin, never()).reloadSettings();
        assertTrue(handler.onTabComplete(sender, null, "smashegg", new String[]{"reload", ""}).isEmpty());
    }
}
