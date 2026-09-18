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

package dev.hboyd.simplefreeze.config;

import dev.hboyd.prismatic.paper.configurate.PaperConfig;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
public class SimpleFreezeConfig extends PaperConfig {
    private static final int CONFIG_VERSION = 2;
    private static final ConfigurationTransformation CONFIG_MIGRATION = ConfigurationTransformation.versionedBuilder()
            .addVersion(CONFIG_VERSION, ConfigurationTransformation.builder()
                    .addAction(NodePath.of(List.of("database-config", "jbdc-uri")), TransformAction.rename("jdbc-uri"))
                    .build())
            .build();

    @Required
    private final DatabaseConfig databaseConfig = new DatabaseConfig(DatabaseConfig.DatabaseType.SQLITE, null);

    @Required
    @Comment("Forces all players to dismount when the controlling player disconnects saving the ridden entity with the disconnecting player")
    private final boolean alwaysDisconnectWithEntity = true;

    public SimpleFreezeConfig(final Path configFilePath) throws IOException {
        super(configFilePath, CONFIG_VERSION, null, null, CONFIG_MIGRATION);
        this.initialize();
    }

    public DatabaseConfig databaseConfig() {
        return this.databaseConfig;
    }

    public boolean alwaysDisconnectWithEntity() {
        return this.alwaysDisconnectWithEntity;
    }
}
