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
package com.xpn.xwiki.api;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.api.XWiki}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class XWikiTest
{
    public static final Random rand = new Random(Calendar.getInstance().getTimeInMillis());

    private Document apiDocument;

    private XWiki apiXWiki;

    private Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();

    @BeforeEach
    public void setup(MockitoOldcore mockitoOldcore) throws XWikiException
    {
        XWikiContext xWikiContext = mockitoOldcore.getXWikiContext();
        this.apiXWiki = new XWiki(mockitoOldcore.getSpyXWiki(), xWikiContext);
        XWikiRightService mockRightService = mockitoOldcore.getMockRightService();
        when(mockRightService.hasProgrammingRights(any(), any())).thenReturn(true);
        when(mockRightService.hasProgrammingRights(any())).thenReturn(true);
        when(mockRightService.hasAccessLevel(any(), any(), any(), any())).thenReturn(true);
        when(mockitoOldcore.getMockVersioningStore().getXWikiDocumentArchive(any(), any()))
            .thenReturn(new XWikiDocumentArchive());

        xWikiContext.setUser("Redtail");
        this.apiDocument =
            new Document(new XWikiDocument(new DocumentReference("xwiki", "MilkyWay", "Fidis")), xWikiContext);
        this.apiDocument.getDocument().setCreator("c" + xWikiContext.getUser());
        this.apiDocument.getDocument().setAuthor("a" + xWikiContext.getUser());
        this.apiDocument.save();
        xWikiContext.setUser("Earth");
    }

    @Test
    public void authorIsntChangedAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Lyre";
        this.apiXWiki.copyDocument("MilkyWay.Fidis", copyName);
        Document copy = this.apiXWiki.getDocument(copyName);

        assertEquals("XWiki.Earth", copy.getAuthor());
    }

    @Test
    public void creatorIsntChangedAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Sirius";
        this.apiXWiki.copyDocument("MilkyWay.Fidis", copyName);
        Document copy = this.apiXWiki.getDocument(copyName);

        assertEquals("XWiki.Earth", copy.getCreator());
    }

    @Test
    public void creationDateAfterDocumentCopy() throws XWikiException
    {
        String copyName = this.apiDocument.getDocumentReference().getName() + "Copy";
        long startTime = (Calendar.getInstance().getTimeInMillis() / 1000) * 1000;
        this.apiXWiki.copyDocument("MilkyWay.Fidis", copyName);
        long endTime = (Calendar.getInstance().getTimeInMillis() / 1000) * 1000;
        long copyCreationTime = this.apiXWiki.getDocument(copyName).getCreationDate().getTime();

        assertTrue(startTime <= copyCreationTime && copyCreationTime <= endTime);
    }

    @Test
    public void getAvailableRendererSyntax(MockitoComponentManager componentManager) throws Exception
    {
        PrintRendererFactory factory1 = componentManager.registerMockComponent(PrintRendererFactory.class,
            Syntax.PLAIN_1_0.toIdString());
        PrintRendererFactory factory2 = componentManager.registerMockComponent(PrintRendererFactory.class,
            Syntax.ANNOTATED_XHTML_1_0.toIdString());
        when(factory1.getSyntax()).thenReturn(Syntax.PLAIN_1_0);
        when(factory2.getSyntax()).thenReturn(Syntax.ANNOTATED_XHTML_1_0);

        assertEquals(Syntax.PLAIN_1_0, this.apiXWiki.getAvailableRendererSyntax("plain", "1.0"));
        assertEquals(Syntax.PLAIN_1_0, this.apiXWiki.getAvailableRendererSyntax("Plain", "1.0"));
        assertEquals(Syntax.PLAIN_1_0, this.apiXWiki.getAvailableRendererSyntax("plain", null));
        assertNull(this.apiXWiki.getAvailableRendererSyntax("plai", "1.0"));
        assertNull(this.apiXWiki.getAvailableRendererSyntax("plai", null));
    }
}
