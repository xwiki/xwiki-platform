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
package org.xwiki.mail.internal;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MailConfigMandatoryDocumentInitializer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class MailConfigMandatoryDocumentInitializerTest
{
    @InjectMockComponents
    private MailConfigMandatoryDocumentInitializer initializer;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    public void updateDocument()
    {
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainwiki");
        XWikiDocument document = new XWikiDocument(new DocumentReference("mainwiki", "Mail", "MailConfig"));

        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);

        this.initializer.updateDocument(document);

        assertEquals("mainwiki:XWiki.superadmin", document.getCreatorReference().toString());
        assertEquals("mainwiki:XWiki.superadmin", document.getAuthorReference().toString());
        assertTrue(document.isHidden());
        assertEquals("Mail Configuration", document.getTitle());
        BaseObject sendMailConfigObject =
            document.getXObject(new LocalDocumentReference("Mail", "SendMailConfigClass"));
        assertNotNull(sendMailConfigObject);
        assertNotNull(document.getXObject(new LocalDocumentReference("Mail", "GeneralMailConfigClass")));
    }
}
