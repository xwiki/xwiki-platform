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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;

/**
 * Unit tests for {@link com.xpn.xwiki.api.XWiki}.
 * 
 * @version $Id$
 */
public class XWikiTest extends AbstractBridgedXWikiComponentTestCase
{
    public static final Random rand = new Random(Calendar.getInstance().getTimeInMillis());

    private com.xpn.xwiki.XWiki xwiki;

    private Document apiDocument;

    private XWiki apiXWiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Mock mockXWikiRightService;

    private Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.xwiki = new com.xpn.xwiki.XWiki();
        getContext().setWiki(this.xwiki);
        this.xwiki.setConfig(new XWikiConfig());

        this.apiXWiki = new XWiki(this.xwiki, getContext());

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new java.lang.Class[] {com.xpn.xwiki.XWiki.class, XWikiContext.class},
                new java.lang.Object[] {this.xwiki, getContext()});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                @Override
                public java.lang.Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    if (XWikiTest.this.docs.containsKey(shallowDoc.getName())) {
                        return XWikiTest.this.docs.get(shallowDoc.getName());
                    } else {
                        return shallowDoc;
                    }
                }
            });
        this.mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                @Override
                public java.lang.Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) XWikiTest.this.mockXWikiStore.proxy());
                    document.setId(rand.nextLong());
                    XWikiTest.this.docs.put(document.getName(), document);
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(returnValue(Collections.EMPTY_LIST));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new java.lang.Class[] {com.xpn.xwiki.XWiki.class,
            XWikiContext.class}, new java.lang.Object[] {this.xwiki, getContext()});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(
            returnValue(new XWikiDocumentArchive()));
        this.mockXWikiVersioningStore.stubs().method("saveXWikiDocArchive").will(returnValue(null));

        this.mockXWikiRightService =
            mock(XWikiRightServiceImpl.class, new java.lang.Class[] {}, new java.lang.Object[] {});
        this.mockXWikiRightService.stubs().method("hasAccessLevel").will(returnValue(true));
        this.mockXWikiRightService.stubs().method("hasProgrammingRights").will(returnValue(true));

        this.xwiki.setStore((XWikiStoreInterface) this.mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) this.mockXWikiVersioningStore.proxy());
        this.xwiki.setRightService((XWikiRightService) this.mockXWikiRightService.proxy());

        getContext().setUser("Redtail");
        this.apiDocument =
            new Document(new XWikiDocument(new DocumentReference("Wiki", "MilkyWay", "Fidis")), getContext());
        this.apiDocument.getDocument().setCreator("c" + getContext().getUser());
        this.apiDocument.getDocument().setAuthor("a" + getContext().getUser());
        this.apiDocument.save();
        getContext().setUser("Earth");
    }

    public void testAuthorIsntChangedAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Lyre";
        this.apiXWiki.copyDocument(this.apiDocument.getName(), copyName);
        Document copy = this.apiXWiki.getDocument(copyName);

        assertEquals("XWiki.Earth", copy.getAuthor());
    }

    public void testCreatorIsntChangedAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Sirius";
        this.apiXWiki.copyDocument(this.apiDocument.getName(), copyName);
        Document copy = this.apiXWiki.getDocument(copyName);

        assertEquals("XWiki.Earth", copy.getCreator());
    }

    public void testCreationDateAfterDocumentCopy() throws XWikiException
    {
        String copyName = this.apiDocument.getName() + "Copy";
        long startTime = (Calendar.getInstance().getTimeInMillis() / 1000) * 1000;
        this.apiXWiki.copyDocument(this.apiDocument.getName(), copyName);
        long endTime = (Calendar.getInstance().getTimeInMillis() / 1000) * 1000;
        long copyCreationTime = this.apiXWiki.getDocument(copyName).getCreationDate().getTime();

        assertTrue(startTime <= copyCreationTime && copyCreationTime <= endTime);
    }

    public void testGetAvailableRendererSyntax()
    {
        assertEquals(Syntax.PLAIN_1_0, this.apiXWiki.getAvailableRendererSyntax("plain", "1.0"));
        assertEquals(Syntax.PLAIN_1_0, this.apiXWiki.getAvailableRendererSyntax("Plain", "1.0"));
        assertEquals(Syntax.PLAIN_1_0, this.apiXWiki.getAvailableRendererSyntax("plain", null));
        assertNull(this.apiXWiki.getAvailableRendererSyntax("plai", "1.0"));
        assertNull(this.apiXWiki.getAvailableRendererSyntax("plai", null));
    }
}
