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
package org.xwiki.model.internal.document;

import org.junit.jupiter.api.Test;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultDocumentAuthors}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
class DefaultDocumentAuthorsTest
{
    @Test
    void copyAuthors()
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        UserReference contentAuthorRef = mock(UserReference.class);
        UserReference creatorRef = mock(UserReference.class);
        UserReference metadataAuthorRef = mock(UserReference.class);
        UserReference displayedAuthorRef = mock(UserReference.class);
        DefaultDocumentAuthors documentAuthors = new DefaultDocumentAuthors(xWikiDocument);
        documentAuthors.setCreator(creatorRef);
        documentAuthors.setContentAuthor(contentAuthorRef);
        documentAuthors.setEffectiveMetadataAuthor(metadataAuthorRef);
        documentAuthors.setOriginalMetadataAuthor(displayedAuthorRef);

        DefaultDocumentAuthors otherDocumentAuthors = new DefaultDocumentAuthors(xWikiDocument);
        assertNotEquals(documentAuthors, otherDocumentAuthors);
        otherDocumentAuthors.copyAuthors(documentAuthors);
        assertEquals(otherDocumentAuthors, documentAuthors);
    }

    @Test
    void getOriginalMetadataAuthor()
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        UserReference contentAuthorRef = mock(UserReference.class);
        UserReference creatorRef = mock(UserReference.class);
        UserReference metadataAuthorRef = mock(UserReference.class);
        UserReference displayedAuthorRef = mock(UserReference.class);
        DefaultDocumentAuthors documentAuthors = new DefaultDocumentAuthors(xWikiDocument);
        documentAuthors.setCreator(creatorRef);
        documentAuthors.setContentAuthor(contentAuthorRef);
        documentAuthors.setEffectiveMetadataAuthor(metadataAuthorRef);
        documentAuthors.setOriginalMetadataAuthor(displayedAuthorRef);
        assertSame(displayedAuthorRef, documentAuthors.getOriginalMetadataAuthor());

        documentAuthors = new DefaultDocumentAuthors(xWikiDocument);
        documentAuthors.setCreator(creatorRef);
        documentAuthors.setContentAuthor(contentAuthorRef);
        documentAuthors.setEffectiveMetadataAuthor(metadataAuthorRef);
        assertSame(GuestUserReference.INSTANCE, documentAuthors.getOriginalMetadataAuthor());
    }

    @Test
    void setEffectiveMetadataAuthor()
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        UserReference effectiveMetadataAuthor = mock(UserReference.class);
        DefaultDocumentAuthors documentAuthors = new DefaultDocumentAuthors(xWikiDocument);
        documentAuthors.setEffectiveMetadataAuthor(effectiveMetadataAuthor);
        verify(xWikiDocument).setMetaDataDirty(true);

        documentAuthors.setEffectiveMetadataAuthor(effectiveMetadataAuthor);
        // should have been triggered only once since it's not modified again
        verify(xWikiDocument).setMetaDataDirty(true);

        documentAuthors.setEffectiveMetadataAuthor(mock(UserReference.class));
        // not the same author, so should be triggered once more
        verify(xWikiDocument, times(2)).setMetaDataDirty(true);
    }
}
