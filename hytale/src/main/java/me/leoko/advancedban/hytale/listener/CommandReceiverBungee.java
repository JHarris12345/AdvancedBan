package me.leoko.advancedban.hytale.listener;

import me.leoko.advancedban.hytale.HytaleMain;
import me.leoko.advancedban.manager.CommandManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by Leoko @ dev.skamps.eu on 24.07.2016.
 */

public class CommandReceiverBungee extends Command {

    /**
     * @param name       name of the command
     * @param permission permission required to use the command. May be null
     */
    public CommandReceiverBungee(String name, String permission) {
        super(name, permission);
    }
    
    @Override
	public void execute(final CommandSender sender, final String[] args) {
    	if (args.length > 0) {
    		args[0] = (HytaleMain.get().getProxy().getPlayer(args[0]) != null ? HytaleMain.get().getProxy().getPlayer(args[0]).getName() : args[0]);
    	}
        CommandManager.get().onCommand(sender, this.getName(), args);
    }
    
}
