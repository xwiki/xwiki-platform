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

package org.xwiki.extension.xar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.lib.action.CustomAction;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.handler.ExtensionHandler;
import org.xwiki.extension.repository.internal.DefaultLocalExtension;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

public class XarExtensionHandlerTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private Map<DocumentReference, XWikiDocument> documents = new HashMap<DocumentReference, XWikiDocument>();

    private XarExtensionHandler handler;

    private ExtensionId localXarExtensiontId;

    private DefaultLocalExtension localXarExtension;

    private ExtensionId localXarExtensiontId2;

    private DefaultLocalExtension localXarExtension2;

    private RepositoryUtil repositoryUtil;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil = new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource());
        this.repositoryUtil.setup();

        // mock

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);
        getContext().setDatabase("xwiki");

        this.localXarExtensiontId = new ExtensionId("test", "1.0");
        this.localXarExtension = new DefaultLocalExtension(null, this.localXarExtensiontId, "xar");
        this.localXarExtension.setFile(new File(this.repositoryUtil.getLocalRepository(), "test-1.0.xar"));

        this.localXarExtensiontId2 = new ExtensionId("test", "2.0");
        this.localXarExtension2 = new DefaultLocalExtension(null, this.localXarExtensiontId2, "xar");
        this.localXarExtension2.setFile(new File(this.repositoryUtil.getLocalRepository(), "test-2.0.xar"));

        // checking

        getMockery().checking(new Expectations() {{
            allowing(mockXWiki).getDocument(with(any(DocumentReference.class)), with(any(XWikiContext.class))); will(new CustomAction("getDocument")
            {
                public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                {
                    XWikiDocument document = documents.get(invocation.getParameter(0));
                    
                    if (document == null) {
                        document = new XWikiDocument((DocumentReference)invocation.getParameter(0));
                    }

                    return document;
                }
            });

            allowing(mockXWiki).saveDocument(with(any(XWikiDocument.class)), with(any(String.class)), with(any(XWikiContext.class))); will(new CustomAction("saveDocument")
            {
                public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument)invocation.getParameter(0);

                    document.incrementVersion();
                    document.setNew(false);

                    documents.put(document.getDocumentReference(), document);

                    return null;
                }
            });

            allowing(mockXWiki).deleteDocument(with(any(XWikiDocument.class)), with(any(XWikiContext.class))); will(new CustomAction("deleteDocument")
            {
                public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                {
                    documents.remove(((XWikiDocument)invocation.getParameter(0)).getDocumentReference());
                    
                    return null;
                }
            });
        }});

        // lookup

        this.handler = (XarExtensionHandler) getComponentManager().lookup(ExtensionHandler.class, "xar");
    }

    @Test
    public void testInstall() throws Exception
    {
        // install

        this.handler.install(this.localXarExtension, "wiki");

        // validate

        XWikiDocument document =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", document.isNew());

        Assert.assertEquals("Wrong content", "content", document.getContent());
        Assert.assertEquals("Wrong author", "XWiki.author", document.getAuthor());
        Assert.assertEquals("Wrong versions", "1.1", document.getVersion());

        BaseClass baseClass = document.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());
    }

    @Test
    public void testUpgrade() throws Exception
    {
        XWikiDocument previousDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));

        previousDocument.setContent("previous content");
        previousDocument.setAuthor("XWiki.previousauthor");

        this.mockXWiki.saveDocument(previousDocument, "", getContext());

        // upgrade

        this.handler.upgrade(this.localXarExtension, this.localXarExtension2, "wiki");

        // validate

        XWikiDocument document =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", document.isNew());

        Assert.assertEquals("Wrong content", "content 2", document.getContent());
        Assert.assertEquals("Wrong author", "XWiki.author", document.getAuthor());
        Assert.assertEquals("Wrong versions", "2.1", document.getVersion());
        
        BaseClass baseClass = document.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());
    }

    @Test
    public void testUninstall() throws Exception
    {
        this.mockXWiki.saveDocument(new XWikiDocument(new DocumentReference("wiki", "space", "page")), "", getContext());

        // uninstall

        this.handler.uninstall(this.localXarExtension, "wiki");

        // validate

        XWikiDocument document =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertTrue("Document wiki.space.page has not been removed from the database", document.isNew());
    }
}
