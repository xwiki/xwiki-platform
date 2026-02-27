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
package org.xwiki.rest.internal.resources.classes;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.PropertyInterface;

/**
 * Base class for {@link ClassPropertyValuesProvider} implementations.
 *
 * @param <T> the property type
 * @version $Id$
 * @since 9.8
 */
public abstract class AbstractClassPropertyValuesProvider<T> implements ClassPropertyValuesProvider
{
    protected static final String META_DATA_LABEL = "label";

    protected static final String META_DATA_COUNT = "count";

    protected static final String META_DATA_ICON = "icon";

    protected static final String META_DATA_HINT = "hint";

    protected static final String META_DATA_URL = "url";

    protected static final String TEXT_FILTER = "text";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("text")
    private QueryFilter textFilter;

    @Override
    public PropertyValues getValues(ClassPropertyReference propertyReference, int limit, Object... filterParameters)
        throws XWikiRestException
    {
        T propertyDefinition = getPropertyDefinition(propertyReference);

        String filter = "";
        if (filterParameters.length > 0 && filterParameters[0] != null) {
            filter = filterParameters[0].toString();
        }

        try {
            if (limit <= 0) {
                return getAllowedValues(propertyDefinition, limit, filter);
            } else {
                return getMixedValues(propertyDefinition, limit, filter);
            }
        } catch (Exception e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public PropertyValue getValue(ClassPropertyReference propertyReference, Object rawValue)
        throws XWikiRestException
    {
        return this.getValueFromQueryResult(rawValue, getPropertyDefinition(propertyReference));
    }

    @SuppressWarnings("unchecked")
    protected T getPropertyDefinition(ClassPropertyReference propertyReference) throws XWikiRestException
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            PropertyInterface property = xcontext.getWiki().getDocument(propertyReference, xcontext).getXClass()
                .get(propertyReference.getName());
            if (property == null) {
                throw new XWikiRestException(String.format("Property [%s] not found.",
                    this.entityReferenceSerializer.serialize(propertyReference)));
            } else if (getPropertyType().isInstance(property)) {
                return (T) property;
            } else {
                throw new XWikiRestException(String.format("This [%s] is not a [%s] property.",
                    this.entityReferenceSerializer.serialize(propertyReference), getPropertyType().getSimpleName()));
            }
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    protected abstract Class<T> getPropertyType();

    protected PropertyValues getValues(Query query, int limit, String filter, T propertyDefinition)
        throws QueryException
    {
        if (limit > 0) {
            query.setLimit(limit);
        }
        if (!StringUtils.isEmpty(filter)) {
            query.addFilter(this.textFilter);
            query.bindValue(TEXT_FILTER).anyChars().literal(filter).anyChars();
        }
        return getValuesFromQueryResults(query.execute(), propertyDefinition);
    }

    /**
     * Execute the given query and create a {@see PropertyValue} with the first result (null if no results).
     *
     * @param query the query to execute
     * @param filter the text filter
     * @param propertyDefinition the property definition
     * @return value of {@see getValueFromQueryResult} with the first query result or null if no results.
     * @throws QueryException if an error occured during the query execution
     */
    protected PropertyValue getValue(Query query, String filter, T propertyDefinition) throws QueryException
    {
        PropertyValue propertyValue = null;

        if (!StringUtils.isEmpty(filter)) {
            query.addFilter(this.textFilter);
            query.bindValue(TEXT_FILTER).literal(filter);
        }

        List<T> result = query.execute();
        if (!result.isEmpty()) {
            propertyValue = getValueFromQueryResult(result.get(0), propertyDefinition);
        }

        return propertyValue;
    }

    protected abstract PropertyValues getAllowedValues(T propertyDefinition, int limit, String filter) throws Exception;

    protected PropertyValues getValuesFromQueryResults(List<Object> results, T propertyDefinition)
    {
        PropertyValues values = new PropertyValues();
        for (Object result : results) {
            PropertyValue value = getValueFromQueryResult(result, propertyDefinition);
            if (value != null) {
                values.getPropertyValues().add(value);
            }
        }
        return values;
    }

    protected PropertyValue getValueFromQueryResult(Object result, T propertyDefinition)
    {
        PropertyValue value = null;

        // Oracle databases treat NULL and empty strings similarly. Thus the list passed as parameter can have some
        // elements being NULL (for XWiki string properties which were empty strings). This means we need to check
        // for NULL and ignore NULL entries from the list.
        if (result instanceof Object[]) {
            Object[] row = (Object[]) result;
            if (row.length > 0 && row[0] != null) {
                value = new PropertyValue(row[0]);
                if (row.length > 1 && row[1] != null) {
                    value.getMetaData().put(getMetaDataType(row[1]), row[1]);
                }
            }
        } else if (result != null) {
            value = new PropertyValue(result);
        }

        return value;
    }

    protected String getMetaDataType(Object value)
    {
        return value instanceof Long ? META_DATA_COUNT : META_DATA_LABEL;
    }

    protected abstract PropertyValues getUsedValues(T propertyDefinition, int limit, String filter)
        throws QueryException;

    private PropertyValues getMixedValues(T propertyDefinition, int limit, String filter) throws Exception
    {
        PropertyValues values = getAllowedValues(propertyDefinition, limit, filter);
        if (values.getPropertyValues().size() < limit) {
            // There may be used values that are not allowed. We try to include those as well.
            PropertyValues usedValues =
                getUsedValues(propertyDefinition, limit - values.getPropertyValues().size(), filter);
            for (PropertyValue usedValue : usedValues.getPropertyValues()) {
                if (values.getPropertyValues().size() >= limit) {
                    break;
                }
                maybeAddUsedValue(values, usedValue);
            }
        }
        return values;
    }

    private void maybeAddUsedValue(PropertyValues values, PropertyValue usedValue)
    {
        PropertyValue value = findValue(values, usedValue.getValue());
        if (value == null) {
            values.getPropertyValues().add(usedValue);
        } else {
            mergeMetaData(value, usedValue);
        }
    }

    private PropertyValue findValue(PropertyValues values, Object data)
    {
        for (PropertyValue value : values.getPropertyValues()) {
            if (Objects.equals(value.getValue(), data)) {
                return value;
            }
        }
        return null;
    }

    private void mergeMetaData(PropertyValue alice, PropertyValue bob)
    {
        bob.getMetaData().forEach((key, value) -> alice.getMetaData().putIfAbsent(key, value));
    }
}
