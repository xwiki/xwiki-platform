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

import java.util.Locale;

import org.xwiki.model.EntityType;

/**
 * Represents a reference to a document in the current wiki.
 * 
 * @version $Id$
 * @since 5.0M1
 */
public class LocalDocumentReference extends AbstractLocalizedEntityReference
{
    /**
     * Create a new Document reference in the current wiki.
     * 
     * @param spaceName the name of the space containing the document, must not be null
     * @param pageName the name of the document, must not be null
     */
    public LocalDocumentReference(String spaceName, String pageName)
    {
        super(pageName, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE));
    }

    /**
     * Create a new Document reference in the current wiki.
     *
     * @param spaceNames an ordered list of the names of the spaces containing the document from root space to last one,
     *            must not be null
     * @param pageName the name of the document
     * @since 7.3M1
     */
    public LocalDocumentReference(Iterable<String> spaceNames, String pageName)
    {
        super(pageName, EntityType.DOCUMENT, constructSpaceReference(spaceNames));
    }

    /**
     * Create a new Document reference in the current wiki.
     * 
     * @param spaceName the name of the space containing the document, must not be null
     * @param pageName the name of the document, must not be null
     * @param locale the new locale for this reference
     * @since 5.3RC1
     */
    public LocalDocumentReference(String spaceName, String pageName, Locale locale)
    {
        super(pageName, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE));

        setLocale(locale);
    }

    /**
     * @param documentReference the full document reference
     * @since 5.2M2
     */
    public LocalDocumentReference(DocumentReference documentReference)
    {
        super(documentReference, documentReference.getWikiReference(), null);
    }

    /**
     * @param reference the reference to clone
     * @since 5.4RC1
     */
    public LocalDocumentReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * @param entityReference the reference
     * @param locale the new locale for this reference, if null, locale is removed
     * @since 5.3RC1
     */
    public LocalDocumentReference(EntityReference entityReference, Locale locale)
    {
        super(entityReference);

        setLocale(locale);
    }

    /**
     * Create a new Document reference in the current wiki.
     * 
     * @param pageName the name of the document, must not be null
     * @param spaceReference the reference of the space, must not be null
     * @since 7.2M1
     */
    public LocalDocumentReference(String pageName, EntityReference spaceReference)
    {
        super(pageName, EntityType.DOCUMENT, spaceReference);
    }

    /**
     * Create a relative space reference in from a list of space names.
     *
     * @param spaceNames the list of space name from root to child
     * @return the space reference
     */
    private static EntityReference constructSpaceReference(Iterable<String> spaceNames)
    {
        EntityReference spaceReference = null;

        for (String spaceName : spaceNames) {
            spaceReference = new EntityReference(spaceName, EntityType.SPACE, spaceReference);
        }

        return spaceReference;
    }
}
