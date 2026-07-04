package su.nightexpress.excellentshop.api.packet;

import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import su.nightexpress.excellentshop.api.menu.SellingMenuAdapter;
import su.nightexpress.excellentshop.api.menu.SellingMenuProvider;
import su.nightexpress.excellentshop.api.packet.display.DisplayAdapter;
import su.nightexpress.excellentshop.api.packet.display.DisplaySettings;

public interface PacketLibrary {

    @NonNull
    String getName();

    @NonNull
    SellingMenuAdapter createSellingMenuAdapter(@NonNull Plugin plugin, @NonNull SellingMenuProvider provider);

    @NonNull
    DisplayAdapter createDisplayAdapter(@NonNull DisplaySettings settings);
}
