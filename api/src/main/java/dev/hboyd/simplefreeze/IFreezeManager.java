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

package dev.hboyd.simplefreeze;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Manages player and their freeze state.
 */
@ApiStatus.NonExtendable
public interface IFreezeManager {

    /**
     * Checks if the given player has at least one freeze entry.
     *
     * @param player Player to check
     * @return true if the Player has at least one freeze entry
     */
    boolean isPlayerFrozen(OfflinePlayer player);

    /**
     * Gets the freeze entry keys currently applied to the Player ordered oldest first.
     *
     * @param offlinePlayer Player to get the freeze entries fo
     * @return freeze entry keys currently applied to the Player
     */
    @Unmodifiable LinkedHashSet<Key> getFreezeEntries(OfflinePlayer offlinePlayer);

    /**
     * Gets a map of all frozen players and their set of freeze entries ordered oldest first.
     *
     * @return Freeze entry keys currently applied to the Player
     */
    @Unmodifiable Map<OfflinePlayer, LinkedHashSet<Key>> getFreezeEntryMap();

    /**
     * Retrieves all players with at least one freeze entry.
     *
     * @return set of all players with at least one freeze entry
     */
    @Unmodifiable Set<OfflinePlayer> frozenPlayers();

    /**
     * Retrieves all players that have an entry with the given key.
     *
     * @param entryKey the key to get entries for
     * @return frozen players
     */
    @Unmodifiable Set<OfflinePlayer> frozenPlayers(Key entryKey);

    /**
     * Adds a freeze entry for the given player.
     * On collision, the entry will be overridden.
     *
     * @param offlinePlayer the player to add the freeze entry to
     * @param entryKey the key to differentiate freeze entries applied
     */
    void addFreezeEntry(OfflinePlayer offlinePlayer, Key entryKey) throws IllegalArgumentException;

    /**
     * Adds a freeze entry for the given player.
     * On collision, the entry will be overridden.
     *
     * @param offlinePlayer the player to freeze
     * @param entryKey the key to differentiate freeze entries applied
     * @param title the title to show to the player (last applied title has precedent)
     */
    void addFreezeEntry(OfflinePlayer offlinePlayer, Key entryKey, @Nullable Component title);

    /**
     * Removes the freeze entry from the given player with the given key.
     * A player is not fully unfrozen until they have no more freeze entries.
     *
     * @param offlinePlayer Player to remove the freeze entry from
     * @param entryKey the key to remove
     * @return true if the player has an entry with the key otherwise false
     * @throws IllegalStateException if the player has no state to restore to when being fully unfrozen
     */
    boolean removeFreezeEntry(OfflinePlayer offlinePlayer, Key entryKey) throws IllegalStateException ;

    /**
     * Removes all freeze entries from the given player.
     * Will restore the player to a default state no matter if any freeze entries are present or not.
     *
     * @param offlinePlayer the player to remove all freeze entries for
     * @return true if the player was restored to a stored pre-freeze state or false if it was to a default state
     */
    boolean forceUnfreezePlayer(OfflinePlayer offlinePlayer);
}
