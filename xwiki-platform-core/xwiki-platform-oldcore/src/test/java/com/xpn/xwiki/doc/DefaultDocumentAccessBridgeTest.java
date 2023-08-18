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
package com.xpn.xwiki.doc;

import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for {@link DefaultDocumentAccessBridge}.
 *
 * @version $Id$
 */
@OldcoreTest
class DefaultDocumentAccessBridgeTest
{
    @InjectMockComponents
    private DefaultDocumentAccessBridge documentAccessBridge;

    @Test
    void testGetUrlEmptyDocument(MockitoOldcore oldcore)
    {
        DocumentReference documentReference = new DocumentReference("Wiki", "Space", "Page");
        XWikiDocument document = new XWikiDocument(documentReference);
        oldcore.getXWikiContext().setDoc(document);
        String action = "view";
        String expectedURL = "/xwiki/bin/view/Main/WebHome";
        doReturn(expectedURL)
            .when(oldcore.getSpyXWiki()).getURL(eq(document.getFullName()), eq(action), anyString(), anyString(),
                eq(oldcore.getXWikiContext()));

        assertEquals(expectedURL, this.documentAccessBridge.getURL("", action, "", ""));
        assertEquals(expectedURL, this.documentAccessBridge.getURL(null, action, "", ""));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void setAttachmentContent(boolean useStream, MockitoOldcore oldcore) throws Exception
    {
        DocumentReference documentReference = new DocumentReference("Wiki", "Space", "Page");
        String fileName = "image.png";
        AttachmentReference attachmentReference = new AttachmentReference(fileName, documentReference);
        byte[] attachmentContent = new byte[] { 42, 23 };
        if (useStream) {
            this.documentAccessBridge.setAttachmentContent(attachmentReference,
                new ByteArrayInputStream(attachmentContent));
        } else {
            this.documentAccessBridge.setAttachmentContent(attachmentReference, attachmentContent);
        }
        XWikiDocument document = oldcore.getSpyXWiki().getDocument(documentReference, oldcore.getXWikiContext());
        XWikiAttachment attachment = document.getAttachment(fileName);
        assertNotNull(attachment);
        assertEquals(fileName, attachment.getFilename());
        assertArrayEquals(attachmentContent,
            IOUtils.toByteArray(attachment.getAttachmentContent(oldcore.getXWikiContext()).getContentInputStream()));
    }

    @Test
    void getCurrentDocumentReference(MockitoOldcore oldcore)
    {
        assertNull(this.documentAccessBridge.getCurrentDocumentReference());

        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        XWikiDocument document = new XWikiDocument(documentReference);
        oldcore.getXWikiContext().setDoc(document);

        assertEquals(documentReference, this.documentAccessBridge.getCurrentDocumentReference());
    }
}
