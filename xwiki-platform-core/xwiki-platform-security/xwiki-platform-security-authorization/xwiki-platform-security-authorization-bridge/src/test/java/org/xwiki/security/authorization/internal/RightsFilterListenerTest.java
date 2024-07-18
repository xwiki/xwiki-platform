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
package org.xwiki.security.authorization.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.internal.requiredrights.DocumentRequiredRightsReader;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;
import com.xpn.xwiki.internal.mandatory.XWikiRightsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Validate {@link RightsFilterListener}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class RightsFilterListenerTest
{
    @MockComponent
    private AuthorizationManager authorization;

    @MockComponent
    private DocumentRequiredRightsReader requiredRightsReader;

    @InjectMockComponents
    private RightsFilterListener listener;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @BeforeEach
    void setUp()
    {
        when(this.requiredRightsReader.readRequiredRights(any())).thenReturn(DocumentRequiredRights.EMPTY);
    }

    @Test
    public void noRight()
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        document.setOriginalDocument(document.clone());

        XWikiDocument before = document.clone();

        this.listener.onEvent(new UserUpdatingDocumentEvent(), document, null);

        assertEquals(before, document);
    }

    @Test
    public void noChange() throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        document.newXObject(XWikiRightsDocumentInitializer.CLASS_REFERENCE, oldcore.getXWikiContext());
        document.setOriginalDocument(document.clone());

        XWikiDocument before = document.clone();

        this.listener.onEvent(new UserUpdatingDocumentEvent(), document, null);

        assertEquals(before, document);
    }

    @Test
    public void newEmptyRightObject() throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        document.setOriginalDocument(document.clone());
        document.newXObject(XWikiRightsDocumentInitializer.CLASS_REFERENCE, oldcore.getXWikiContext());

        XWikiDocument before = document.clone();

        this.listener.onEvent(new UserUpdatingDocumentEvent(), document, null);

        assertEquals(before, document);
    }

    @Test
    public void newAllowedRightObject() throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        document.setOriginalDocument(document.clone());
        BaseObject rightObject =
            document.newXObject(XWikiRightsDocumentInitializer.CLASS_REFERENCE, oldcore.getXWikiContext());
        rightObject.setStringValue("levels", "view");

        XWikiDocument before = document.clone();

        this.listener.onEvent(new UserUpdatingDocumentEvent(), document, null);

        assertEquals(before, document);
    }

    @Test
    public void newDeniedRightObject() throws XWikiException, AccessDeniedException
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject rightObject =
            document.newXObject(XWikiRightsDocumentInitializer.CLASS_REFERENCE, oldcore.getXWikiContext());
        document.setOriginalDocument(document.clone());
        document.getOriginalDocument().removeXObjects(XWikiRightsDocumentInitializer.CLASS_REFERENCE);
        rightObject.setStringValue("levels", "view");

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.VIEW, null,
            document.getDocumentReference());

        XWikiDocument before = document.clone();

        this.listener.onEvent(new UserUpdatingDocumentEvent(), document, null);

        assertNotEquals(before, document);
        assertEquals(document.getOriginalDocument(), document);
    }

    @Test
    void removedDeniedRightObject() throws XWikiException, AccessDeniedException
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        document.setOriginalDocument(document.clone());
        BaseObject rightObject =
            document.newXObject(XWikiRightsDocumentInitializer.CLASS_REFERENCE, oldcore.getXWikiContext());
        rightObject.setStringValue("levels", "view");
        document.setOriginalDocument(document.clone());
        document.removeXObjects(XWikiRightsDocumentInitializer.CLASS_REFERENCE);

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.VIEW, null,
            document.getDocumentReference());

        XWikiDocument before = document.clone();
        this.listener.onEvent(new UserUpdatingDocumentEvent(), document, null);

        assertEquals(document.getOriginalDocument(), document);
        assertTrue(document.getXObjectsToRemove().isEmpty());
        assertNotEquals(before, document);
    }
}
