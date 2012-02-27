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

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultXObjectDocument}.
 * 
 * @version $Id$
 */
public class DefaultXObjectDocumentTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Map<String, XWikiDocument> documents = new HashMap<String, XWikiDocument>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.xwiki = new XWiki();
        getContext().setWiki(this.xwiki);

        // //////////////////////////////////////////////////
        // XWikiHibernateStore

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {this.xwiki,
            getContext()});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                @Override
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
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);

                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    documents.put(document.getFullName(), document);

                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(returnValue(Collections.EMPTY_LIST));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {
            this.xwiki, getContext()});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));
        this.mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore.proxy());

        // ////////////////////////////////////////////////////////////////////////////////
        // XWikiRightService

        this.xwiki.setRightService(new XWikiRightService()
        {
            @Override
            public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
            {
                return true;
            }

            @Override
            public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
                throws XWikiException
            {
                return true;
            }

            @Override
            public boolean hasAdminRights(XWikiContext context)
            {
                return true;
            }

            @Override
            public boolean hasProgrammingRights(XWikiContext context)
            {
                return true;
            }

            @Override
            public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
            {
                return true;
            }

            @Override
            public List listAllLevels(XWikiContext context) throws XWikiException
            {
                return Collections.EMPTY_LIST;
            }

            @Override
            public boolean hasWikiAdminRights(XWikiContext context)
            {
                return true;
            }
        });
    }

    // ///////////////////////////////////////////////////////////////////////////////////////:
    // Tests

    private final String DEFAULT_SPACE = "Space";

    private final String DEFAULT_DOCNAME = "Document";

    private final String DEFAULT_DOCFULLNAME = DEFAULT_SPACE + "." + DEFAULT_DOCNAME;

    public void testInitXObjectDocumentEmpty() throws XWikiException
    {
        documents.clear();

        // ///

        XClassManager sclass = XClassManagerTest.DispatchXClassManager.getInstance(getContext());
        DefaultXObjectDocument sdoc = (DefaultXObjectDocument) sclass.newXObjectDocument(getContext());

        assertNotNull(sdoc);
        assertTrue(sdoc.isNew());

        com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

        assertNotNull(obj);
        assertEquals(sdoc.getXClassManager(), sclass);
    }

    public void testInitXObjectDocumentDocName() throws XWikiException
    {
        documents.clear();

        // ///

        XClassManager sclass = XClassManagerTest.DispatchXClassManager.getInstance(getContext());
        DefaultXObjectDocument sdoc =
            (DefaultXObjectDocument) sclass.newXObjectDocument(DEFAULT_DOCFULLNAME, 0, getContext());

        assertNotNull(sdoc);
        assertTrue(sdoc.isNew());

        com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

        assertNotNull(obj);
        assertEquals(sdoc.getXClassManager(), sclass);
    }

    public void testInitXObjectDocumentDocNameExists() throws XWikiException
    {
        documents.clear();

        // ///

        XWikiDocument doc = xwiki.getDocument(DEFAULT_DOCFULLNAME, getContext());
        xwiki.saveDocument(doc, getContext());

        XClassManager sclass = XClassManagerTest.DispatchXClassManager.getInstance(getContext());
        DefaultXObjectDocument sdoc =
            (DefaultXObjectDocument) sclass.newXObjectDocument(DEFAULT_DOCFULLNAME, 0, getContext());

        assertNotNull(sdoc);
        assertTrue(sdoc.isNew());

        com.xpn.xwiki.api.Object obj = sdoc.getObject(sclass.getClassFullName());

        assertNotNull(obj);
        assertEquals(sdoc.getXClassManager(), sclass);
    }

    public void testMergeObject() throws XWikiException
    {
        XClassManager sclass = XClassManagerTest.DispatchXClassManager.getInstance(getContext());
        DefaultXObjectDocument sdoc1 = (DefaultXObjectDocument) sclass.newXObjectDocument(getContext());

        DefaultXObjectDocument sdoc2 = (DefaultXObjectDocument) sclass.newXObjectDocument(getContext());

        sdoc1.setStringValue(XClassManagerTest.FIELD_string, "valuesdoc1");
        sdoc1.setStringValue(XClassManagerTest.FIELD_string2, "value2sdoc1");

        sdoc2.setStringValue(XClassManagerTest.FIELD_string, "valuesdoc2");
        sdoc2.setIntValue(XClassManagerTest.FIELD_int, 2);

        sdoc1.mergeObject(sdoc2);

        assertEquals("The field is not overwritten", sdoc1.getStringValue(XClassManagerTest.FIELD_string),
            sdoc2.getStringValue(XClassManagerTest.FIELD_string));
        assertEquals("The field is removed", "value2sdoc1", sdoc1
            .getStringValue(XClassManagerTest.FIELD_string2));
        assertEquals("The field is not added", sdoc1.getIntValue(XClassManagerTest.FIELD_int), sdoc1
            .getIntValue(XClassManagerTest.FIELD_int));
    }
}
