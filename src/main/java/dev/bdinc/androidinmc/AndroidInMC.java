package dev.bdinc.androidinmc;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public final class AndroidInMC extends JavaPlugin {

    private static AndroidInMC instance;

    private static final int MAX_WIDTH = 100;
    private static final int MAX_HEIGHT = 100;

    public static ArrayList<Location> latestLocation = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        ColorManager.setupColorMap();
        Objects.requireNonNull(getCommand("processimage")).setExecutor(new ProcessImageCommand());
        Objects.requireNonNull(getCommand("processvideo")).setExecutor(new ProcessVideoCommand());
        Objects.requireNonNull(getCommand("undoimage")).setExecutor(new UndoCommand());
    }

    public static AndroidInMC getInstance() {
        return instance;
    }

    public void processImage(BufferedImage image, Location location) {
        World world = location.getWorld();
        int x = location.getBlockX() - MAX_WIDTH / 2;
        int y = location.getBlockY() - 75;
        int z = location.getBlockZ() - MAX_HEIGHT / 2;

        latestLocation.add(location);

        pasteImage(world, x, y, z, image);
    }

    public void processVideo(URL url, Location location) {
        World world = location.getWorld();
        int x = location.getBlockX() - MAX_WIDTH / 2;
        int y = location.getBlockY() - 75;
        int z = location.getBlockZ() - MAX_HEIGHT / 2;

        latestLocation.add(location);

        // Download video
        File file = getVideoFromURL(url);
        assert file != null;

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {

            Java2DFrameConverter converter = new Java2DFrameConverter();

            new BukkitRunnable() {

                Java2DFrameConverter converter = new Java2DFrameConverter();
                Frame frame;
                int frameNumber = 0;
                boolean isFirst = true;

                @Override
                public void run() {
                    try {
                        if (isFirst) {
                            grabber.start();
                            isFirst = false;
                            return;
                        }

                        if ((frame = grabber.grab()) != null) {
                            System.out.println("Processing frame " + frameNumber);
                            BufferedImage bufferedImage = converter.getBufferedImage(frame);
                            if (bufferedImage == null) {
                                return;
                            }
                            pasteImage(world, x, y, z, bufferedImage);
                        } else {
                            cancel();
                        }
                        frameNumber++;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }.runTaskTimer(this, 0, 1);

            grabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void pasteImage(World world, int x, int y, int z, BufferedImage image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            double ratio = (double) width / (double) height;
            if (ratio > 1) {
                width = MAX_WIDTH;
                height = (int) (width / ratio);
            } else {
                height = MAX_HEIGHT;
                width = (int) (height * ratio);
            }
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();
            image = bufferedImage;
        }


        for (int i = 0; i < image.getWidth(null); i++) {
            for (int j = 0; j < image.getHeight(null); j++) {
                Color color = new Color(image.getRGB(i, j));
                Material material = ColorManager.getBlock(color);

                assert world != null;
                world.getBlockAt(x + i, y, z + j).setType(material, false);
            }
        }
    }

    public void undoLastImage() {
        Location location = latestLocation.get(latestLocation.size() - 1);
        World world = location.getWorld();
        int x = location.getBlockX() - MAX_WIDTH / 2;
        int y = location.getBlockY() - 75;
        int z = location.getBlockZ() - MAX_HEIGHT / 2;

        for (int i = 0; i < MAX_WIDTH; i++) {
            for (int j = 0; j < MAX_HEIGHT; j++) {
                assert world != null;
                world.getBlockAt(x + i, y, z + j).setType(Material.AIR, false);
            }
        }
    }

    public BufferedImage getImageFromURL(URL url) {
        try {
            BufferedImage image = ImageIO.read(url);
            return image;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Download video file from URL and save it then return the file path
    public File getVideoFromURL(URL url) {
        try {
            // Create random file name
            String fileName = "video" + System.currentTimeMillis() + ".mp4";
            File file = new File(fileName);
            FileUtils.copyURLToFile(url, file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
