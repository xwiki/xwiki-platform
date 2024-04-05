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

import java.util.Date;

import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

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
     * Retrieve the full name of the document, in the <code>Space.Name</code> format, for example {@code Main.WebHome}.
     * 
     * @return A <code>String</code> representation of the document's full name.
     * @deprecated use {@link #getDocumentReference} instead
     */
    @Deprecated
    String getFullName();

    /**
     * @return the document's reference (without the document locale)
     * @since 2.2M1
     */
    DocumentReference getDocumentReference();

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
    
    /**
     * @return the Syntax id representing the syntax used for the current document. For example "xwiki/1.0" represents
     *         the first version XWiki syntax while "xwiki/2.0" represents version 2.0 of the XWiki Syntax.
     * @deprecated since 3.0M1 use {@link #getSyntax()} instead
     */
    @Deprecated
    String getSyntaxId();

    /**
     * @return the Syntax id representing the syntax used for the current document. For example "xwiki/1.0" represents
     *         the first version XWiki syntax while "xwiki/2.0" represents version 2.0 of the XWiki Syntax.
     * @since 3.0M1
     */
    Syntax getSyntax();

    /**
     * @return the page to which the document belongs to (eg "WebHome")
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    String getPageName();
    
    /**
     * Return the full local space reference. For example a document located in sub-space <code>space11</code> of space
     * <code>space1</code> will return <code>space1.space11</code>.
     * 
     * @return the space to which the document belongs to (eg "Main")
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    String getSpaceName();

    /**
     * Retrieve the name of the virtual wiki this document belongs to.
     *
     * @return A <code>String</code> representation of the document's wiki name.
     * @deprecated since 2.2M1 use {@link #getDocumentReference()}  instead
     */
    @Deprecated
    String getWikiName();

    /**
     * @return the document's title or null if not set
     */
    String getTitle();

    /**
     * @return the prepared version of the title (for example in case of Velocity the compiled VelocityTemplate
     *         instance)
     * @since 16.3.0RC1
     */
    default Object getPreparedTitle()
    {
        return null;
    }

    /**
     * @param preparedTitle the prepared version of the title (for example in case of Velocity the compiled
     *            VelocityTemplate instance)
     * @since 16.3.0RC1
     */
    default void setPreparedTitle(Object preparedTitle)
    {
        
    }

    /**
     * @return a string identifying the current version of this document
     */
    String getVersion();

    /**
     * Return a cloned version of the document content as {@link XDOM}.
     * 
     * @return the XDOM for the document
     * @since 3.0M3
     */
    XDOM getXDOM();

    /**
     * Return a cloned and prepared version of the document content as {@link XDOM}.
     * 
     * @return the prepared version of the XDOM
     * @since 16.3.0RC1
     */
    default XDOM getPreparedXDOM()
    {
        return getXDOM();
    }

    /**
     * @return the document's content author user reference
     * @since 7.2M1
     * @deprecated since 14.0RC1 rely on {@link #getAuthors()}.
     */
    @Deprecated
    DocumentReference getContentAuthorReference();

    /**
     * @return the various authors information of a document.
     * @since 14.0RC1
     */
    default DocumentAuthors getAuthors()
    {
        return null;
    }

    /**
     * @return the creation date of the current document.
     * @since 12.8RC1
     * @since 12.6.3
     */
    default Date getCreationDate()
    {
        return null;
    }

    /**
     * @return {@code true} if the document is hidden.
     * @since 13.1
     * @since 12.10.5
     * @since 12.6.8
     */
    default Boolean isHidden()
    {
        return false;
    }

    /**
     * @return the last save date of the current document.
     * @since 14.0RC1
     */
    default Date getDate()
    {
        return null;
    }

    /**
     * @return {@code true} if the document is restricted, i.e., transformations should be executed in restricted mode
     * @since 15.2RC1
     * @since 14.10.7
     */
    @Unstable
    default boolean isRestricted()
    {
        return false;
    }
}
