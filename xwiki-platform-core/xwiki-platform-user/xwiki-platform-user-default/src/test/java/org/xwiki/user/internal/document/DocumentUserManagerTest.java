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
package org.xwiki.user.internal.document;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentUserManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DocumentUserManagerTest
{
    @InjectMockComponents
    private DocumentUserManager userManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void exists() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(this.contextProvider.get()).thenReturn(xcontext);
        when(this.wikiDescriptorManager.exists("wiki")).thenReturn(true);
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(reference, xcontext)).thenReturn(document);
        when(document.isNew()).thenReturn(false);
        when(document.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT, new EntityReference("XWiki",
            EntityType.SPACE)))).thenReturn(mock(BaseObject.class));

        assertTrue(this.userManager.exists(new DocumentUserReference(reference, true)));
    }

    @Test
    void existsWhenDocumentIsNotAUser() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(this.contextProvider.get()).thenReturn(xcontext);
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(false);
        when(xwiki.getDocument(reference, xcontext)).thenReturn(document);
        when(document.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT, new EntityReference("XWiki",
            EntityType.SPACE)))).thenReturn(null);

        assertFalse(this.userManager.exists(new DocumentUserReference(reference, true)));
    }

    @Test
    void existsWhenNoDocument() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(this.contextProvider.get()).thenReturn(xcontext);
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(true);
        when(xwiki.getDocument(reference, xcontext)).thenReturn(document);

        assertFalse(this.userManager.exists(new DocumentUserReference(reference, true)));
    }

    @Test
    void existsWhenError() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(this.contextProvider.get()).thenReturn(xcontext);
        when(xwiki.getDocument(reference, xcontext)).thenThrow(new XWikiException(0, 0, "error"));
        when(this.wikiDescriptorManager.exists("wiki")).thenReturn(true);

        DocumentUserReference userReference = new DocumentUserReference(reference, true);
        UserException userException = assertThrows(UserException.class, () -> this.userManager.exists(userReference));
        assertEquals("Failed to check if document [wiki:space.user] holds an XWiki user or not. ",
            userException.getMessage());
        assertEquals(XWikiException.class, userException.getCause().getClass());
    }

    @Test
    void existsWhenUnknownWiki() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(this.contextProvider.get()).thenReturn(xcontext);
        when(this.wikiDescriptorManager.exists("wiki")).thenReturn(false);
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        assertFalse(this.userManager.exists(new DocumentUserReference(reference, true)));
    }

    @Test
    void existsWhenWikiDescriptorManagerError() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(this.contextProvider.get()).thenReturn(xcontext);
        when(this.wikiDescriptorManager.exists("wiki")).thenThrow(WikiManagerException.class);
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUserReference userReference = new DocumentUserReference(reference, true);
        UserException userException = assertThrows(UserException.class, () -> this.userManager.exists(userReference));
        assertEquals("Failed to determine if wiki [wiki] exists.", userException.getMessage());
        assertEquals(WikiManagerException.class, userException.getCause().getClass());
    }
}
