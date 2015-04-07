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

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.slf4j.Logger;
import org.xwiki.job.event.status.PushLevelProgressEvent;
import org.xwiki.job.event.status.StepProgressEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultWikiCopierTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiCopier> mocker =
            new MockitoComponentMockingRule(DefaultWikiCopier.class);

    private QueryManager queryManager;

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private ObservationManager observationManager;

    private Logger logger;
    
    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;
    
    @Before
    public void setUp() throws Exception
    {   
        queryManager = mocker.getInstance(QueryManager.class);
        observationManager = mocker.getInstance(ObservationManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
    }
    
    @Test
    public void copyDocuments() throws Exception
    {   
        // Mocks
        Query query = mock(Query.class);
        when(queryManager.createQuery("select distinct doc.fullName from Document as doc", Query.XWQL)).
                thenReturn(query);
        
        List<String> documentList = Arrays.asList("Space.Doc1", "Space.Doc2", "Space.Doc3");
        when(query.<String>execute()).thenReturn(documentList);
        
        WikiReference fromWikiReference = new WikiReference("wikiA");
        DocumentReference docRef1 = new DocumentReference("wikiA", "Space", "Doc1");
        DocumentReference docRef2 = new DocumentReference("wikiA", "Space", "Doc2");
        DocumentReference docRef3 = new DocumentReference("wikiA", "Space", "Doc3");
        DocumentReference copydocRef1 = new DocumentReference("wikiB", "Space", "Doc1");
        DocumentReference copydocRef2 = new DocumentReference("wikiB", "Space", "Doc2");
        DocumentReference copydocRef3 = new DocumentReference("wikiB", "Space", "Doc3");
        when(documentReferenceResolver.resolve(eq("Space.Doc1"), eq(fromWikiReference))).thenReturn(docRef1);
        when(documentReferenceResolver.resolve(eq("Space.Doc2"), eq(fromWikiReference))).thenReturn(docRef2);
        when(documentReferenceResolver.resolve(eq("Space.Doc3"), eq(fromWikiReference))).thenReturn(docRef3);
        
        // Test
        mocker.getComponentUnderTest().copyDocuments("wikiA" ,"wikiB", false);

        // Verify
        verify(query).setWiki("wikiA");
        InOrder inOrder = inOrder(observationManager, xwiki, mocker.getMockedLogger());
        inOrder.verify(observationManager).notify(any(PushLevelProgressEvent.class), any(DefaultWikiCopier.class));
        
        inOrder.verify(mocker.getMockedLogger()).info("Copying document [{}] to [{}].", docRef1, copydocRef1);
        inOrder.verify(xwiki).copyDocument(docRef1, copydocRef1, null, true, true, xcontext);
        inOrder.verify(mocker.getMockedLogger()).info("Done copying document [{}] to [{}].", docRef1, copydocRef1);
        inOrder.verify(observationManager).notify(any(StepProgressEvent.class), any(DefaultWikiCopier.class));
        
        inOrder.verify(mocker.getMockedLogger()).info("Copying document [{}] to [{}].", docRef2, copydocRef2);
        inOrder.verify(xwiki).copyDocument(docRef2, copydocRef2, null, true, true, xcontext);
        inOrder.verify(mocker.getMockedLogger()).info("Done copying document [{}] to [{}].", docRef2, copydocRef2);
        inOrder.verify(observationManager).notify(any(StepProgressEvent.class), any(DefaultWikiCopier.class));
        
        inOrder.verify(mocker.getMockedLogger()).info("Copying document [{}] to [{}].", docRef3, copydocRef3);
        inOrder.verify(xwiki).copyDocument(docRef3, copydocRef3, null, true, true, xcontext);
        inOrder.verify(mocker.getMockedLogger()).info("Done copying document [{}] to [{}].", docRef3, copydocRef3);
        inOrder.verify(observationManager, calls(2)).notify(any(Event.class), any(DefaultWikiCopier.class));
        
    }
}
