package moe.kyuunex.moe_utils.modules.printer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;

public class PrinterUtils {
    public static Printer PRINTER;
    public static FakePlayerEntity fakePlayer;

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(PrinterUtils.class);
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        initFakePlayer();
    }


    public static void initFakePlayer() {
        if (mc.player != null) {
            if (fakePlayer == null || mc.player.clientWorld != fakePlayer.clientWorld) {
                fakePlayer = new FakePlayerEntity(mc.player, "~", 1000, false);
            }
        }
    }
}
