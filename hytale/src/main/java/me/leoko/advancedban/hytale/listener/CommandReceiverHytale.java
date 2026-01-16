package me.leoko.advancedban.hytale.listener;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.HytaleMain;
import me.leoko.advancedban.manager.CommandManager;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Created by Leoko @ dev.skamps.eu on 24.07.2016.
 */

public class CommandReceiverHytale extends CommandBase {

    public CommandReceiverHytale(@Nonnull String name, @Nonnull String description, @Nonnull String permission) {
        super(name, description);
        this.requirePermission(permission);
        this.setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String fullArgs[] = ctx.getInputString().split("\\s+");
        String[] args = Arrays.copyOfRange(fullArgs, 1, fullArgs.length);

        if (args.length > 0) {
            PlayerRef target = (PlayerRef) Universal.get().getMethods().getPlayer(args[0]);
            args[0] = target != null ? target.getUsername() : args[0];
        }

        CommandManager.get().onCommand(ctx.sender(), this.getName(), args);
    }
}
