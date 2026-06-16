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
package org.xwiki.index.tree;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IndexTreeScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class IndexTreeScriptServiceTest
{
    @InjectMockComponents
    private IndexTreeScriptService indexTreeScriptService;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    @Test
    void normalizeEntityTreeNodeId()
    {
        // Null safe check.
        assertNull(this.indexTreeScriptService.normalizeEntityTreeNodeId(null));
        assertEquals(0, this.logCapture.size());

        // Normalize the id of a pseudo entity tree node (no associated entity).
        assertEquals("farm:*", this.indexTreeScriptService.normalizeEntityTreeNodeId("farm:*"));
        assertEquals("noEntityTreeNodeId", this.indexTreeScriptService.normalizeEntityTreeNodeId("noEntityTreeNodeId"));

        // Normalize an actual entity (attachment) tree node id.
        AttachmentReference attachmentReference =
            new AttachmentReference("image.png", new DocumentReference("wiki", "Current", "Page"));
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "attachment:image.png"))
            .thenReturn(attachmentReference);
        when(this.entityTreeNodeIdConverter.convert(String.class, attachmentReference))
            .thenReturn("attachment:wiki:Current.Page@image.png");
        assertEquals("attachment:wiki:Current.Page@image.png",
            this.indexTreeScriptService.normalizeEntityTreeNodeId("attachment:image.png"));
        assertEquals(0, this.logCapture.size());

        // Normalize a document pseudo tree node id.
        DocumentReference documentReference = new DocumentReference("wiki", "Current", "OtherPage");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:OtherPage"))
            .thenReturn(documentReference);
        when(this.entityTreeNodeIdConverter.convert(String.class, documentReference))
            .thenReturn("document:wiki:Current.OtherPage");
        assertEquals("translations:wiki:Current.OtherPage",
            this.indexTreeScriptService.normalizeEntityTreeNodeId("tranSLations:OtherPage"));
        assertEquals(0, this.logCapture.size());

        // Try to normalize an entity tree node id for which the converter fails to convert the node id to an entity
        // reference.
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "attachment:bad.png")).thenReturn(null);
        assertEquals("attachment:bad.png", this.indexTreeScriptService.normalizeEntityTreeNodeId("attachment:bad.png"));
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to normalize the given entity tree node id [attachment:bad.png].",
            this.logCapture.getMessage(0));

        // Try to normalize a document pseudo tree node id for which the converter fails to convert the document node id
        // to a document reference.
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:bad.png")).thenReturn(null);
        assertEquals("translations:bad.png",
            this.indexTreeScriptService.normalizeEntityTreeNodeId("translations:bad.png"));
        assertEquals(2, this.logCapture.size());
        assertEquals("Failed to normalize the given entity tree node id [translations:bad.png].",
            this.logCapture.getMessage(1));
    }
}
