package dev.bdinc.minecraft_video_player;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.plugin.java.JavaPlugin;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public final class Main extends JavaPlugin {

    private static Main instance;

    public static int MAX_WIDTH = 100;
    public static int MAX_HEIGHT = 100;
    public static int MAX_FPS = 30;
    public static boolean speedMode = true;

    public static ArrayList<Location> latestLocation = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        ColorManager.setupColorMap();
        Objects.requireNonNull(getCommand("processimage")).setExecutor(new ProcessImageCommand());
        Objects.requireNonNull(getCommand("processvideo")).setExecutor(new ProcessVideoCommand());
        Objects.requireNonNull(getCommand("processstream")).setExecutor(new ProcessStreamCommand());
        Objects.requireNonNull(getCommand("setres")).setExecutor(new SetResCommand());
        Objects.requireNonNull(getCommand("undoimage")).setExecutor(new UndoCommand());
    }

    public static Main getInstance() {
        return instance;
    }

    public void processImage(BufferedImage image, Location location) {
        World world = location.getWorld();
        int x = location.getBlockX() - MAX_WIDTH / 2;
        int y = location.getBlockY() - 5;
        int z = location.getBlockZ() - MAX_HEIGHT / 2;

        latestLocation.add(location);

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

        pasteImage(world, x, y, z, image);
    }

    public void processStream(File streamDirectory, Location location) {
        StreamFrameProcessor streamFrameProcessor = new StreamFrameProcessor(this, location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        streamFrameProcessor.start(streamDirectory);
    }

    public void processVideo(URL url, Location location) {
        World world = location.getWorld();
        int x = location.getBlockX() - MAX_WIDTH / 2;
        int y = location.getBlockY() - 10;
        int z = location.getBlockZ() - MAX_HEIGHT / 2;

        latestLocation.add(location);

        // Download video
        Bukkit.broadcastMessage("§aDownloading video...");
        File file = getVideoFromURL(url);
        assert file != null;

        // Resize video
        Bukkit.broadcastMessage("§aResizing video...");
        File resizedFile;
        try {
            resizedFile = resizeVideo(file, MAX_WIDTH, MAX_HEIGHT);
        } catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
            throw new RuntimeException(e);
        }

        // Process video
        Bukkit.broadcastMessage("§aProcessing video...");
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(resizedFile)) {

            Java2DFrameConverter converter = new Java2DFrameConverter();
            VideoFrameProcessor videoFrameProcessor = new VideoFrameProcessor(this, world, x, y, z);
            videoFrameProcessor.start();

            final boolean[] isStarted = {true};
            new Thread(() -> {
                try {
                    if (isStarted[0]) {
                        grabber.start();
                        isStarted[0] = false;
                    }
                } catch (FFmpegFrameGrabber.Exception e) {
                    throw new RuntimeException(e);
                }
                try {
                    Frame frame;
                    long lastFrameTime = System.currentTimeMillis();
                    boolean isFinished = false;
                    while (!isFinished) {
                        if (grabber.getLengthInFrames() <= grabber.getFrameNumber()) {
                            isFinished = true;
                        }

                        if ((System.currentTimeMillis() - lastFrameTime) >= (1000 / (grabber.getFrameRate() > MAX_FPS ? MAX_FPS : grabber.getFrameRate()))) {
                            frame = grabber.grab();
                            BufferedImage bufferedImage = converter.getBufferedImage(frame);
                            if (bufferedImage != null) {
                                videoFrameProcessor.addFrame(bufferedImage);
                                lastFrameTime = System.currentTimeMillis();
                            }
                        }
                    }
                    grabber.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File resizeVideo(File video, int width, int height) throws FrameGrabber.Exception, FrameRecorder.Exception {
        long startTime = System.currentTimeMillis();

        // Create a FrameGrabber to read the input video
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(video);
        frameGrabber.start();

        // Calculate the aspect ratio of the original video
        double aspectRatio = (double) frameGrabber.getImageWidth() / frameGrabber.getImageHeight();

        // Calculate the new width and height based on the desired dimension and aspect ratio
        int newWidth, newHeight;
        double targetAspectRatio = (double) width / height;
        if (aspectRatio > targetAspectRatio) {
            newWidth = width;
            newHeight = (int) (width / aspectRatio);
        } else {
            newWidth = (int) (height * aspectRatio);
            newHeight = height;
        }

        // Create a FrameRecorder to write the output video
        String outputFilename = video.getAbsolutePath().substring(0, video.getAbsolutePath().lastIndexOf(".")) + "_resized.mp4";
        FFmpegFrameRecorder frameRecorder = new FFmpegFrameRecorder(outputFilename, newWidth, newHeight);
        frameRecorder.setVideoCodec(frameGrabber.getVideoCodec());
        frameRecorder.setFormat("mp4");
        frameRecorder.setFrameRate(frameGrabber.getFrameRate());
        frameRecorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        frameRecorder.start();

        // Initialize OpenCVFrameConverter
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        // Process each frame
        Frame frame;
        while ((frame = frameGrabber.grab()) != null) {
            // Convert the frame to a Mat object
            Mat mat = converter.convertToMat(frame);

            // Resize the frame
            Mat resizedMat = new Mat();
            if (mat != null) {
                org.bytedeco.opencv.global.opencv_imgproc.resize(mat, resizedMat, new Size(newWidth, newHeight));
            } else {
                // Handle the case when the input frame (mat) is null
                // You can choose to skip this frame or take appropriate action
                continue; // Skip the current frame and move to the next iteration
            }

            // Convert the resized Mat back to a Frame
            Frame resizedFrame = converter.convert(resizedMat);

            // Record the resized frame
            frameRecorder.record(resizedFrame);
        }

        // Release resources
        frameGrabber.stop();
        frameRecorder.stop();

        // Delete the original file
        File resizedVideo = new File(outputFilename);
        if (resizedVideo.exists()) {
            try {
                FileUtils.forceDelete(video);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        Bukkit.broadcastMessage("Time taken to resize: " + (endTime - startTime) + "ms");

        return resizedVideo;
    }


    void pasteImage(World world, int x, int y, int z, BufferedImage image) {
        long startTime = System.currentTimeMillis();

        long startOfPasteTime = System.currentTimeMillis();
        for (int i = 0; i < image.getWidth(null); i++) {
            for (int j = 0; j < image.getHeight(null); j++) {
                Color color = new Color(image.getRGB(i, j));
                Material material = ColorManager.getBlock(color);
                if (!world.getBlockAt(x + i, y, z + j).getType().equals(material)) {
                    setBlockInNativeWorld(world, x + i, y, z + j, material, false);
                }
            }
        }
        long endOfPasteTime = System.currentTimeMillis();
        Bukkit.broadcastMessage("Time taken to paste: " + (endOfPasteTime - startOfPasteTime) + "ms");
        long endTime = System.currentTimeMillis();
        Bukkit.broadcastMessage("Time taken to complete: " + (endTime - startTime) + "ms");
    }

    public void undoLastImage() {
        Location location = latestLocation.get(latestLocation.size() - 1);
        World world = location.getWorld();
        int x = location.getBlockX() - MAX_WIDTH / 2;
        int y = location.getBlockY() - 10;
        int z = location.getBlockZ() - MAX_HEIGHT / 2;

        for (int i = 0; i < MAX_WIDTH; i++) {
            for (int j = 0; j < MAX_HEIGHT; j++) {
                assert world != null;
                setBlockInNativeWorld(world, x + i, y, z + j, Material.AIR, false);
            }
        }
    }

    public BufferedImage getImageFromURL(URL url) {
        try {
            return ImageIO.read(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Download video file from URL and save it then return the file path
    public File getVideoFromURL(URL url) {
        long startTime = System.currentTimeMillis();
        try {
            // Create random file name
            String fileName = "video" + System.currentTimeMillis() + ".mp4";
            File file = new File(fileName);
            FileUtils.copyURLToFile(url, file);
            long endTime = System.currentTimeMillis();
            Bukkit.broadcastMessage("Time taken to download: " + (endTime - startTime) + "ms");
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void setBlockInNativeWorld(World world, int x, int y, int z, Material material, boolean applyPhysics) {
        ServerLevel nmsWorld = ((CraftWorld) world).getHandle();
        BlockPos blockPos = new BlockPos(x, y, z);
        nmsWorld.setBlock(blockPos, CraftMagicNumbers.getBlock(material).defaultBlockState(), applyPhysics ? 3 : 2);
    }

}
