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

package dev.hboyd.simplefreeze.util;

import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import org.jspecify.annotations.NonNull;

public class PacketEventsUtil {
    private PacketEventsUtil() {}

    public static @NonNull Vector3d toVector3d(org.bukkit.Location location) {
        return new Vector3d(location.x(), location.y(), location.z());
    }

    public static @NonNull Location toPacketEventsLocation(org.bukkit.Location location) {
        return new Location(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
    }
}
