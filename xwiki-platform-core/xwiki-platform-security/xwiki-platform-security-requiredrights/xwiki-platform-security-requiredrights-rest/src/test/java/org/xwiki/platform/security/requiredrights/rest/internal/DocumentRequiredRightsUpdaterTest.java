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
package org.xwiki.platform.security.requiredrights.rest.internal;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.internal.document.DocumentRequiredRightsReader;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentRequiredRightsUpdater}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(DocumentRequiredRightsReader.class)
class DocumentRequiredRightsUpdaterTest
{
    private static final String REQUIRED_RIGHTS_SAVE_SUMMARY_KEY = "security.requiredrights.rest.saveSummary";

    private static final String REQUIRED_RIGHTS_SAVE_SUMMARY = "Updated required rights";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private ModelContext modelContext;

    @InjectMockComponents
    private DocumentRequiredRightsUpdater updater;

    @BeforeEach
    void setUp()
    {
        when(this.localizationManager.getTranslationPlain(REQUIRED_RIGHTS_SAVE_SUMMARY_KEY))
            .thenReturn(REQUIRED_RIGHTS_SAVE_SUMMARY);
    }

    @Test
    void testUpdateRequiredRightsEnforceChanged() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights();
        requiredRights.setEnforce(true);

        Document document = mock();
        when(document.isEnforceRequiredRights()).thenReturn(false);
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);

        EntityReference currentEntityReference = mock();
        when(this.modelContext.getCurrentEntityReference()).thenReturn(currentEntityReference);

        this.updater.updateRequiredRights(requiredRights, document);

        verify(document).setEnforceRequiredRights(true);
        verify(document).save(REQUIRED_RIGHTS_SAVE_SUMMARY);
        verify(this.modelContext).setCurrentEntityReference(DOCUMENT_REFERENCE);
        verify(this.modelContext).setCurrentEntityReference(currentEntityReference);
    }

    @Test
    void testUpdateRequiredRightsNoChange() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights();
        requiredRights.setEnforce(true);

        Document document = mock();
        when(document.isEnforceRequiredRights()).thenReturn(true);
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);

        this.updater.updateRequiredRights(requiredRights, document);

        verify(document, never()).setEnforceRequiredRights(anyBoolean());
        verify(document, never()).save(anyString());
    }

    @Test
    void testUpdatedRequiredRightsNoChangeWithExistingRights() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights();
        requiredRights.setEnforce(true);
        DocumentRequiredRight right1 = new DocumentRequiredRight();
        right1.setRight("admin");
        right1.setScope("wiki");
        requiredRights.getRights().add(right1);

        Object xObject = mock();
        when(xObject.getValue(DocumentRequiredRightsReader.PROPERTY_NAME)).thenReturn("wiki_admin");
        Document document = mock();
        when(document.isEnforceRequiredRights()).thenReturn(true);
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(document.getObjects(DocumentRequiredRightsReader.CLASS_REFERENCE)).thenReturn(List.of(xObject));

        this.updater.updateRequiredRights(requiredRights, document);

        verify(document, never()).newObject(DocumentRequiredRightsReader.CLASS_REFERENCE);
        verify(document, never()).removeObject(any());
        verify(xObject, never()).set(any(), any());
        verify(document, never()).save(anyString());
    }

    @Test
    void testUpdateRequiredRightsWithNewRights() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights();
        requiredRights.setEnforce(true);
        DocumentRequiredRight right1 = new DocumentRequiredRight();
        right1.setRight("script");
        right1.setScope("DOCUMENT");
        requiredRights.getRights().add(right1);

        Document document = mock();
        when(document.isEnforceRequiredRights()).thenReturn(true);
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(document.getObjects(DocumentRequiredRightsReader.CLASS_REFERENCE)).thenReturn(new ArrayList<>());
        Object xObject = mock();
        when(document.newObject(DocumentRequiredRightsReader.CLASS_REFERENCE)).thenReturn(xObject);

        this.updater.updateRequiredRights(requiredRights, document);

        verify(document).newObject(DocumentRequiredRightsReader.CLASS_REFERENCE);
        verify(document).save(REQUIRED_RIGHTS_SAVE_SUMMARY);
        verify(xObject).set(DocumentRequiredRightsReader.PROPERTY_NAME, "script");
    }

    @ParameterizedTest
    @CsvSource({
        "programming,,programming",
        "programming,null,programming",
        "admin,SPACE,admin",
        "script,WIKI,wiki_script"
    })
    void testUpdateRequiredRightsWithChangedRight(String right, String scope, String expected) throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights();
        requiredRights.setEnforce(true);
        DocumentRequiredRight right1 = new DocumentRequiredRight();
        right1.setRight(right);
        right1.setScope(scope);
        requiredRights.getRights().add(right1);

        Object xObject1 = mock();
        when(xObject1.getValue(DocumentRequiredRightsReader.PROPERTY_NAME)).thenReturn("admin_wiki");
        Document document = mock();
        when(document.isEnforceRequiredRights()).thenReturn(true);
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(document.getObjects(DocumentRequiredRightsReader.CLASS_REFERENCE)).thenReturn(List.of(xObject1));

        this.updater.updateRequiredRights(requiredRights, document);

        verify(document, never()).newObject(DocumentRequiredRightsReader.CLASS_REFERENCE);
        verify(xObject1).set(DocumentRequiredRightsReader.PROPERTY_NAME, expected);
        verify(document).save(REQUIRED_RIGHTS_SAVE_SUMMARY);
    }

    @Test
    void testUpdateRequiredRightsRemoveExistingObjects() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights();
        requiredRights.setEnforce(true);

        Object existingObject = mock();
        when(existingObject.getValue(DocumentRequiredRightsReader.PROPERTY_NAME)).thenReturn("script");

        Document document = mock();
        when(document.isEnforceRequiredRights()).thenReturn(true);
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(document.getObjects(DocumentRequiredRightsReader.CLASS_REFERENCE))
            .thenReturn(List.of(existingObject));

        this.updater.updateRequiredRights(requiredRights, document);

        verify(document).removeObject(existingObject);
        verify(document).save(REQUIRED_RIGHTS_SAVE_SUMMARY);
    }

    @Test
    void testUpdateRequiredRightsIllegalRight()
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights();
        requiredRights.setEnforce(true);
        DocumentRequiredRight right = new DocumentRequiredRight();
        right.setRight("ILLEGAL");
        requiredRights.getRights().add(right);

        Document document = mock();
        when(document.getDocumentReference()).thenReturn(mock());

        assertThrows(WebApplicationException.class, () -> this.updater.updateRequiredRights(requiredRights, document));
    }
}
