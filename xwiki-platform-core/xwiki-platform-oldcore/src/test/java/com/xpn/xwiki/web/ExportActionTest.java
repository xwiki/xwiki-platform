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
package com.xpn.xwiki.web;

import java.util.Arrays;

import javax.servlet.ServletOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.BeanOutputFilterStream;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Unit tests for {@link com.xpn.xwiki.web.ExportAction}.
 *
 * @version $Id$
 * @since 6.3M2
 */
public class ExportActionTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    @Test
    public void exportFullSpaceUsingWildcardsAsXAR() throws Exception
    {
        ExportAction action = new ExportAction();

        XWikiContext context = oldcore.getXWikiContext();

        // Make it a XAR export
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.get("format")).thenReturn("xar");
        context.setRequest(request);

        // Set other request parameters
        when(request.get("name")).thenReturn("myexport");
        // Export all pages in the "Space" space
        when(request.getParameterValues("pages")).thenReturn(new String[] {"Space.%"});

        // Make the current user have programming rights
        when(oldcore.getMockRightService().hasWikiAdminRights(context)).thenReturn(true);

        // Register some mock resolver to resolve passed page references
        when(oldcore.getMockStore().searchDocumentsNames("where doc.fullName like ?", Arrays.asList("Space.%"),
            context)).thenReturn(Arrays.asList("Space.Page1", "Space.Page2"));
        when(oldcore.getMockRightService().hasAccessLevel("view", "XWiki.XWikiGuest", "xwiki:Space.Page1", context))
            .thenReturn(true);
        when(oldcore.getMockRightService().hasAccessLevel("view", "XWiki.XWikiGuest", "xwiki:Space.Page2", context))
            .thenReturn(true);
        DocumentReferenceResolver<String> resolver =
            oldcore.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve("xwiki:Space.Page1")).thenReturn(new DocumentReference("xwiki", "Space", "Page1"));
        when(resolver.resolve("xwiki:Space.Page2")).thenReturn(new DocumentReference("xwiki", "Space", "Page2"));

        // Register some mock filters so that the export does nothing.
        InputFilterStreamFactory inputFilterStreamFactory = oldcore.getMocker().registerMockComponent(
            InputFilterStreamFactory.class, FilterStreamType.XWIKI_INSTANCE.serialize());
        when(inputFilterStreamFactory.createInputFilterStream(anyMap())).thenReturn(mock(InputFilterStream.class));
        BeanOutputFilterStreamFactory beanOutputFilterStreamFactory = mock(BeanOutputFilterStreamFactory.class);
        oldcore.getMocker().registerComponent(OutputFilterStreamFactory.class,
            FilterStreamType.XWIKI_XAR_CURRENT.serialize(), beanOutputFilterStreamFactory);
        when(beanOutputFilterStreamFactory.createOutputFilterStream(any(XAROutputProperties.class))).thenReturn(
            mock(BeanOutputFilterStream.class));

        // Set response stream
        XWikiResponse response = mock(XWikiResponse.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);
        context.setResponse(response);

        String result = action.render(oldcore.getXWikiContext());

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Verify that the parameters passed to the input stream factory are defining the correct pages
        ArgumentCaptor<DocumentInstanceInputProperties> properties =
            ArgumentCaptor.forClass(DocumentInstanceInputProperties.class);
        verify(inputFilterStreamFactory).createInputFilterStream(properties.capture());
        assertEquals(false, properties.getValue().isVerbose());
        assertEquals(false, properties.getValue().isWithJRCSRevisions());
        assertEquals(false, properties.getValue().isWithRevisions());
        assertEquals(true, properties.getValue().getEntities().matches(
            new DocumentReference("xwiki", "Space", "Page1")));
        assertEquals(true, properties.getValue().getEntities().matches(
            new DocumentReference("xwiki", "Space", "Page2")));
    }
}
