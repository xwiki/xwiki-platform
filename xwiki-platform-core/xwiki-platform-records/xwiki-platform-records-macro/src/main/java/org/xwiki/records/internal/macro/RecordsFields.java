/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.records.internal.macro;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Tells what the Records table can do with the fields of a data type, the way the {@code liveTable} source and Live
 * Data see them.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component(roles = RecordsFields.class)
@Singleton
public class RecordsFields
{
    private static final String FIELDS_UNREADABLE = "error.fieldsUnreadable";

    /**
     * The filter the {@code liveTable} source matches as a number, which it cannot do with a value that is not one.
     */
    private static final String NUMBER_FILTER = "number";

    /**
     * The filter the {@code liveTable} source matches against its two parameters below.
     */
    private static final String BOOLEAN_FILTER = "boolean";

    private static final List<String> BOOLEAN_VALUES = List.of("trueValue", "falseValue");

    /**
     * Tells whether the chosen data type still exists.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Reads the fields of the chosen data type.
     */
    @Inject
    private LiveDataSourceManager liveDataSourceManager;

    /**
     * The defaults of the {@code liveTable} source, which say whether a field can be sorted and filtered, and with
     * which filter, whenever the field's own descriptor leaves that to its type.
     */
    @Inject
    @Named(RecordsMacro.SOURCE)
    private Provider<LiveDataConfiguration> sourceDefaults;

    @Inject
    private RecordsMessages messages;

    /**
     * @param dataType the reference of the data type
     * @param serializedDataType the same reference, serialized, to name the data type in the error message
     * @return whether the data type exists
     * @throws MacroExecutionException when its existence cannot be checked
     */
    public boolean exists(DocumentReference dataType, String serializedDataType) throws MacroExecutionException
    {
        try {
            return this.documentAccessBridge.exists(dataType);
        } catch (Exception e) {
            throw new MacroExecutionException(this.messages.translate(FIELDS_UNREADABLE, serializedDataType), e);
        }
    }

    /**
     * Reads the fields the data type's source offers: the page metadata, the data type's fields and the Live Data
     * pseudo-columns, in the order the source reports them.
     *
     * @param dataType the serialized reference of the data type
     * @return the fields, by identifier
     * @throws MacroExecutionException when the source cannot be reached or its properties cannot be read
     */
    public Map<String, RecordField> get(String dataType) throws MacroExecutionException
    {
        Source source = new Source(RecordsMacro.SOURCE);
        source.setParameter(RecordsMacro.CLASS_NAME_PARAMETER, dataType);
        Collection<LiveDataPropertyDescriptor> descriptors;
        try {
            descriptors = this.liveDataSourceManager.get(source)
                .orElseThrow(() -> new MacroExecutionException(this.messages.translate(RecordsMessages.RENDER_FAILED)))
                .getProperties().get();
        } catch (LiveDataException e) {
            throw new MacroExecutionException(this.messages.translate(FIELDS_UNREADABLE, dataType), e);
        }
        Map<String, LiveDataPropertyDescriptor> types = new HashMap<>();
        this.sourceDefaults.get().getMeta().getPropertyTypes().forEach(type -> types.putIfAbsent(type.getId(), type));
        Map<String, RecordField> fields = new LinkedHashMap<>();
        for (LiveDataPropertyDescriptor descriptor : descriptors) {
            if (descriptor.getId() != null) {
                fields.putIfAbsent(descriptor.getId(), getField(descriptor, types.get(descriptor.getType())));
            }
        }
        return fields;
    }

    /**
     * Resolves what the table can do with a field the way Live Data does: from the field's own descriptor, and from the
     * defaults of its type for whatever the descriptor leaves unset. A field of a type with no defaults can be neither
     * sorted nor filtered on.
     *
     * @param descriptor the descriptor of the field
     * @param type the defaults of the field's type, {@code null} when there are none
     * @return what the table can do with the field
     */
    private RecordField getField(LiveDataPropertyDescriptor descriptor, LiveDataPropertyDescriptor type)
    {
        boolean sortable = isEnabled(descriptor.isSortable(), type == null ? null : type.isSortable());
        boolean filterable = isEnabled(descriptor.isFilterable(), type == null ? null : type.isFilterable());
        FilterDescriptor filter = descriptor.getFilter() != null || type == null ? descriptor.getFilter()
            : type.getFilter();
        return new RecordField(sortable, filterable, getAcceptedValues(filter));
    }

    private static boolean isEnabled(Boolean own, Boolean typeDefault)
    {
        return own != null ? own : Boolean.TRUE.equals(typeDefault);
    }

    /**
     * Tells which values the {@code liveTable} source can match a field with. It matches a number filter by parsing the
     * value, and a boolean one by comparing it with the filter's own true and false values, so anything else matches no
     * entry. An empty value is always accepted, since it filters nothing.
     *
     * @param filter the filter of the field, {@code null} when it has none
     * @return whether a decoded filter value fits the field
     */
    private static Predicate<String> getAcceptedValues(FilterDescriptor filter)
    {
        String filterId = filter == null ? null : filter.getId();
        if (NUMBER_FILTER.equals(filterId)) {
            return value -> value.isEmpty() || NumberUtils.isCreatable(value);
        } else if (BOOLEAN_FILTER.equals(filterId)) {
            List<String> accepted = BOOLEAN_VALUES.stream().map(filter.getParameters()::get).filter(Objects::nonNull)
                .map(String::valueOf).toList();
            return value -> value.isEmpty() || accepted.contains(value);
        }
        return value -> true;
    }
}
