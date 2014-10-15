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
package com.xpn.xwiki.objects.classes;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TextAreaClass}.
 * 
 * @version $Id$
 */
public class TextAreaClassTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Before
    public void configure() throws Exception
    {
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");

        Utils.setComponentManager(mocker);
    }

    @Test
    public void displayViewDropsPermissions()
    {
        TextAreaClass textArea = new TextAreaClass();
        StringBuffer buffer = new StringBuffer();
        BaseCollection<?> object = mock(BaseCollection.class);
        XWikiContext context = mock(XWikiContext.class);

        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        when(context.getDoc()).thenReturn(doc);
        when(object.getOwnerDocument()).thenReturn(doc);

        textArea.displayView(buffer, "address", null, object, context);

        verify(context).dropPermissions();
        // Verify that the permissions are restored at the end.
        verify(context).remove(XWikiConstant.DROPPED_PERMISSIONS);
    }
}
