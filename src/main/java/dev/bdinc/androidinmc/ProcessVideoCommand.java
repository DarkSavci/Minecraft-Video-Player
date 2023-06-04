package dev.bdinc.androidinmc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

public class ProcessVideoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length != 1) {
            sender.sendMessage("Usage: /processvideo <url>");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return false;
        }

        Player player = (Player) sender;

        String url = args[0];
        sender.sendMessage("URL is set to " + url);
        try {
            AndroidInMC.getInstance().processVideo(new URL(url), player.getLocation());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
