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

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.internal.filter.XWikiDocumentFilter;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterCollection;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Base class to help implement {@link EntityOutputFilterStream}.
 * 
 * @param <E> the type of the entity
 * @version $Id$
 * @since 9.0RC1
 */
public abstract class AbstractEntityOutputFilterStream<E> implements EntityOutputFilterStream<E>
{
    protected static final Pattern VALID_VERSION = Pattern.compile("\\d*\\.\\d*");

    protected E entity;

    protected DocumentInstanceOutputProperties properties;

    protected Object filter;

    protected EntityReference currentEntityReference;

    @Inject
    @Named("relative")
    protected EntityReferenceResolver<String> relativeResolver;

    @Inject
    @Named("current")
    protected DocumentReferenceResolver<EntityReference> documentEntityResolver;

    @Inject
    @Named("user/current")
    protected DocumentReferenceResolver<EntityReference> userEntityResolver;

    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> documentStringResolver;

    @Inject
    @Named("user/current")
    protected DocumentReferenceResolver<String> userStringResolver;

    @Inject
    @Named("document")
    protected UserReferenceResolver<DocumentReference> userDocumentResolver;

    @Inject
    protected ConverterManager converter;

    protected List<EntityOutputFilterStream<?>> children;

    protected boolean enabled = true;

    protected void initialize(EntityOutputFilterStream<?>... children)
    {
        this.children = new ArrayList<>(children.length);

        for (EntityOutputFilterStream<?> child : children) {
            child.disable();

            this.children.add(child);
        }
    }

