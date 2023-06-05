package dev.bdinc.minecraft_video_player;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;

import java.awt.*;
import java.util.*;

public class ColorManager {

    public static HashMap<Material, Color> colorMap = new HashMap<>();

    public static Color getColor(Block block) {
        CraftBlock cb = (CraftBlock) block;
        net.minecraft.world.level.block.state.BlockState bs = cb.getNMS();
        net.minecraft.world.level.material.MaterialColor mc = bs.getMapColor(cb.getCraftWorld().getHandle(), cb.getPosition());
        return new Color(mc.col);
    }

    public static Color getColor(Material material) {
        net.minecraft.world.level.block.Block block = CraftMagicNumbers.getBlock(material);
        net.minecraft.world.level.material.MaterialColor mc = block.defaultMaterialColor();
        return new Color(mc.col);
    }

    public static void setupColorMap() {
        Iterator<Material> iterator = Arrays.stream(Material.values()).iterator();

        World world = Bukkit.getWorlds().get(0);
        Block defaultBlock = world.getBlockAt(0, 0, 0);
        while (iterator.hasNext()) {
            Material material = iterator.next();

            // Removing some blocks that are not suitable for this
            if (!material.isBlock() || material.isAir() || !material.isSolid()) continue;
            if (material.name().contains("GLASS") || material.equals(Material.BARRIER)) continue;
            if (material.name().contains("POWDER") || material.name().contains("SAND") || material.name().contains("GRAVEL")) continue;
            if (material.equals(Material.REDSTONE_LAMP) || material.name().contains("SHULKER") || material.name().contains("GLAZED")) continue;

            if (Main.speedMode) {
                if (material.name().toUpperCase().contains("WOOL") || material.name().toUpperCase().contains("CONCRETE") || material.name().toUpperCase().contains("TERRACOTTA")){
                    colorMap.put(material, getColor(material));
                }
            }

        }
        world.getBlockAt(0, 0, 0).setType(defaultBlock.getType());
        world.getBlockAt(0, 0, 0).setBlockData(defaultBlock.getBlockData(), false);
    }

    public static boolean isCube(Block block) {
        VoxelShape voxelShape = block.getCollisionShape();
        BoundingBox boundingBox = block.getBoundingBox();
        return (voxelShape.getBoundingBoxes().size() == 1
                && boundingBox.getWidthX() == 1.0
                && boundingBox.getHeight() == 1.0
                && boundingBox.getWidthZ() == 1.0
        );
    }

    // Get the closest material to the color
    public static Material lastMaterial;
    public static Color lastColor;

//    public static Material getBlock(Color color) {
//        // Check if the color is the same as the last color
//        if (lastColor != null && lastColor.equals(color)) {
//            return lastMaterial;
//        }
//
//        // Create a priority queue to store the distance between the color and the material
//        PriorityQueue<Map.Entry<Material, Double>> distanceQueue = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
//
//        colorMap.forEach((material, materialColor) -> {
//            double distance = getDistance(color, materialColor);
//            distanceQueue.offer(Map.entry(material, distance));
//        });
//
//        if (distanceQueue.isEmpty()) {
//            return Material.AIR;
//        }
//
//        // Set the last color and material
//        lastColor = color;
//        lastMaterial = distanceQueue.peek().getKey();
//
//        // Get the closest material
//        return distanceQueue.poll().getKey();
//    }

    public static Material getBlock(Color color) {
        if (lastColor != null && lastColor.equals(color)) {
            return lastMaterial;
        }

        double minDistance = Double.MAX_VALUE;
        Material closestMaterial = Material.AIR;

        // Iterate through the color map and find the closest color
        for (Map.Entry<Material, Color> materialColorEntry : colorMap.entrySet()) {
            Material material = materialColorEntry.getKey();
            Color materialColor = materialColorEntry.getValue();
            double distance = getDistance(color, materialColor);
            if (distance < minDistance) {
                minDistance = distance;
                closestMaterial = material;
            }
        }

        lastMaterial = closestMaterial;
        lastColor = color;
        return closestMaterial;
    }

    public static double getDistance(Color color1, Color color2) {
        double redDistance = Math.pow(color1.getRed() - color2.getRed(), 2);
        double greenDistance = Math.pow(color1.getGreen() - color2.getGreen(), 2);
        double blueDistance = Math.pow(color1.getBlue() - color2.getBlue(), 2);
        return Math.sqrt(redDistance + greenDistance + blueDistance);
    }
}
