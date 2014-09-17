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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import javax.script.SimpleScriptContext;

import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.internal.ScriptExecutionContextInitializer;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Unit tests for {@link LucenePlugin}. with different Analyzer
 */
@AllComponents
public class LuceneAnalyzerTest
{
    /**
     * Make sure the index folder is not reused.
     */
    private final static String INDEXDIR = "target" + File.separator + "luceneAnalyzerTest-" + new Date().getTime();

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private XWikiDocument document;

    private LucenePlugin lucenePlugin;

    @Before
    public void setUp() throws Exception
    {
        // create lucene index dir
        File f = new File(INDEXDIR);
        if (!f.exists()) {
            f.mkdirs();
        }

        this.document =
            new XWikiDocument(new DocumentReference("xwiki", "space", "lucene analyzer test page"), Locale.GERMAN);
        this.document.setSyntax(Syntax.XWIKI_2_1);
        this.document.setContent("Paris Iasi Berlin München");
    }

    private void prepareAndIndexWithAnalyzer(Class analyzer) throws Exception
    {
        this.oldcore.getExecutionContext().setProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID,
            new SimpleScriptContext());
        VelocityContext velocityContext = new VelocityContext();
        this.oldcore.getExecutionContext().setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID,
            velocityContext);
        final String analyzerToTest = analyzer.getName();

        when(this.oldcore.getMockXWiki().Param("xwiki.plugins.lucene.indexdir")).thenReturn(INDEXDIR);
        when(this.oldcore.getMockXWiki().ParamAsLong("xwiki.plugins.lucene.indexinterval", 30l)).thenReturn(1l);
        when(this.oldcore.getMockXWiki().ParamAsLong("xwiki.plugins.lucene.maxQueueSize", 1000l)).thenReturn(1000l);
        when(
            this.oldcore.getMockXWiki().Param("xwiki.plugins.lucene.analyzer",
                "org.apache.lucene.analysis.standard.StandardAnalyzer")).thenReturn(analyzerToTest);

        doReturn(document).when(this.oldcore.getMockXWiki()).getDocument(any(DocumentReference.class),
            any(XWikiContext.class));
        doReturn(true).when(this.oldcore.getMockXWiki()).exists(any(DocumentReference.class), any(XWikiContext.class));
        
        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);

        when(
            this.oldcore.getMockRightService().hasAccessLevel(any(String.class), any(String.class), any(String.class),
                any(XWikiContext.class))).thenReturn(true);

        this.lucenePlugin = new LucenePlugin("test", "test", this.oldcore.getXWikiContext());
        this.lucenePlugin.init(this.oldcore.getXWikiContext());
        this.lucenePlugin.queueDocument(this.document, this.oldcore.getXWikiContext());

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
        SearchResults searchResults =
            lucenePlugin.getSearchResults(query, "", "xwiki", null, this.oldcore.getXWikiContext());

        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("München");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.oldcore.getXWikiContext());
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
        SearchResults searchResults =
            lucenePlugin.getSearchResults(query, "", "xwiki", null, this.oldcore.getXWikiContext());
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("München");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.oldcore.getXWikiContext());
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("Munchen");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.oldcore.getXWikiContext());
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        query = createQueryString("Muenchen");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.oldcore.getXWikiContext());
        assertFalse(searchResults.getResults().isEmpty());
        assertEquals(1, searchResults.getHitcount());

        // next search should return nothing
        query = createQueryString("Hamburg");
        searchResults = lucenePlugin.getSearchResults(query, "", "xwiki", null, this.oldcore.getXWikiContext());
        assertTrue(searchResults.getResults().isEmpty());
    }
}
