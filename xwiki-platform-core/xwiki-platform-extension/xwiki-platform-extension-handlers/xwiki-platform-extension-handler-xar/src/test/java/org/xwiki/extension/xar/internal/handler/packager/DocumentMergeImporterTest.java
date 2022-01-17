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
package org.xwiki.extension.xar.internal.handler.packager;

import java.util.Locale;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.extension.xar.question.ConflictQuestion.ConflictType;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.internal.document.DefaultDocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.internal.document.DocumentDocumentReferenceUserReferenceResolver;
import org.xwiki.user.internal.document.DocumentDocumentReferenceUserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializerManager;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DocumentMergeImporter}.
 * 
 * @version $Id$
 */
@ComponentList({
    ContextComponentManagerProvider.class,
    DefaultXWikiDocumentMerger.class,
    DocumentDocumentReferenceUserReferenceSerializer.class,
    DocumentDocumentReferenceUserReferenceResolver.class
})
@ReferenceComponentList
@ComponentTest
public class DocumentMergeImporterTest
{
    @InjectMockComponents
    private DocumentMergeImporter documentMergeImporter;

    @MockComponent
    private MergeManager mergeManager;

    @MockComponent
    private MergeConflictDecisionsManager conflictDecisionsManager;

    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page", Locale.ROOT);

    @MockComponent
    private MandatoryDocumentInitializerManager initializerManager;

    @MockComponent
    private JobContext jobContext;

    private XWikiDocument previousDocument;

    private XWikiDocument currentDocument;

    private XWikiDocument nextDocument;

    private XWikiDocument mergedDocument;

    private PackageConfiguration configuration;

    private MergeDocumentResult mergeResult;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private JobStatus jobStatus;

    @MockComponent
    private Execution execution;

    private ExecutionContext econtext;

    @BeforeComponent
    public void registerComponents(MockitoComponentManager componentManager) throws Exception
    {
        this.xcontext = mock(XWikiContext.class);

        Provider<XWikiContext> xcontextProvider = componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        this.xwiki = mock(XWiki.class);

        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        // documents

        this.previousDocument = mock(XWikiDocument.class, "previous");
        when(this.previousDocument.isNew()).thenReturn(false);
        when(this.previousDocument.getDocumentReferenceWithLocale()).thenReturn(this.documentReference);

        this.currentDocument = mock(XWikiDocument.class, "current");

        when(this.currentDocument.isNew()).thenReturn(false);
        when(this.currentDocument.getDocumentReferenceWithLocale()).thenReturn(this.documentReference);
        when(this.xwiki.getDocument(same(this.documentReference), same(xcontext))).thenReturn(this.currentDocument);

        this.nextDocument = mock(XWikiDocument.class, "next");
        when(this.nextDocument.getAuthors()).thenReturn(new DefaultDocumentAuthors(this.currentDocument));
        when(this.nextDocument.isNew()).thenReturn(false);
        when(this.nextDocument.getDocumentReferenceWithLocale()).thenReturn(this.documentReference);

        this.mergedDocument = mock(XWikiDocument.class, "merged");
        when(this.mergedDocument.getAuthors()).thenReturn(new DefaultDocumentAuthors(this.mergedDocument));
        when(this.mergedDocument.isNew()).thenReturn(false);
        when(this.mergedDocument.getDocumentReferenceWithLocale()).thenReturn(this.documentReference);

        when(this.currentDocument.clone()).thenReturn(this.mergedDocument);

        // merge

        this.configuration = new PackageConfiguration();

        this.mergeResult = new MergeDocumentResult(this.currentDocument, this.previousDocument, this.nextDocument);
        this.mergeResult.setMergeResult(mergedDocument);
        when(this.mergeManager.mergeDocument(same(this.previousDocument), same(this.nextDocument),
            same(this.currentDocument), any(MergeConfiguration.class))).thenReturn(this.mergeResult);

        // job status

        this.jobStatus = mock(JobStatus.class);
        this.configuration.setJobStatus(this.jobStatus);
        Job job = mock(Job.class);
        when(this.jobContext.getCurrentJob()).thenReturn(job);
        when(job.getStatus()).thenReturn(this.jobStatus);

        // execution

        this.econtext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(this.econtext);
    }

    private void setInteractive()
    {
        Request request = mock(Request.class);

        when(this.jobStatus.getRequest()).thenReturn(request);
        when(request.isInteractive()).thenReturn(true);
    }

    // Merge

