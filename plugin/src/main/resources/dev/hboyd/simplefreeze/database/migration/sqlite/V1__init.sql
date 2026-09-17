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

/* Drop pre fly-way metadata table */
DROP TABLE IF EXISTS simple_freeze_metadata;

CREATE TABLE IF NOT EXISTS freeze_entry
(
    player_uuid BLOB NOT NULL,
    entry_key   TEXT NOT NULL,
    title       TEXT,
    added       INT  NOT NULL,
    CONSTRAINT freeze_entry_pk
        PRIMARY KEY (player_uuid, entry_key) ON CONFLICT REPLACE
);

CREATE TABLE IF NOT EXISTS pre_freeze_state
(
    uuid                           BLOB    NOT NULL
        CONSTRAINT pre_freeze_state_pk
            PRIMARY KEY ON CONFLICT REPLACE,
    velocity                       TEXT    NOT NULL,
    fire_ticks                     INT     NOT NULL,
    freeze_ticks                   INT     NOT NULL,
    freeze_ticking_locked          TINYINT NOT NULL,
    fall_distance                  FLOAT   NOT NULL,
    silent                         INT     NOT NULL,
    no_physics                     TINYINT NOT NULL,
    has_gravity                    TINYINT NOT NULL,
    potion_effects_json            TEXT,
    no_damage_ticks                INT,
    next_arrow_removal             INT,
    next_bee_stinger_removal       INT,
    sleeping_ignored               TINYINT,
    warden_warning_level           INT,
    warden_warning_cooldown        INT,
    warden_time_since_last_warning INT
);
