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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
            if (material.equals(Material.REDSTONE_LAMP) || material.name().contains("SHULKER")) continue;

            world.getBlockAt(0, 0, 0).setType(material);
            Block block = world.getBlockAt(0, 0, 0);
            if (!isCube(block)) {
                continue;
            }
            colorMap.put(material, getColor(block));

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
    public static Material getBlock(Color color) {
        double minDistance = Double.MAX_VALUE;
        Material closestMaterial = Material.AIR;

        for (Map.Entry<Material, Color> materialColorEntry : colorMap.entrySet()) {
            Material material = materialColorEntry.getKey();
            Color materialColor = materialColorEntry.getValue();
            double distance = getDistance(color, materialColor);
            if (distance < minDistance) {
                minDistance = distance;
                closestMaterial = material;
            }
        }

        return closestMaterial;
    }

    public static double getDistance(Color color1, Color color2) {
        double redDistance = Math.pow(color1.getRed() - color2.getRed(), 2);
        double greenDistance = Math.pow(color1.getGreen() - color2.getGreen(), 2);
        double blueDistance = Math.pow(color1.getBlue() - color2.getBlue(), 2);
        return Math.sqrt(redDistance + greenDistance + blueDistance);
    }
}
