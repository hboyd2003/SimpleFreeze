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

import dev.hboyd.prismatic.configurate.PaperConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
public class SimpleFreezeConfig extends PaperConfig {
    private static final String CONFIG_VERSION = "1";

    @Required
    private DatabaseConfig databaseConfig = new DatabaseConfig(DatabaseConfig.DatabaseType.SQLITE, null);

    @Required
    @Comment("Forces all players to dismount when the controlling player disconnects saving the rode entity with the disconnecting player")
    private boolean alwaysDisconnectWithEntity = true;

    @Required
    @Comment("DO NOT CHANGE THIS VALUE")
    private String configVersion = CONFIG_VERSION;

    public SimpleFreezeConfig(Path configFilePath) throws IOException {
        initialize(configFilePath);
    }

    public DatabaseConfig databaseConfig() {
        return this.databaseConfig;
    }

    public boolean alwaysDisconnectWithEntity() {
        return this.alwaysDisconnectWithEntity;
    }

    public String configVersion() {
        return this.configVersion;
    }
}
