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

import java.io.StringReader;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link HeadingNameNamingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
@ComponentTest
@AllComponents
class HeadingNameNamingCriterionTest
{
    @InjectMockComponents
    private HeadingNameNamingCriterion namingCriterion;

    @Inject
    @Named("xwiki/2.1")
    private Parser xwikiParser;

    @MockComponent
    private DocumentAccessBridge docBridge;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getDocumentReference() throws Exception
    {
        this.namingCriterion.getParameters()
            .setBaseDocumentReference(new DocumentReference("test", "Parent", "WebHome"));

        XDOM xdom = this.xwikiParser.parse(new StringReader("=Child="));
        Block sectionBlock = xdom.getChildren().get(0);
        // Test normal heading-name naming for nested page.
        assertEquals(new DocumentReference("test", Arrays.asList("Parent", "Child"), "WebHome"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));

        this.namingCriterion.getParameters().setUseTerminalPages(true);
        assertEquals(new DocumentReference("test", "Parent", "Child"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));

        this.namingCriterion.getParameters().setBaseDocumentReference(new DocumentReference("test", "Test", "Test"));

        xdom = this.xwikiParser.parse(new StringReader("=Heading="));
        sectionBlock = xdom.getChildren().get(0);
        // Test normal heading-name naming for terminal page.
        assertEquals(new DocumentReference("test", "Test", "Heading"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));

        this.namingCriterion.getParameters().setUseTerminalPages(false);
        assertEquals(new DocumentReference("test", Arrays.asList("Test", "Heading"), "WebHome"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));
        this.namingCriterion.getParameters().setUseTerminalPages(true);

        // Test name clash resolution
        assertEquals(new DocumentReference("test", "Test", "Heading-1"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));
        when(this.docBridge.exists(new DocumentReference("test", "Test", "Heading-2"))).thenReturn(true);
        assertEquals(new DocumentReference("test", "Test", "Heading-3"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));
        when(this.docBridge.exists(new DocumentReference("test", "Test", "Heading-4")))
            .thenThrow(new RuntimeException("Reason"));
        assertEquals(new DocumentReference("test", "Test", "Heading-4"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));
        assertEquals("Failed to check the existence of the document with reference [test:Test.Heading-4]."
            + " Root cause is [RuntimeException: Reason].", this.logCapture.getMessage(0));

        // Test prepend base page name.
        this.namingCriterion.getParameters().setParameter(HeadingNameNamingCriterion.PARAM_PREPEND_BASE_PAGE_NAME,
            true);
        assertEquals(new DocumentReference("test", "Test", "Test-Heading"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));
        assertEquals(new DocumentReference("test", "Test", "Test-Heading-1"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));
        this.namingCriterion.getParameters().setParameter(HeadingNameNamingCriterion.PARAM_PREPEND_BASE_PAGE_NAME,
            false);

        // Test heading text cleaning (replacing)
        xdom = this.xwikiParser.parse(new StringReader("= This-Very.Weird:Heading! ="));
        sectionBlock = xdom.getChildren().get(0);
        assertEquals(new DocumentReference("test", "Test", "This-Very-Weird-Heading!"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));

        // Test heading text cleaning (stripping)
        xdom = this.xwikiParser.parse(new StringReader("= This?Is@A/Very#Weird~Heading ="));
        sectionBlock = xdom.getChildren().get(0);
        assertEquals(new DocumentReference("test", "Test", "ThisIsAVeryWeirdHeading"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));

        // Test page name truncation.
        xdom = this.xwikiParser
            .parse(new StringReader("=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa="));
        sectionBlock = xdom.getChildren().get(0);
        assertEquals(768,
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())).toString().length());
        // Test fallback operation
        assertEquals(new DocumentReference("test", "Test", "Test-1"), this.namingCriterion.getDocumentReference(xdom));

        // Test fallback operation under empty heading names
        xdom = this.xwikiParser.parse(new StringReader("=   ="));
        sectionBlock = xdom.getChildren().get(0);
        assertEquals(new DocumentReference("test", "Test", "Test-2"),
            this.namingCriterion.getDocumentReference(new XDOM(sectionBlock.getChildren())));
    }

    @Test
    void getDocumentName() throws Exception
    {
        this.namingCriterion.getParameters()
            .setBaseDocumentReference(new DocumentReference("test", "Parent", "WebHome"));

        XDOM xdom = new XDOM(Arrays.asList(new HeaderBlock(Arrays.asList(new WordBlock("Child")), HeaderLevel.LEVEL1)));
        assertEquals("test:Parent.Child.WebHome", this.namingCriterion.getDocumentName(xdom));
    }
}
