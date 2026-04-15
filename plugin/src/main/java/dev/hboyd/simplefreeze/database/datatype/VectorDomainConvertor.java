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

package dev.hboyd.simplefreeze.database.datatype;

import org.bukkit.util.Vector;
import org.seasar.doma.ExternalDomain;
import org.seasar.doma.jdbc.domain.DomainConverter;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

@ExternalDomain
public class VectorDomainConvertor implements DomainConverter<Vector, byte[]> {

    @Override
    public byte[] fromDomainToValue(Vector vector) {
        return ByteBuffer.allocate(24)
                .putDouble(vector.getX())
                .putDouble(vector.getY())
                .putDouble(vector.getZ())
                .array();
    }

    @Override
    public Vector fromValueToDomain(byte[] value) {
        DoubleBuffer doubleBuffer = ByteBuffer.wrap(value).asDoubleBuffer();
        return new Vector(doubleBuffer.get(0),
                doubleBuffer.get(1),
                doubleBuffer.get(2));
    }
}