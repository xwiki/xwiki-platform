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
package org.xwiki.wiki.internal.provisioning;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.Logger;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class DefaultWikiCopierTest
{
    @InjectMockComponents
    private DefaultWikiCopier defaultWikiCopier;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private JobProgressManager progress;

    @MockComponent
    private Logger logger;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void copyDocuments() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("select distinct doc.fullName from Document as doc",
            Query.XWQL)).thenReturn(query);

        List<String> documentList = List.of("Space.Doc1", "Space.Doc2", "Space.Doc3");
        when(query.<String>execute()).thenReturn(documentList);

        WikiReference fromWikiReference = new WikiReference("wikiA");
        DocumentReference docRef1 = new DocumentReference("wikiA", "Space", "Doc1");
        DocumentReference docRef2 = new DocumentReference("wikiA", "Space", "Doc2");
        DocumentReference docRef3 = new DocumentReference("wikiA", "Space", "Doc3");
        DocumentReference copydocRef1 = new DocumentReference("wikiB", "Space", "Doc1");
        DocumentReference copydocRef2 = new DocumentReference("wikiB", "Space", "Doc2");
        DocumentReference copydocRef3 = new DocumentReference("wikiB", "Space", "Doc3");
        when(this.documentReferenceResolver.resolve(eq("Space.Doc1"), eq(fromWikiReference))).thenReturn(docRef1);
        when(this.documentReferenceResolver.resolve(eq("Space.Doc2"), eq(fromWikiReference))).thenReturn(docRef2);
        when(this.documentReferenceResolver.resolve(eq("Space.Doc3"), eq(fromWikiReference))).thenReturn(docRef3);

        // Test
        this.defaultWikiCopier.copyDocuments("wikiA", "wikiB", false);

        // Verify
        verify(query).setWiki("wikiA");
        InOrder inOrder = inOrder(this.progress, this.xwiki, this.logger);
        inOrder.verify(this.progress).pushLevelProgress(3, this.defaultWikiCopier);

        inOrder.verify(this.progress).startStep(this.defaultWikiCopier);
        inOrder.verify(this.logger).info("Copying document [{}] to [{}].", docRef1, copydocRef1);
        inOrder.verify(this.xwiki).copyDocument(docRef1, copydocRef1, null, true, true, this.xcontext);
        inOrder.verify(this.logger).info("Done copying document [{}] to [{}].", docRef1, copydocRef1);

        inOrder.verify(this.progress).startStep(this.defaultWikiCopier);
        inOrder.verify(this.logger).info("Copying document [{}] to [{}].", docRef2, copydocRef2);
        inOrder.verify(this.xwiki).copyDocument(docRef2, copydocRef2, null, true, true, this.xcontext);
        inOrder.verify(this.logger).info("Done copying document [{}] to [{}].", docRef2, copydocRef2);

        inOrder.verify(this.progress).startStep(this.defaultWikiCopier);
        inOrder.verify(this.logger).info("Copying document [{}] to [{}].", docRef3, copydocRef3);
        inOrder.verify(this.xwiki).copyDocument(docRef3, copydocRef3, null, true, true, this.xcontext);
        inOrder.verify(this.logger).info("Done copying document [{}] to [{}].", docRef3, copydocRef3);

        inOrder.verify(this.progress).popLevelProgress(this.defaultWikiCopier);
    }
}
