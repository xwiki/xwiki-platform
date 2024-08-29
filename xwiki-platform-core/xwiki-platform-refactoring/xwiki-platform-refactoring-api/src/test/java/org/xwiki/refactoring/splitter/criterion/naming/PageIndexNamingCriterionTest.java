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
package org.xwiki.refactoring.splitter.criterion.naming;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link PageIndexNamingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
@ComponentTest
class PageIndexNamingCriterionTest
{
    @InjectMockComponents
    private PageIndexNamingCriterion namingCriterion;

    @MockComponent
    private DocumentAccessBridge docBridge;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getDocumentReference() throws Exception
    {
        this.namingCriterion.getParameters().setBaseDocumentReference(new DocumentReference("test", "Some", "Page"));
        this.namingCriterion.getParameters().setUseTerminalPages(true);

        XDOM xdom = new XDOM(Arrays.asList(new HeaderBlock(Arrays.asList(new WordBlock("Title")), HeaderLevel.LEVEL1)));
        for (int i = 1; i < 10; i++) {
            assertEquals(new DocumentReference("test", "Some", "Page-" + i),
                this.namingCriterion.getDocumentReference(xdom));
        }

        this.namingCriterion.getParameters().setUseTerminalPages(false);

        when(this.docBridge.exists(new DocumentReference("test", Arrays.asList("Some", "Page-10"), "WebHome")))
            .thenReturn(true);
        when(this.docBridge.exists(new DocumentReference("test", Arrays.asList("Some", "Page-10-1"), "WebHome")))
            .thenThrow(new RuntimeException("Reason"));
        assertEquals(new DocumentReference("test", Arrays.asList("Some", "Page-10-1"), "WebHome"),
            this.namingCriterion.getDocumentReference(xdom));
        assertEquals("Failed to check the existence of the document with reference [test:Some.Page-10-1.WebHome]."
            + " Root cause is [RuntimeException: Reason].", this.logCapture.getMessage(0));

        this.namingCriterion.getParameters()
            .setBaseDocumentReference(new DocumentReference("test", "TopPage", "WebHome"));
        assertEquals(new DocumentReference("test", Arrays.asList("TopPage", "TopPage-11"), "WebHome"),
            this.namingCriterion.getDocumentReference(xdom));

        this.namingCriterion.getParameters().setUseTerminalPages(true);
        assertEquals(new DocumentReference("test", "TopPage", "TopPage-12"),
            this.namingCriterion.getDocumentReference(xdom));
    }
}
