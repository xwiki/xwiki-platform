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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.xwiki.livedata.WithParameters;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.ComputedFieldClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
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
public class LiveTableLiveDataPropertyStore implements LiveDataPropertyDescriptorStore, WithParameters
{
    private static final String STRING = "String";

    private static final String DATE = "Date";

    private static final String USERS = "Users";

    private static final String LINK = "link";

    private static final String HTML = "html";

    private static final String ID = "id";

    private static final String ACTIONS = "actions";

    private static final String PROPERTY_HREF = "propertyHref";

    private static final String TEXT = "text";

    @SuppressWarnings("serial")
    private static class PropertyDescriptorList extends ArrayList<LiveDataPropertyDescriptor>
    {
        protected LiveDataPropertyDescriptor add(String id, String type, String displayer, boolean sortable)
        {
            LiveDataPropertyDescriptor property = new LiveDataPropertyDescriptor();
            property.setId(id);
            property.setType(type);
            if (displayer != null) {
                property.getDisplayer().put(ID, displayer);
            }
            if (!sortable) {
                property.getFilter().put(ID, "none");
            }
            property.setSortable(sortable);
            add(property);
            return property;
        }
    }

    private final Map<String, Object> parameters = new HashMap<>();

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private ContextualLocalizationManager l10n;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public Map<String, Object> getParameters()
    {
        return this.parameters;
    }

    @Override
    public boolean add(LiveDataPropertyDescriptor propertyDescriptor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<LiveDataPropertyDescriptor> get(String propertyId) throws LiveDataException
    {
        return get().stream().filter(property -> Objects.equals(property.getId(), propertyId)).findFirst();
    }

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

    @Override
    public boolean update(LiveDataPropertyDescriptor propertyDescriptor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<LiveDataPropertyDescriptor> remove(String propertyId)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("serial")
    private List<LiveDataPropertyDescriptor> getDocumentProperties()
    {
        return new PropertyDescriptorList()
        {
            {
                add("doc.name", STRING, LINK, true);
                add("doc.title", STRING, LINK, true);
                add("doc.space", STRING, LINK, true).getDisplayer().put(PROPERTY_HREF, "doc.space_url");
                add("doc.location", STRING, HTML, true);
                add("doc.fullName", STRING, LINK, true);
                // Display as text because the returned value is the formatted date.
                add("doc.creationDate", DATE, TEXT, true);
                add("doc.date", DATE, TEXT, true);
                add("doc.creator", USERS, TEXT, true);
                add("doc.author", USERS, LINK, true).getDisplayer().put(PROPERTY_HREF, "doc.author_url");
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
        descriptor.setSortable(!(xproperty instanceof ComputedFieldClass || xproperty instanceof PasswordClass));
        // The returned property value is the displayer output.
        descriptor.getDisplayer().put(ID, HTML);
        return descriptor;
    }

    private List<LiveDataPropertyDescriptor> translate(List<LiveDataPropertyDescriptor> properties)
    {
        return properties.stream().map(this::translate).collect(Collectors.toList());
    }

    private LiveDataPropertyDescriptor translate(LiveDataPropertyDescriptor property)
    {
        String translationPrefix = String.valueOf(getParameters().getOrDefault("translationPrefix", ""));
        if (property.getName() == null) {
            property.setName(this.l10n.getTranslationPlain(translationPrefix + property.getId()));
        }
        if (property.getDescription() == null) {
            property.setDescription(this.l10n.getTranslationPlain(translationPrefix + property.getId() + ".hint"));
        }
        return property;
    }
}
