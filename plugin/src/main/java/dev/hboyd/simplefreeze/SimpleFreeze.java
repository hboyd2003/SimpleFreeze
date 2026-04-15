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

import com.mysql.cj.jdbc.MysqlDataSource;
import dev.hboyd.simplefreeze.command.FreezeCommands;
import dev.hboyd.simplefreeze.command.SimpleFreezeCommand;
import dev.hboyd.simplefreeze.config.SimpleFreezeConfig;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.mariadb.jdbc.MariaDbDataSource;
import org.seasar.doma.jdbc.SimpleConfig;
import org.seasar.doma.jdbc.dialect.Dialect;
import org.seasar.doma.jdbc.dialect.MysqlDialect;
import org.seasar.doma.jdbc.dialect.SqliteDialect;
import org.seasar.doma.slf4j.Slf4jJdbcLogger;
import org.slf4j.Logger;
import org.sqlite.JDBC;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public final class SimpleFreeze extends JavaPlugin implements ISimpleFreeze {
    public static final ComponentLogger LOGGER = ComponentLogger.logger(ID);
    public static final SimpleFreeze INSTANCE = new SimpleFreeze();

    private @MonotonicNonNull FreezeManager freezeManager;
    private @MonotonicNonNull FreezeCommands freezeCommands;
    private @MonotonicNonNull SimpleFreezeCommand simpleFreezeCommand;
    private @MonotonicNonNull SimpleFreezeConfig simpleFreezeConfig;

    private SimpleFreeze() {
    }

    @Override
    public void onEnable() {
        LOGGER.info("Simple Freeze {} - Copyright (C) 2026 Harrison Boyd - Licensed under LGPLv3", this.getPluginMeta().getVersion());

        if (!Bukkit.getAllowFlight())
            LOGGER.error(Component.text("Flight is not allowed! Players frozen in air may get kicked! Change the \"allow-flight\" setting in server.properties to fix this."));

        try {
            this.simpleFreezeConfig = new SimpleFreezeConfig(this.getDataPath().resolve("config.conf"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file", e);
        }

        this.freezeManager = new FreezeManager(buildDomaConfiguration(), this.simpleFreezeConfig.alwaysDisconnectWithEntity());
        Bukkit.getPluginManager().registerEvents(this.freezeManager, this);

        // Commands
        this.freezeCommands = new FreezeCommands(this.freezeManager);
        this.simpleFreezeCommand = new SimpleFreezeCommand(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
        {
            this.freezeCommands.register(event.registrar());
            this.simpleFreezeCommand.register(event.registrar());
        });

        // Translations
        MiniMessageTranslationStore translationStore = MiniMessageTranslationStore.create(Key.key(this, "lang"));

        ResourceBundle bundle = ResourceBundle.getBundle("dev.hboyd.simplefreeze.lang", Locale.US);
        translationStore.registerAll(Locale.US, bundle, false);

        GlobalTranslator.translator().addSource(translationStore);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this.freezeManager);
    }

    @Override
    public ComponentLogger getComponentLogger() {
        return LOGGER;
    }

    @Override
    public Logger getSLF4JLogger() {
        return LOGGER;
    }

    @Override
    public IFreezeManager freezeManager() {
        return this.freezeManager;
    }

    @Override
    public HoverEvent<Component> asHoverEvent(UnaryOperator<Component> op) {
        return HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, op.apply(Component.text("v" + this.getPluginMeta().getVersion())));
    }

    private SimpleConfig buildDomaConfiguration() {
        DataSource dataSource;
        try {
            dataSource = switch (this.simpleFreezeConfig.databaseConfig().databaseType()) {
                case MYSQL -> new MysqlDataSource();
                case SQLITE -> {
                    SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
                    sqLiteDataSource.setUrl(Optional.ofNullable(this.simpleFreezeConfig.databaseConfig().jdbcURI())
                            .map(URI::toString)
                            .orElse(JDBC.PREFIX + getDataPath().resolve("database.sqlite")));
                    yield sqLiteDataSource;
                }
                case MARIADB ->
                        new MariaDbDataSource(Optional.ofNullable(this.simpleFreezeConfig.databaseConfig().jdbcURI()).orElseThrow().toString());
            };
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }

        Dialect dialect = switch (this.simpleFreezeConfig.databaseConfig().databaseType()) {
            case MYSQL, MARIADB -> new MysqlDialect();
            case SQLITE -> new SqliteDialect();
        };

        return SimpleConfig.builder(dataSource, dialect).jdbcLogger(new Slf4jJdbcLogger()).build();
    }

}
