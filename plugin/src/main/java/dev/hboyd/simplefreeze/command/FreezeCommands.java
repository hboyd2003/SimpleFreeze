/*
 * Simple Freeze
 * Copyright (c) 2026 Harrison Boyd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.hboyd.simplefreeze.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.hboyd.prismatic.MessageUtil;
import dev.hboyd.prismatic.brigadier.BrigadierCommand;
import dev.hboyd.prismatic.brigadier.CommandUtil;
import dev.hboyd.prismatic.brigadier.argument.CustomOfflinePlayerArgument;
import dev.hboyd.prismatic.brigadier.argument.CustomOfflinePlayerArgumentResolver;
import dev.hboyd.simplefreeze.FreezeManager;
import dev.hboyd.simplefreeze.ISimpleFreeze;
import dev.hboyd.simplefreeze.SimpleFreeze;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.util.*;
import java.util.stream.Collectors;

public class FreezeCommands implements BrigadierCommand {
    private static final Key FREEZE_ENTRY_KEY = Key.key(ISimpleFreeze.NAMESPACE, "command");
    private static final SimpleCommandExceptionType PLAYER_ALREADY_FROZEN_EXCEPTION =
            new SimpleCommandExceptionType(MessageUtil.translatableMessage("simplefreeze.error.already_frozen"));
    private static final SimpleCommandExceptionType PLAYER_NOT_FROZEN_EXCEPTION =
            new SimpleCommandExceptionType(MessageUtil.translatableMessage("simplefreeze.error.not_frozen"));

    private final FreezeManager freezeManager;
    private final LiteralCommandNode<CommandSourceStack> freezeCommand;
    private final LiteralCommandNode<CommandSourceStack> unfreezeCommand;

    public FreezeCommands(FreezeManager freezeManager) {
        CustomOfflinePlayerArgument unfrozenPlayersArgument = new CustomOfflinePlayerArgument(
                offlinePlayer -> !freezeManager.frozenPlayers(FREEZE_ENTRY_KEY).contains(offlinePlayer),
                false,
                PLAYER_ALREADY_FROZEN_EXCEPTION);

        this.freezeManager = freezeManager;
        this.freezeCommand = Commands.literal("freeze")
                .requires(stack -> stack.getSender().hasPermission("simplefreeze.command.freeze"))
                .then(Commands.argument("players", unfrozenPlayersArgument)
                        .executes(this::freezePlayer)
                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                .executes(this::freezePlayerWithTitle))).build();

        this.unfreezeCommand = Commands.literal("unfreeze")
                .requires(stack -> stack.getSender().hasPermission("simplefreeze.command.unfreeze"))
                .then(Commands.argument("players", CustomOfflinePlayerArgument.OFFLINE_PLAYERS)
                        .executes(this::unfreezePlayer)
                        .then(Commands.argument("force", BoolArgumentType.bool())
                                .executes(this::unfreezePlayerForceable)))
                .build();
    }

    @Override
    public void register(Commands registrar) {
        registrar.register(this.freezeCommand, "Freezes one or more players with an optional MiniMessage title displayed");
        registrar.register(this.unfreezeCommand, "Unfreezes one or more players optionally force unfreezing them settings the player to a default state.");
    }

    @Override
    public Collection<Permission> permissions() {
        return List.of();
    }


    private int freezePlayer(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        final Collection<OfflinePlayer> players = commandContext.getArgument("players", CustomOfflinePlayerArgumentResolver.class)
                .resolve(commandContext.getSource());

        players.forEach(player -> this.freezeManager.addFreezeEntry(player, FREEZE_ENTRY_KEY));

        sendFeedback(commandContext,
                "simplefreeze.command.freeze.froze_player",
                "simplefreeze.command.freeze.froze_players",
                players);

        return Command.SINGLE_SUCCESS;
    }

    private int freezePlayerWithTitle(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        final Collection<OfflinePlayer> players = commandContext.getArgument("players", CustomOfflinePlayerArgumentResolver.class)
                .resolve(commandContext.getSource());
        final Component titleComponent = MiniMessage.miniMessage().deserialize(commandContext.getArgument("title", String.class));

        players.forEach(player -> this.freezeManager.addFreezeEntry(player, FREEZE_ENTRY_KEY, titleComponent));

        sendFeedback(commandContext,
                "simplefreeze.command.freeze.froze_player",
                "simplefreeze.command.freeze.froze_players",
                players);

        return Command.SINGLE_SUCCESS;
    }

    private int unfreezePlayer(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        final Collection<OfflinePlayer> players = commandContext.getArgument("players", CustomOfflinePlayerArgumentResolver.class)
                .resolve(commandContext.getSource());

        List<OfflinePlayer> filteredPlayers = players.stream()
                .filter(player -> this.freezeManager.getFreezeEntries(player).contains(FREEZE_ENTRY_KEY))
                .toList();

        if (filteredPlayers.isEmpty()) throw PLAYER_NOT_FROZEN_EXCEPTION.create();

        int maxEntries = 0;
        boolean sameEntries = true;
        Set<Key> firstFreezeEntries = null;
        for (OfflinePlayer player : filteredPlayers) {
            this.freezeManager.removeFreezeEntry(player, FREEZE_ENTRY_KEY);
            Set<Key> freezeEntries = this.freezeManager.getFreezeEntries(player);

            if (firstFreezeEntries == null) firstFreezeEntries = Set.copyOf(freezeEntries);
            else if (sameEntries)
                sameEntries = firstFreezeEntries.size() == freezeEntries.size()
                        && freezeEntries.containsAll(firstFreezeEntries)
                        && firstFreezeEntries.containsAll(freezeEntries);

            if (maxEntries < freezeEntries.size()) maxEntries = freezeEntries.size();
        }
        filteredPlayers.forEach(player -> this.freezeManager.removeFreezeEntry(player, FREEZE_ENTRY_KEY));

        String translationKey = "simplefreeze.command.unfreeze.unfroze_player";
        final ComponentLike playerArgument;
        if (filteredPlayers.size() > 1) {
            translationKey += "s.";
            playerArgument = Argument.numeric("player_count", filteredPlayers.size());
        } else {
            translationKey += ".";
            playerArgument = Argument.component("player_name", getPlayerComponent(filteredPlayers.getFirst()));
        }

        if (maxEntries == 0) {
            CommandUtil.sendTranslatableResponse(commandContext, translationKey + "full", playerArgument);
            return Command.SINGLE_SUCCESS;
        }

        final ComponentLike freezeEntryArgument;
        if (!sameEntries || maxEntries > 1) {
            freezeEntryArgument = Argument.component("freeze_entry_count",
                    Component.text(firstFreezeEntries.size())
                        .hoverEvent(Component.text(firstFreezeEntries.stream()
                                .map(Key::asString)
                                .collect(Collectors.joining(", ")))));
            if (!sameEntries) translationKey += "ambiguous";
            else translationKey += "multiple";
        }
        else {
            freezeEntryArgument = Argument.string("freeze_entry_key", firstFreezeEntries.iterator().next().asString());
            translationKey += "single";
        }

        CommandUtil.sendTranslatableResponse(commandContext, translationKey, playerArgument, freezeEntryArgument);

        return Command.SINGLE_SUCCESS;
    }

    private int unfreezePlayerForceable(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        final Collection<OfflinePlayer> players = commandContext.getArgument("players", CustomOfflinePlayerArgumentResolver.class)
                .resolve(commandContext.getSource());
        final boolean force = commandContext.getArgument("force", Boolean.class);

        if (!force) return unfreezePlayer(commandContext);

        players.forEach(this.freezeManager::forceUnfreezePlayer);

        sendFeedback(commandContext,
                "simplefreeze.command.unfreeze.force.unfroze_player",
                "simplefreeze.command.unfreeze.force.unfroze_players",
                players);

        return Command.SINGLE_SUCCESS;
    }

    private void sendFeedback(CommandContext<CommandSourceStack> commandContext, String singularKey, String pluralKey, Collection<OfflinePlayer> offlinePlayers) {
        if (offlinePlayers.size() > 1)
            CommandUtil.sendTranslatableResponse(commandContext, pluralKey,
                    Argument.numeric("player_count", offlinePlayers.size()));
        else CommandUtil.sendTranslatableResponse(commandContext, singularKey,
                    Argument.component("player_name", getPlayerComponent(offlinePlayers.iterator().next())));
    }

    private Component getPlayerComponent(OfflinePlayer offlinePlayer) {
        final Player player = offlinePlayer.getPlayer();
        final HoverEventSource<?> playerHoverEventSource;
        if (player != null) playerHoverEventSource = player;
        else playerHoverEventSource = Component.text(offlinePlayer.getName() + "\n" + offlinePlayer.getUniqueId());

        String playerName = offlinePlayer.getName();
        if (playerName == null) playerName = offlinePlayer.getUniqueId().toString();

        return Component.text(playerName).hoverEvent(playerHoverEventSource);
    }
}
