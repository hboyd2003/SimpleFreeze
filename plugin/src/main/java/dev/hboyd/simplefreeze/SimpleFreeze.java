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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.hboyd.simplefreeze.command.FreezeCommands;
import dev.hboyd.simplefreeze.command.SimpleFreezeCommand;
import dev.hboyd.simplefreeze.config.DatabaseConfig;
import dev.hboyd.simplefreeze.config.SimpleFreezeConfig;
import dev.hboyd.simplefreeze.util.DomaSlf4jDelegateLogger;
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
import org.flywaydb.core.Flyway;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.SimpleConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static dev.hboyd.simplefreeze.config.DatabaseConfig.DatabaseType.SQLITE;

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
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load configuration file", e);
        }

        final Config databaseConfig;
        try {
            databaseConfig = this.configureDatabase();
        } catch (final URISyntaxException e) {
            throw new RuntimeException("Failed to configure database", e);
        }

        this.freezeManager = new FreezeManager(databaseConfig, this.simpleFreezeConfig.alwaysDisconnectWithEntity());
        Bukkit.getPluginManager().registerEvents(this.freezeManager, this);

        // Commands
        this.freezeCommands = new FreezeCommands(this.freezeManager);
        this.simpleFreezeCommand = new SimpleFreezeCommand(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            this.freezeCommands.register(event.registrar());
            this.simpleFreezeCommand.register(event.registrar());
        });

        // Translations
        final MiniMessageTranslationStore translationStore = MiniMessageTranslationStore.create(Key.key(this, "lang"));

        final ResourceBundle bundle = ResourceBundle.getBundle("dev.hboyd.simplefreeze.lang", Locale.US);
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
    public HoverEvent<Component> asHoverEvent(final UnaryOperator<Component> op) {
        return HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, op.apply(Component.text("v" + this.getPluginMeta().getVersion())));
    }

    private SimpleConfig configureDatabase() throws URISyntaxException {
        URI jdbcUri = this.simpleFreezeConfig.databaseConfig().jdbcURI();
        final DatabaseConfig.DatabaseType databaseType = this.simpleFreezeConfig.databaseConfig().databaseType();

        if (jdbcUri == null) {
            if (databaseType != SQLITE)
                throw new IllegalStateException("jdbc-uri is null but is required for " + databaseType);
            jdbcUri = URI.create(org.sqlite.JDBC.PREFIX + this.getDataPath().resolve("database.sqlite").toAbsolutePath().toUri().getPath());
        }

        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("SimpleFreeze-" + databaseType);
        hikariConfig.setDriverClassName(databaseType.driverClass().getCanonicalName());
        hikariConfig.setJdbcUrl(jdbcUri.toString());
        if (databaseType == SQLITE) hikariConfig.setMaximumPoolSize(1);
        final HikariDataSource hikariDataSource = withContextClassLoader(() -> new HikariDataSource(hikariConfig));

        Flyway.configure(SimpleFreeze.class.getClassLoader())
                .dataSource(hikariDataSource)
                .locations("classpath:dev/hboyd/simplefreeze/database/migration/" + databaseType.dialect().getName())
                .baselineOnMigrate(true)
                .load()
                .migrate();

        return SimpleConfig.builder(hikariDataSource, databaseType.dialect())
                .jdbcLogger(new DomaSlf4jDelegateLogger(LOGGER))
                .build();
    }

    /**
     * Execute the given action with the SimpleFreeze class loader and return its result.
     *
     * @param action the action
     * @param <T> the action return type
     * @return the result
     */
    private static <T> T withContextClassLoader(final Supplier<T> action) {
        final Thread thread = Thread.currentThread();
        final ClassLoader previous = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(SimpleFreeze.class.getClassLoader());
            return action.get();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }
}
