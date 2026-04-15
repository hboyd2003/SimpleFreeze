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

import dev.hboyd.configurateNBT.NBTCompression;
import dev.hboyd.configurateNBT.NBTConfigurationLoader;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.NamingSchemes;

import java.io.IOException;

public class PersistentDataTypeTypeSerializerAdapter<C> implements PersistentDataType<PersistentDataContainer, C> {

    private final Class<C> type;
    private final NBTConfigurationLoader nbtLoader;
    private final ObjectMapper<C> objectMapper;

    public PersistentDataTypeTypeSerializerAdapter(Class<C> type) {
        this(type, null);
    }

    public PersistentDataTypeTypeSerializerAdapter(Class<C> type, @Nullable TypeSerializerCollection serializers) {
        this.type = type;
        this.nbtLoader = NBTConfigurationLoader.builder()
                .defaultOptions(options -> {
                    if (serializers != null)
                        return options.serializers(options.serializers().childBuilder()
                                .registerAll(serializers)
                                .build());
                    else return options;
                })
                .compression(NBTCompression.NONE)
                .build();

        try {
            this.objectMapper = ObjectMapper.factoryBuilder()
                    .defaultNamingScheme(NamingSchemes.CAMEL_CASE)
                    .build()
                    .get(type);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public Class<C> getComplexType() {
        return this.type;
    }

    @Override
    public PersistentDataContainer toPrimitive(C complex, PersistentDataAdapterContext context) {
        PersistentDataContainer pdc = context.newPersistentDataContainer();

        try {
            BasicConfigurationNode node = this.nbtLoader.createNode();
            this.objectMapper.save(complex, node);
            pdc.readFromBytes(this.nbtLoader.saveToBytes(node));
        } catch (SerializationException e) {
            throw new RuntimeException(e); // TODO: Correct exception
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return pdc;
    }

    @Override
    public C fromPrimitive(PersistentDataContainer pdc, PersistentDataAdapterContext context) {
        try {
            BasicConfigurationNode node = this.nbtLoader.loadFromBytes(pdc.serializeToBytes());
            return this.objectMapper.load(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
