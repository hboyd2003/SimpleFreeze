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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.hboyd.chasm.font.StyledGlyph;
import dev.hboyd.prismatic.MessageUtil;
import dev.hboyd.prismatic.brigadier.BrigadierCommand;
import dev.hboyd.prismatic.brigadier.argument.CustomOfflinePlayerArgument;
import dev.hboyd.prismatic.brigadier.argument.CustomOfflinePlayerArgumentResolver;
import dev.hboyd.prismatic.text.GroupedMessageRenderer;
import dev.hboyd.prismatic.text.GroupedMessageStyle;
import dev.hboyd.prismatic.text.PaginatedListRenderer;
import dev.hboyd.prismatic.text.PaginatedListStyle;
import dev.hboyd.simplefreeze.SimpleFreeze;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.Permission;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.*;

public class SimpleFreezeCommand implements BrigadierCommand {
    private static final SimpleCommandExceptionType NO_FROZEN_PLAYERS_EXCEPTION =
            new SimpleCommandExceptionType(MessageUtil.translatableMessage("simplefreeze.command.simplefreeze.list.error.no_frozen_players"));
    private static final SimpleCommandExceptionType PLAYER_NOT_FROZEN_EXCEPTION =
            new SimpleCommandExceptionType(MessageUtil.translatableMessage("simplefreeze.error.not_frozen"));

    private static final DateTimeFormatter ISO_LOCAL_DATE = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendLiteral(' ')
            .appendOffsetId()
            .toFormatter();


    private final SimpleFreeze simpleFreeze;
    private final LiteralCommandNode<CommandSourceStack> simpleFreezeCommand;

    public SimpleFreezeCommand(SimpleFreeze simpleFreeze) {
        this.simpleFreeze = simpleFreeze;
        this.simpleFreezeCommand = Commands.literal("simplefreeze")
                .then(Commands.literal("version")
                        .requires(stack -> stack.getSender().hasPermission("simplefreeze.command.simplefreeze.version"))
                        .executes(this::version))
                .then(Commands.literal("status")
                        .requires(stack -> stack.getSender().hasPermission("simplefreeze.command.simplefreeze.status"))
                        .executes(this::status)
                        .then(Commands.argument("player", new CustomOfflinePlayerArgument(() -> simpleFreeze.freezeManager().frozenPlayers(), true, PLAYER_NOT_FROZEN_EXCEPTION))
                                .executes(this::playerStatus)))
                .then(Commands.literal("list")
                        .executes(this::list))
                .build();

    }

    @Override
    public void register(Commands commands) {
        commands.register(this.simpleFreezeCommand, "Commands for information about SimpleFreeze and its status", List.of("sf"));
    }

    @Override
    public Collection<Permission> permissions() {
        return List.of();
    }

    private int version(CommandContext<CommandSourceStack> commandContext) {
        commandContext.getSource().getSender()
                .sendMessage(Component.translatable("simplefreeze.command.version",
                        Argument.string("version", this.simpleFreeze.getPluginMeta().getVersion())));

        return Command.SINGLE_SUCCESS;
    }

    private int status(CommandContext<CommandSourceStack> commandContext) {
        final Map<OfflinePlayer, LinkedHashSet<Key>> freezeEntryMap = this.simpleFreeze.freezeManager().getFreezeEntryMap();

        List<Component> responseLines = List.of(
                Component.translatable("simplefreeze.command.simplefreeze.status.total",
                        Argument.numeric("player_count", freezeEntryMap.size())),
                Component.translatable("simplefreeze.command.simplefreeze.status.uniqueKeys",
                        Argument.numeric("freeze_entry_count", freezeEntryMap.values().stream()
                                .flatMap(Collection::stream)
                                .distinct()
                                .count())));

        GroupedMessageRenderer.sendGroupedMessage(commandContext.getSource().getSender(),
                Component.translatable("simplefreeze.command.simplefreeze.status.header"),
                responseLines,
                GroupedMessageStyle.builder().spacingGlyph(new StyledGlyph('-', Style.empty())).build());

        return Command.SINGLE_SUCCESS;
    }

    private int playerStatus(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        OfflinePlayer offlinePlayer = commandContext.getArgument("player", CustomOfflinePlayerArgumentResolver.class)
                .resolve(commandContext.getSource()).iterator().next();

        LinkedHashSet<Key> freezeEntries = this.simpleFreeze.freezeManager().getFreezeEntries(offlinePlayer);

        List<Component> responseLines = List.of(
                Component.translatable("simplefreeze.command.simplefreeze.status.player.totalentries",
                        Argument.numeric("freeze_entry_count", (long) freezeEntries.size())),
                Component.translatable("simplefreeze.command.simplefreeze.status.player.entries",
                        Argument.string("freeze_entries", freezeEntries.stream()
                                .map(Key::asString)
                                .collect(Collectors.joining(",")))),
                Component.translatable("simplefreeze.command.simplefreeze.status.player.lastEntry",
                        Argument.string("freeze_entry", freezeEntries.getFirst().asString())),
                Component.translatable("simplefreeze.command.simplefreeze.status.player.frozenSince",
                        Argument.string("frozen_timestamp", ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(Instant.now()))));

        GroupedMessageRenderer.sendGroupedMessage(commandContext.getSource().getSender(),
                Component.translatable("simplefreeze.command.simplefreeze.status.player.header",
                        Argument.string("player_name", offlinePlayer.getName())),
                responseLines);

        return Command.SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ArrayList<Component> frozenPlayerList = new ArrayList<>();
        for (OfflinePlayer frozenPlayer : this.simpleFreeze.freezeManager().frozenPlayers()) {
            LinkedHashSet<Key> freezeEntries = this.simpleFreeze.freezeManager().getFreezeEntries(frozenPlayer);

            Component hoverComponent;
            if (freezeEntries.size() == 1)
                hoverComponent = Component.translatable("simplefreeze.command.simplefreeze.list.frozen_by",
                        Argument.string("plugin_name", freezeEntries.getFirst().toString()));
            else
                hoverComponent = Component.translatable("simplefreeze.command.simplefreeze.list.frozen_by_count",
                        Argument.numeric("plugin_count", freezeEntries.size()));

            String name = Optional.ofNullable(frozenPlayer.getName()).orElse(frozenPlayer.getUniqueId().toString());
            frozenPlayerList.add(Component.text(name).hoverEvent(HoverEvent.showText(hoverComponent)));
        }

        if (frozenPlayerList.isEmpty())
            throw NO_FROZEN_PLAYERS_EXCEPTION.create();

        PaginatedListRenderer.sendPaginatedList(commandContext.getSource().getSender(),
                frozenPlayerList,
                Component.text("Frozen Players"),
                PaginatedListStyle.DEFAULT);
        return Command.SINGLE_SUCCESS;
    }
}
