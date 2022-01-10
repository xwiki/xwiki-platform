package org.xwiki.model.internal.document;/*
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

import org.junit.jupiter.api.Test;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultDocumentAuthors}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
class DefaultDocumentAuthorsTest
{
    @Test
    void constructorClone()
    {
        UserReference contentAuthorRef = mock(UserReference.class);
        UserReference creatorRef = mock(UserReference.class);
        UserReference metadataAuthorRef = mock(UserReference.class);
        UserReference displayedAuthorRef = mock(UserReference.class);
        DefaultDocumentAuthors documentAuthors = new DefaultDocumentAuthors();
        documentAuthors.setCreator(creatorRef);
        documentAuthors.setContentAuthor(contentAuthorRef);
        documentAuthors.setEffectiveMetadataAuthor(metadataAuthorRef);
        documentAuthors.setOriginalMetadataAuthor(displayedAuthorRef);

        DefaultDocumentAuthors otherAuthors = new DefaultDocumentAuthors(documentAuthors);
        assertEquals(documentAuthors, otherAuthors);
    }

    @Test
    void getDisplayedAuthor()
    {
        UserReference contentAuthorRef = mock(UserReference.class);
        UserReference creatorRef = mock(UserReference.class);
        UserReference metadataAuthorRef = mock(UserReference.class);
        UserReference displayedAuthorRef = mock(UserReference.class);
        DefaultDocumentAuthors documentAuthors = new DefaultDocumentAuthors();
        documentAuthors.setCreator(creatorRef);
        documentAuthors.setContentAuthor(contentAuthorRef);
        documentAuthors.setEffectiveMetadataAuthor(metadataAuthorRef);
        documentAuthors.setOriginalMetadataAuthor(displayedAuthorRef);
        assertSame(displayedAuthorRef, documentAuthors.getOriginalMetadataAuthor());

        documentAuthors = new DefaultDocumentAuthors();
        documentAuthors.setCreator(creatorRef);
        documentAuthors.setContentAuthor(contentAuthorRef);
        documentAuthors.setEffectiveMetadataAuthor(metadataAuthorRef);
        assertSame(metadataAuthorRef, documentAuthors.getOriginalMetadataAuthor());
    }
}
