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

import org.seasar.doma.*;

@Dao
public interface SimpleFreezeMetadataDao {
    @Script
    @Sql("""
            CREATE TABLE IF NOT EXISTS simple_freeze_metadata
            (
                entry_key TEXT NOT NULL
                    CONSTRAINT simple_freeze_metadata_pk
                        PRIMARY KEY
                            ON CONFLICT replace,
                entry     TEXT
            );
            """)
    void createIfNotExists();

    @Select
    @Sql("""
            SELECT *
              FROM simple_freeze_metadata
             WHERE entry_key == pre_freeze_state_schema_version
            """)
    String getPreFreezeStateSchemaVersion();

    @Insert
    @Sql("""
            REPLACE INTO simple_freeze_metadata (entry_key, entry)
            VALUES (pre_freeze_state_schema_version, /*version*/0)
            """)
    int setPreFreezeStateSchemaVersion(String version);

    @Select
    @Sql("""
            SELECT *
              FROM simple_freeze_metadata
             WHERE entry_key == freeze_entry_schema_version
            """)
    String getFreezeEntrySchemaVersion();


    @Insert
    @Sql("""
            REPLACE INTO simple_freeze_metadata (entry_key, entry)
            VALUES (freeze_entry_schema_version, /*version*/0)
            """)
    int setFreezeEntrySchemaVersion(String version);
}
