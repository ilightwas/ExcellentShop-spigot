package su.nightexpress.excellentshop.integration.packetevents;

import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import su.nightexpress.excellentshop.api.menu.SellingMenuProvider;
import su.nightexpress.excellentshop.api.packet.PacketLibrary;
import su.nightexpress.excellentshop.api.packet.display.DisplayAdapter;
import su.nightexpress.excellentshop.api.packet.display.DisplaySettings;
import su.nightexpress.excellentshop.integration.packetevents.adapter.PacketEventsDisplayAdapter;
import su.nightexpress.excellentshop.integration.packetevents.adapter.PacketEventsSellingMenuAdapter;

public class PacketEventsHook implements PacketLibrary {

    @Override
    @NonNull
    public String getName() {
        return "packetevents";
    }

    @Override
    @NonNull
    public PacketEventsSellingMenuAdapter createSellingMenuAdapter(@NonNull Plugin plugin,
                                                                   @NonNull SellingMenuProvider provider) {
        return new PacketEventsSellingMenuAdapter(provider);
    }

    @Override
    @NonNull
    public DisplayAdapter createDisplayAdapter(@NonNull DisplaySettings settings) {
        return new PacketEventsDisplayAdapter(settings);
    }
}
