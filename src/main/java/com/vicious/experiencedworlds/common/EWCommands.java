package com.vicious.experiencedworlds.common;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vicious.experiencedworlds.ExperiencedWorlds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EWCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("experiencedworlds")
                        .then(Commands.literal("border")
                                .then(Commands.literal("size")
                                        .executes(ctx->message(ctx,EWChatMessage.from("<1experiencedworlds.bordersize>", ExperiencedWorlds.getBorder().getTransformedBorderSize()))))
                                .then(Commands.literal("expansions")
                                        .executes(ctx->message(ctx,EWChatMessage.from("<1experiencedworlds.expansions>", ExperiencedWorlds.getBorder().getExpansions())))
                                )
                                .then(Commands.literal("multiplier")
                                        .executes(ctx->message(ctx,EWChatMessage.from("<1experiencedworlds.multiplier>", ExperiencedWorlds.getBorder().getSizeMultiplier())))
                                )
                        );
        event.getDispatcher().register(cmd);
    }
    private static int message(CommandContext<CommandSourceStack> ctx, EWChatMessage cm){
        CommandSourceStack stack = ctx.getSource();
        cm.send(stack);
        return 1;
    }
}
