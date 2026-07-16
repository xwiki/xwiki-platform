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
package org.xwiki.web;

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.criteria.impl.XWikiCriteriaServiceImpl;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code changes.vm} template.
 *
 * @version $Id$
 */
class ChangesPageTest extends PageTest
{
    private static final String CHANGES_VM = "changes.vm";

    private static final String INVALID_REVISION = "xar:'>Invalid<'";

    private static final String ENCODED_INVALID_REVISION = "xar%3A%27%3EInvalid%3C%27";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "TestSpace", "TestPage");

    private TemplateManager templateManager;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        this.xwiki.setCriteriaService(new XWikiCriteriaServiceImpl(this.context));

        // Create several revisions of the document for testing
        createDocumentRevisions();

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context));

        // Get the mock database revision provider that was set up by MockitoOldcore.
        DocumentRevisionProvider databaseRevisionProvider =
            this.oldcore.getMocker().getInstance(DocumentRevisionProvider.class, "database");

        // Mock the default revision provider to handle xar: revision numbers for the test document.
        when(this.documentRevisionProvider.getRevision(any(DocumentReference.class), anyString()))
            .thenAnswer(invocation -> {
                DocumentReference reference = invocation.getArgument(0);
                String revision = invocation.getArgument(1);
                if (Strings.CS.startsWith(revision, "xar:")
                    && DOCUMENT_REFERENCE.equals(reference.withoutLocale())) {
                    return this.context.getDoc();
                }
                return databaseRevisionProvider.getRevision(reference, revision);
            });
    }

    private void createDocumentRevisions() throws Exception
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setTitle("Test Page for Changes");

        // Revision 1.1.
        document.setContent("Initial content");
        document.setComment("Initial version");
        this.xwiki.saveDocument(document, this.context);

        // Revision 2.1.
        document.setContent("Initial content\nSecond line");
        document.setComment("Added second line");
        this.xwiki.saveDocument(document, this.context);

        // Revision 3.1.
        document.setContent("Initial content\nSecond line\nThird line");
        document.setComment("Added third line");
        this.xwiki.saveDocument(document, this.context);

        // Revision 4.1.
        document.setContent("Initial content\nSecond line\nFourth line");
        document.setComment("Added fourth line");
        this.xwiki.saveDocument(document, this.context);
    }

    @Test
    void renderChangesTemplateWithBasicParameters() throws Exception
    {
        this.stubRequest.put("rev1", "1.1");
        this.stubRequest.put("rev2", "2.1");

        String result = this.templateManager.render(CHANGES_VM);

        assertTrue(result.contains("document-title"));
        assertTrue(result.contains("Test Page for Changes"));
    }

    @Test
    void renderChangesTemplateWithInvalidRev2Parameters() throws Exception
    {
        // Set up request parameters for changes view with invalid revision numbers
        this.stubRequest.put("rev1", "2.1");
        this.stubRequest.put("rev2", INVALID_REVISION);

        String result = this.templateManager.render(CHANGES_VM);

        // Verify that the revision has been sanitized.
        assertThat(result, not(containsString(INVALID_REVISION)));
        // Verify that the URL-escaped revision is present in the result.
        assertThat(result, containsString(ENCODED_INVALID_REVISION));
    }

    @Test
    void renderChangesTemplateWithInvalidRev1Parameters() throws Exception
    {
        // Set up request parameters for changes view with invalid revision numbers
        this.stubRequest.put("rev1", INVALID_REVISION);
        this.stubRequest.put("rev2", "3.1");

        String result = this.templateManager.render(CHANGES_VM);

        // Verify that the revision has been sanitized.
        assertThat(result, not(containsString(INVALID_REVISION)));
        // Verify that the URL-escaped revision is present in the result.
        assertThat(result, containsString(ENCODED_INVALID_REVISION));
    }
}
