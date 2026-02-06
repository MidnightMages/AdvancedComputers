package dev.asdf00.mc.advcomp;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class NetCodeUtils {
    private static final AtomicInteger MSG_ID_PROVIDER = new AtomicInteger(0);
    private static final String PROTOCOL_VERSION = "0.1.a";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AdvancedComputers.MODID, "simple_networking_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static <MSG extends NetworkMessage> void registerMessage(Class<MSG> mscClazz, Function<FriendlyByteBuf, MSG> decoder) {
        INSTANCE.registerMessage(MSG_ID_PROVIDER.getAndAdd(1), mscClazz, (msg, buf) -> msg.encode(buf), decoder, ((msg, ctx) -> msg.handle(ctx.get())));
    }

    public static <MSG> void sendToServer(MSG message) {
        //System.out.println("Sending packet to server: %s".formatted(message));
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToClient(PacketDistributor.PacketTarget target, MSG message) {
        //System.out.println("Sending packet to client: %s".formatted(message));
        INSTANCE.send(target, message);
    }

    public static void writeStringToBuf(FriendlyByteBuf buf, String data) {
        var bytes = data.getBytes(StandardCharsets.UTF_8);
        buf.writeByteArray(bytes);
    }

    public static String readStringFromBuf(FriendlyByteBuf buf) {
        try {
            var bytes = buf.readByteArray();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            AdvancedComputers.LOGGER.warn("Got invalid message from other party");
            return null;
        }
    }

    public interface NetworkMessage {
        void encode(FriendlyByteBuf buffer);

        void handle(NetworkEvent.Context ctx);
    }
}
