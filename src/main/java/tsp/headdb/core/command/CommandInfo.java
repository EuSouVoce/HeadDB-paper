package tsp.headdb.core.command;

import org.bukkit.command.CommandSender;
import tsp.headdb.HeadDB;
import tsp.headdb.core.util.BuildProperties;
import tsp.smartplugin.player.PlayerUtils;

public class CommandInfo extends SubCommand {

    public CommandInfo() {
        super("info", new String[]{"i"});
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        if (HeadDB.getInstance().getConfig().getBoolean("showAdvancedPluginInfo")) {
            BuildProperties build = HeadDB.getInstance().getBuildProperties();
            PlayerUtils.sendMessage(sender, "&7Running &6HeadDB - " + build.getVersion() + " &7(&6Build " + build.getBuildNumber() + "&7)");
            PlayerUtils.sendMessage(sender, "&7Created by &6" + HeadDB.getInstance().getDescription().getAuthors());
            PlayerUtils.sendMessage(sender, "&7Compiled on &6" + build.getTimestamp() + " &7by &6" + build.getAuthor());
        } else {
            PlayerUtils.sendMessage(sender, "&7Running &6HeadDB &7by &6TheSilentPro (Silent)");
            PlayerUtils.sendMessage(sender, "&7GitHub: &6https://github.com/TheSilentPro/HeadDB");
        }
    }

}
