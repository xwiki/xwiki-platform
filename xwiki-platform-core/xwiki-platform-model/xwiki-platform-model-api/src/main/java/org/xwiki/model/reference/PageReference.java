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
package org.xwiki.model.reference;

import java.beans.Transient;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;

import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;

/**
 * Represents a reference to a page. Note that nested pages are supported. A page is essentially the same thing as a
 * document. A document represents the data displayed in a wiki page, stored in a {@code Document} object. Both a
 * {@link PageReference} and a {@link DocumentReference} point to a {@code Document}.
 * <p>
 * In XWiki 7.2, we introduced the ability to have pages inside pages (we called it
 * <a href="https://www.xwiki.org/xwiki/bin/view/Documentation/UserGuide/Features/ContentOrganization/">Nested
 * Pages</a>). We did that by creating a space to represent a Nested Page. Indeed, a space always has {@code WebHome}
 * document and this document contains the content of the Nested Page. In other words all Nested Page are located in
 * {@code WebHome} documents.
 * <p>
 * This means that a reference to a Nested Page always ends with {@code WebHome}, which is an implementation detail, and
 * hard to remember and use (e.g. {@code A.B.C.WebHome}). Thus we introduced the concept of a Page Reference to
 * reference a Nested Page and this allows us to drop the {@code Webhome} part in the reference (e.g. {@code A.B.C}).
 *
 * @version $Id$
 * @since 10.6RC1
 */
public class PageReference extends AbstractLocalizedEntityReference
{
    /**
     * The {@link Type} for a {@code Provider<PageReference>}.
     */
    public static final Type TYPE_PROVIDER = new DefaultParameterizedType(null, Provider.class, PageReference.class);

    /**
     * Special constructor that transforms a generic entity reference into a {@link PageReference}. It checks the
     * validity of the passed reference (ie correct type).
     *
     * @param reference the entity reference to transforms
     * @exception IllegalArgumentException if the passed reference is not a valid page reference
     */
    public PageReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone a page reference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     */
    protected PageReference(EntityReference reference, EntityReference oldReference, EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * Create a page reference based on a page name and a parent wiki reference.
     *
     * @param pageName the name of the page
     * @param parent the wiki reference
     */
    public PageReference(String pageName, WikiReference parent)
    {
        this(pageName, (EntityReference) parent);
    }

    /**
     * Create a page reference based on a page name and a parent page reference.
     *
     * @param pageName the name of the page
     * @param parent the page reference
     */
    public PageReference(String pageName, PageReference parent)
    {
        this(pageName, (EntityReference) parent);
    }

    /**
     * Create a page reference based on a page name and a parent entity reference. The entity reference may be either a
     * wiki or a page reference.
     *
     * @param pageName the name of the page
     * @param parent the entity reference
     */
    public PageReference(String pageName, EntityReference parent)
    {
        super(pageName, EntityType.PAGE, parent);
    }

    /**
     * Create a page reference based on a page name and a parent entity reference. The entity reference may be either a
     * wiki or a page reference.
     *
     * @param pageName the name of the page
     * @param parent the entity reference
     * @param locale the {@link Locale} of the page.
     */
    public PageReference(String pageName, EntityReference parent, Locale locale)
    {
        super(pageName, EntityType.PAGE, parent, locale);
    }

    /**
     * Clone an EntityReference and change/add the passed Locale.
     *
     * @param reference the reference to clone
     * @param locale the {@link Locale} of the new reference
     */
    public PageReference(EntityReference reference, Locale locale)
    {
        super(reference, locale);
    }

    /**
     * Create a page reference based on a page name and a parent page reference.
     *
     * @param wikiName the name of the wiki
     * @param pageNames the pages names
     */
    public PageReference(String wikiName, List<String> pageNames)
    {
        this(pageNames.get(pageNames.size() - 1), pageNames.size() > 1
            ? new PageReference(wikiName, pageNames.subList(0, pageNames.size() - 1)) : new WikiReference(wikiName));
    }

    /**
     * Create a page reference based on a page name and a parent page reference.
     *
     * @param wikiName the name of the wiki
     * @param pageNames the pages names
     * @param locale the {@link Locale} of the entity.
     */
    public PageReference(String wikiName, List<String> pageNames, Locale locale)
    {
        this(
            pageNames.get(pageNames.size() - 1), pageNames.size() > 1
                ? new PageReference(wikiName, pageNames.subList(0, pageNames.size() - 1)) : new WikiReference(wikiName),
            locale);
    }

    /**
     * Create a page reference based on a page name and a parent page reference.
     *
     * @param wikiName the name of the wiki
     * @param pageName the root page name
     * @param pageNames the children page names
     */
    public PageReference(String wikiName, String pageName, String... pageNames)
    {
        this(wikiName, toList(pageName, pageNames));
    }

    /**
     * Create a new page reference from local page reference and wiki reference.
     * 
     * @param localPageReference the page reference without the wiki reference
     * @param wikiReference the wiki reference
     */
    public PageReference(LocalPageReference localPageReference, WikiReference wikiReference)
    {
        super(localPageReference, null, wikiReference);
    }

    /**
     * Clone an PageReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public PageReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    static List<String> toList(String inputPageName, String... inputPageNames)
    {
        List<String> pageNames = new ArrayList<>(inputPageNames.length + 1);

        pageNames.add(inputPageName);

        for (String pageName : inputPageNames) {
            pageNames.add(pageName);
        }

        return pageNames;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to verify the validity of the passed parent.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setParent(EntityReference)
     * @exception IllegalArgumentException if the passed parent is not a valid page reference parent (ie either a page
     *                reference or a wiki reference)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent instanceof PageReference || parent instanceof WikiReference) {
            super.setParent(parent);

            return;
        }

        if (parent == null || (parent.getType() != EntityType.PAGE && parent.getType() != EntityType.WIKI)) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] in a page reference");
        }

        if (parent.getType() == EntityType.PAGE) {
            super.setParent(new PageReference(parent));
        } else {
            super.setParent(new WikiReference(parent));
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to verify the validity of the passed type.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     * @exception IllegalArgumentException if the passed type is not a page type
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.PAGE) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a page reference");
        }

        super.setType(type);
    }

    @Override
    public PageReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (newParent == oldParent) {
            return this;
        }

        return new PageReference(this, oldParent, newParent);
    }

    @Override
    public PageReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new PageReference(this, newParent);
    }

    /**
     * @return the reference of the wiki containing this page
     */
    @Transient
    public WikiReference getWikiReference()
    {
        return (WikiReference) extractReference(EntityType.WIKI);
    }

    @Override
    public String toString()
    {
        // Compared to EntityReference we don't print the type since the type is already indicated by the fact that
        // this is a PageReference instance.
        return TOSTRING_SERIALIZER.serialize(this);
    }
}
