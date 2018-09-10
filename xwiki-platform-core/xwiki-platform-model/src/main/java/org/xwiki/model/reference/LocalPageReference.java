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

import java.util.List;
import java.util.Locale;

import org.xwiki.model.EntityType;
import org.xwiki.stability.Unstable;

/**
 * Represents a reference to a page in the current wiki.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
@Unstable
public class LocalPageReference extends AbstractLocalizedEntityReference
{
    /**
     * Create a new Page reference in the current wiki.
     * 
     * @param pageName the name of the page containing the page, must not be null
     * @param pageNames an ordered list of the names of the pages containing the page from root page to last one, must
     *            not be null
     */
    public LocalPageReference(String pageName, String... pageNames)
    {
        this(PageReference.toList(pageName, pageNames));
    }

    /**
     * Create a new Page reference in the current wiki.
     *
     * @param pageNames an ordered list of the names of the pages containing the page from root page to last one, must
     *            not be null
     */
    public LocalPageReference(List<String> pageNames)
    {
        this(pageNames.get(pageNames.size() - 1),
            pageNames.size() > 1 ? new LocalPageReference(pageNames.subList(0, pageNames.size() - 1)) : null);
    }

    /**
     * Create a new Page reference in the current wiki.
     * 
     * @param pageName the name of the page containing the page, must not be null
     * @param locale the new locale for this reference
     */
    public LocalPageReference(String pageName, Locale locale)
    {
        super(pageName, EntityType.PAGE, locale);
    }

    /**
     * @param reference the reference to clone
     */
    public LocalPageReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * @param entityReference the reference
     * @param locale the new locale for this reference, if null, locale is removed
     */
    public LocalPageReference(EntityReference entityReference, Locale locale)
    {
        super(entityReference, locale);
    }

    /**
     * Create a new Page reference in the current wiki.
     * 
     * @param pageName the name of the page, must not be null
     * @param pageReference the reference of the page, must not be null
     */
    public LocalPageReference(String pageName, EntityReference pageReference)
    {
        super(pageName, EntityType.PAGE, pageReference);
    }

    /**
     * @param pageReference the full page reference
     */
    public LocalPageReference(PageReference pageReference)
    {
        super(pageReference, pageReference.getWikiReference(), null);
    }

    /**
     * Clone an DocumentVersionReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public LocalPageReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to verify the validity of the passed parent.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setParent(EntityReference)
     * @exception IllegalArgumentException if the passed parent is not a valid page reference parent (ie either a page
     *                reference or null)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent != null && parent.getType() != EntityType.PAGE) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] in a local page reference");
        }

        super.setParent(parent != null ? new LocalPageReference(parent) : null);
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
    public LocalPageReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new LocalPageReference(this, newParent);
    }

    @Override
    public String toString()
    {
        // Compared to EntityReference we don't print the type since the type is already indicated by the fact that
        // this is a LocalPageReference instance.
        return TOSTRING_SERIALIZER.serialize(this);
    }
}
