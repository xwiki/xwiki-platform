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
package org.xwiki.store.filesystem.internal;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.UnexpectedException;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachments}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(TestEnvironment.class)
class TemporaryAttachmentsTest
{
    @BeforeEach
    void setup(MockitoComponentManager componentManager)
    {
        // The local copy of the content is a temporary file of the environment.
        Utils.setComponentManager(componentManager);
    }

    @Test
    void attachWithUnreadableContent()
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Space", "Page"));
        XWikiAttachment uploaded = new XWikiAttachment();
        uploaded.setFilename("file.txt");
        XWikiAttachmentContent content = mock(XWikiAttachmentContent.class);
        when(content.getContentInputStream()).thenThrow(new UnexpectedException("blob deleted"));
        uploaded.setAttachment_content(content);

        UnexpectedException exception =
            assertThrows(UnexpectedException.class, () -> TemporaryAttachments.attach(document, uploaded));

        assertEquals("Failed to read the temporary attachment [file.txt] of [xwiki:Space.Page]",
            exception.getMessage());
        verify(document, never()).setAttachment(any());
    }

    @Test
    void attachWithoutPreviousAttachment()
    {
        XWikiDocument document = mock(XWikiDocument.class);
        XWikiAttachment uploaded = new XWikiAttachment();
        uploaded.setFilename("file.txt");
        XWikiAttachmentContent content = mock(XWikiAttachmentContent.class);
        when(content.getContentInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        uploaded.setAttachment_content(content);

        TemporaryAttachments.attach(document, uploaded);

        verify(document).setAttachment(any());
    }
}
