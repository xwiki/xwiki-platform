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
package com.xpn.xwiki.plugin.feed;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;

/**
 * Unit tests for {@link SyndEntryDocumentSource}.
 */
public class SyndEntryDocumentSourceTest extends AbstractBridgedXWikiComponentTestCase
{
    public static final String INCONSISTENCY = "Inconsistency!";

    public static final String POLYMORPHISM_INCONSISTENCY = "Polymorphism inconsistency!";

    public static final String ACCESS_RIGHTS_VIOLATED = "Access rights are violated!";

    public static final String PARAMETERS_IGNORED = "Parameters are ignored!";

    public static final String SVG_MIME_TYPE = "image/svg+xml";

    public static final String PNG_MIME_TYPE = "image/png";

    public static final String ARTICLE_CLASS_NAME = "XWiki.ArticleClass";

    protected SyndEntryDocumentSource source;

    protected XWikiDocument doc;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        mockUp();

        getContext().setUser("Condor");

        doc = new XWikiDocument(new DocumentReference("Wiki", "MilkyWay", "Fidis"));
        doc.setCreator("Condor");
        doc.setAuthor("Albatross");
        doc.setTitle("Fidis from MilkyWay");
        doc.setContent("blah blah blah..");
        doc.setSyntax(Syntax.XWIKI_1_0);

        initArticleClass();

        doc.createNewObject(ARTICLE_CLASS_NAME, getContext());
        doc.setStringValue(ARTICLE_CLASS_NAME, "title", "Old story");
        doc.setStringValue(ARTICLE_CLASS_NAME, "content", "Once upon a <i>time</i> there was..");
        List<String> categories = new ArrayList<String>();
        categories.add("News");
        categories.add("Information");
        doc.setStringListValue(ARTICLE_CLASS_NAME, "category", categories);

        getContext().getWiki().saveDocument(doc, getContext());
        getContext().setDoc(doc);

        // Ensure that no Velocity Templates are going to be used when executing Velocity since otherwise
        // the Velocity init would fail (since by default the macros.vm templates wouldn't be found as we're
        // not providing it in our unit test resources).
        getConfigurationSource().setProperty("xwiki.render.velocity.macrolist", "");

