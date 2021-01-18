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

import java.util.Collections;
import java.util.Date;

import javax.inject.Named;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SaveAction}.
 *
 * @version $Id$
 */
@ComponentList
@ReferenceComponentList
@OldcoreTest(mockXWiki = false)
public class SaveActionTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "FooBar");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private EntityNameValidationManager entityNameValidationManager;

    @MockComponent
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource propertiesConf;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    private XWikiContext context;

    private SaveAction saveAction;

    private XWikiRequest mockRequest;

    private XWikiResponse mockResponse;

    private XWikiDocument mockDocument;

    private XWikiDocument mockClonedDocument;

    private EditForm mockForm;

    private XWiki xWiki;

    @BeforeEach
    void setup()
    {
        this.saveAction = new SaveAction();

        context = oldcore.getXWikiContext();

        xWiki = mock(XWiki.class);
        context.setWiki(this.xWiki);

        mockRequest = mock(XWikiRequest.class);
        context.setRequest(mockRequest);

        mockResponse = mock(XWikiResponse.class);
        context.setResponse(mockResponse);

        mockDocument = mock(XWikiDocument.class);
        context.setDoc(mockDocument);

        mockClonedDocument = mock(XWikiDocument.class);
        when(mockDocument.clone()).thenReturn(mockClonedDocument);

        mockForm = mock(EditForm.class);
        context.setForm(mockForm);
        when(this.entityNameValidationConfiguration.useValidation()).thenReturn(false);

        context.setUserReference(USER_REFERENCE);
    }

    @Test
    void newDocumentInvalidName() throws Exception
    {
        when(mockDocument.isNew()).thenReturn(true);
        DocumentReference documentReference = new DocumentReference("XWiki", "Foo", "Bar");
        when(mockDocument.getDocumentReference()).thenReturn(documentReference);
        when(this.entityNameValidationConfiguration.useValidation()).thenReturn(true);
        EntityNameValidation entityNameValidation = mock(EntityNameValidation.class);
        when(this.entityNameValidationManager.getEntityReferenceNameStrategy()).thenReturn(entityNameValidation);
        when(entityNameValidation.isValid(documentReference)).thenReturn(false);

        assertTrue(saveAction.save(this.context));
        assertEquals("entitynamevalidation.create.invalidname", context.get("message"));
        assertArrayEquals(new Object[] { "Foo.Bar" }, (Object[]) context.get("messageParameters"));
    }

    @Test
    void validSave() throws Exception
    {
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("1.2"));
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        when(mockClonedDocument.getLock(this.context)).thenReturn(mock(XWikiLock.class));
        when(mockForm.getTemplate()).thenReturn("");
        assertFalse(saveAction.save(this.context));
        assertEquals(new Version("1.2"), this.context.get("SaveAction.savedObjectVersion"));

        verify(mockClonedDocument).readFromTemplate("", this.context);
        verify(mockClonedDocument).setAuthor("XWiki.FooBar");
        verify(mockClonedDocument).setMetaDataDirty(true);
        verify(this.xWiki).checkSavingDocument(USER_REFERENCE, mockClonedDocument, "My Changes", false, this.context);
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
        verify(mockClonedDocument).removeLock(this.context);
    }

    @Test
    void validSaveNewTranslation() throws Exception
    {
        when(mockForm.getLanguage()).thenReturn("fr");
        when(mockClonedDocument.getTranslatedDocument("fr", this.context)).thenReturn(mockClonedDocument);
        when(mockClonedDocument.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "My", "Page"));
        when(mockClonedDocument.getStore()).thenReturn(this.oldcore.getMockStore());
        when(xWiki.getStore()).thenReturn(this.oldcore.getMockStore());
        context.put("ajax", true);
        when(xWiki.isMultiLingual(this.context)).thenReturn(true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.1");
        when(mockRequest.getParameter("isNew")).thenReturn("true");
        assertFalse(saveAction.save(this.context));
        assertEquals(new Version("1.1"), this.context.get("SaveAction.savedObjectVersion"));
        verify(this.xWiki).checkSavingDocument(eq(USER_REFERENCE), any(XWikiDocument.class), eq(""),
            eq(false), eq(this.context));
        verify(this.xWiki).saveDocument(any(XWikiDocument.class), eq(""), eq(false), eq(this.context));
    }

    @Test
    void validSaveOldTranslation() throws Exception
    {
        when(mockForm.getLanguage()).thenReturn("fr");
        XWikiDocument translation = mock(XWikiDocument.class);
        when(mockForm.getTemplate()).thenReturn("");
        when(mockClonedDocument.getTranslatedDocument("fr", this.context)).thenReturn(translation);
        when(translation.clone()).thenReturn(mockClonedDocument);
        when(xWiki.getStore()).thenReturn(this.oldcore.getMockStore());
        context.put("ajax", true);
        when(xWiki.isMultiLingual(this.context)).thenReturn(true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.3");
        when(mockRequest.getParameter("editingVersionDate")).thenReturn("1000");
        when(translation.getRCSVersion()).thenReturn(new Version("1.3"));
        when(translation.getDate()).thenReturn(new Date(0));
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("1.4"));
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        assertFalse(saveAction.save(this.context));
        assertEquals(new Version("1.4"), this.context.get("SaveAction.savedObjectVersion"));
        verify(this.xWiki).checkSavingDocument(USER_REFERENCE, mockClonedDocument, "My Changes", false, this.context);
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
    }

    /**
     * This tests aims at checking the usecase when uploading an image in the WYSIWYG editor before the first save
     * and saving afterwards.
     */
    @Test
    void validSaveRequestImageUploadAndConflictCheck() throws Exception
    {
        when(mockDocument.getRCSVersion()).thenReturn(new Version("1.2"));
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("1.2"));
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        when(mockClonedDocument.getLock(this.context)).thenReturn(mock(XWikiLock.class));
        when(mockForm.getTemplate()).thenReturn("");
        when(this.propertiesConf.getProperty("edit.conflictChecking.enabled")).thenReturn(true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.1");
        context.put("ajax", true);
        when(mockRequest.getParameter("forceSave")).thenReturn("");
        when(mockRequest.getParameter("isNew")).thenReturn("true");

        when(mockDocument.getDate()).thenReturn(new Date(42));
        when(mockRequest.getParameter("editingVersionDate")).thenReturn("43");
        when(this.documentRevisionProvider.getRevision(mockDocument, "1.1")).thenReturn(mock(XWikiDocument.class));
        when(mockDocument.getContentDiff("1.1", "1.2", context)).thenReturn(Collections.emptyList());
        when(mockDocument.getMetaDataDiff("1.1", "1.2", context)).thenReturn(Collections.emptyList());
        when(mockDocument.getObjectDiff("1.1", "1.2", context)).thenReturn(Collections.emptyList());

        assertFalse(saveAction.save(this.context));
        assertEquals(new Version("1.2"), this.context.get("SaveAction.savedObjectVersion"));

        verify(mockClonedDocument).readFromTemplate("", this.context);
        verify(mockClonedDocument).setAuthor("XWiki.FooBar");
        verify(mockClonedDocument).setMetaDataDirty(true);
        verify(this.xWiki).checkSavingDocument(USER_REFERENCE, mockClonedDocument, "My Changes", false, this.context);
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
        verify(mockClonedDocument).removeLock(this.context);

    }
}
