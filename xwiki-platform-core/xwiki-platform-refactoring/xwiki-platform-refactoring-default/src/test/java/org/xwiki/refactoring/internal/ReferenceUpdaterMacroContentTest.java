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
package org.xwiki.refactoring.internal;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.job.JobContext;
import org.xwiki.model.internal.reference.DefaultReferenceAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringAttachmentReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.rendering.internal.parser.LinkParser;
import org.xwiki.rendering.internal.resolver.AttachmentResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.resolver.DefaultResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.resolver.DocumentResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.resolver.RelativeAttachmentResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.resolver.RelativeDocumentResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.resolver.RelativeResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentDocumentReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentPageReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentSpaceReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentWikiReferenceProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the reference written into the content of a macro by a rename is escaped, so that a name containing the
 * closing marker of that macro cannot close it in the middle of the reference.
 * <p>
 * Both a document rename and an attachment rename end up in
 * {@link ReferenceUpdater#update(DocumentReference, EntityReference, EntityReference)}, which is what this test
 * drives, with a real parser and a real renderer.
 *
 * @version $Id$
 */
@ComponentList({
    DefaultReferenceUpdater.class,
    DefaultReferenceRenamer.class,
    ResourceReferenceRenamer.class,
    DefaultMacroRefactoring.class,
    DefaultResourceReferenceEntityReferenceResolver.class,
    DocumentResourceReferenceEntityReferenceResolver.class,
    AttachmentResourceReferenceEntityReferenceResolver.class,
    RelativeResourceReferenceEntityReferenceResolver.class,
    RelativeDocumentResourceReferenceEntityReferenceResolver.class,
    RelativeAttachmentResourceReferenceEntityReferenceResolver.class,
    LinkParser.class,
    CurrentDocumentReferenceProvider.class,
    CurrentSpaceReferenceProvider.class,
    CurrentPageReferenceProvider.class,
    CurrentWikiReferenceProvider.class,
    DefaultReferenceAttachmentReferenceResolver.class,
    DefaultStringAttachmentReferenceResolver.class,
    InfoMessageMacro.class
})
@XWikiSyntax21ComponentList
@DefaultRenderingConfigurationComponentList
class ReferenceUpdaterMacroContentTest extends PageTest
{
    private static final DocumentReference LINKING_DOCUMENT = new DocumentReference("xwiki", "Space", "Linking");

    private static final DocumentReference TARGET_DOCUMENT = new DocumentReference("xwiki", "Space", "Target");

    private static final String LAST_LINE = "End of the box.";

    private static final String CONTENT_FORMAT = "{{info}}\n"
        + "Beginning of the box: [[the link label>>%1$s]]\n"
        + "\n"
        + "Attachment: [[the attachment label>>attach:%2$s]]\n"
        + "\n"
        + "Image: [[image:%2$s]]\n"
        + "\n"
        + LAST_LINE + "\n"
        + "{{/info}}";

    /**
     * Mocked because the reference update is driven directly instead of through a refactoring job.
     */
    @MockComponent
    private JobContext jobContext;

    /**
     * Mocked because only its presence matters here: it is what tells the reference parsers that they are parsing
     * wiki references and not URLs.
     */
    @MockComponent
    private WikiModel wikiModel;

    @Test
    void updateDocumentReferenceWithMacroSyntaxInTheName() throws Exception
    {
        DocumentReference newTarget = new DocumentReference("xwiki", "Space", "{{/info}}Target");

        String updatedContent = update(String.format(CONTENT_FORMAT, "Target", "Target@file.txt"), TARGET_DOCUMENT,
            newTarget);

        assertContentIsStillInsideTheMacro(updatedContent);
        assertEquals(String.format(CONTENT_FORMAT, "~{~{/info}}Target", "~{~{/info}}Target@file.txt"),
            updatedContent);
    }

    @Test
    void updateAttachmentReferenceWithMacroSyntaxInTheName() throws Exception
    {
        AttachmentReference oldAttachment = new AttachmentReference("file.txt", TARGET_DOCUMENT);
        AttachmentReference newAttachment = new AttachmentReference("{{/info}}file.txt", TARGET_DOCUMENT);

        String updatedContent = update(String.format(CONTENT_FORMAT, "Target", "Target@file.txt"), oldAttachment,
            newAttachment);

        assertContentIsStillInsideTheMacro(updatedContent);
        assertEquals(String.format(CONTENT_FORMAT, "Target", "Target@~{~{/info}}file.txt"), updatedContent);
    }

    /**
     * Saves the passed content in the linking document, updates the references it holds and returns the content that
     * has been saved back.
     *
     * @param content the content of the linking document before the rename
     * @param oldReference the reference of the renamed entity
     * @param newReference the new reference of the renamed entity
     * @return the content of the linking document after the rename
     */
    private String update(String content, EntityReference oldReference, EntityReference newReference) throws Exception
    {
        // The renamed document must exist, otherwise the reference of a link that points to it is resolved to the
        // non-terminal document of the same name instead and thus doesn't match the renamed reference.
        this.xwiki.saveDocument(new XWikiDocument(TARGET_DOCUMENT), this.context);

        XWikiDocument document = new XWikiDocument(LINKING_DOCUMENT);
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent(content);
        this.xwiki.saveDocument(document, this.context);

        ReferenceUpdater updater = this.componentManager.getInstance(ReferenceUpdater.class);
        updater.update(LINKING_DOCUMENT, oldReference, newReference);

        return this.xwiki.getDocument(LINKING_DOCUMENT, this.context).getContent();
    }

    /**
     * Asserts that the content parses back to a single macro that holds all of it, i.e. that the new name didn't close
     * the macro in the middle of the reference.
     *
     * @param content the content to check
     */
    private void assertContentIsStillInsideTheMacro(String content) throws Exception
    {
        Parser parser = this.componentManager.getInstance(Parser.class, Syntax.XWIKI_2_1.toIdString());
        XDOM xdom = parser.parse(new StringReader(content));

        List<MacroBlock> macros = xdom.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT);
        assertEquals(1, macros.size(), "The content must parse back to a single macro.");
        assertTrue(macros.get(0).getContent().contains(LAST_LINE),
            "The whole content must still be inside the macro, but the macro only contains ["
                + macros.get(0).getContent() + "].");
    }
}
