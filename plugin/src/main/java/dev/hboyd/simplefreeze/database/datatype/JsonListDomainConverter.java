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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.seasar.doma.jdbc.domain.DomainConverter;

import java.util.Collection;
import java.util.List;

public class JsonListDomainConverter<A, T extends DomainConverter<A, String>> {
    private final T converter;

    public JsonListDomainConverter(T converter) {
        this.converter = converter;
    }

    public JsonElement toJson(Collection<A> complex) {
        JsonArray jsonArray = new JsonArray();
        complex.forEach(item -> jsonArray.add(new Gson().fromJson(this.converter.fromDomainToValue(item), JsonElement.class)));
        return jsonArray;
    }

    public Collection<A> fromJson(JsonElement element) {
        if (element == null) return List.of();
        JsonArray jsonArray = element.getAsJsonArray();
        return jsonArray.asList().stream()
                .map(JsonElement::toString)
                .map(this.converter::fromValueToDomain)
                .toList();
    }
}