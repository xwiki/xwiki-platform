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

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Base class for {@link ClassPropertyValuesProvider} implementations that work with list of users and groups
 * properties.
 * 
 * @param <T> the property type
 * @version $Id$
 * @since 9.8
 */
public abstract class AbstractUsersAndGroupsClassPropertyValuesProvider<T extends ListClass>
    extends AbstractListClassPropertyValuesProvider<T>
{
    @Inject
    protected Logger logger;

    @Inject
    protected WikiDescriptorManager wikiDescriptorManager;

    @Inject
    @Named("document")
    private QueryFilter documentFilter;

    @Inject
    @Named("viewable")
    private QueryFilter viewableFilter;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactSerializer;

    @Override
    protected PropertyValues getUsedValues(T propertyDefinition, int limit, String filter) throws QueryException
    {
        Query query = this.usedValuesQueryBuilder.build(propertyDefinition);
        // We know the used values are document references so we can check view access in a better way than what the
        // used values query builder does by default.
        query.getFilters().clear();
        query.addFilter(this.documentFilter);
        query.addFilter(this.viewableFilter);
        return getValues(query, limit, filter, propertyDefinition);
    }

    protected abstract QueryBuilder<T> getAllowedValuesQueryBuilder();

    protected PropertyValues getLocalAllowedValues(T propertyDefinition, int limit, String filter) throws Exception
    {
        return getValues(this.getAllowedValuesQueryBuilder().build(propertyDefinition), limit, filter,
            propertyDefinition);
    }

    protected PropertyValues getGlobalAllowedValues(T propertyDefinition, int limit, String filter) throws Exception
    {
        Query query = this.getAllowedValuesQueryBuilder().build(propertyDefinition);
        query.setWiki(this.wikiDescriptorManager.getMainWikiId());
        return getValues(query, limit, filter, propertyDefinition);
    }

    protected PropertyValues getLocalAndGlobalAllowedValues(T propertyDefinition, int limit, String filter)
        throws Exception
    {
        PropertyValues localUsers = getLocalAllowedValues(propertyDefinition, limit, filter);
        PropertyValues globalUsers = getGlobalAllowedValues(propertyDefinition, limit, filter);
        Iterator<PropertyValue> localUsersIterator = localUsers.getPropertyValues().iterator();
        Iterator<PropertyValue> globalUsersIterator = globalUsers.getPropertyValues().iterator();
        PropertyValues users = new PropertyValues();
        int oldSize;
        do {
            oldSize = users.getPropertyValues().size();
            if (localUsersIterator.hasNext() && (limit <= 0 || users.getPropertyValues().size() < limit)) {
                users.getPropertyValues().add(localUsersIterator.next());
            }
            if (globalUsersIterator.hasNext() && (limit <= 0 || users.getPropertyValues().size() < limit)) {
                users.getPropertyValues().add(globalUsersIterator.next());
            }
        } while (oldSize < users.getPropertyValues().size());

        users.getPropertyValues().sort((alice, bob) -> {
            String aliceName = alice.getMetaData().getOrDefault(META_DATA_LABEL, alice.getValue()).toString();
            String bobName = bob.getMetaData().getOrDefault(META_DATA_LABEL, alice.getValue()).toString();
            return aliceName.compareToIgnoreCase(bobName);
        });

        return users;
    }

    @Override
    protected PropertyValue getValueFromQueryResult(Object result, T propertyDefinition)
    {
        PropertyValue value = super.getValueFromQueryResult(result, propertyDefinition);
        if (value != null && value.getValue() instanceof DocumentReference) {
            DocumentReference documentReference = (DocumentReference) value.getValue();
            WikiReference wikiReference =
                propertyDefinition.getOwnerDocument().getDocumentReference().getWikiReference();
            // Serialize the user/group reference relative to the wiki were the property is defined.
            value.setValue(this.compactSerializer.serialize(documentReference, wikiReference));
            value.getMetaData().put(META_DATA_LABEL,
                getLabel(documentReference, value.getMetaData().get(META_DATA_LABEL)));
            value.getMetaData().put(META_DATA_ICON, getIcon(documentReference));
            value.getMetaData().put("url", getURL(documentReference));
        }
        return value;
    }

    protected String getLabel(DocumentReference documentReference, Object currentLabel)
    {
        String label = currentLabel == null ? "" : currentLabel.toString().trim();
        return label.isEmpty() ? documentReference.getName() : label;
    }

    protected abstract String getIcon(DocumentReference documentReference);

    protected String getURL(DocumentReference documentReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        return xcontext.getWiki().getURL(documentReference, xcontext);
    }
}
