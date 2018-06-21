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

/**
 * Represents a reference to a page in the current wiki.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
public class LocalPageReference extends AbstractLocalizedEntityReference
{
    /**
     * Create a new Page reference in the current wiki.
     * 
     * @param pageName the name of the page containing the document, must not be null
     */
    public LocalPageReference(String pageName)
    {
        super(pageName, EntityType.PAGE);
    }

    /**
     * Create a new Page reference in the current wiki.
     *
     * @param pageNames an ordered list of the names of the pages containing the document from root page to last one,
     *            must not be null
     */
    public LocalPageReference(List<String> pageNames)
    {
        this(pageNames.get(pageNames.size() - 1),
            pageNames.size() > 1 ? new LocalPageReference(pageNames.subList(0, pageNames.size() - 1)) : null);
    }

    /**
     * Create a new Page reference in the current wiki.
     * 
     * @param pageName the name of the page containing the document, must not be null
     * @param locale the new locale for this reference
     */
    public LocalPageReference(String pageName, Locale locale)
    {
        super(pageName, EntityType.PAGE);

        setLocale(locale);
    }

    /**
     * @param documentReference the full document reference
     */
    public LocalPageReference(PageReference documentReference)
    {
        super(documentReference, documentReference.getWikiReference(), null);
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
        super(entityReference);

        setLocale(locale);
    }

    /**
     * Create a new Page reference in the current wiki.
     * 
     * @param pageName the name of the document, must not be null
     * @param pageReference the reference of the page, must not be null
     */
    public LocalPageReference(String pageName, EntityReference pageReference)
    {
        super(pageName, EntityType.PAGE, pageReference);
    }
}
