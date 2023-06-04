package dev.bdinc.androidinmc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ProcessStreamCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return false;
        }

        Player player = (Player) sender;


        AndroidInMC.getInstance().processStream(new File("./stream_output"), player.getLocation());

        return false;
    }
}
