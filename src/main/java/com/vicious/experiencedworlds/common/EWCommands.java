package com.vicious.experiencedworlds.common;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vicious.experiencedworlds.ExperiencedWorlds;
import com.vicious.experiencedworlds.common.config.EWCFG;
import com.vicious.experiencedworlds.common.data.EWWorldData;
import com.vicious.experiencedworlds.common.data.IWorldSpecificEWDat;
import com.vicious.viciouscore.common.capability.VCCapabilities;
import com.vicious.viciouscore.common.util.FuckLazyOptionals;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Consumer;

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
                )
                .then(Commands.literal("config")
                        .requires((ctx)->ctx.hasPermission(Commands.LEVEL_ADMINS) || inIntegratedServer(ctx))
                        .then(Commands.literal("reload")
                                .executes(ctx->{
                                    EWCFG.getInstance().load();
                                    EWCFG.getInstance().save();
                                    return 1;
                                })))
                .then(Commands.literal("world")
                        .executes((ctx)->{
                            EWChatMessage.from(getWorld(ctx).getLevelData()).send(ctx);
                            return 1;
                        })
                        .requires((ctx)->ctx.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.literal("bonusbordermultiplier")
                                .executes(ctx->{
                                    ServerLevel l = getWorld(ctx);
                                    if(l != null) {
                                        if (FuckLazyOptionals.getOrNull(l.getCapability(VCCapabilities.LEVELDATA)) instanceof IWorldSpecificEWDat ew) {
                                            EWChatMessage.from("<2experiencedworlds.worldmultiplier>", ServerHelper.getLevelName(l), ew.getExperiencedWorlds().multiplier.getValue()).send(ctx.getSource());
                                        }
                                    }
                                    else{
                                        EWChatMessage.from("This command is player only").send(ctx.getSource());
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("value",DoubleArgumentType.doubleArg(0))
                                        .executes(ctx->setWorldMultiplier(getWorld(ctx),ctx))))
                        .then(Commands.literal("bonusbordersize")
                                .executes(ctx->{
                                    ServerLevel l = getWorld(ctx);
                                    if(l != null) {
                                        if (FuckLazyOptionals.getOrNull(l.getCapability(VCCapabilities.LEVELDATA)) instanceof IWorldSpecificEWDat ew) {
                                            EWChatMessage.from("<2experiencedworlds.worldsize>", ServerHelper.getLevelName(l), ew.getExperiencedWorlds().startingSize.getValue()).send(ctx.getSource());
                                        }
                                    }
                                    else{
                                        EWChatMessage.from("This command is player only").send(ctx.getSource());
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("value",DoubleArgumentType.doubleArg(0)))
                        )
                );
        event.getDispatcher().register(cmd);
    }

    /**
     * Allows usage on singleplayer and LAN worlds.
     */
    private static boolean inIntegratedServer(CommandSourceStack ctx) {
        return !(ctx.getServer() instanceof DedicatedServer);
    }

    private static int message(CommandContext<CommandSourceStack> ctx, EWChatMessage cm){
        CommandSourceStack stack = ctx.getSource();
        cm.send(stack);
        return 1;
    }
    private static ServerLevel getWorld(String levelName){
        return ServerHelper.getLevelByName(levelName);
    }
    private static ServerLevel getWorld(CommandContext<CommandSourceStack> ctx){
        if(ctx.getSource().getEntity() instanceof ServerPlayer sp){
            return sp.getLevel();
        }
        else{
            EWChatMessage.from("This command is player only").send(ctx);
            return null;
        }
    }
    private static int setWorldMultiplier(ServerLevel level, CommandContext<CommandSourceStack> ctx){
        ifExists(level,ctx,(dat)->{
            dat.multiplier.setValue(DoubleArgumentType.getDouble(ctx,"value"));
            EWChatMessage.from("<1experiencedworlds.setworldmultiplier>",dat.multiplier.getValue()).send(ctx);
        });
        EWEventHandler.growBorder();
        return 1;
    }
    private static int setWorldSize(ServerLevel level, CommandContext<CommandSourceStack> ctx){
        ifExists(level,ctx,(dat)->{
            dat.startingSize.setValue(DoubleArgumentType.getDouble(ctx,"value"));
            EWChatMessage.from("<1experiencedworlds.setworldsize>",dat.startingSize.getValue()).send(ctx);
        });
        EWEventHandler.growBorder();
        return 1;
    }
    private static void ifExists(ServerLevel level, CommandContext<CommandSourceStack> ctx, Consumer<EWWorldData> cons){
        if(level != null) {
            if (FuckLazyOptionals.getOrNull(level.getCapability(VCCapabilities.LEVELDATA)) instanceof IWorldSpecificEWDat ew) {
                cons.accept(ew.getExperiencedWorlds());
            }
        }
        else{
            EWChatMessage.from("<1experiencedworlds.nosuchlevel>", StringArgumentType.getString(ctx,"world")).send(ctx);
        }
    }
}
