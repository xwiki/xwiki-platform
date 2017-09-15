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

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Provides values for Database List properties.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("DBList")
@Singleton
public class DBListClassPropertyValuesProvider implements ClassPropertyValuesProvider
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private QueryBuilder<DBListClass> allowedValuesQueryBuilder;

    @Inject
    @Named("usedValues")
    private QueryBuilder<ListClass> usedValuesQueryBuilder;

    @Inject
    @Named("text")
    private QueryFilter textFilter;

    @Inject
    private AuthorExecutor authorExecutor;

    @Override
    public PropertyValues getValues(ClassPropertyReference propertyReference, int limit, Object... filterParameters)
        throws XWikiRestException
    {
        DBListClass dbListClass = getPropertyDefinition(propertyReference);

        String filter = "";
        if (filterParameters.length > 0 && filterParameters[0] != null) {
            filter = filterParameters[0].toString();
        }

        try {
            if (filter.isEmpty() || limit <= 0) {
                return getAllowedValues(dbListClass, limit, filter);
            } else {
                return getMixedValues(dbListClass, limit, filter);
            }
        } catch (Exception e) {
            throw new XWikiRestException(e);
        }
    }

    private DBListClass getPropertyDefinition(ClassPropertyReference propertyReference) throws XWikiRestException
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            PropertyInterface property = xcontext.getWiki().getDocument(propertyReference, xcontext).getXClass()
                .get(propertyReference.getName());
            if (property == null) {
                throw new XWikiRestException(String.format("Property [%s] not found.",
                    this.entityReferenceSerializer.serialize(propertyReference)));
            } else if (property instanceof DBListClass) {
                return (DBListClass) property;
            } else {
                throw new XWikiRestException(String.format("This [%s] is not a Database List property.",
                    this.entityReferenceSerializer.serialize(propertyReference)));
            }
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    private PropertyValues getValues(Query query, int limit, String filter) throws QueryException
    {
        if (limit > 0) {
            query.setLimit(limit);
        }
        if (!StringUtils.isEmpty(filter)) {
            query.addFilter(this.textFilter);
            query.bindValue("text").anyChars().literal(filter).anyChars();
        }
        return getValuesFromQueryResults(query.execute());
    }

    private PropertyValues getAllowedValues(DBListClass dbListClass, int limit, String filter) throws Exception
    {
        // Execute the query with the rights of the class last author because the query may not be safe.
        return this.authorExecutor.call(() -> {
            return getValues(this.allowedValuesQueryBuilder.build(dbListClass), limit, filter);
        }, dbListClass.getOwnerDocument().getAuthorReference());
    }

    private PropertyValues getValuesFromQueryResults(List<Object> results)
    {
        PropertyValues values = new PropertyValues();
        for (Object result : results) {
            // Oracle databases treat NULL and empty strings similarly. Thus the list passed as parameter can have some
            // elements being NULL (for XWiki string properties which were empty strings). This means we need to check
            // for NULL and ignore NULL entries from the list.
            if (result instanceof String) {
                PropertyValue value = new PropertyValue();
                value.setValue(result);
                values.getPropertyValues().add(value);
            } else if (result instanceof Object[]) {
                Object[] row = (Object[]) result;
                if (row.length > 0) {
                    PropertyValue value = new PropertyValue();
                    value.setValue(row[0]);
                    value.setMetaData(new HashMap<>());
                    values.getPropertyValues().add(value);
                    if (row.length > 1) {
                        value.getMetaData().put(getMetaDataType(row[1]), row[1]);
                    }
                }
            }
        }
        return values;
    }

    private String getMetaDataType(Object value)
    {
        return value instanceof Long ? "count" : "label";
    }

    private PropertyValues getUsedValues(ListClass listClass, int limit, String filter) throws QueryException
    {
        return getValues(this.usedValuesQueryBuilder.build(listClass), limit, filter);
    }

    private PropertyValues getMixedValues(DBListClass dbListClass, int limit, String filter) throws Exception
    {
        PropertyValues values = getAllowedValues(dbListClass, limit, filter);
        if (values.getPropertyValues().size() < limit) {
            // There may be used values that are not allowed. We try to include those as well.
            PropertyValues usedValues = getUsedValues(dbListClass, limit - values.getPropertyValues().size(), filter);
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
