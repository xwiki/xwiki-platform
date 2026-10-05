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
package com.xpn.xwiki.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.cache.rendering.RenderingCache;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link EditAction}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class EditActionTest
{
    private static final DocumentReference USER_DOCUMENT_REFERENCE = new DocumentReference("wiki", "XWiki", "user");

    private static final UserReference USER_REFERENCE = mock(UserReference.class);

    private static final UserReference OTHERUSER_REFERENCE = mock(UserReference.class);

    @InjectMockComponents
    private EditAction action;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @MockComponent
    private CSRFToken csrf;

    @MockComponent
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    @MockComponent
    private JobProgressManager progress;

    @MockComponent
    private ContextualLocalizationManager localization;

    @MockComponent
    private RenderingCache renderingCache;

    @MockComponent
    @Named("plain/1.0")
    private BlockRenderer plainRenderer;

    @Mock
    private XWikiRequest request;

    @BeforeEach
    public void beforeEach()
    {
        when(this.documentReferenceUserReferenceResolver.resolve(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.documentReferenceUserReferenceSerializer.serialize(USER_REFERENCE)).thenReturn(USER_DOCUMENT_REFERENCE);

        this.oldcore.getXWikiContext().setUserReference(USER_DOCUMENT_REFERENCE);

        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub.Builder().
            setRequestParameters(Map.of("form_token", new String[] {"tokenvalue"})).build());
    }

    private String initAndRenderAction() throws XWikiException
    {
        EditForm form = new EditForm();
        form.reset(this.request);

        this.oldcore.getXWikiContext().setForm(form);

        return this.action.render(this.oldcore.getXWikiContext());
    }

    @Test
    void documentAuthorsWhenDocumentDoesNotExist() throws XWikiException
    {
        XWikiDocument document = oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(document);

        initAndRenderAction();

        document = this.oldcore.getXWikiContext().getDoc();

        assertSame(USER_REFERENCE, document.getAuthors().getContentAuthor());
        assertSame(USER_REFERENCE, document.getAuthors().getCreator());
        assertSame(USER_REFERENCE, document.getAuthors().getEffectiveMetadataAuthor());
        assertSame(USER_REFERENCE, document.getAuthors().getOriginalMetadataAuthor());
    }

    @Test
    void documentAuthorsWhenDocumentExist() throws XWikiException
    {
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        document.getAuthors().setCreator(OTHERUSER_REFERENCE);
        document.getAuthors().setContentAuthor(OTHERUSER_REFERENCE);
        document.getAuthors().setEffectiveMetadataAuthor(OTHERUSER_REFERENCE);
        document.getAuthors().setOriginalMetadataAuthor(OTHERUSER_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(document);
        this.oldcore.getXWikiContext().put("tdoc", document);

        initAndRenderAction();

        document = this.oldcore.getXWikiContext().getDoc();

        assertSame(OTHERUSER_REFERENCE, document.getAuthors().getContentAuthor());
        assertSame(OTHERUSER_REFERENCE, document.getAuthors().getCreator());
        assertSame(OTHERUSER_REFERENCE, document.getAuthors().getEffectiveMetadataAuthor());
        assertSame(OTHERUSER_REFERENCE, document.getAuthors().getOriginalMetadataAuthor());
    }

    @Test
    void documentAuthorsWhenDocumentExistAndContentIsModifiedAndInvalidCSRF() throws XWikiException
    {
        documentAuthorsWhenDocumentExistAndContentIsModified(false);
    }

    @Test
    void documentAuthorsWhenDocumentExistAndContentIsModifiedAndValidCSRF() throws XWikiException
    {
        documentAuthorsWhenDocumentExistAndContentIsModified(true);
    }

    void documentAuthorsWhenDocumentExistAndContentIsModified(boolean validToken) throws XWikiException
    {
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        document.getAuthors().setCreator(OTHERUSER_REFERENCE);
        document.getAuthors().setContentAuthor(OTHERUSER_REFERENCE);
        document.getAuthors().setEffectiveMetadataAuthor(OTHERUSER_REFERENCE);
        document.getAuthors().setOriginalMetadataAuthor(OTHERUSER_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(document);
        this.oldcore.getXWikiContext().put("tdoc", document);

        when(this.request.getParameter("content")).thenReturn("modified content");

        when(this.csrf.isTokenValid("tokenvalue")).thenReturn(validToken);

        initAndRenderAction();

        document = this.oldcore.getXWikiContext().getDoc();

        assertSame(USER_REFERENCE, document.getAuthors().getContentAuthor());
        assertSame(OTHERUSER_REFERENCE, document.getAuthors().getCreator());
        assertSame(OTHERUSER_REFERENCE, document.getAuthors().getEffectiveMetadataAuthor());
        assertSame(OTHERUSER_REFERENCE, document.getAuthors().getOriginalMetadataAuthor());
        assertEquals(!validToken, document.isRestricted());
    }

    @Test
    void restrictedWhenDocumentModifiedBeforeInput() throws XWikiException
    {
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(document);
        this.oldcore.getXWikiContext().put("tdoc", document);

        document.setMetaDataDirty(true);

        initAndRenderAction();

        document = this.oldcore.getXWikiContext().getDoc();

        assertFalse(document.isRestricted());
        verifyNoInteractions(this.csrf);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void sectionTitleFromRequestContentRenderedWithCurrentUserRights(boolean validToken) throws XWikiException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.section.edit", 1);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        // The sections of xwiki/1.0 content are extracted without parsing the content.
        document.setSyntax(Syntax.XWIKI_1_0);
        document.setContent("1 Section\n\nSection content");
        document.getAuthors().setContentAuthor(OTHERUSER_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(document);
        this.oldcore.getXWikiContext().put("tdoc", document);
        // The saved document is the secure document of the request.
        this.oldcore.getXWikiContext().put("sdoc", document);

        String sectionTitle = "{{velocity}}modified{{/velocity}}";
        when(this.request.getParameter("content")).thenReturn("1 " + sectionTitle + "\n\nModified content");
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub.Builder().setRequestParameters(
            Map.of("form_token", new String[] { "tokenvalue" }, "section", new String[] { "1" })).build());
        when(this.csrf.isTokenValid("tokenvalue")).thenReturn(validToken);

        when(this.renderingCache.getRenderedContent(any(), any(), any())).thenReturn(null);

        // Remember the secure document used for rendering the section title.
        List<XWikiDocument> sectionTitleSecureDocuments = new ArrayList<>();
        List<Boolean> sectionTitleRestricted = new ArrayList<>();
        when(this.documentDisplayer.display(any(), any())).then(invocation -> {
            XWikiDocument displayedDocument = invocation.getArgument(0);
            if (sectionTitle.equals(displayedDocument.getContent())) {
                sectionTitleSecureDocuments.add((XWikiDocument) this.oldcore.getXWikiContext().get("sdoc"));
                sectionTitleRestricted.add(displayedDocument.isRestricted());
            }
            return new XDOM(List.of());
        });

        initAndRenderAction();

        assertEquals(1, sectionTitleSecureDocuments.size());
        XWikiDocument secureDocument = sectionTitleSecureDocuments.get(0);
        assertSame(USER_REFERENCE, secureDocument.getAuthors().getContentAuthor());
        assertEquals(!validToken, secureDocument.isRestricted());
        assertEquals(List.of(!validToken), sectionTitleRestricted);

        document = this.oldcore.getXWikiContext().getDoc();
        assertSame(USER_REFERENCE, document.getAuthors().getContentAuthor());
        assertEquals(!validToken, document.isRestricted());
    }
}
