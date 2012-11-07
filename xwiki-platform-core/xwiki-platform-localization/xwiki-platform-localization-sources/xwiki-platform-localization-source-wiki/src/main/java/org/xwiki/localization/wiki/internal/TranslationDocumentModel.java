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
package org.xwiki.localization.wiki.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Informations about the way to store translation documents.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public interface TranslationDocumentModel
{
    /**
     * Name of the space where to store the xclass.
     */
    String TRANSLATIONCLASS_REFERENCE_SPACE = "XWiki";

    /**
     * Name of the document where to store the xclass.
     */
    String TRANSLATIONCLASS_REFERENCE_NAME = "TranslationDocumentClass";

    /**
     * The local reference of the xlcass.
     */
    EntityReference TRANSLATIONCLASS_REFERENCE = new EntityReference(TRANSLATIONCLASS_REFERENCE_NAME,
        EntityType.DOCUMENT, new EntityReference(TRANSLATIONCLASS_REFERENCE_SPACE, EntityType.SPACE));

    /**
     * The local reference of the xclass as String.
     */
    String TRANSLATIONCLASS_REFERENCE_STRING = TRANSLATIONCLASS_REFERENCE_SPACE + '.' + TRANSLATIONCLASS_REFERENCE_NAME;

    /**
     * The name of the property containing the scope information.
     */
    String TRANSLATIONCLASS_PROP_SCOPE = "scope";

    /**
     * The possible values of the scope property.
     * 
     * @version $Id$
     */
    enum Scope
    {
        /**
         * Register the translation bundle for the whole farm.
         */
        GLOBAL,

        /**
         * Register the translation bundle for the document wiki.
         */
        WIKI,

        /**
         * Register the translation bundle for the document author.
         */
        USER,

        /**
         * Don't register the translation bundle automatically.
         */
        ON_DEMAND
    }
}
