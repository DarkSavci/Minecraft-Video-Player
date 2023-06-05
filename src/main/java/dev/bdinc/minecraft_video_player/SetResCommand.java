package dev.bdinc.minecraft_video_player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetResCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length != 3) {
            sender.sendMessage("§c/setres <width< <height> <fps>.");
            return false;
        }

        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        int fps = Integer.parseInt(args[2]);

        if (width < 1 || height < 1 || fps < 1) {
            sender.sendMessage("§c/setres <width< <height> <fps<.");
            return false;
        }

        sender.sendMessage("§aResolution set to " + width + "x" + height + " at " + fps + " FPS.");

        Main.MAX_WIDTH = width;
        Main.MAX_HEIGHT = height;
        Main.MAX_FPS = fps;
        return false;
    }
}