        source = new SyndEntryDocumentSource();
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Setup display configuration.
        Mock mockDisplayConfiguration = registerMockComponent(DisplayConfiguration.class);
        mockDisplayConfiguration.stubs().method("getDocumentDisplayerHint").will(returnValue("default"));
        mockDisplayConfiguration.stubs().method("getTitleHeadingDepth").will(returnValue(2));
    }

    private void mockUp() throws Exception
    {
        final Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();
        final XWikiContext context = getContext();

        // Set URL/Request
        context.setRequest(new XWikiServletRequestStub());
        context.setURL(new URL("http://localhost:8080/xwiki/bin/view/MilkyWay/Fidis"));

        final XWiki xwiki = new XWiki(new XWikiConfig(), context)
        {
            @Override
            public String getXWikiPreference(String prefname, String defaultValue, XWikiContext context)
            {
                return defaultValue;
            }
        };
        context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/", "bin/"));

        final Mock mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {xwiki, context});
        mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    if (docs.containsKey(shallowDoc.getName())) {
                        return docs.get(shallowDoc.getName());
                    } else {
                        return shallowDoc;
                    }
                }
            });
        mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    docs.put(document.getName(), document);
                    return null;
                }
            });
        mockXWikiStore.stubs().method("getTranslationList").will(returnValue(Collections.EMPTY_LIST));
        mockXWikiStore.stubs().method("exists").will(returnValue(false));

        final Mock mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {
            xwiki, context});
        mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));
        mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(returnValue(null));

        xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore.proxy());

        final Mock mockXWikiRightsService = mock(XWikiRightServiceImpl.class, new Class[] {}, new Object[] {});
        mockXWikiRightsService.stubs().method("hasAccessLevel").will(
            new CustomStub("Implements XWikiRightService.hasAccessLevel")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    // String right = (String) invocation.parameterValues.get(0);
                    String user = (String) invocation.parameterValues.get(1);
                    // String doc = (String) invocation.parameterValues.get(2);
                    // we give access to all the users with an even name length
                    return new Boolean(user.length() % 2 == 0);
                }
            });
        xwiki.setRightService((XWikiRightService) mockXWikiRightsService.proxy());
    }

    protected BaseClass initArticleClass() throws XWikiException
    {
        XWikiDocument doc = getContext().getWiki().getDocument(ARTICLE_CLASS_NAME, getContext());
        boolean needsUpdate = doc.isNew();

        BaseClass bclass = doc.getXClass();
        bclass.setName(ARTICLE_CLASS_NAME);

        needsUpdate |= bclass.addTextField("title", "Title", 64);
        needsUpdate |= bclass.addTextAreaField("content", "Content", 45, 4);
        needsUpdate |= bclass.addTextField("category", "Category", 64);

        if (needsUpdate) {
            getContext().getWiki().saveDocument(doc, getContext());
        }
        return bclass;
    }

    protected SyndEntryImpl source(Object obj)
    {
        return source(obj, Collections.EMPTY_MAP);
    }

    protected SyndEntryImpl source(Object obj, Map params)
    {
        SyndEntryImpl entry = new SyndEntryImpl();
        try {
            source.source(entry, obj, params, getContext());
        } catch (Exception e) {
        }
        return entry;
    }

    /**
     * Computes the sum of lengths of all the text nodes from the given XML fragment.
     * 
     * @param xmlFragment the XML fragment to be parsed
     * @return the number of characters in all the text nodes within the given XML fragment
     */
    protected int getXMLContentLength(String xmlFragment)
    {
        return SyndEntryDocumentSource.innerTextLength(SyndEntryDocumentSource.tidy(xmlFragment,
            SyndEntryDocumentSource.TIDY_HTML_CONFIG));
    }

    /**
     * Tests if two successive calls of the source method with the same argument have the same result.
     */
    public void testSourceConsistency()
    {
        Assert.assertEquals(INCONSISTENCY, source(doc), source(doc));
    }

    /**
     * Tests if different calls of the source method have the same result when the argument passed points to the same
     * document, irrespective of its type: {@link XWikiDocument}, {@link Document}, and {@link String}.
     */
    public void testSourcePolymorphism()
    {
        SyndEntryImpl fromXDoc = source(doc);
        SyndEntryImpl fromDoc = source(doc.newDocument(getContext()));
        SyndEntryImpl fromFullName = source(doc.getFullName());
        Assert.assertEquals(POLYMORPHISM_INCONSISTENCY, fromXDoc, fromDoc);
        Assert.assertEquals(POLYMORPHISM_INCONSISTENCY, fromXDoc, fromFullName);
        Assert.assertEquals(POLYMORPHISM_INCONSISTENCY, fromDoc, fromFullName);
    }

    /**
     * Tests if the source method obeys the access rights.
     * 
     * @throws XWikiException
     */
    public void testSourceAccessRights() throws XWikiException
    {
        // odd user name length implies no access rights
        getContext().setUser("XWiki.Albatross");
        try {
            source.source(new SyndEntryImpl(), doc, Collections.EMPTY_MAP, getContext());
            Assert.fail(ACCESS_RIGHTS_VIOLATED);
        } catch (XWikiException expected) {
            // we should get an exception
            Assert.assertEquals(XWikiException.ERROR_XWIKI_ACCESS_DENIED, expected.getCode());
        }
        // even user name length implies all access rights
        getContext().setUser("Condor");
        source.source(new SyndEntryImpl(), doc, Collections.EMPTY_MAP, getContext());
        // we shouldn't get an exception
    }

    /**
     * Tests if {@link SyndEntryDocumentSource#CONTENT_TYPE} parameter is used correctly.
     */
    public void testSourceContentType()
    {
        Map instanceParams = new HashMap();
        instanceParams.put(SyndEntryDocumentSource.CONTENT_TYPE, SVG_MIME_TYPE);
        source.setParams(instanceParams);
        Assert.assertEquals(PARAMETERS_IGNORED, SVG_MIME_TYPE, source(doc).getDescription().getType());

        Map methodParams = new HashMap();
        methodParams.put(SyndEntryDocumentSource.CONTENT_TYPE, PNG_MIME_TYPE);
        SyndEntry entry = source(doc, methodParams);
        Assert.assertEquals(PARAMETERS_IGNORED, PNG_MIME_TYPE, entry.getDescription().getType());
    }

    /**
     * Tests if {@link SyndEntryDocumentSource#CONTENT_LENGTH} parameter is used correctly when the
     * {@link SyndEntryDocumentSource#CONTENT_TYPE} is <i>text/plain</i>.
     */
    public void testArticleSourcePlainContentLength()
    {
        int maxLength = 15;
        Map params = new HashMap();
        params.put(SyndEntryDocumentSource.CONTENT_TYPE, "text/plain");
        params.put(SyndEntryDocumentSource.CONTENT_LENGTH, new Integer(maxLength));
        params.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, ARTICLE_CLASS_NAME + "_content");
        source.setParams(params);
        doc.setStringValue(ARTICLE_CLASS_NAME, "content", "Somewhere in la Mancha, in a place..");
        Assert.assertTrue(doc.display("content", getContext()).length() > maxLength);
        int descriptionLength = source(doc).getDescription().getValue().length();
        Assert.assertTrue(PARAMETERS_IGNORED, descriptionLength <= maxLength);
    }

    /**
     * Tests if {@link SyndEntryDocumentSource#CONTENT_LENGTH} parameter is used correctly when the
     * {@link SyndEntryDocumentSource#CONTENT_TYPE} is <i>text/html</i>.
     */
    public void testArticleSourceHTMLContentLength()
    {
        int maxLength = 16;
        Map params = new HashMap();
        params.put(SyndEntryDocumentSource.CONTENT_TYPE, "text/html");
        params.put(SyndEntryDocumentSource.CONTENT_LENGTH, new Integer(maxLength));
        params.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, ARTICLE_CLASS_NAME + "_content");
        doc.setStringValue(ARTICLE_CLASS_NAME, "content",
            "Somewhere \n\tin   <i>la</i> <a href=\"http://www.mancha.es\">  Mancha</a>, in a place..");
        Assert.assertTrue(getXMLContentLength(doc.display("content", getContext())) > maxLength);
        String description = source(doc, params).getDescription().getValue();
        int descriptionLength = getXMLContentLength(description);
        Assert.assertTrue(PARAMETERS_IGNORED, descriptionLength <= maxLength);
    }

    public void testArticleSourceXMLContentLength()
    {
        int maxLength = 17;
        Map params = new HashMap();
        params.put(SyndEntryDocumentSource.CONTENT_TYPE, "text/xml");
        params.put(SyndEntryDocumentSource.CONTENT_LENGTH, new Integer(maxLength));
        params.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, ARTICLE_CLASS_NAME + "_content");
        doc.setStringValue(ARTICLE_CLASS_NAME, "content",
            "<text>Somewhere \n\tin   la <region>  Mancha</region>, in a place..</text>");
        Assert.assertTrue(getXMLContentLength(doc.display("content", getContext())) > maxLength);
        String description = source(doc, params).getDescription().getValue();
        int descriptionLength = getXMLContentLength(description);
        Assert.assertTrue(PARAMETERS_IGNORED, descriptionLength <= maxLength);
    }
    
    public void testPreviewContentEncoding()
    {
        String snippet = "<p>Test ê</p>";
        String transformedHTML = SyndEntryDocumentSource.getHTMLPreview(snippet, 10);
        Assert.assertEquals(snippet, transformedHTML);
        String transformedXML = SyndEntryDocumentSource.getXMLPreview(snippet, 10);
        Assert.assertEquals(snippet, transformedXML);

        String plainSnippet = " Test Text ê Rest ";
        String previewExpected = "Test Text ê";
        String transformedPlain = SyndEntryDocumentSource.getPlainPreview(plainSnippet, 12);
        Assert.assertEquals(previewExpected, transformedPlain);
    }
}
