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

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.lib.action.CustomAction;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.task.InstallRequest;
import org.xwiki.extension.task.Task;
import org.xwiki.extension.task.TaskManager;
import org.xwiki.extension.task.UninstallRequest;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.extension.xar.internal.repository.XarLocalExtension;
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

    private Map<DocumentReference, Map<String, XWikiDocument>> documents =
        new HashMap<DocumentReference, Map<String, XWikiDocument>>();

    private ExtensionId localXarExtensiontId1;

    private ExtensionId localXarExtensiontId2;

    private RepositoryUtil repositoryUtil;
    
    private TaskManager taskManager;
    
    private LocalExtensionRepository localExtensionRepository;
    
    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        // mock

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);
        getContext().setDatabase("xwiki");

        this.localXarExtensiontId1 = new ExtensionId("test", "1.0");
        this.localXarExtensiontId2 = new ExtensionId("test", "2.0");
        
        // classes
        
        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

        // checking

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockXWiki).getDocument(with(any(DocumentReference.class)), with(any(XWikiContext.class)));
                will(new CustomAction("getDocument")
                {
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        Map<String, XWikiDocument> documentLanguages = documents.get(invocation.getParameter(0));

                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<String, XWikiDocument>();
                            documents.put((DocumentReference) invocation.getParameter(0), documentLanguages);
                        }

                        XWikiDocument document = documentLanguages.get("en");

                        if (document == null) {
                            document = new XWikiDocument((DocumentReference) invocation.getParameter(0));
                        }

                        return document;
                    }
                });

                allowing(mockXWiki).saveDocument(with(any(XWikiDocument.class)), with(any(String.class)),
                    with(any(XWikiContext.class)));
                will(new CustomAction("saveDocument")
                {
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        XWikiDocument document = (XWikiDocument) invocation.getParameter(0);

                        document.incrementVersion();
                        document.setNew(false);

                        Map<String, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<String, XWikiDocument>();
                            documents.put(document.getDocumentReference(), documentLanguages);
                        }

                        documentLanguages.put(document.getRealLanguage(), document);

                        return null;
                    }
                });

                allowing(mockXWiki).deleteDocument(with(any(XWikiDocument.class)), with(any(XWikiContext.class)));
                will(new CustomAction("deleteDocument")
                {
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        XWikiDocument document = (XWikiDocument) invocation.getParameter(0);

                        Map<String, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                        if (documentLanguages != null) {
                            documentLanguages.remove(document.getRealLanguage());
                        }

                        return null;
                    }
                });
                
                allowing(mockXWiki).getXClass(with(any(DocumentReference.class)), with(any(XWikiContext.class)));
                will(new CustomAction("getXClass")
                {
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        DocumentReference documentReference = (DocumentReference) invocation.getParameter(0);

                        return classes.get(documentReference.getName());
                    }
                });
            }
        });

        // lookup

        this.taskManager = getComponentManager().lookup(TaskManager.class);
        this.localExtensionRepository = getComponentManager().lookup(LocalExtensionRepository.class, "xar");
    }

    private XarLocalExtension install(ExtensionId extensionId, String namespace) throws Exception
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(extensionId);
        installRequest.addNamespace(namespace);
        Task installTask = this.taskManager.install(installRequest);

        if (installTask.getExceptions() != null) {
            throw installTask.getExceptions().get(0);
        }

        return (XarLocalExtension) this.localExtensionRepository.resolve(extensionId);
    }
    
    protected void uninstall(ExtensionId extensionId, String namespace) throws Exception
    {
        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(extensionId);
        uninstallRequest.addNamespace(namespace);
        Task uninstallTask = this.taskManager.uninstall(uninstallRequest);

        if (uninstallTask.getExceptions() != null) {
            throw uninstallTask.getExceptions().get(0);
        }
    }

    @Test
    public void testInstall() throws Exception
    {
        // install

        install(this.localXarExtensiontId1, "wiki");

        // validate

        XWikiDocument page = this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", page.isNew());

        Assert.assertEquals("Wrong content", "content", page.getContent());
        Assert.assertEquals("Wrong author", "XWiki.author", page.getAuthor());
        Assert.assertEquals("Wrong versions", "1.1", page.getVersion());

        BaseClass baseClass = page.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiDocument page1 =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertFalse("Document wiki.space2.page2 has not been saved in the database", page1.isNew());
    }

    @Test
    public void testUpgrade() throws Exception
    {
        install(this.localXarExtensiontId1, "wiki");

        // upgrade

        install(this.localXarExtensiontId2, "wiki");

        // validate

        XWikiDocument page = this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", page.isNew());

        Assert.assertEquals("Wrong content", "content 2", page.getContent());
        Assert.assertEquals("Wrong author", "XWiki.author", page.getAuthor());
        Assert.assertEquals("Wrong versions", "2.1", page.getVersion());

        BaseClass baseClass = page.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiDocument page2 =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space2", "page2"), getContext());

        Assert.assertFalse("Document wiki.space2.page2 has not been saved in the database", page2.isNew());

        XWikiDocument page1 =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertTrue("Document wiki.space1.page1 has not been removed from the database", page1.isNew());
    }

    @Test
    public void testUninstall() throws Exception
    {
        install(this.localXarExtensiontId1, "wiki");

        // uninstall

        uninstall(this.localXarExtensiontId1, "wiki");

        // validate

        XWikiDocument page = this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertTrue("Document wiki.space.page has not been removed from the database", page.isNew());

        XWikiDocument page1 =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertTrue("Document wiki.space1.page1 has not been removed from the database", page1.isNew());
    }
}
