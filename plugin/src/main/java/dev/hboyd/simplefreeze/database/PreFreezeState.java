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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.hboyd.simplefreeze.database.datatype.JsonListDomainConverter;
import dev.hboyd.simplefreeze.database.datatype.PotionEffectDomainConverter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.seasar.doma.Id;
import org.seasar.doma.jdbc.entity.NamingType;

import java.util.Collection;
import java.util.UUID;

@org.seasar.doma.Entity(naming = NamingType.SNAKE_LOWER_CASE)
public final class PreFreezeState {
    private static final JsonListDomainConverter<PotionEffect, PotionEffectDomainConverter> POTION_EFFECT_LIST_CONVERTER
            = new JsonListDomainConverter<>(new PotionEffectDomainConverter());

    @Id
    public UUID uuid;

    // Entities
    private Vector velocity;
    private int fireTicks;
    private float fallDistance;
    private int freezeTicks;
    private boolean freezeTickingLocked;
    private boolean silent;
    private boolean noPhysics;
    private boolean hasGravity;

    // Living entities
    private JsonElement potionEffectsJson;
    private int noDamageTicks;
    private int nextArrowRemoval;
    private int nextBeeStingerRemoval;

    // Players
    private boolean sleepingIgnored;
    private int wardenWarningLevel;
    private int wardenWarningCooldown;
    private int wardenTimeSinceLastWarning;

    PreFreezeState() {}

    public void setWith(Entity entity) {
        this.uuid = entity.getUniqueId();
        this.velocity = entity.getVelocity();
        this.fireTicks = entity.getFireTicks();
        this.fallDistance = entity.getFallDistance();
        this.freezeTickingLocked =  entity.isFreezeTickingLocked();
        this.freezeTicks = entity.getFreezeTicks();
        this.silent = entity.isSilent();
        this.noPhysics = entity.hasNoPhysics();
        this.hasGravity = entity.hasGravity();

        if (entity instanceof LivingEntity livingEntity) {
            this.potionEffectsJson = POTION_EFFECT_LIST_CONVERTER.toJson(livingEntity.getActivePotionEffects());
            this.noDamageTicks = livingEntity.getNoDamageTicks();
            this.nextArrowRemoval = livingEntity.getNextArrowRemoval();
            this.nextBeeStingerRemoval = livingEntity.getNextBeeStingerRemoval();

            if (livingEntity instanceof Player player) {
                this.sleepingIgnored = player.isSleepingIgnored();
                this.wardenWarningLevel = player.getWardenWarningLevel();
                this.wardenWarningCooldown = player.getWardenWarningCooldown();
                this.wardenTimeSinceLastWarning = player.getWardenTimeSinceLastWarning();
            }
        }
    }

    public void restoreTo(Entity entity) {
        entity.setVelocity(this.velocity);
        entity.setFireTicks(this.fireTicks);
        entity.setFallDistance(this.fallDistance);
        entity.lockFreezeTicks(this.freezeTickingLocked);
        entity.setFreezeTicks(this.freezeTicks);
        entity.setSilent(this.silent);
        entity.setNoPhysics(this.noPhysics);
        entity.setGravity(this.hasGravity);

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.clearActivePotionEffects();
            livingEntity.addPotionEffects(POTION_EFFECT_LIST_CONVERTER.fromJson(this.potionEffectsJson));
            livingEntity.setNoDamageTicks(this.noDamageTicks);
            livingEntity.setNextArrowRemoval(this.nextArrowRemoval);
            livingEntity.setNextBeeStingerRemoval(this.nextBeeStingerRemoval);

            if (livingEntity instanceof Player player) {
                player.setSleepingIgnored(this.sleepingIgnored);
                player.setWardenWarningLevel(this.wardenWarningLevel);
                player.setWardenWarningCooldown(this.wardenWarningCooldown);
                player.setWardenTimeSinceLastWarning(this.wardenTimeSinceLastWarning);
            }
        }
    }

    public static PreFreezeState of(Entity entity) {
        PreFreezeState preFreezeState = new PreFreezeState();
        preFreezeState.setWith(entity);
        return preFreezeState;
    }

    public static PreFreezeState defaultOf(Entity entity) {
        PreFreezeState preFreezeState = new PreFreezeState();

        preFreezeState.uuid = entity.getUniqueId();
        preFreezeState.velocity = new Vector();
        preFreezeState.fireTicks = 0;
        preFreezeState.fallDistance = 0;
        preFreezeState.freezeTicks = 0;
        preFreezeState.freezeTickingLocked = false;
        preFreezeState.silent = false;
        preFreezeState.noPhysics = false;
        preFreezeState.hasGravity = true;

        if (entity instanceof LivingEntity livingEntity) {
            preFreezeState.potionEffectsJson = new JsonArray();
            preFreezeState.noDamageTicks = 0;
            preFreezeState.nextArrowRemoval = 0;
            preFreezeState.nextBeeStingerRemoval = 0;

            if (livingEntity instanceof Player)  {
                preFreezeState.sleepingIgnored = false;
                preFreezeState.wardenWarningLevel = 0;
                preFreezeState.wardenWarningCooldown = 0;
                preFreezeState.wardenTimeSinceLastWarning = 0;
            }
        }

        return preFreezeState;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public Vector velocity() {
        return this.velocity;
    }

    public int fireTicks() {
        return this.fireTicks;
    }

    public float fallDistance() {
        return this.fallDistance;
    }

    public int freezeTicks() {
        return this.freezeTicks;
    }

    public boolean freezeTickingLocked() {
        return this.freezeTickingLocked;
    }

    public boolean silent() {
        return this.silent;
    }

    public boolean noPhysics() {
        return this.noPhysics;
    }

    public boolean hasGravity() {
        return this.hasGravity;
    }

    public Collection<PotionEffect> potionEffects() {
        return POTION_EFFECT_LIST_CONVERTER.fromJson(this.potionEffectsJson);
    }

    public int noDamageTicks() {
        return this.noDamageTicks;
    }

    public int nextArrowRemoval() {
        return this.nextArrowRemoval;
    }

    public int nextBeeStingerRemoval() {
        return this.nextBeeStingerRemoval;
    }

    public boolean sleepingIgnored() {
        return this.sleepingIgnored;
    }

    public int wardenWarningLevel() {
        return this.wardenWarningLevel;
    }

    public int wardenWarningCooldown() {
        return this.wardenWarningCooldown;
    }

    public int wardenTimeSinceLastWarning() {
        return this.wardenTimeSinceLastWarning;
    }
}
