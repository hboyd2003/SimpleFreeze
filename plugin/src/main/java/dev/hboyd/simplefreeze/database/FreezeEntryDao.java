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

package dev.hboyd.simplefreeze.database;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.seasar.doma.*;

import java.util.*;

@Dao
public interface FreezeEntryDao {
    @Script
    @Sql("""
            CREATE TABLE IF NOT EXISTS freeze_entry
            (
                player_uuid BLOB NOT NULL,
                entry_key   TEXT NOT NULL,
                title       TEXT,
                added       INT  NOT NULL,
                CONSTRAINT freeze_entry_pk
                    PRIMARY KEY (player_uuid, entry_key) ON CONFLICT REPLACE
            )
            """)
    void createIfNotExists();

    @Select
    @Sql("""
            SELECT /*%expand*/* FROM freeze_entry
            """)
    List<FreezeEntry> get();

    @Select
    @Sql("""
            SELECT /*%expand*/*
              FROM freeze_entry
             WHERE player_uuid = /* playerUuid */0
            """)
    List<FreezeEntry> get(UUID playerUuid);

    @Select
    @Sql("""
            SELECT DISTINCT player_uuid FROM freeze_entry
            """)
    List<UUID> getUuids();

    @Select
    @Sql("""
            SELECT DISTINCT player_uuid
              FROM freeze_entry
             WHERE entry_key = /* entryKey */0
            """)
    List<UUID> getUuids(Key entryKey);

    @Select
    @Sql("""
            SELECT entry_key
              FROM freeze_entry
             WHERE player_uuid = /* playerUuid */0
             ORDER BY added DESC
            """)
    List<Key> getKeys(UUID playerUuid);


    @Select
    @Sql("""
            SELECT title
              FROM freeze_entry
             WHERE player_uuid = /* playerUuid */0
             ORDER BY added DESC
             LIMIT 1
            """)
    Optional<Component> getLastTitle(UUID playerUuid);

    @Select
    @Sql("""
            SELECT count(*)
              FROM FreezeEntry
            """)
    int count();

    @Select
    @Sql("""
            SELECT count(*)
              FROM freeze_entry
             WHERE player_uuid = /* playerUuid */0
            """)
    boolean exists(UUID playerUuid);

    @Delete
    @Sql("""
            DELETE FROM freeze_entry
             WHERE player_uuid = /* uuid */0
               AND entry_key = /* entryKey */1
            """)
    int delete(UUID uuid, Key entryKey);

    @Delete
    @Sql("""
            DELETE FROM freeze_entry
             WHERE entry_key = /* entryKey */0
            """)
    int deleteAll(Key entryKey);

    @Delete
    @Sql("""
            DELETE FROM freeze_entry
             WHERE player_uuid = /* playerUuid */0
            """)
    int deleteAll(UUID playerUuid);

    @Insert
    int insert(FreezeEntry freezeEntry);

    @Update
    int update(FreezeEntry freezeEntry);

    @Delete
    int delete(FreezeEntry freezeEntry);
}
