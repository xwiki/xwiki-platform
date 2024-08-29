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
package org.xwiki.attachment.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.attachment.configuration.AttachmentConfiguration;
import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.attachment.refactoring.MoveAttachmentRequest;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;

import ch.qos.logback.classic.Level;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Test of {@link AttachmentScriptService}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@ComponentTest
class AttachmentScriptServiceTest
{
    private static final DocumentReference SOURCE_LOCATION = new DocumentReference("xwiki", "Space", "Source");

    private static final DocumentReference TARGET_LOCATION = new DocumentReference("xwiki", "Space", "Target");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User1");

    @InjectMockComponents
    private AttachmentScriptService attachmentScriptService;

    @MockComponent
    private AttachmentsManager attachmentsManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void createMoveRequest()
    {
        MoveAttachmentRequest actual =
            this.attachmentScriptService.createMoveRequest(SOURCE_LOCATION, "old.txt", TARGET_LOCATION, "newName",
                USER_REFERENCE, true,
                false);
        MoveAttachmentRequest expected = new MoveAttachmentRequest();
        expected.setEntityReferences(singletonList(new AttachmentReference("old.txt", SOURCE_LOCATION)));
        expected.setProperty(MoveAttachmentRequest.DESTINATION, new AttachmentReference("newName", TARGET_LOCATION));
        expected.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, true);
        expected.setInteractive(true);
        expected.setProperty(MoveAttachmentRequest.UPDATE_REFERENCES, false);
        expected.setUserReference(USER_REFERENCE);
        // Reset the ids before comparing because we are not interested in the random parts. 
        actual.setId("");
        expected.setId("");
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void locationAvailable(boolean available) throws Exception
    {
        when(this.attachmentsManager.available(new AttachmentReference("newName", TARGET_LOCATION))).thenReturn(
            available);
        assertEquals(available, this.attachmentScriptService.locationAvailable(TARGET_LOCATION, "newName"));
    }

    @Test
    void locationAvailableError() throws Exception
    {
        when(this.attachmentsManager.available(new AttachmentReference("newName", TARGET_LOCATION)))
            .thenThrow(new XWikiException());
        assertFalse(this.attachmentScriptService.locationAvailable(TARGET_LOCATION, "newName"));
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "Failed to check if [newName] exists [xwiki:Space.Target]. Cause: [XWikiException: Error number 0 in 0].",
            this.logCapture.getMessage(0));
    }

    @Test
    void getDefaultConfiguration()
    {
        AttachmentConfiguration configuration = this.attachmentScriptService.getConfiguration();
        assertFalse(configuration.isCommentsEnabled());
    }
}
