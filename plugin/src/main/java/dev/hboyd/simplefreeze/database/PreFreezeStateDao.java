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

import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Sql;

import java.util.Optional;
import java.util.UUID;

@Dao
public interface PreFreezeStateDao {
    @Sql("""
            SELECT /*%expand*/*
              FROM pre_freeze_state
             WHERE uuid = /* uuid */0
            """)
    @Select
    Optional<PreFreezeState> get(UUID uuid);

    @Select
    @Sql("""
            SELECT count(*)
              FROM pre_freeze_state
            """)
    int count();

    @Select
    @Sql("""
            SELECT count(*)
              FROM pre_freeze_state
             WHERE uuid = /* uuid */0
            """)
    boolean exists(UUID uuid);

    @Insert
    int insert(PreFreezeState preFreezeState);

    @Delete
    @Sql("""
            DELETE FROM pre_freeze_state
             WHERE uuid = /* uuid */0
            """)
    int delete(UUID uuid);
}
