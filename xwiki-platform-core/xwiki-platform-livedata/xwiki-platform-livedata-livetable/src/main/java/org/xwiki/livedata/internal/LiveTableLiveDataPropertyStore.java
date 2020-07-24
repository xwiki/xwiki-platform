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
package org.xwiki.livedata.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * {@link LiveDataPropertyDescriptorStore} implementation that exposes the known live table columns as live data
 * properties.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@Component
@Named("liveTable/property")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class LiveTableLiveDataPropertyStore extends AbstractLiveDataPropertyDescriptorStore
{
    private static final String STRING = "String";

    private static final String DATE = "Date";

    private static final String USERS = "Users";

    private static final String LINK = "link";

    private static final String HTML = "html";

    private static final String ACTIONS = "actions";

    private static final String PROPERTY_HREF = "propertyHref";

    private static final String TEXT = "text";

    @SuppressWarnings("serial")
    private static class PropertyDescriptorList extends ArrayList<LiveDataPropertyDescriptor>
    {
        protected LiveDataPropertyDescriptor add(String id, String type, String displayer, Boolean sortable)
        {
            LiveDataPropertyDescriptor property = new LiveDataPropertyDescriptor();
            property.setId(id);
            property.setType(type);
            if (displayer != null) {
                property.getDisplayer().setId(displayer);
            }
            if (Boolean.FALSE.equals(sortable)) {
                // If we cannot sort on this property then we cannot filter either.
                property.setFilterable(false);
            }
            property.setSortable(sortable);
            add(property);
            return property;
        }
    }

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public Collection<LiveDataPropertyDescriptor> get() throws LiveDataException
    {
        List<LiveDataPropertyDescriptor> properties = new ArrayList<>();
        properties.addAll(translate(getDocumentProperties()));
        try {
            // Class properties use their own translation process.
            properties.addAll(getClassProperties());
        } catch (Exception e) {
            throw new LiveDataException("Failed to retrieve class properties.", e);
        }
        return properties;
    }

    @SuppressWarnings("serial")
    private List<LiveDataPropertyDescriptor> getDocumentProperties()
    {
        return new PropertyDescriptorList()
        {
            {
                add("doc.name", STRING, LINK, null);
                add("doc.title", STRING, LINK, null);
                add("doc.space", STRING, LINK, null).getDisplayer().put(PROPERTY_HREF, "doc.space_url");
                add("doc.location", STRING, HTML, null);
                add("doc.fullName", STRING, LINK, null);
                // Display as text because the returned value is the formatted date.
                add("doc.creationDate", DATE, TEXT, null);
                add("doc.date", DATE, TEXT, null);
                add("doc.creator", USERS, TEXT, null);
                add("doc.author", USERS, LINK, null).getDisplayer().put(PROPERTY_HREF, "doc.author_url");
                add("doc.objectCount", "Number", null, false);

                add("_images", STRING, HTML, false);
                add("_attachments", STRING, HTML, false);
                add("_actions", STRING, ACTIONS, false).getDisplayer().put(ACTIONS, Arrays.asList("edit", "delete"));
                add("_avatar", STRING, HTML, false);
            }
        };
    }

    private List<LiveDataPropertyDescriptor> getClassProperties() throws Exception
    {
        Object className = getParameters().get("className");
        if (className instanceof String) {
            DocumentReference classReference = this.currentDocumentReferenceResolver.resolve((String) className);
            return getClassProperties(classReference);
        } else {
            return Collections.emptyList();
        }
    }

    private List<LiveDataPropertyDescriptor> getClassProperties(DocumentReference classReference) throws Exception
    {
        if (this.authorization.hasAccess(Right.VIEW, classReference)) {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument classDoc = xcontext.getWiki().getDocument(classReference, xcontext);
            return classDoc.getXClass().getEnabledProperties().stream().map(this::getLiveDataPropertyDescriptor)
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private LiveDataPropertyDescriptor getLiveDataPropertyDescriptor(PropertyClass xproperty)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(xproperty.getName());
        descriptor.setName(xproperty.getTranslatedPrettyName(xcontext));
        descriptor.setDescription(xproperty.getHint());
        descriptor.setType(xproperty.getClassType());
        // List properties are sortable by default, but only if they have single selection.
        if (xproperty instanceof ListClass && ((ListClass) xproperty).isMultiSelect()) {
            descriptor.setSortable(false);
        }
        // The returned property value is the displayer output.
        descriptor.getDisplayer().setId(HTML);
        if (xproperty.newProperty() instanceof StringListProperty) {
            // The default live table results page currently supports only exact matching for list properties with
            // multiple selection and no relational storage (selected values are stored concatenated on a single
            // database column).
            descriptor.getFilter().getOperators().add(createOperator("equals"));
        }
        return descriptor;
    }
}
