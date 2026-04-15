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
import com.google.gson.JsonElement;
import org.seasar.doma.jdbc.domain.DomainConverter;

public interface JsonDomainConverter<T> extends DomainConverter<T, String> {

    @Override
    default String fromDomainToValue(T object) {
        return toJson(object).toString();
    }

    JsonElement toJson(T value);

    @Override
    default T fromValueToDomain(String value) {
        return fromJson(new Gson().fromJson(value, JsonElement.class));
    }

    T fromJson(JsonElement element);
}
