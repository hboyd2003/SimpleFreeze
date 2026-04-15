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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.seasar.doma.ExternalDomain;

import java.util.Optional;

@ExternalDomain
public class PotionEffectDomainConverter implements JsonDomainConverter<PotionEffect> {
    private static final String TYPE_KEY = "effect";
    private static final String DURATION_KEY = "duration";
    private static final String AMPLIFIER_KEY = "amplifier";
    private static final String AMBIENT_KEY = "ambient";
    private static final String PARTICLES_KEY = "particles";
    private static final String ICON_KEY = "icon";
    private static final String HIDDEN_EFFECT_KEY = "hidden_effect";

    @Override
    public JsonElement toJson(PotionEffect potionEffect) {
        JsonObject potionEffectJson = new JsonObject();

        potionEffectJson.add(TYPE_KEY, new JsonPrimitive(potionEffect.getType().key().toString()));
        potionEffectJson.add(DURATION_KEY, new JsonPrimitive(potionEffect.getDuration()));
        potionEffectJson.add(AMPLIFIER_KEY, new JsonPrimitive(potionEffect.getAmplifier()));
        potionEffectJson.add(AMBIENT_KEY, new JsonPrimitive(potionEffect.isAmbient()));
        potionEffectJson.add(PARTICLES_KEY, new JsonPrimitive(potionEffect.hasParticles()));
        potionEffectJson.add(ICON_KEY, new JsonPrimitive(potionEffect.hasIcon()));

        if (potionEffect.getHiddenPotionEffect() != null)
            potionEffectJson.add(HIDDEN_EFFECT_KEY, toJson(potionEffect.getHiddenPotionEffect()));

        return potionEffectJson;
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public PotionEffect fromJson(JsonElement element) {
        if (element == null) return null;
        JsonObject jsonObject = element.getAsJsonObject();
        Key effectKey = Key.key(jsonObject.getAsJsonPrimitive(TYPE_KEY).getAsString());
        //noinspection UnstableApiUsage
        return new PotionEffect(Registry.MOB_EFFECT.get(effectKey),
                jsonObject.getAsJsonPrimitive(DURATION_KEY).getAsInt(),
                jsonObject.getAsJsonPrimitive(AMPLIFIER_KEY).getAsInt(),
                jsonObject.getAsJsonPrimitive(AMBIENT_KEY).getAsBoolean(),
                jsonObject.getAsJsonPrimitive(PARTICLES_KEY).getAsBoolean(),
                jsonObject.getAsJsonPrimitive(ICON_KEY).getAsBoolean(),
                Optional.ofNullable(jsonObject.getAsJsonObject(HIDDEN_EFFECT_KEY))
                        .map(this::fromJson)
                        .orElse(null)
        );
    }
}