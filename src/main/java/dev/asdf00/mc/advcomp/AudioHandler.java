package dev.asdf00.mc.advcomp;

import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioHandler {
    private static final ConcurrentLinkedQueue<QueuedSoundWithPosClient> queuedSoundsToPlay = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<QueuedSoundWithPos> queuedSoundsToPlayServer = new ConcurrentLinkedQueue<>();
    private static final ArrayList<PlayingAudioSource> playingAudioSources = new ArrayList<>();
    private static final int SAMPLE_RATE = 44100;

    public static void onClientTick() {
        // clean up sounds that are done playing
        for (int i = playingAudioSources.size() - 1; i >= 0; i--) {
            var playing = playingAudioSources.get(i);
            if (AL10.alGetSourcei(playing.sourceId, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alDeleteSources(playing.sourceId);
                AL10.alDeleteBuffers(playing.bufferId);

                playingAudioSources.remove(i);
            }
        }

        // spawn new sounds
        QueuedSoundWithPosClient sound;
        while ((sound = queuedSoundsToPlay.poll()) != null) {
            final float maxAudioSpawnDistance = 2 * Config.audioMaxDistance;
            final float spawnDistanceCutoffSq = maxAudioSpawnDistance * maxAudioSpawnDistance;
            var ply = Minecraft.getInstance().player;
            if (ply != null && ply.position().distanceToSqr(sound.pos.getCenter()) < spawnDistanceCutoffSq)
                startPlayingAudio(sound);
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        while (true) {
            QueuedSoundWithPos sound = queuedSoundsToPlayServer.poll();
            if (sound == null) break;

            NetCodeUtils.sendToClient(PacketDistributor.TRACKING_CHUNK.with(() -> sound.level.getChunkAt(sound.pos)),
                    new PlaySoundToClientEvent(sound));
        }
    }

    /**
     * Called on serverside from any thread, emits a packet andtells clients to synthesize and play a sound with the given settings
     */
    public static void queueSoundOnClientsAt(Level level, BlockPos pos, QueuedSound sound) {
        assert level != null;
        queuedSoundsToPlayServer.add(new QueuedSoundWithPos(level, pos, sound));
    }


    private static void checkThrowAlError() {
        var alCode = AL10.alGetError();
        RuntimeAssert.RuntimeAssert(alCode == 0, "OpenAl errored :C %s".formatted(alCode));
    }

    private static void startPlayingAudio(QueuedSoundWithPosClient sound) {
        // some bits from https://github.com/MightyPirates/OpenComputers/blob/master-MC1.7.10/src/main/scala/li/cil/oc/util/Audio.scala#L127 -- thank you <3

        final int maxDistance = Config.audioMaxDistance;
        AL10.alGetError(); // clear any old errors
        var bufferId = AL10.alGenBuffers();
        checkThrowAlError();

        boolean failedAtSomePoint = true; // flag will be cleared on success
        int numAlRepeats = Math.max(1, (int) (sound.queuedSound.duration / 0.25)); // cut it up in smaller bits so we save some OpenAL memory :D
        try {
            int numSamples = (int) (SAMPLE_RATE * sound.queuedSound.duration / numAlRepeats);
            byte[] buffer = new byte[numSamples];

            for (int i = 0; i < numSamples; i++) {
                double t = i / (float) SAMPLE_RATE;

                // make a square sound
                byte value = (byte) (Byte.MAX_VALUE * (Math.sin(2 * Math.PI * sound.queuedSound.frequency * t) > 0 ? 1 : 0));
                if (i >= 1) // smooth over the last few samples to make the lower frequences better to listen to
                    value = (byte) ((value + buffer[i - 1]) / 2f);

                buffer[i] = value;
            }

            // somehow we need to do this instead of ByteBuffer.wrap() otherwise we an immediate jvm crash on AL10.alBufferData
            var buffer2 = BufferUtils.createByteBuffer(numSamples);
            buffer2.put(buffer);
            buffer2.flip();
            AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO8, buffer2, SAMPLE_RATE);
            checkThrowAlError();
            var sourceId = AL10.alGenSources();
            checkThrowAlError();
            try {
                AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
                for (int i = 0; i < numAlRepeats; i++) {
                    AL10.alSourceQueueBuffers(sourceId, bufferId);
                }
                checkThrowAlError();

                // shift it to the center of the block
                AL10.alSource3f(sourceId, AL10.AL_POSITION, sound.pos.getX() + 0.5f, sound.pos.getY() + 0.5f, sound.pos.getZ() + 0.5f);
                AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, Math.min(16, maxDistance));
                AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
                // since minecraft is using AL_INVERSE_DISTANCE_CLAMPED this isnt a cutoff, so use a larger value here and block the sound from playing
                // initially if way too far away
                AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, maxDistance * 10);
                AL10.alSourcef(sourceId, AL10.AL_GAIN, Config.audioVolume);
                AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, 5.0f);
                AL10.alSourcef(sourceId, AL10.AL_MIN_GAIN, 0);

                checkThrowAlError();

                AL10.alSourcePlay(sourceId);
                checkThrowAlError();

                playingAudioSources.add(new PlayingAudioSource(sourceId, bufferId));
                failedAtSomePoint = false;
            } finally {
                if (failedAtSomePoint) // only do cleanup if it broke; otherwise we do it after the sound finished playing
                    AL10.alDeleteSources(sourceId);
            }
        } finally {
            if (failedAtSomePoint) {
                AL10.alDeleteBuffers(bufferId);
                AdvancedComputers.LOGGER.warn("Oh no, looks like audio synthesizing is breaking :("); // probably will trigger a crash, but if not, drop a hint
            }
        }
    }

    public static class PlaySoundToClientEvent implements NetCodeUtils.NetworkMessage {
        private final BlockPos pos;
        private final double freq;
        private final double duration;

        private PlaySoundToClientEvent(QueuedSoundWithPos sound) {
            this(sound.pos, sound.sound.frequency, sound.sound.duration);
        }

        private PlaySoundToClientEvent(BlockPos pos, double freq, double duration) {
            this.pos = pos;
            this.freq = freq;
            this.duration = duration;
        }

        public static PlaySoundToClientEvent decode(FriendlyByteBuf buffer) {
            return new PlaySoundToClientEvent(buffer.readBlockPos(), buffer.readDouble(), buffer.readDouble());
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeDouble(freq);
            buffer.writeDouble(duration);
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            queuedSoundsToPlay.add(new QueuedSoundWithPosClient(pos, new QueuedSound(freq, duration)));
            ctx.setPacketHandled(true);
        }
    }


    public record QueuedSound(double frequency, double duration) {
    }

    private record QueuedSoundWithPos(Level level, BlockPos pos, QueuedSound sound) {
    }

    public record QueuedSoundWithPosClient(BlockPos pos, QueuedSound queuedSound) {
    }

    public record PlayingAudioSource(int sourceId, int bufferId) {
    }
}
