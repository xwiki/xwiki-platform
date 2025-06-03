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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SyndEntryDocumentSource}.
 */
@OldcoreTest
@AllComponents
class SyndEntryDocumentSourceTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    public static final String INCONSISTENCY = "Inconsistency!";

    public static final String POLYMORPHISM_INCONSISTENCY = "Polymorphism inconsistency!";

    public static final String ACCESS_RIGHTS_VIOLATED = "Access rights are violated!";

    public static final String PARAMETERS_IGNORED = "Parameters are ignored!";

    public static final String SVG_MIME_TYPE = "image/svg+xml";

    public static final String PNG_MIME_TYPE = "image/png";

    public static final String ARTICLE_CLASS_NAME = "XWiki.ArticleClass";

    protected SyndEntryDocumentSource source;

    protected XWikiDocument doc;

    @BeforeEach
    protected void beforeEach() throws Exception
    {
        mockUp();

        this.oldcore.getXWikiContext().setUser("Condor");

        this.doc =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "MilkyWay", "Fidis"));
        this.doc.setCreator("Condor");
        this.doc.setAuthor("Albatross");
        this.doc.setTitle("Fidis from MilkyWay");
        this.doc.setContent("blah blah blah..");
        this.doc.setSyntax(Syntax.XWIKI_2_1);

        initArticleClass();

        this.doc.createNewObject(ARTICLE_CLASS_NAME, this.oldcore.getXWikiContext());
        this.doc.setStringValue(ARTICLE_CLASS_NAME, "title", "Old story");
        this.doc.setStringValue(ARTICLE_CLASS_NAME, "content", "Once upon a <i>time</i> there was..");
        List<String> categories = new ArrayList<String>();
        categories.add("News");
        categories.add("Information");
        this.doc.setStringListValue(ARTICLE_CLASS_NAME, "category", categories);

        this.oldcore.getXWikiContext().getWiki().saveDocument(this.doc, this.oldcore.getXWikiContext());
        this.doc = this.oldcore.getXWikiContext().getWiki().getDocument(this.doc, this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(this.doc);

        // Ensure that no Velocity Templates are going to be used when executing Velocity since otherwise
        // the Velocity init would fail (since by default the macros.vm templates wouldn't be found as we're
        // not providing it in our unit test resources).
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.render.velocity.macrolist", "");

        this.source = new SyndEntryDocumentSource();
    }

    @AfterComponent
    public void afterComponent() throws Exception
    {
        // Unregister XWikiCfgConfigurationSource so that it's mocked by MockitoOldcore
        this.oldcore.getMocker().unregisterComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT);

        // Setup display configuration.
        DisplayConfiguration mockDisplayConfiguration =
            this.oldcore.getMocker().registerMockComponent(DisplayConfiguration.class);
        when(mockDisplayConfiguration.getDocumentDisplayerHint()).thenReturn("default");
        when(mockDisplayConfiguration.getTitleHeadingDepth()).thenReturn(2);
    }

    private void mockUp() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();

        // Set URL/Request
        context.setRequest(new XWikiServletRequestStub());
        context.setResponse(new XWikiServletResponseStub());
        context.setURL(new URL("http://localhost:8080/xwiki/bin/view/MilkyWay/Fidis"));

        context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/", "bin/"));

        when(this.oldcore.getMockRightService().hasAccessLevel(any(), any(), any(), any())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                // String right = invocation.getArgument(0);
                String user = invocation.getArgument(1);
                // String doc = invocation.getArgument(2);
                // we give access to all the users with an even name length
                return new Boolean(user.length() % 2 == 0);
            }
        });
    }

    protected BaseClass initArticleClass() throws XWikiException
    {
        XWikiDocument doc =
            this.oldcore.getXWikiContext().getWiki().getDocument(ARTICLE_CLASS_NAME, this.oldcore.getXWikiContext());
        boolean needsUpdate = doc.isNew();

        BaseClass bclass = doc.getXClass();
        bclass.setName(ARTICLE_CLASS_NAME);

        needsUpdate |= bclass.addTextField("title", "Title", 64);
        needsUpdate |= bclass.addTextAreaField("content", "Content", 45, 4);
        needsUpdate |= bclass.addTextField("category", "Category", 64);

        if (needsUpdate) {
            this.oldcore.getXWikiContext().getWiki().saveDocument(doc, this.oldcore.getXWikiContext());
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
            this.source.source(entry, obj, params, this.oldcore.getXWikiContext());
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
        return SyndEntryDocumentSource
            .innerTextLength(SyndEntryDocumentSource.tidy(xmlFragment, SyndEntryDocumentSource.TIDY_HTML_CONFIG));
    }

    /**
     * Tests if two successive calls of the source method with the same argument have the same result.
     */
    @Test
    public void testSourceConsistency()
    {
        assertEquals(source(this.doc), source(this.doc), INCONSISTENCY);
    }

    /**
     * Tests if different calls of the source method have the same result when the argument passed points to the same
     * document, irrespective of its type: {@link XWikiDocument}, {@link Document}, and {@link String}.
     */
    @Test
    public void testSourcePolymorphism()
    {
        SyndEntryImpl fromXDoc = source(this.doc);
        SyndEntryImpl fromDoc = source(this.doc.newDocument(this.oldcore.getXWikiContext()));
        SyndEntryImpl fromFullName = source(this.doc.getFullName());
        assertEquals(fromXDoc, fromDoc, POLYMORPHISM_INCONSISTENCY);
        assertEquals(fromXDoc, fromFullName, POLYMORPHISM_INCONSISTENCY);
        assertEquals(fromDoc, fromFullName, POLYMORPHISM_INCONSISTENCY);
    }

    /**
     * Tests if the source method obeys the access rights.
     * 
     * @throws XWikiException
     */
    @Test
    public void testSourceAccessRights() throws XWikiException
    {
        // odd user name length implies no access rights
        this.oldcore.getXWikiContext().setUser("XWiki.Albatross");
        try {
            this.source.source(new SyndEntryImpl(), doc, Collections.EMPTY_MAP, this.oldcore.getXWikiContext());
            fail(ACCESS_RIGHTS_VIOLATED);
        } catch (XWikiException expected) {
            // we should get an exception
            assertEquals(XWikiException.ERROR_XWIKI_ACCESS_DENIED, expected.getCode());
        }
        // even user name length implies all access rights
        this.oldcore.getXWikiContext().setUser("Condor");
        this.source.source(new SyndEntryImpl(), doc, Collections.EMPTY_MAP, this.oldcore.getXWikiContext());
        // we shouldn't get an exception
    }

    /**
     * Tests if {@link SyndEntryDocumentSource#CONTENT_TYPE} parameter is used correctly.
     */
    @Test
    public void testSourceContentType()
    {
        Map instanceParams = new HashMap();
        instanceParams.put(SyndEntryDocumentSource.CONTENT_TYPE, SVG_MIME_TYPE);
        this.source.setParams(instanceParams);
        assertEquals(SVG_MIME_TYPE, source(this.doc).getDescription().getType(), PARAMETERS_IGNORED);

        Map methodParams = new HashMap();
        methodParams.put(SyndEntryDocumentSource.CONTENT_TYPE, PNG_MIME_TYPE);
        SyndEntry entry = source(this.doc, methodParams);
        assertEquals(PNG_MIME_TYPE, entry.getDescription().getType(), PARAMETERS_IGNORED);
    }

    /**
     * Tests if {@link SyndEntryDocumentSource#CONTENT_LENGTH} parameter is used correctly when the
     * {@link SyndEntryDocumentSource#CONTENT_TYPE} is <i>text/plain</i>.
     */
    @Test
    public void testArticleSourcePlainContentLength()
    {
        int maxLength = 15;
        Map params = new HashMap();
        params.put(SyndEntryDocumentSource.CONTENT_TYPE, "text/plain");
        params.put(SyndEntryDocumentSource.CONTENT_LENGTH, maxLength);
        params.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, ARTICLE_CLASS_NAME + "_content");
        this.source.setParams(params);
        this.doc.setStringValue(ARTICLE_CLASS_NAME, "content", "Somewhere in la Mancha, in a place..");
        assertTrue(this.doc.display("content", this.oldcore.getXWikiContext()).length() > maxLength);
        int descriptionLength = source(this.doc).getDescription().getValue().length();
        assertTrue(descriptionLength <= maxLength, PARAMETERS_IGNORED);
    }

    /**
     * Tests if {@link SyndEntryDocumentSource#CONTENT_LENGTH} parameter is used correctly when the
     * {@link SyndEntryDocumentSource#CONTENT_TYPE} is <i>text/html</i>.
     */
    @Test
    public void testArticleSourceHTMLContentLength()
    {
        int maxLength = 16;
        Map params = new HashMap();
        params.put(SyndEntryDocumentSource.CONTENT_TYPE, "text/html");
        params.put(SyndEntryDocumentSource.CONTENT_LENGTH, maxLength);
        params.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, ARTICLE_CLASS_NAME + "_content");
        this.doc.setStringValue(ARTICLE_CLASS_NAME, "content",
            "Somewhere \n\tin   <i>la</i> <a href=\"http://www.mancha.es\">  Mancha</a>, in a place..");
        assertTrue(getXMLContentLength(this.doc.display("content", this.oldcore.getXWikiContext())) > maxLength);
        String description = source(this.doc, params).getDescription().getValue();
        int descriptionLength = getXMLContentLength(description);
        assertTrue(descriptionLength <= maxLength, PARAMETERS_IGNORED);
    }

    @Test
    public void testArticleSourceXMLContentLength()
    {
        int maxLength = 17;
        Map params = new HashMap();
        params.put(SyndEntryDocumentSource.CONTENT_TYPE, "text/xml");
        params.put(SyndEntryDocumentSource.CONTENT_LENGTH, maxLength);
        params.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, ARTICLE_CLASS_NAME + "_content");
        this.doc.setStringValue(ARTICLE_CLASS_NAME, "content",
            "<text>Somewhere \n\tin   la <region>  Mancha</region>, in a place..</text>");
        assertTrue(getXMLContentLength(this.doc.display("content", this.oldcore.getXWikiContext())) > maxLength);
        String description = source(this.doc, params).getDescription().getValue();
        int descriptionLength = getXMLContentLength(description);
        assertTrue(descriptionLength <= maxLength, PARAMETERS_IGNORED);
    }

    @Test
    public void testPreviewContentEncoding()
    {
        String snippet = "<p>Test ê</p>";
        String transformedHTML = SyndEntryDocumentSource.getHTMLPreview(snippet, 10);
        assertEquals(snippet, transformedHTML);
        String transformedXML = SyndEntryDocumentSource.getXMLPreview(snippet, 10);
        assertEquals(snippet, transformedXML);

        String plainSnippet = " Test Text ê Rest ";
        String previewExpected = "Test Text ê";
        String transformedPlain = SyndEntryDocumentSource.getPlainPreview(plainSnippet, 12);
        assertEquals(previewExpected, transformedPlain);
    }
}
