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

import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
}
