package sh.qnx.moe.utility.packets;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import sh.qnx.moe.MoeUtils;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PacketUtils {

    protected static boolean modifyCurrentTickRots = false;
    protected static long currentTick = 0;
    protected static Map<Long, List<BlockPlacement>> queuedBlockPlacement = new HashMap<>();
    protected static float modifiedYaw = 0f;
    protected static float modifiedPitch = 0f;


    public static void rotate(float pitch, float yaw, boolean update) {
        assert mc.player != null;

        if (update) {
            modifyCurrentTickRots = true;
            modifiedYaw = yaw;
            modifiedPitch = pitch;
        } else {
            mc.player.setXRot(pitch);
            mc.player.setYRot(yaw);
        }
    }

    public static void rotate(float pitch, float yaw) {
        rotate(pitch, yaw, false);
    }


    public static void queuePlacementForNextTick(BlockPlacement placement) {
        if (!queuedBlockPlacement.containsKey(currentTick + 1)) {
            queuedBlockPlacement.put(currentTick + 1, new ArrayList<>());
        }

        queuedBlockPlacement.get(currentTick + 1).add(placement);
    }

    public static void send(Packet<?> packet) {
        if (mc.getConnection() == null) {
            return;
        }

        send(mc.getConnection().getConnection(), packet);
    }

    public static void send(Connection connection, Packet<?> packet) {
        if (connection == null) {
            MoeUtils.LOGGER.error("Connection is null");
            return;
        }

        connection.send(packet, null, true);
    }

}
