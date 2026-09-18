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

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.Nullable;
import org.seasar.doma.jdbc.AbstractJdbcLogger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.Objects;
import java.util.function.Supplier;

public class DomaSlf4jDelegateLogger extends AbstractJdbcLogger<Level> {
    private static final Marker DOMA_MARKER = MarkerFactory.getMarker("Doma");

    private final ComponentLogger logger;

    public DomaSlf4jDelegateLogger(final ComponentLogger logger) {
        Objects.requireNonNull(logger, "logger");
        super(Level.DEBUG);
        this.logger = logger;
    }

    @Override
    protected void logDaoMethodEntering(final String callerClassName,
                                        final String callerMethodName,
                                        final Object[] args,
                                        final Level level,
                                        final Supplier<String> messageSupplier) {
        super.logDaoMethodEntering(callerClassName, callerMethodName, args, level, messageSupplier);
    }

    @Override
    protected void log(
            @Nullable Level level,
            final String callerClassName,
            final String callerMethodName,
            final Throwable throwable,
            final Supplier<String> messageSupplier) {
        if (level == null) level = this.defaultLevel;
        if (!this.logger.isEnabledForLevel(level)) return;

        final LoggingEventBuilder loggingEventBuilder = this.logger.atLevel(level != null ? level : this.defaultLevel)
                .addMarker(DOMA_MARKER)
                .setCause(throwable);

        if (callerClassName != null) {
            String markerName = callerClassName;
            if (callerMethodName != null) markerName += "#" + callerMethodName;

            loggingEventBuilder.addMarker(MarkerFactory.getMarker(markerName));
        }

        loggingEventBuilder.log(messageSupplier);
    }
}
