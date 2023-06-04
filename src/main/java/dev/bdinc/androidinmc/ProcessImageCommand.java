package dev.bdinc.androidinmc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

public class ProcessImageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length != 1) {
            sender.sendMessage("Usage: /test <url>");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return false;
        }

        Player player = (Player) sender;

        String url = args[0];
        sender.sendMessage("URL is set to " + url);
        sender.sendMessage("Getting image!");
        try {
            BufferedImage image = AndroidInMC.getInstance().getImageFromURL(new URL(url));
            if (image == null) {
                sender.sendMessage("Image is null!");
                return false;
            }
            sender.sendMessage("Image is now gathered!");
            sender.sendMessage("Processing image!");
            AndroidInMC.getInstance().processImage(image, player.getLocation());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
