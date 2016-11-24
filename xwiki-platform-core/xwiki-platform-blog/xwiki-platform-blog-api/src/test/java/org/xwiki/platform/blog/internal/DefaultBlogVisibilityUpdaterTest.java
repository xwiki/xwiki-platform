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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultBlogVisibilityUpdaterTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultBlogVisibilityUpdater> mocker =
            new MockitoComponentMockingRule<>(DefaultBlogVisibilityUpdater.class);

    private void test(boolean isPublished, boolean isHidden, boolean hiddenExpected) throws Exception
    {
        // Mock
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("chocolate", "Blog", "HelloWorld"));
        BaseObject object = mock(BaseObject.class);
        when(document.getXObject(eq(new DocumentReference("chocolate", "Blog", "BlogPostClass")))).thenReturn(object);
        when(object.getIntValue("published")).thenReturn(isPublished ? 1 : 0);
        when(object.getIntValue("hidden")).thenReturn(isHidden ? 1 : 0);

        // Test
        mocker.getComponentUnderTest().synchronizeHiddenMetadata(document);

        // Verify
        verify(document).setHidden(hiddenExpected);
    }

    @Test
    public void testNonPublishedNotHidden() throws Exception
    {
        test(false, false, true);
    }

    @Test
    public void testPublishedNotHidden() throws Exception
    {
        test(true, false, false);
    }

    @Test
    public void testPublishedAndHidden() throws Exception
    {
        test(true, true, true);
    }

    @Test
    public void testWhenNoObject() throws Exception
    {
        // Mock
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("chocolate", "Blog", "HelloWorld"));

        // Test
        mocker.getComponentUnderTest().synchronizeHiddenMetadata(document);

        // Verify
        verify(document, never()).setHidden(anyBoolean());
    }
}