    @Test
    public void testMergeNoChange() throws ComponentLookupException, Exception
    {
        this.mergeResult.setModified(false);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.xcontext, times(1)).getUserReference();
        verifyNoMoreInteractions(this.xcontext);
        verifyNoInteractions(this.xwiki);
    }

    @Test
    public void testMergeNoCurrent() throws ComponentLookupException, Exception
    {
        this.documentMergeImporter.importDocument("comment", this.previousDocument, null, this.nextDocument,
            this.configuration);

        verifyNoInteractions(this.xwiki, this.xcontext);
    }

    @Test
    public void testMergeChanges() throws ComponentLookupException, Exception
    {
        this.mergeResult.setModified(true);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.xwiki).saveDocument(same(this.mergedDocument), eq("comment"), eq(false), same(this.xcontext));
    }

    @Test
    public void testMergeChangesNoCustom() throws ComponentLookupException, Exception
    {
        this.mergeResult.setModified(true);
        when(this.currentDocument.equalsData(same(this.previousDocument))).thenReturn(true);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.xwiki).saveDocument(same(this.nextDocument), eq("comment"), eq(false), same(this.xcontext));
    }

    // Merge interactive

    @Test
    public void testMergeInteractiveChangesNoConflict() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verifyNoInteractions(this.jobStatus);
        verify(this.xwiki).saveDocument(same(this.mergedDocument), eq("comment"), eq(false), same(this.xcontext));
    }

    @Test
    public void testMergeInteractiveChangesNoConflictAsk() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);

        this.configuration.setConflictAction(ConflictType.MERGE_SUCCESS, GlobalAction.ASK);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.jobStatus, times(1)).ask(any());
        verify(this.xwiki).saveDocument(same(this.mergedDocument), eq("comment"), eq(false), same(this.xcontext));
    }

    @Test
    public void testMergeInteractiveChangesNoCustomNoConflictAsk() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);
        when(this.currentDocument.equalsData(same(this.previousDocument))).thenReturn(true);

        this.configuration.setConflictAction(ConflictType.MERGE_SUCCESS, GlobalAction.ASK);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verifyNoInteractions(this.jobStatus);
        verify(this.xwiki).saveDocument(same(this.nextDocument), eq("comment"), eq(false), same(this.xcontext));
    }

    private void answerGlobalAction(final GlobalAction action, final boolean always) throws InterruptedException
    {
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation)
            {
                ConflictQuestion question = (ConflictQuestion) invocation.getArguments()[0];
                question.setGlobalAction(action);
                question.setAlways(always);
                return null;
            }
        }).when(this.jobStatus).ask(any());
    }

    @Test
    public void testMergeInteractiveChangesConflictAnswerCurrent() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);
        this.mergeResult.getLog().error("error");

        answerGlobalAction(GlobalAction.CURRENT, false);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        // another try

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.jobStatus, times(2)).ask(any());

        verify(this.xcontext, times(2)).getUserReference();
        verifyNoMoreInteractions(this.xcontext);
        verifyNoInteractions(this.xwiki);
    }

    @Test
    public void testMergeInteractiveChangesConflictAnswerNext() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);
        this.mergeResult.getLog().error("error");

        answerGlobalAction(GlobalAction.NEXT, false);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.xwiki).saveDocument(same(this.nextDocument), eq("comment"), eq(false), same(this.xcontext));

        // another try

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.jobStatus, times(2)).ask(any());
        verify(this.xwiki, times(2)).saveDocument(same(this.nextDocument), eq("comment"), eq(false),
            same(this.xcontext));
    }

    @Test
    public void testMergeInteractiveChangesConflictAnswerMerged() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);
        this.mergeResult.getLog().error("error");

        answerGlobalAction(GlobalAction.MERGED, false);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.xwiki).saveDocument(same(this.mergedDocument), eq("comment"), eq(false), same(this.xcontext));

        // another try

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.jobStatus, times(2)).ask(any());
        verify(this.xwiki, times(2)).saveDocument(same(this.mergedDocument), eq("comment"), eq(false),
            same(this.xcontext));
    }

    @Test
    public void testMergeInteractiveChangesConflictAnswerPrevious() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);
        this.mergeResult.getLog().error("error");

        answerGlobalAction(GlobalAction.PREVIOUS, false);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.xwiki).saveDocument(same(this.previousDocument), eq("comment"), eq(false), same(this.xcontext));

        // another try

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.jobStatus, times(2)).ask(any());
        verify(this.xwiki, times(2)).saveDocument(same(this.previousDocument), eq("comment"), eq(false),
            same(this.xcontext));
    }

    @Test
    public void testMergeInteractiveChangesConflictAnswerPreviousAlways() throws ComponentLookupException, Exception
    {
        setInteractive();
        this.configuration.setUser(new DocumentReference("wiki", "space", "user"));

        this.mergeResult.setModified(true);
        this.mergeResult.getLog().error("error");

        answerGlobalAction(GlobalAction.PREVIOUS, true);

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        verify(this.xwiki).saveDocument(same(this.previousDocument), eq("comment"), eq(false), same(this.xcontext));

        // another try

        this.documentMergeImporter.importDocument("comment", this.previousDocument, this.currentDocument,
            this.nextDocument, this.configuration);

        // Make sure we don't ask the job status this time
        verify(this.jobStatus, times(1)).ask(any());
        verify(this.xwiki, times(2)).saveDocument(same(this.previousDocument), eq("comment"), eq(false),
            same(this.xcontext));
    }

    // No merge

    @Test
    public void testNoMergeNoCurrent() throws ComponentLookupException, Exception
    {
        when(this.currentDocument.isNew()).thenReturn(true);

        this.documentMergeImporter.importDocument("comment", null, null, this.nextDocument,
            this.configuration);

        verify(this.xwiki).saveDocument(same(this.nextDocument), eq("comment"), eq(false), same(this.xcontext));
    }

    @Test
    public void testNoMergeDifferent() throws ComponentLookupException, Exception
    {
        when(this.currentDocument.equalsData(same(this.nextDocument))).thenReturn(false);

        this.documentMergeImporter.importDocument("comment", null, this.currentDocument, this.nextDocument,
            this.configuration);

        verify(this.xwiki).saveDocument(same(this.nextDocument), eq("comment"), eq(false), same(this.xcontext));
    }

    @Test
    public void testNoMergeNoChange() throws ComponentLookupException, Exception
    {
        when(this.currentDocument.equalsData(same(this.nextDocument))).thenReturn(true);

        this.documentMergeImporter.importDocument("comment", null, this.currentDocument, this.nextDocument,
            this.configuration);

        verifyNoInteractions(this.xwiki, this.xcontext);
    }
}
