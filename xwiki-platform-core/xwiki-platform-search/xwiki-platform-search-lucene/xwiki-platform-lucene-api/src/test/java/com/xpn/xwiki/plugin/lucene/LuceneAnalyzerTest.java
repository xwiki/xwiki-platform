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
package com.xpn.xwiki.plugin.lucene;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import javax.script.SimpleScriptContext;
import javax.servlet.ServletContext;

import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.velocity.VelocityContext;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.context.internal.DefaultExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.internal.ScriptExecutionContextInitializer;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;

/**
 * Unit tests for {@link LucenePlugin}. with different Analyzer
 */
@MockingRequirement(DefaultExecutionContextManager.class)
@AllComponents
public class LuceneAnalyzerTest extends AbstractMockingComponentTestCase<ExecutionContextManager>
{
    /**
     * Make sure the index folder is not reused.
     */
    private final static String INDEXDIR = "target" + File.separator + "luceneAnalyzerTest-" + new Date().getTime();

    private XWikiDocument document;

    private XWikiContext xwikiContext;

    private XWiki xwiki;

    private MemoryConfigurationSource source;

    private LucenePlugin lucenePlugin;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        // create lucene index dir
        File f = new File(INDEXDIR);
        if (!f.exists()) {
            f.mkdirs();
        }

        // Using ClassImposteriser to be able to mock classes like XWiki and XWikiDocument
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);

        Utils.setComponentManager(getComponentManager());

        this.document = new XWikiDocument(new DocumentReference("xwiki", "space", "lucene analyzer test page"));
        this.document.setSyntax(Syntax.XWIKI_2_1);
        this.document.setContent("Paris Iasi Berlin München");
        this.document.setLocale(Locale.GERMAN);
    }

    private void prepareAndIndexWithAnalyzer(Class analyzer) throws Exception
    {
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext executionContext = new ExecutionContext();

        this.xwiki = getMockery().mock(XWiki.class);

        this.xwikiContext = new XWikiContext();
        this.xwikiContext.setDatabase("xwiki");
        this.xwikiContext.setWiki(this.xwiki);
        ServletContext mockServletContext = getMockery().mock(ServletContext.class);
        XWikiServletContext xwikiServletContext = new XWikiServletContext(mockServletContext);
        this.xwikiContext.setEngineContext(xwikiServletContext);

        final XWikiStoreInterface xwikiStore = getMockery().mock(XWikiStoreInterface.class);
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);

        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xwikiContext);
        executionContext.setProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID, new SimpleScriptContext());
        VelocityContext velocityContext = new VelocityContext();
        executionContext.setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID, velocityContext);
        final String analyzerToTest = analyzer.getName();

        getMockery().checking(new Expectations()
        {
            {
                allowing(execution).getContext();
                will(returnValue(executionContext));

                allowing(xwiki).Param("xwiki.plugins.lucene.indexdir");
                will(returnValue(INDEXDIR));

                allowing(xwiki).ParamAsLong("xwiki.plugins.lucene.indexinterval", 30l);
                will(returnValue(1l));

                allowing(xwiki).ParamAsLong("xwiki.plugins.lucene.maxQueueSize", 1000l);
                will(returnValue(1000l));

                allowing(xwiki)
                        .Param("xwiki.plugins.lucene.analyzer", "org.apache.lucene.analysis.standard.StandardAnalyzer");
                will(returnValue(analyzerToTest));

                ignoring(execution);

                allowing(xwiki).getDocument(with(any(DocumentReference.class)), with((any(XWikiContext.class))));
                will(returnValue(document));
                allowing(xwiki).prepareResources(with(any(XWikiContext.class)));
                allowing(xwiki).getLanguagePreference(with(any(XWikiContext.class)));
                will(returnValue("en"));
                allowing(xwiki).getStore();
                will(returnValue(xwikiStore));

                allowing(xwikiStore).loadXWikiDoc(with(any(XWikiDocument.class)), with(any(XWikiContext.class)));
                will(returnValue(document));
                ignoring(xwikiStore);
                allowing(xwiki).exists(with(any(DocumentReference.class)), with(any(XWikiContext.class)));
                will(returnValue(true));
                allowing(xwiki).getRightService();
                will(returnValue(rightService));

                allowing(rightService)
                        .hasAccessLevel(with(any(String.class)), with(any(String.class)), with(any(String.class)),
                                with(any(XWikiContext.class)));
                will(returnValue(true));
                ignoring(xwiki);
            }
        });

        lucenePlugin = new LucenePlugin("test", "test", this.xwikiContext);
        lucenePlugin.init(this.xwikiContext);
        lucenePlugin.queueDocument(this.document, this.xwikiContext);

        // wait for IndexUpdater-Thread to finish
        Thread.sleep(1000);
    }

    private String createQueryString(String searchString)
    {
        return "+(" + searchString + ") +hidden:false";
    }

    @Test
    public void searchWithStandardAnalyzer() throws Exception
    {
        prepareAndIndexWithAnalyzer(StandardAnalyzer.class);
        assertEquals(1, lucenePlugin.getLuceneDocCount());
        assertEquals(0, lucenePlugin.getQueueSize());
        String query = createQueryString("Paris");
        SearchResults searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.xwikiContext);

        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("München");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.xwikiContext);
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());
    }

    @Test
    public void searchWithGermanAnalyzer() throws Exception
    {
        prepareAndIndexWithAnalyzer(GermanAnalyzer.class);
        assertEquals(1, lucenePlugin.getLuceneDocCount());
        assertEquals(0, lucenePlugin.getQueueSize());

        String query = createQueryString("Paris");
        SearchResults searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.xwikiContext);
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("München");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.xwikiContext);
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("Munchen");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.xwikiContext);
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("Muenchen");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.xwikiContext);
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        // next search should return nothing
        query = createQueryString("Hamburg");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.xwikiContext);
        assertTrue(searchResults.getResults().isEmpty());
    }
}
