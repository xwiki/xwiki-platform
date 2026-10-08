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
package org.xwiki.store;

import java.util.Locale;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;

/**
 * Thrown when a document can't be saved because it isn't based on the revision currently stored: the document was
 * created, modified or deleted by another request (possibly on another cluster node) after it was loaded. Saving it
 * anyway would overwrite the changes of the other request, so the caller should load the document again, re-apply its
 * changes and save it again.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Unstable
public class DocumentRevisionConflictException extends XWikiException
{
    private static final long serialVersionUID = 1L;

    private final transient DocumentReference documentReference;

    private final String expectedRevision;

    private final String storedRevision;

    /**
     * @param documentReference the reference of the document that couldn't be saved, including its locale
     * @param expectedRevision the revision the saved document is based on, {@code null} if it is based on a document
     *     that doesn't exist
     * @param storedRevision the revision currently stored, {@code null} if the document doesn't exist
     */
    public DocumentRevisionConflictException(DocumentReference documentReference, String expectedRevision,
        String storedRevision)
    {
        this(documentReference, expectedRevision, storedRevision, null);
    }

    /**
     * @param documentReference the reference of the document that couldn't be saved, including its locale
     * @param expectedRevision the revision the saved document is based on, {@code null} if it is based on a document
     *     that doesn't exist
     * @param storedRevision the revision currently stored, {@code null} if the document doesn't exist
     * @param cause the failure caused by the conflict, e.g. a database constraint violation
     */
    public DocumentRevisionConflictException(DocumentReference documentReference, String expectedRevision,
        String storedRevision, Throwable cause)
    {
        super(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC_REVISION_CONFLICT,
            "Document {0} was modified concurrently: the saved document is based on revision {1} but the stored"
                + " revision is {2}",
            cause,
            new Object[] { describe(documentReference), describe(expectedRevision), describe(storedRevision) });

        this.documentReference = documentReference;
        this.expectedRevision = expectedRevision;
        this.storedRevision = storedRevision;
    }

    private static DocumentReference describe(DocumentReference documentReference)
    {
        // Show the locale only for translations.
        Locale locale = documentReference.getLocale();
        return locale == null || Locale.ROOT.equals(locale) ? new DocumentReference(documentReference, (Locale) null)
            : documentReference;
    }

    private static String describe(String revision)
    {
        return revision != null ? '[' + revision + ']' : "[none]";
    }

    /**
     * @return the reference of the document that couldn't be saved, including its locale
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     * @return the revision the saved document is based on, {@code null} if it is based on a document that doesn't exist
     */
    public String getExpectedRevision()
    {
        return this.expectedRevision;
    }

    /**
     * @return the revision currently stored, {@code null} if the document doesn't exist
     */
    public String getStoredRevision()
    {
        return this.storedRevision;
    }
}
