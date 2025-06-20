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
package com.xpn.xwiki.internal.filter.output;

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
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.user.UserFilter;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(UserInstanceOutputFilterStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class UserInstanceOutputFilterStream extends AbstractBeanOutputFilterStream<UserInstanceOutputProperties>
    implements UserInstanceOutputFilter
{
    private static final EntityReference DEFAULT_SPACE = new EntityReference("XWiki", EntityType.SPACE);

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Inject
    private WikiDescriptorManager wikis;

    @Inject
    private EntityReferenceSerializer<String> serializer;

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

    private String getCurrentWiki()
    {
        String wiki = this.currentWiki;

        if (wiki == null) {
            wiki = this.wikis.getCurrentWikiId();
        }

        return wiki;
    }

    private DocumentReference getUserDocumentReference(String id)
    {
        return new DocumentReference(getCurrentWiki(), DEFAULT_SPACE.getName(), id);
    }

    private XWikiDocument getUserDocument(String id) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.getWiki().getDocument(getUserDocumentReference(id), xcontext);
    }

    private DocumentReference getGroupDocumentReference(String id)
    {
        return new DocumentReference(getCurrentWiki(), DEFAULT_SPACE.getName(), id);
    }

    private XWikiDocument getGroupDocument(String id) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.getWiki().getDocument(getGroupDocumentReference(id), xcontext);
    }

    // Events

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentWiki = name;
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentWiki = null;
    }

    @Override
    public void beginUser(String name, FilterEventParameters parameters) throws FilterException
    {
        XWikiDocument userDocument;
        try {
            userDocument = getUserDocument(name);

            // Safer to clone for thread safety and in case the save fail
            userDocument = userDocument.clone();
        } catch (XWikiException e) {
            throw new FilterException("Failed to get an XWikiDocument for user name [" + name + "]", e);
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
            throw new FilterException("Failed to get user class", e);
        }

        BaseObject userObject = userDocument.getXObject(userClass.getReference());
        if (userObject == null) {
            // Create object
            try {
                userObject = userDocument.newXObject(userClass.getReference(), xcontext);
            } catch (XWikiException e) {
                throw new FilterException("Failed to create user object", e);
            }

            // Setup right on user profile
            try {
                xcontext.getWiki().protectUserPage(userDocument.getFullName(), "edit", userDocument, xcontext);
            } catch (XWikiException e) {
                throw new FilterException("Failed to initialize user", e);
            }
        }

        // Update user properties
        userClass.fromValueMap(map, userObject);

        if (userDocument.isNew()) {
            // Authors
            userDocument.setCreatorReference(userDocument.getDocumentReference());
            userDocument.setAuthorReference(userDocument.getDocumentReference());
            userDocument.setContentAuthorReference(userDocument.getDocumentReference());

            // Dates
            if (this.properties.isVersionPreserved()) {
                if (parameters.containsKey(UserFilter.PARAMETER_CREATION_DATE)) {
                    userDocument.setCreationDate(getDate(UserFilter.PARAMETER_CREATION_DATE, parameters, new Date()));
                }
                if (parameters.containsKey(UserFilter.PARAMETER_REVISION_DATE)) {
                    userDocument.setDate(getDate(UserFilter.PARAMETER_REVISION_DATE, parameters, new Date()));
                    userDocument
                        .setContentUpdateDate(getDate(UserFilter.PARAMETER_REVISION_DATE, parameters, new Date()));
                }
            }

            // Set false to force the date and authors we want
            userDocument.setMetaDataDirty(false);
            userDocument.setContentDirty(false);
        }

        // Save
        try {
            xcontext.getWiki().saveDocument(userDocument, this.properties.getSaveComment(), xcontext);
        } catch (XWikiException e) {
            throw new FilterException("Failed to save user document", e);
        }
    }

    @Override
    public void endUser(String name, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    @Deprecated
    public void beginGroup(String name, FilterEventParameters parameters) throws FilterException
    {
        beginGroupContainer(name, parameters);
    }

    @Override
    public void beginGroupContainer(String name, FilterEventParameters parameters) throws FilterException
    {
        // Init members
        this.members = new ArrayList<String>();
    }

    private void addMember(String member, XWikiDocument groupDocument, BaseClass groupClass, XWikiContext xcontext)
        throws FilterException
    {
        BaseObject memberObject;
        try {
            memberObject = groupDocument.newXObject(groupClass.getReference(), xcontext);
        } catch (XWikiException e) {
            throw new FilterException("Failed to add a group member object", e);
        }

        memberObject.setStringValue("member", member);
    }

    @Override
    @Deprecated
    public void endGroup(String name, FilterEventParameters parameters) throws FilterException
    {
        endGroupContainer(name, parameters);
    }

    @Override
    public void endGroupContainer(String name, FilterEventParameters parameters) throws FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument groupDocument;
        try {
            groupDocument = getGroupDocument(name);
        } catch (XWikiException e) {
            throw new FilterException("Failed to get an XWikiDocument for group name [" + name + "]", e);
        }

        BaseClass groupClass;
        try {
            groupClass = xcontext.getWiki().getGroupClass(xcontext);
        } catch (XWikiException e) {
            throw new FilterException("Failed to get group class", e);
        }

        if (this.members.isEmpty()) {
            // Put an empty member so that the document is "marked" as group
            addMember("", groupDocument, groupClass, xcontext);
        } else {
            for (String member : this.members) {
                addMember(member, groupDocument, groupClass, xcontext);
            }
        }

        // Save
        try {
            xcontext.getWiki().saveDocument(groupDocument, this.properties.getSaveComment(), xcontext);
        } catch (XWikiException e) {
            throw new FilterException("Failed to save group document", e);
        }

        // Reset members
        this.members = null;
    }

    private void addMember(String name)
    {
        EntityReference memberReference = this.relativeResolver.resolve(name, EntityType.DOCUMENT, DEFAULT_SPACE);

        this.members.add(this.serializer.serialize(memberReference));
    }

    @Override
    public void onGroupMemberUser(String name, FilterEventParameters parameters) throws FilterException
    {
        addMember(name);
    }

    @Override
    public void onGroupMemberGroup(String name, FilterEventParameters parameters) throws FilterException
    {
        addMember(this.properties.getGroupPrefix() + name + this.properties.getGroupSuffix());
    }
}
