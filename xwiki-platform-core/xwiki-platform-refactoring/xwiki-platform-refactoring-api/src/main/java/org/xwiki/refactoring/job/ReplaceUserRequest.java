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
package org.xwiki.refactoring.job;

import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;

/**
 * A job request that can be used to replace user references.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
public class ReplaceUserRequest extends EntityRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getOldUserReference()
     */
    private static final String PROPERTY_OLD_USER_REFERENCE = "oldUserReference";

    /**
     * @see #getNewUserReference()
     */
    private static final String PROPERTY_NEW_USER_REFERENCE = "newUserReference";

    /**
     * @see #isReplaceDocumentAuthor()
     */
    private static final String PROPERTY_REPLACE_DOCUMENT_AUTHOR = "replaceDocumentAuthor";

    /**
     * @see #isReplaceDocumentContentAuthor()
     */
    private static final String PROPERTY_REPLACE_DOCUMENT_CONTENT_AUTHOR = "replaceDocumentContentAuthor";

    /**
     * @see #isReplaceDocumentCreator()
     */
    private static final String PROPERTY_REPLACE_DOCUMENT_CREATOR = "replaceDocumentCreator";

    /**
     * Default constructor.
     */
    public ReplaceUserRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public ReplaceUserRequest(Request request)
    {
        super(request);
    }

    /**
     * Sets the user reference to replace.
     * 
     * @param oldUserReference the user reference to replace
     */
    public void setOldUserReference(DocumentReference oldUserReference)
    {
        setProperty(PROPERTY_OLD_USER_REFERENCE, oldUserReference);
    }

    /**
     * @return the user reference to replace
     */
    public DocumentReference getOldUserReference()
    {
        return getProperty(PROPERTY_OLD_USER_REFERENCE);
    }

    /**
     * Sets the user reference replacement.
     * 
     * @param newUserRefernce the user reference replacement
     */
    public void setNewUserReference(DocumentReference newUserRefernce)
    {
        setProperty(PROPERTY_NEW_USER_REFERENCE, newUserRefernce);
    }

    /**
     * @return the user reference replacement
     */
    public DocumentReference getNewUserReference()
    {
        return getProperty(PROPERTY_NEW_USER_REFERENCE);
    }

    /**
     * Sets whether to replace the author of existing pages.
     * 
     * @param replace {@code true} to replace the page author, {@code false} to preserve it
     */
    public void setReplaceDocumentAuthor(boolean replace)
    {
        setProperty(PROPERTY_REPLACE_DOCUMENT_AUTHOR, replace);
    }

    /**
     * @return whether to replace the author of existing pages
     */
    public boolean isReplaceDocumentAuthor()
    {
        return getProperty(PROPERTY_REPLACE_DOCUMENT_AUTHOR, false);
    }

    /**
     * Sets whether to replace the content author of existing pages.
     * 
     * @param replace {@code true} to replace the page content author, {@code false} to preserve it
     */
    public void setReplaceDocumentContentAuthor(boolean replace)
    {
        setProperty(PROPERTY_REPLACE_DOCUMENT_CONTENT_AUTHOR, replace);
    }

    /**
     * @return whether to replace the content author of existing pages
     */
    public boolean isReplaceDocumentContentAuthor()
    {
        return getProperty(PROPERTY_REPLACE_DOCUMENT_CONTENT_AUTHOR, false);
    }

    /**
     * Sets whether to replace the creator of existing pages.
     * 
     * @param replace {@code true} to replace the page creator, {@code false} to preserve it
     */
    public void setReplaceDocumentCreator(boolean replace)
    {
        setProperty(PROPERTY_REPLACE_DOCUMENT_CREATOR, replace);
    }

    /**
     * @return whether to replace the creator of existing pages
     */
    public boolean isReplaceDocumentCreator()
    {
        return getProperty(PROPERTY_REPLACE_DOCUMENT_CREATOR, false);
    }
}
