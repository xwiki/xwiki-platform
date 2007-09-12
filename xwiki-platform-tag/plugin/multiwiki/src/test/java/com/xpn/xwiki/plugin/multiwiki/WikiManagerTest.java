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

package com.xpn.xwiki.plugin.multiwiki;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.multiwiki.WikiManager}.
 * 
 * @version $Id: $
 */
public class WikiManagerTest extends MockObjectTestCase
{
    private XWikiContext context;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Map databases = new HashMap();

    private static final String WIKI_NAME = "xwiki";
    private static final String TARGET_WIKI_NAME = "wikitosave";
    
    protected void setUp() throws XWikiException
    {
        this.context = new XWikiContext();
        this.context.setVirtual(true);
        this.xwiki = new XWiki(new XWikiConfig(), this.context);

        databases.put(WIKI_NAME, new HashMap());
        
        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {this.xwiki, this.context});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    XWikiContext context = (XWikiContext) invocation.parameterValues.get(1);
                    
                    String database = context.getDatabase();
                    
                    if (database == null || database.length() == 0)
                        database = WIKI_NAME;
                    
                    if (!databases.containsKey(database))
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                            "Database " + database + " does not exists.");
                    
                    Map docs = (Map)databases.get(database);
                    
                    if (docs.containsKey(shallowDoc.getFullName())) {
                        return docs.get(shallowDoc.getFullName());
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
                    XWikiContext context = (XWikiContext) invocation.parameterValues.get(1);
                    
                    String database = context.getDatabase();
                    
                    if (database == null || database.length() == 0)
                        database = WIKI_NAME;
                    
                    if (!databases.containsKey(database))
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                            "Database " + database + " does not exists.");
                    
                    Map docs = (Map)databases.get(database);
                    
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    document.setDatabase(context.getDatabase());
                    
                    docs.put(document.getFullName(), document);
                    
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("createWiki").will(
            new CustomStub("Implements XWikiStoreInterface.createWiki")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    String wikiName = (String) invocation.parameterValues.get(0);
                    
                    if (databases.containsKey(wikiName))
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CREATE_DATABASE,
                            "Database " + wikiName + " already exists.");
                    
                    databases.put(wikiName, new HashMap());
                    
                    return null;
                }
            });
        /*this.mockXWikiStore.stubs().method("deleteWiki").will(
            new CustomStub("Implements XWikiStoreInterface.deleteWiki")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    String wikiName = (String) invocation.parameterValues.get(0);
                    
                    if (!databases.containsKey(wikiName))
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETE_DATABASE,
                            "Database " + wikiName + " does not exists.");
                    
                    databases.remove(wikiName);
                    
                    return null;
                }
            });*/
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
    
    private static final String DOCSPACE = "DocumentSpace";
    private static final String DOCNAME = "DocumentName";
    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;
    
    public void testSaveDocument() throws XWikiException
    {
        context.setDatabase(WIKI_NAME);

        databases.clear();
        databases.put(TARGET_WIKI_NAME, new HashMap());

        /////
        
        XWikiDocument doc = new XWikiDocument("DocumentSpace", "DocumentName");
        
        WikiManager.getInstance().saveDocument(TARGET_WIKI_NAME, doc, context);
        
        assertEquals(WIKI_NAME, context.getDatabase());
        assertTrue(databases.containsKey(TARGET_WIKI_NAME) && ((Map)databases.get(TARGET_WIKI_NAME)).containsKey(doc.getFullName()));
    }
    
    public void testGetDocument() throws XWikiException
    {
        testSaveDocument();
        
        /////
        
        XWikiDocument doc = WikiManager.getInstance().getDocument(TARGET_WIKI_NAME, DOCFULLNAME, context);
        
        assertFalse(doc.isNew());
        assertEquals(WIKI_NAME, context.getDatabase());
    }
    
    public void testSearchDocuments()
    {
        
    }
    
    public void testCreateNewWikiFromPackage()
    {
        
    }
    
    public void testCreateNewWikiFromTemplate()
    {
        
    }
    
    public void testCreateNewWiki()
    {
        
    }
    
    public void testDeleteWiki()
    {
        
    }
    
    public void testGetWiki()
    {
        
    }
    
    public void testGetWikiDocumentList()
    {
        
    }
    
    public void testIsWikiExist()
    {
        
    }
    
    public void testGetWikiTemplate()
    {
        
    }
    
    public void testGetWikiTemplateList()
    {
        
    }
    
    public void testCreateWikiTemplate()
    {
        
    }
}
