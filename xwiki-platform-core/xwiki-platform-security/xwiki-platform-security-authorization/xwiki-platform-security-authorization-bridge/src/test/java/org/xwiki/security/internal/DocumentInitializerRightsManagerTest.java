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
package org.xwiki.security.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiRightsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.xwiki.security.internal.XWikiConstants.ALLOW_FIELD_NAME;
import static org.xwiki.security.internal.XWikiConstants.GROUPS_FIELD_NAME;
import static org.xwiki.security.internal.XWikiConstants.LEVELS_FIELD_NAME;
import static org.xwiki.security.internal.XWikiConstants.LOCAL_CLASS_REFERENCE;

/**
 * Test of {@link DocumentInitializerRightsManager}.
 *
 * @version $Id$
 */
@OldcoreTest
@AllComponents
class DocumentInitializerRightsManagerTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DocumentInitializerRightsManager rightsManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiDocument document;

    private XWikiContext xWikiContext;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        this.xWikiContext = this.oldcore.getXWikiContext();
        String wikiId = this.xWikiContext.getWikiId();
        this.document = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference(wikiId, "Space", "Page"), this.xWikiContext);
        XWikiDocument xWikiRigths = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference(wikiId, "XWiki", "XWikiRights"), this.xWikiContext);
        componentManager.<MandatoryDocumentInitializer>getInstance(MandatoryDocumentInitializer.class,
            XWikiRightsDocumentInitializer.CLASS_REFERENCE_STRING).updateDocument(xWikiRigths);
        this.oldcore.getSpyXWiki().saveDocument(xWikiRigths, this.xWikiContext);
    }

    @Test
    void restrictToAdminSkipWhenAlreadyHasRights() throws Exception
    {
        BaseObject baseObject = this.document.newXObject(LOCAL_CLASS_REFERENCE, this.xWikiContext);
        baseObject.setLargeStringValue(LEVELS_FIELD_NAME, "edit");
        assertFalse(this.rightsManager.restrictToAdmin(this.document));
    }

    @Test
    void restrictToAdminBadlyInitialized() throws Exception
    {
        BaseObject object = this.document.newXObject(LOCAL_CLASS_REFERENCE, this.xWikiContext);
        object.setLargeStringValue(GROUPS_FIELD_NAME, "");
        object.setLargeStringValue(LEVELS_FIELD_NAME, "");
        object.setIntValue(ALLOW_FIELD_NAME, 1);
        assertTrue(this.rightsManager.restrictToAdmin(this.document));
        BaseObject xObject = this.document.getXObject(LOCAL_CLASS_REFERENCE);
        assertEquals("XWiki.XWikiAdminGroup", xObject.getStringValue(GROUPS_FIELD_NAME));
        assertEquals("view,edit,delete", xObject.getStringValue(LEVELS_FIELD_NAME));
        assertEquals("1", xObject.getStringValue(ALLOW_FIELD_NAME));
    }

    @Test
    void restrictToAdmin()
    {
        assertTrue(this.rightsManager.restrictToAdmin(this.document));
        assertEquals(1, this.document.getXObjects(LOCAL_CLASS_REFERENCE).size());
        BaseObject xObject = this.document.getXObject(LOCAL_CLASS_REFERENCE);
        assertEquals("XWiki.XWikiAdminGroup", xObject.getStringValue(GROUPS_FIELD_NAME));
        assertEquals("view,edit,delete", xObject.getStringValue(LEVELS_FIELD_NAME));
        assertEquals("1", xObject.getStringValue(ALLOW_FIELD_NAME));
    }

    @Test
    void restrictToAdminWithException() throws XWikiException
    {
        this.document = spy(this.document);
        doThrow(XWikiException.class).when(this.document)
            .newXObject(any(EntityReference.class), any(XWikiContext.class));
        assertFalse(this.rightsManager.restrictToAdmin(this.document));
        assertEquals(0, this.document.getXObjects(LOCAL_CLASS_REFERENCE).size());
        assertEquals("Error adding a [XWiki.XWikiRights] object to the document [xwiki:Space.Page]",
            this.logCapture.getMessage(0));
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
    }
}