    protected void disableChildren()
    {
        if (this.children != null) {
            for (EntityOutputFilterStream<?> child : this.children) {
                child.disable();
            }
        }
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public void enable()
    {
        this.enabled = true;
    }

    @Override
    public void disable()
    {
        this.enabled = false;

        disableChildren();
    }

    @Override
    public Object getFilter()
    {
        if (this.filter == null) {
            this.filter = createFilter();
        }

        return this.filter;
    }

    protected Object createFilter()
    {
        if (this.children != null) {
            List<XWikiDocumentFilter> filters = new ArrayList<>(this.children.size() + 1);
            for (EntityOutputFilterStream<?> child : this.children) {
                filters.add((XWikiDocumentFilter) child.getFilter());
            }
            filters.add(this);

            return new XWikiDocumentFilterCollection(filters);
        }

        return this;
    }

    @Override
    public void setProperties(DocumentInstanceOutputProperties properties)
    {
        this.properties = properties;

        if (this.children != null) {
            for (EntityOutputFilterStream<?> child : this.children) {
                child.setProperties(properties);
            }
        }
    }

    @Override
    public E getEntity()
    {
        return this.entity;
    }

    @Override
    public void setEntity(E entity)
    {
        this.entity = entity;
    }

    protected <T> T get(Type type, String key, FilterEventParameters parameters, T def)
    {
        return get(type, key, parameters, def, true, true);
    }

    protected <T> T get(Type type, String key, FilterEventParameters parameters, T def, boolean replaceNull,
        boolean convert)
    {
        if (parameters == null) {
            return def;
        }

        if (!parameters.containsKey(key)) {
            return def;
        }

        Object value = parameters.get(key);

        if (value == null) {
            return replaceNull ? def : null;
        }

        if (TypeUtils.isInstance(value, type)) {
            return (T) value;
        }

        return convert ? this.converter.convert(type, value) : (T) value;
    }

    protected Date getDate(String key, FilterEventParameters parameters, Date def)
    {
        return get(Date.class, key, parameters, def);
    }

    protected String getString(String key, FilterEventParameters parameters, String def)
    {
        return get(String.class, key, parameters, def);
    }

    protected boolean getBoolean(String key, FilterEventParameters parameters, boolean def)
    {
        return get(boolean.class, key, parameters, def);
    }

    protected int getInt(String key, FilterEventParameters parameters, int def)
    {
        return get(int.class, key, parameters, def);
    }

    protected Syntax getSyntax(String key, FilterEventParameters parameters, Syntax def)
    {
        return get(Syntax.class, key, parameters, def);
    }

    protected EntityReference getEntityReference(String key, FilterEventParameters parameters, EntityReference def)
    {
        Object reference = get(Object.class, key, parameters, def, false, false);

        if (reference != null && !(reference instanceof EntityReference)) {
            reference = this.relativeResolver.resolve(reference.toString(), EntityType.DOCUMENT);
        }

        return (EntityReference) reference;
    }

    protected DocumentReference getDocumentReference(String key, FilterEventParameters parameters,
        DocumentReference def)
    {
        Object reference = get(Object.class, key, parameters, def, false, false);

        if (reference != null && !(reference instanceof DocumentReference)) {
            if (reference instanceof EntityReference) {
                reference =
                    this.documentEntityResolver.resolve((EntityReference) reference, this.currentEntityReference);
            } else {
                reference = this.documentStringResolver.resolve(reference.toString(), this.currentEntityReference);
            }
        }

        return (DocumentReference) reference;
    }

    protected UserReference getUserReference(String key, FilterEventParameters parameters, UserReference def)
    {
        UserReference userReference = def;

        Object reference = get(Object.class, key, parameters, def, false, false);

        if (reference != null && !(reference instanceof UserReference)) {
            if (reference instanceof DocumentReference) {
                userReference = this.userDocumentResolver.resolve((DocumentReference) reference);
            } else {
                userReference = this.userDocumentResolver.resolve(toUserDocumentReference(reference));
            }
        }

        return userReference;
    }

    protected DocumentReference getUserDocumentReference(String key, FilterEventParameters parameters,
        DocumentReference def)
    {
        DocumentReference userReference = def;

        Object reference = get(Object.class, key, parameters, def, false, false);

        if (reference != null && !(reference instanceof DocumentReference)) {
            userReference = toUserDocumentReference(reference);
        }

        return userReference;
    }

    protected DocumentReference toUserDocumentReference(Object reference)
    {
        DocumentReference userDocumentReference;

        if (reference instanceof EntityReference) {
            userDocumentReference =
                this.userEntityResolver.resolve((EntityReference) reference, this.currentEntityReference != null
                    ? this.currentEntityReference.extractReference(EntityType.WIKI) : null);
        } else {
            userDocumentReference =
                this.userStringResolver.resolve(reference.toString(), this.currentEntityReference != null
                    ? this.currentEntityReference.extractReference(EntityType.WIKI) : null);
        }

        if (userDocumentReference != null && userDocumentReference.getName().equals(XWikiRightService.GUEST_USER)) {
            userDocumentReference = null;
        }

        return userDocumentReference;
    }

    // XWikiDocumentFilter

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.WIKI, this.currentEntityReference);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.SPACE, this.currentEntityReference);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.DOCUMENT, this.currentEntityReference);
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void beginWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws FilterException
    {

    }

    @Override
    public void beginWikiDocumentAttachment(String name, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiDocumentAttachment(String name, InputSource content, Long size, FilterEventParameters parameters)
        throws FilterException
    {

    }

    @Override
    public void beginWikiAttachmentRevisions(FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiAttachmentRevisions(FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void beginWikiAttachmentRevision(String revision, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiAttachmentRevision(String revision, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws FilterException
    {

    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters) throws FilterException
    {

    }

    @Override
    public void onWikiClassPropertyField(String name, String value, FilterEventParameters parameters)
        throws FilterException
    {

    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        if (name != null) {
            this.currentEntityReference = new EntityReference(name, EntityType.OBJECT, this.currentEntityReference);
        }
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        if (this.currentEntityReference.getType() == EntityType.OBJECT) {
            this.currentEntityReference = this.currentEntityReference.getParent();
        }
    }

    @Override
    public void onWikiObjectProperty(String name, Object value, FilterEventParameters parameters) throws FilterException
    {

    }
}
