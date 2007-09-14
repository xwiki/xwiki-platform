/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors.
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

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultSuperDocument}.
 *
 * @version $Id: $
 */
public class DefaultSuperDocumentTest extends MockObjectTestCase
{
    private XWikiContext context;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Map documents = new HashMap();

    protected void setUp() throws XWikiException
    {
        this.context = new XWikiContext();
        this.xwiki = new XWiki(new XWikiConfig(), this.context);

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {this.xwiki, this.context});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);

                    if (documents.containsKey(shallowDoc.getFullName())) {
                        return documents.get(shallowDoc.getFullName());
                    } else {
                        return shallowDoc;
                    }
                }
            });
        this.mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);

                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    documents.put(document.getFullName(), document);

                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(
            returnValue(Collections.EMPTY_LIST));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class,
            XWikiContext.class}, new Object[] {this.xwiki, this.context});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(
            returnValue(null));
        this.mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(
            returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore
            .proxy());
    }

    /////////////////////////////////////////////////////////////////////////////////////////:
    // Tests

    private final String DEFAULT_SPACE = "Space";
    private final String DEFAULT_DOCNAME = "Space";
    private final String DEFAULT_DOCFULLNAME = DEFAULT_SPACE + "." + DEFAULT_DOCNAME;

    public void testInitSuperDocumentEmpty() throws XWikiException
    {
        documents.clear();

        /////

        TestAbstractSuperClassTest.DispatchSuperClass sclass = TestAbstractSuperClassTest.DispatchSuperClass.getInstance(context);
        DefaultSuperDocument sdoc = (DefaultSuperDocument)sclass.newSuperDocument(context);

        assertNotNull(sdoc);
        assertTrue(sdoc.isNew());

        com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

        assertNotNull(obj);
        assertEquals(sdoc.getSuperClass(), sclass);
    }

    public void testInitSuperDocumentDocName() throws XWikiException
    {
        documents.clear();

        /////

        TestAbstractSuperClassTest.DispatchSuperClass sclass = TestAbstractSuperClassTest.DispatchSuperClass.getInstance(context);
        DefaultSuperDocument sdoc = (DefaultSuperDocument)sclass.newSuperDocument(DEFAULT_DOCFULLNAME, context);

        assertNotNull(sdoc);
        assertTrue(sdoc.isNew());

        com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

        assertNotNull(obj);
        assertEquals(sdoc.getSuperClass(), sclass);
    }

    public void testInitSuperDocumentDocNameExists() throws XWikiException
    {
        documents.clear();

        /////

        XWikiDocument doc = xwiki.getDocument(DEFAULT_DOCFULLNAME, context);
        xwiki.saveDocument(doc, context);

        TestAbstractSuperClassTest.DispatchSuperClass sclass = TestAbstractSuperClassTest.DispatchSuperClass.getInstance(context);
        DefaultSuperDocument sdoc = (DefaultSuperDocument)sclass.newSuperDocument(DEFAULT_DOCFULLNAME, context);

        assertNotNull(sdoc);
        assertTrue(sdoc.isNew());

        com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

        assertNotNull(obj);
        assertEquals(sdoc.getSuperClass(), sclass);
    }
}