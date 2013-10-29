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
package org.xwiki.wikistream.instance.internal.output;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.user.UserFilter;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStream;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Named(UserOutputInstanceWikiStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class UserOutputInstanceWikiStream extends AbstractBeanOutputWikiStream<UserOutputProperties> implements
    UserInstanceOutputFilter
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ConverterManager converter;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private String currentWiki;

    private List<String> members;

    @Override
    public void close() throws IOException
    {
        // Nothing to close
    }

    private <T> T get(Type type, String key, FilterEventParameters parameters, T def)
    {
        if (!parameters.containsKey(key)) {
            return def;
        }

        Object value = parameters.get(key);

        if (value == null) {
            return null;
        }

        if (TypeUtils.isInstance(value, type)) {
            return (T) value;
        }

        return this.converter.convert(type, value);
    }

    private Date getDate(String key, FilterEventParameters parameters, Date def)
    {
        return get(Date.class, key, parameters, def);
    }

    private String getString(String key, FilterEventParameters parameters, String def)
    {
        return get(String.class, key, parameters, def);
    }

    private boolean getBoolean(String key, FilterEventParameters parameters, boolean def)
    {
        return get(boolean.class, key, parameters, def);
    }

    private int getInt(String key, FilterEventParameters parameters, int def)
    {
        return get(int.class, key, parameters, def);
    }

    private Syntax getSyntax(String key, FilterEventParameters parameters, Syntax def)
    {
        return get(Syntax.class, key, parameters, def);
    }

    private EntityReference getEntityReference(String key, FilterEventParameters parameters, EntityReference def)
    {
        Object reference = get(Object.class, key, parameters, def);

        if (reference instanceof EntityReference) {
            return (EntityReference) reference;
        }

        return reference != null ? this.relativeResolver.resolve(reference.toString(), EntityType.DOCUMENT, parameters)
            : def;
    }

    private DocumentReference getUserDocumentReference(String id)
    {
        return new DocumentReference(this.currentWiki, "XWiki", id);
    }

    private XWikiDocument getUserDocument(String id) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.getWiki().getDocument(getUserDocumentReference(id), xcontext);
    }

    private DocumentReference getGroupDocumentReference(String id)
    {
        return new DocumentReference(this.currentWiki, "XWiki", id);
    }

    private XWikiDocument getGroupDocument(String id) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.getWiki().getDocument(getGroupDocumentReference(id), xcontext);
    }

    // Events

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentWiki = name;
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentWiki = null;
    }

    @Override
    public void beginUser(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        XWikiDocument userDocument;
        try {
            userDocument = getUserDocument(name);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to get an XWikiDocument for user name [" + name + "]", e);
        }

        Map<String, Object> map = new HashMap<String, Object>();

        // First name
        if (parameters.containsKey(PARAMETER_FIRSTNAME)) {
            map.put("first_name", getString(PARAMETER_FIRSTNAME, parameters, ""));
        }

        // Last name
        if (parameters.containsKey(PARAMETER_LASTNAME)) {
            map.put("last_name", getString(PARAMETER_LASTNAME, parameters, ""));
        }

        // Email
        if (parameters.containsKey(PARAMETER_EMAIL)) {
            map.put("email", getString(PARAMETER_EMAIL, parameters, ""));
        }

        // Active
        map.put("active", getBoolean(PARAMETER_ACTIVE, parameters, true) ? 1 : 0);

        XWikiContext xcontext = this.xcontextProvider.get();

        BaseClass userClass;
        try {
            userClass = xcontext.getWiki().getUserClass(xcontext);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to get user class", e);
        }

        BaseObject userObject = userDocument.getXObject(userClass.getReference());
        if (userObject == null) {
            // Create object
            try {
                userObject = userDocument.newXObject(userClass.getReference(), xcontext);
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to create user object", e);
            }

            // Setup right on user profile
            try {
                xcontext.getWiki().protectUserPage(userDocument.getFullName(), "edit", userDocument, xcontext);
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to initialize user", e);
            }
        }

        // Update user properties
        userClass.fromValueMap(map, userObject);

        // Dates
        if (userDocument.isNew() && this.properties.isPreserveVersion()) {
            if (parameters.containsKey(UserFilter.PARAMETER_CREATION_DATE)) {
                userDocument.setCreationDate(getDate(UserFilter.PARAMETER_CREATION_DATE, parameters, new Date()));
            }
            if (parameters.containsKey(UserFilter.PARAMETER_REVISION_DATE)) {
                userDocument.setDate(getDate(UserFilter.PARAMETER_CREATION_DATE, parameters, new Date()));
                userDocument.setContentUpdateDate(getDate(UserFilter.PARAMETER_CREATION_DATE, parameters, new Date()));
            }

            // Set false to force the date we want
            userDocument.setMetaDataDirty(false);
        }

        // Save
        try {
            xcontext.getWiki().saveDocument(userDocument, this.properties.getSaveComment(), xcontext);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to save user document", e);
        }

        // Add the user to default groups
        try {
            xcontext.getWiki().setUserDefaultGroup(userDocument.getFullName(), xcontext);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to add user to default groups", e);
        }
    }

    @Override
    public void endUser(String name, FilterEventParameters parameters) throws WikiStreamException
    {

    }

    @Override
    public void beginGroup(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // Init members
        this.members = new ArrayList<String>();
    }

    @Override
    public void endGroup(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument groupDocument;
        try {
            groupDocument = getGroupDocument(name);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to get an XWikiDocument for group name [" + name + "]", e);
        }

        BaseClass groupClass;
        try {
            groupClass = xcontext.getWiki().getGroupClass(xcontext);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to get group class", e);
        }

        for (String member : this.members) {
            BaseObject memberObject;
            try {
                memberObject = groupDocument.newXObject(groupClass.getReference(), xcontext);
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to add a group member object", e);
            }

            memberObject.setStringValue("member", member);
        }

        // Save
        try {
            xcontext.getWiki().saveDocument(groupDocument, this.properties.getSaveComment(), xcontext);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to save group document", e);
        }

        // Reset members
        this.members = null;
    }

    @Override
    public void onGroupMemberUser(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.members.add(name);
    }

    @Override
    public void onGroupMemberGroup(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.members.add(name);
    }
}
