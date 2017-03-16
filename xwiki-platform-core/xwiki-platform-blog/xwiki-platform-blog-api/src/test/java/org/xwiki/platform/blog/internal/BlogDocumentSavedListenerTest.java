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
package org.xwiki.platform.blog.internal;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.platform.blog.events.BlogPostPublishedEvent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class BlogDocumentSavedListenerTest
{
    @Rule
    public MockitoComponentMockingRule<BlogDocumentSavedListener> mocker =
            new MockitoComponentMockingRule<>(BlogDocumentSavedListener.class);

    private ObservationManager observationManager;
    private XWikiDocument document;
    private XWikiDocument originalDocument;
    private BaseObject blogPostObj;
    private BaseObject previousblogPostObj;
    private DocumentReference classRef
            = new DocumentReference("xwiki", Arrays.asList("Blog"), "BlogPostClass");
    private DocumentUpdatedEvent event = new DocumentUpdatedEvent();

    @Before
    public void setUp() throws Exception
    {
        observationManager = mocker.getInstance(ObservationManager.class);
        document = mock(XWikiDocument.class);
        originalDocument = mock(XWikiDocument.class);
        blogPostObj = mock(BaseObject.class);
        previousblogPostObj = mock(BaseObject.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Blog", "Whatever"));
        when(document.getOriginalDocument()).thenReturn(originalDocument);
    }

    @Test
    public void onEvent1() throws Exception
    {
        when(document.getXObject(classRef)).thenReturn(blogPostObj);
        when(originalDocument.getXObject(classRef)).thenReturn(previousblogPostObj);
        when(blogPostObj.getIntValue("published")).thenReturn(1);
        when(blogPostObj.getIntValue("hidden")).thenReturn(0);
        when(previousblogPostObj.getIntValue("published")).thenReturn(0);
        when(previousblogPostObj.getIntValue("hidden")).thenReturn(0);

        // Test
        mocker.getComponentUnderTest().onEvent(event, document, null);

        // Checks
        verify(observationManager).notify(any(BlogPostPublishedEvent.class),
                eq("org.xwiki.platform:xwiki-platform-blog-api"), eq(document));
    }

    @Test
    public void onEvent2() throws Exception
    {
        when(document.getXObject(classRef)).thenReturn(blogPostObj);
        when(originalDocument.getXObject(classRef)).thenReturn(previousblogPostObj);
        when(blogPostObj.getIntValue("published")).thenReturn(0);
        when(blogPostObj.getIntValue("hidden")).thenReturn(1);
        when(previousblogPostObj.getIntValue("published")).thenReturn(1);
        when(previousblogPostObj.getIntValue("hidden")).thenReturn(0);

        // Test
        mocker.getComponentUnderTest().onEvent(event, document, null);

        // Checks
        verifyZeroInteractions(observationManager);
    }

    @Test
    public void onEvent3() throws Exception
    {
        when(document.getXObject(classRef)).thenReturn(blogPostObj);
        when(originalDocument.getXObject(classRef)).thenReturn(previousblogPostObj);
        when(blogPostObj.getIntValue("published")).thenReturn(0);
        when(blogPostObj.getIntValue("hidden")).thenReturn(0);
        when(previousblogPostObj.getIntValue("published")).thenReturn(0);
        when(previousblogPostObj.getIntValue("hidden")).thenReturn(0);

        // Test
        mocker.getComponentUnderTest().onEvent(event, document, null);

        // Checks
        verifyZeroInteractions(observationManager);
    }

    @Test
    public void onEvent4() throws Exception
    {
        // There were no object previously
        when(document.getXObject(classRef)).thenReturn(blogPostObj);
        when(blogPostObj.getIntValue("published")).thenReturn(1);
        when(blogPostObj.getIntValue("hidden")).thenReturn(0);
        // Test
        mocker.getComponentUnderTest().onEvent(event, document, null);

        // Checks
        verify(observationManager).notify(any(BlogPostPublishedEvent.class),
                eq("org.xwiki.platform:xwiki-platform-blog-api"), eq(document));
    }

    @Test
    public void onEvent5() throws Exception
    {
        when(document.getXObject(classRef)).thenReturn(blogPostObj);
        when(originalDocument.getXObject(classRef)).thenReturn(previousblogPostObj);
        when(blogPostObj.getIntValue("published")).thenReturn(1);
        when(blogPostObj.getIntValue("hidden")).thenReturn(0);
        when(previousblogPostObj.getIntValue("published")).thenReturn(0);
        when(previousblogPostObj.getIntValue("hidden")).thenReturn(1);

        // Test
        mocker.getComponentUnderTest().onEvent(event, document, null);

        // Checks
        verify(observationManager).notify(any(BlogPostPublishedEvent.class),
                eq("org.xwiki.platform:xwiki-platform-blog-api"), eq(document));
    }

    @Test
    public void onEvent6() throws Exception
    {
        when(document.getXObject(classRef)).thenReturn(blogPostObj);
        when(originalDocument.getXObject(classRef)).thenReturn(previousblogPostObj);
        when(blogPostObj.getIntValue("published")).thenReturn(1);
        when(blogPostObj.getIntValue("hidden")).thenReturn(0);
        when(previousblogPostObj.getIntValue("published")).thenReturn(1);
        when(previousblogPostObj.getIntValue("hidden")).thenReturn(0);

        // Test
        mocker.getComponentUnderTest().onEvent(event, document, null);

        // Checks
        verifyZeroInteractions(observationManager);
    }
}
