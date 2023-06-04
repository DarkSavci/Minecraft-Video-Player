package dev.bdinc.androidinmc;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoFrameProcessor {

    private final AndroidInMC plugin;
    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private final BlockingQueue<BufferedImage> frameQueue;

    public VideoFrameProcessor(AndroidInMC plugin, World world, int x, int y, int z) {
        this.plugin = plugin;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.frameQueue = new LinkedBlockingQueue<>();
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                BufferedImage frame = frameQueue.poll();
                if (frame != null) {
                    plugin.pasteImage(world, x, y, z, frame);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void addFrame(BufferedImage frame) {
        frameQueue.offer(frame);
    }

}
