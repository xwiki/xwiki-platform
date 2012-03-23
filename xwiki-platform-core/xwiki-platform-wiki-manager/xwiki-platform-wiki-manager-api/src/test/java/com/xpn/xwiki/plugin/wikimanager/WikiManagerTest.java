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
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.wikimanager.WikiManager}.
 * 
 * @version $Id$
 */
public class WikiManagerTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String MAIN_WIKI_NAME = "xwiki";

    private static final String TARGET_WIKI_NAME = "wikitosave";

    private Map<String, Map<String, XWikiDocument>> databases = new HashMap<String, Map<String, XWikiDocument>>();

    private WikiManager wikiManager;

    private Map<String, XWikiDocument> getDocuments(String database, boolean create) throws XWikiException
    {
        if (database == null) {
            database = getContext().getDatabase();
        }

        if (database == null || database.length() == 0) {
            database = MAIN_WIKI_NAME;
        }

        if (!this.databases.containsKey(database)) {
            if (create) {
                this.databases.put(database, new HashMap<String, XWikiDocument>());
            } else {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Database " + database + " does not exists.");
            }
        }

        return this.databases.get(database);
    }

    private XWikiDocument getDocument(DocumentReference documentReference) throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(documentReference);

        Map<String, XWikiDocument> docs = getDocuments(document.getDatabase(), false);

        if (docs.containsKey(document.getFullName())) {
            return docs.get(document.getFullName());
        } else {
            return document;
        }
    }
    
    private XWikiDocument getDocument(String documentFullName) throws XWikiException
    {
        XWikiDocument document = new XWikiDocument();
        document.setFullName(documentFullName);

        Map<String, XWikiDocument> docs = getDocuments(document.getDatabase(), false);

        if (docs.containsKey(document.getFullName())) {
            return docs.get(document.getFullName());
        } else {
            return document;
        }
    }

    private void saveDocument(XWikiDocument document) throws XWikiException
    {
        document.setNew(false);
        getDocuments(document.getDatabase(), true).put(document.getFullName(), document);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        getContext().setDatabase(MAIN_WIKI_NAME);
        getContext().setMainXWiki(MAIN_WIKI_NAME);

        this.databases.put(MAIN_WIKI_NAME, new HashMap<String, XWikiDocument>());

        Mock mockXWiki = mock(XWiki.class, new Class[] {}, new Object[] {});
        mockXWiki.stubs().method("Param").will(returnValue(""));

        Mock mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {
            mockXWiki.proxy(), getContext()});

        Mock mockXWikiRightService = mock(XWikiRightServiceImpl.class, new Class[] {}, new Object[] {});

        mockXWiki.stubs().method("getDocument").with(isA(DocumentReference.class), ANYTHING).will(new CustomStub("Implements XWiki.getDocument")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                return getDocument((DocumentReference) invocation.parameterValues.get(0));
            }
        });
        mockXWiki.stubs().method("getDocument").with(isA(String.class), ANYTHING).will(new CustomStub("Implements XWiki.getDocument")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                return getDocument((String) invocation.parameterValues.get(0));
            }
        });
        mockXWiki.stubs().method("saveDocument").will(new CustomStub("Implements XWiki.saveDocument")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                saveDocument((XWikiDocument) invocation.parameterValues.get(0));

                return null;
            }
        });
        mockXWiki.stubs().method("getXClass").will(new CustomStub("Implements XWiki.getClass")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                DocumentReference classReference = (DocumentReference) invocation.parameterValues.get(0);
                XWikiContext context = (XWikiContext) invocation.parameterValues.get(1);

                XWikiDocument doc = context.getWiki().getDocument(classReference, context);

                return doc.getXClass();
            }
        });
        mockXWiki.stubs().method("clearName").will(new CustomStub("Implements XWiki.clearName")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                return invocation.parameterValues.get(0);
            }
        });
        mockXWiki.stubs().method("getStore").will(returnValue(mockXWikiStore.proxy()));
        mockXWiki.stubs().method("getRightService").will(returnValue(mockXWikiRightService.proxy()));

        getContext().setWiki((XWiki) mockXWiki.proxy());

        mockXWikiStore.stubs().method("createWiki").will(new CustomStub("Implements XWikiStoreInterface.createWiki")
        {
            @Override
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

        this.wikiManager = new WikiManager(null);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////:
    // Tests

    private static final String DOCSPACE = "DocumentSpace";

    private static final String DOCNAME = "DocumentName";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    public void testGetDocument() throws XWikiException
    {
        saveDocument(new XWikiDocument(new DocumentReference(TARGET_WIKI_NAME, DOCSPACE, DOCNAME)));

        // ///

        XWikiDocument doc = this.wikiManager.getDocument(TARGET_WIKI_NAME, DOCFULLNAME, getContext());

        assertFalse(doc.isNew());
        assertEquals(MAIN_WIKI_NAME, getContext().getDatabase());
    }

    public void testGetWikiFromNameWhenInAnotherWiki() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        this.databases.get(MAIN_WIKI_NAME).put("XWiki.XWikiServerWikiname", doc);

        getContext().setDatabase("anotherwiki");

        Wiki wiki = this.wikiManager.getWikiFromName("WikInamE", getContext());

        assertSame(doc, wiki.getDocument());
    }

    public void testGetWikiAliasWhenDocumentDoesNorExists() throws XWikiException
    {
        try {
            this.wikiManager.getWikiAlias("WikInamE", 0, true, getContext());

            fail("getWikiAlias should throw WikiManagerException when alias document does not exists");
        } catch (WikiManagerException expected) {
            // getWikiAlias should throw WikiManagerException when alias document does not exists
            assertEquals(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS, expected.getCode());
        }
    }

    public void testGetWikiAliasWhenDocumentDoesNotContainsClass() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        this.databases.get(MAIN_WIKI_NAME).put("XWiki.XWikiServerWikiname", doc);

        try {
            this.wikiManager.getWikiAlias("WikInamE", 0, true, getContext());

            fail("getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists");
        } catch (WikiManagerException expected) {
            // getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists
            assertEquals(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS, expected.getCode());
        }
    }

    public void testGetWikiAlias() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        this.databases.get(MAIN_WIKI_NAME).put("XWiki.XWikiServerWikiname", doc);

        try {
            this.wikiManager.getWikiAlias("WikInamE", 0, true, getContext());

            fail("getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists");
        } catch (WikiManagerException expected) {
            // getWikiAlias should throw XObjectDocumentDoesNotExistException when alias document does not exists
            assertEquals(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS, expected.getCode());
        }
    }
}
