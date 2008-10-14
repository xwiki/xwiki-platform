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
package org.xwiki.bridge;

/**
 * Exposes methods for accessing Documents. This is temporary until we remodel the Model classes and the Document
 * services. The implementation is actually the XWikiDocument class, so this is just a light interface that hides the
 * old xwiki-core.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public interface DocumentModelBridge
{
    /**
     * Retrieve the full name of the document, in the <code>Space.Name</code> format, for example <tt>Main.WebHome</tt>.
     * 
     * @return A <code>String</code> representation of the document's full name.
     */
    String getFullName();

    /**
     * Retrieve the name of the virtual wiki this document belongs to.
     * 
     * @return A <code>String</code> representation of the document's wiki name.
     */
    String getWikiName();

    /**
     * Retrieve the actual language of the document variant. If this is a translation, then it is the language of the
     * translation. If this is the original variant of the document, then it it is the default language of the document.
     * 
     * @return The document's language in a 2-letter code.
     */
    String getRealLanguage();

    /**
     * Retrieves the textual content of the document.
     * 
     * @return The document's content.
     */
    String getContent();

    /**
     * Retrieves a copy of the document before it was changed.
     * 
     * @return the copy of this Document instance before any modification was made to it.
     */
    DocumentModelBridge getOriginalDocument();
}
