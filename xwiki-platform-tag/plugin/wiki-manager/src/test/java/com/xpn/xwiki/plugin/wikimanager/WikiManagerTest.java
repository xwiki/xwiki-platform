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

package com.xpn.xwiki.plugin.wikimanager;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.wikimanager.WikiManager}.
 * 
 * @version $Id$
 */
public class WikiManagerTest extends MockObjectTestCase
{
    private XWikiContext context;

    private Map<String, Map<String, XWikiDocument>> databases = new HashMap<String, Map<String, XWikiDocument>>();

    private static final String MAIN_WIKI_NAME = "xwiki";

    private static final String TARGET_WIKI_NAME = "wikitosave";

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        this.context = new XWikiContext();
        this.context.setDatabase(MAIN_WIKI_NAME);
        this.context.setMainXWiki(MAIN_WIKI_NAME);

        this.databases.put(MAIN_WIKI_NAME, new HashMap<String, XWikiDocument>());

        Mock mockXWiki = mock(XWiki.class, new Class[] {}, new Object[] {});
        mockXWiki.stubs().method("Param").will(returnValue(""));

        Mock mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {
            mockXWiki.proxy(), this.context});

        Mock mockXWikiRightService = mock(XWikiRightServiceImpl.class, new Class[] {}, new Object[] {});

        mockXWiki.stubs().method("getDocument").will(new CustomStub("Implements XWiki.getDocument")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                String docFullName = (String) invocation.parameterValues.get(0);
                XWikiDocument shallowDoc = new XWikiDocument();
                shallowDoc.setFullName(docFullName);

                String database = shallowDoc.getDatabase();

                if (database == null) {
                    database = context.getDatabase();
                }

                if (database == null || database.length() == 0)
                    database = MAIN_WIKI_NAME;

                if (!databases.containsKey(database))
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Database " + database + " does not exists.");

                Map<String, XWikiDocument> docs = databases.get(database);

                if (docs.containsKey(shallowDoc.getFullName())) {
                    return docs.get(shallowDoc.getFullName());
                } else {
                    return shallowDoc;
                }
            }
        });
        mockXWiki.stubs().method("saveDocument").will(new CustomStub("Implements XWiki.saveDocument")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);

                String database = document.getDatabase();

                if (database == null) {
                    database = context.getDatabase();
                }

                if (database == null || database.length() == 0)
                    database = MAIN_WIKI_NAME;

                if (!databases.containsKey(database))
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Database " + database + " does not exists.");

                Map<String, XWikiDocument> docs = databases.get(database);

                document.setNew(false);
                document.setDatabase(context.getDatabase());

                docs.put(document.getFullName(), document);

                return null;
            }
        });
        mockXWiki.stubs().method("getClass").will(new CustomStub("Implements XWiki.getClass")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                String classFullName = (String) invocation.parameterValues.get(0);
                XWikiContext context = (XWikiContext) invocation.parameterValues.get(1);

                XWikiDocument doc = context.getWiki().getDocument(classFullName, context);

                return doc.getxWikiClass();
            }
        });
        mockXWiki.stubs().method("clearName").will(new CustomStub("Implements XWiki.clearName")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                return invocation.parameterValues.get(0);
            }
        });
        mockXWiki.stubs().method("getStore").will(returnValue(mockXWikiStore.proxy()));
        mockXWiki.stubs().method("getRightService").will(returnValue(mockXWikiRightService.proxy()));

        this.context.setWiki((XWiki) mockXWiki.proxy());

        mockXWikiStore.stubs().method("createWiki").will(new CustomStub("Implements XWikiStoreInterface.createWiki")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                String wikiName = (String) invocation.parameterValues.get(0);

                if (databases.containsKey(wikiName))
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CREATE_DATABASE, "Database " + wikiName
                            + " already exists.");

                databases.put(wikiName, new HashMap<String, XWikiDocument>());

                return null;
            }
        });

        mockXWikiRightService.stubs().method("hasProgrammingRights").will(returnValue(true));
    }

    // ///////////////////////////////////////////////////////////////////////////////////////:
    // Tests

    private static final String DOCSPACE = "DocumentSpace";

    private static final String DOCNAME = "DocumentName";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    public void testSaveDocument() throws XWikiException
    {
        context.setDatabase(MAIN_WIKI_NAME);

        databases.clear();
        databases.put(TARGET_WIKI_NAME, new HashMap<String, XWikiDocument>());

        // ///

        XWikiDocument doc = new XWikiDocument("DocumentSpace", "DocumentName");

        WikiManager.getInstance().saveDocument(TARGET_WIKI_NAME, doc, "", context);

        assertEquals(MAIN_WIKI_NAME, context.getDatabase());
        assertTrue(databases.containsKey(TARGET_WIKI_NAME)
            && databases.get(TARGET_WIKI_NAME).containsKey(doc.getFullName()));
    }

    public void testGetDocument() throws XWikiException
    {
        testSaveDocument();

        // ///

        XWikiDocument doc = WikiManager.getInstance().getDocument(TARGET_WIKI_NAME, DOCFULLNAME, context);

        assertFalse(doc.isNew());
        assertEquals(MAIN_WIKI_NAME, context.getDatabase());
    }

    public void testGetWikiFromNameWhenInAnotherWiki() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        databases.get(MAIN_WIKI_NAME).put("XWiki.XWikiServerWikiname", doc);

        context.setDatabase("anotherwiki");

        Wiki wiki = WikiManager.getInstance().getWikiFromName("WikInamE", context);

        assertSame(doc, wiki.getDocument());
    }

    public void testGetWikiAliasWhenDocumentDoesNorExists() throws XWikiException
    {
        try {
            WikiManager.getInstance().getWikiAlias("WikInamE", 0, true, context);

            fail("getWikiAlias should throw WikiManagerException when alias document does not exists");
        } catch (WikiManagerException expected) {
            // getWikiAlias should throw WikiManagerException when alias document does not exists
            assertEquals(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS, expected.getCode());
        }
    }

    public void testGetWikiAliasWhenDocumentDoesNotContainsClass() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        databases.get(MAIN_WIKI_NAME).put("XWiki.XWikiServerWikiname", doc);
        
        try {
            WikiManager.getInstance().getWikiAlias("WikInamE", 0, true, context);

            fail("getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists");
        } catch (WikiManagerException expected) {
            // getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists
            assertEquals(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS, expected.getCode());
        }
    }
    
    public void testGetWikiAlias() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        databases.get(MAIN_WIKI_NAME).put("XWiki.XWikiServerWikiname", doc);
        
        try {
            WikiManager.getInstance().getWikiAlias("WikInamE", 0, true, context);

            fail("getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists");
        } catch (WikiManagerException expected) {
            // getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists
            assertEquals(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS, expected.getCode());
        }
    }
}
