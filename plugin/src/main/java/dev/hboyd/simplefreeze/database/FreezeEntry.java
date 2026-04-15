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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.seasar.doma.*;
import org.seasar.doma.jdbc.entity.NamingType;

import java.time.Instant;
import java.util.UUID;

@Entity(naming = NamingType.SNAKE_LOWER_CASE)
@Table
public class FreezeEntry {
    private @MonotonicNonNull UUID playerUuid;
    private @MonotonicNonNull Key entryKey;
    private @Nullable Component title;
    private @MonotonicNonNull Instant added;

    @ApiStatus.Internal
    FreezeEntry() {}

    public FreezeEntry(UUID playerUuid, Key entryKey, @Nullable Component title) {
        this.playerUuid = playerUuid;
        this.entryKey = entryKey;
        this.title = title;
        this.added = Instant.now();
    }

    public UUID playerUuid() {
        return this.playerUuid;
    }

    public void playerUuid(UUID uuid) {
        this.playerUuid = uuid;
    }

    public Key entryKey() {
        return this.entryKey;
    }

    public void entryKey(Key key) {
        this.entryKey = key;
    }

    public @Nullable Component title() {
        return this.title;
    }

    public void title(@Nullable Component title) {
        this.title = title;
    }

    public Instant added() {
        return this.added;
    }
}