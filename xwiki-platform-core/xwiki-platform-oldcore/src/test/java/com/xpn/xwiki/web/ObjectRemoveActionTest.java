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

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ObjectRemoveAction}.
 *
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class ObjectRemoveActionTest
{
    private static final String CLASS_NAME = "XWiki.XWikiUsers";

    @InjectMockComponents
    private ObjectRemoveAction objectRemoveAction;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @MockComponent
    private StoreConfiguration storeConfiguration;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiDocument clonedDocument;

    @Mock
    private ObjectRemoveForm form;

    @Mock
    private BaseObject object;

    @Mock
    private DocumentAuthors authors;

    @Mock
    private UserReference currentUserReference;

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = this.oldcore.getXWikiContext();
        this.context.setDoc(this.document);
        this.context.setForm(this.form);
        this.context.setRequest(this.request);

        when(this.document.clone()).thenReturn(this.clonedDocument);
        when(this.clonedDocument.getOriginalDocument()).thenReturn(this.document);
        when(this.clonedDocument.getAuthors()).thenReturn(this.authors);
        DocumentReference documentReference = new DocumentReference("xwiki", "Foo", "Bar", Locale.ENGLISH);
        when(this.clonedDocument.getDocumentReference()).thenReturn(documentReference);
        when(this.clonedDocument.getDocumentReferenceWithLocale()).thenReturn(documentReference);

        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.currentUserReference);
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);
    }

    @Test
    void removeNonCommentObject() throws Exception
    {
        when(this.form.getClassName()).thenReturn(CLASS_NAME);
        when(this.form.getClassId()).thenReturn(2);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when(this.clonedDocument.resolveClassReference(CLASS_NAME)).thenReturn(classReference);
        when(this.clonedDocument.getXObject(classReference, 2)).thenReturn(this.object);

        assertFalse(this.objectRemoveAction.action(this.context));

        verify(this.clonedDocument).removeXObject(this.object);
        verify(this.context.getWiki()).saveDocument(this.clonedDocument, "core.comment.deleteObject", true, true,
            this.context);
    }
}
