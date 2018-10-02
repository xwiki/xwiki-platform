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
import java.util.List;

import javax.servlet.ServletOutputStream;

import org.junit.jupiter.api.Test;
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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.web.ExportAction}.
 *
 * @version $Id$
 * @since 6.3M2
 */
@OldcoreTest
public class ExportActionTest
{
    @InjectMockitoOldcore
    public MockitoOldcore oldcore;

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
        when(request.getParameterValues("pages")).thenReturn(new String[] {"Space.%25"});
        when(request.getCharacterEncoding()).thenReturn("UTF-8");

        // Make the current user have programming rights
        when(oldcore.getMockRightService().hasWikiAdminRights(context)).thenReturn(true);

        // Query Manager-related mocking
        QueryManager queryManager = oldcore.getMocker().registerMockComponent(QueryManager.class);
        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.HQL))).thenReturn(query);
        when(query.setWiki("xwiki")).thenReturn(query);
        when(query.bindValues(any(List.class))).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("Space.Page1", "Space.Page2"));

        // Register some mock resolver to resolve passed page references
        AuthorizationManager authorizationManager = oldcore.getMocker().registerMockComponent(AuthorizationManager.class);

        DocumentReference page1Ref = new DocumentReference("xwiki", "Space", "Page1");
        DocumentReference page2Ref = new DocumentReference("xwiki", "Space", "Page2");
        when(authorizationManager.hasAccess(Right.VIEW, context.getUserReference(), page1Ref)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, context.getUserReference(), page2Ref)).thenReturn(true);

        DocumentReferenceResolver<String> resolver =
            oldcore.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve("xwiki:Space.Page1")).thenReturn(page1Ref);
        when(resolver.resolve("xwiki:Space.Page2")).thenReturn(page2Ref);

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
        assertFalse(properties.getValue().isVerbose());
        assertFalse(properties.getValue().isWithJRCSRevisions());
        assertFalse(properties.getValue().isWithRevisions());

        assertTrue(properties.getValue().getEntities().matches(page1Ref));
        assertTrue(properties.getValue().getEntities().matches(page2Ref));
    }

    @Test
    public void exportXARSpaceUsingUncheckedPagesAndOtherPagesArgument() throws Exception
    {
        // Scenario:
        // We want to export all Space.% that contains 3 pages, but with exclusion of Page1 and 2
        // to export all Foo.% and to export all Bar.Baz.% except Bar.Baz.WebHome
        //
        // This test will check the request is properly built and the pages are correctly resolved by checking
        // result only on Space: we ignore the others for sake of simplicity
        ExportAction action = new ExportAction();

        XWikiContext context = oldcore.getXWikiContext();
        context.setDoc(new XWikiDocument(new DocumentReference("xwiki", "Space",
            "Page1")));

        // Make it a XAR export
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.get("format")).thenReturn("xar");
        context.setRequest(request);

        // Set other request parameters
        when(request.get("name")).thenReturn("myexport");
        when(request.getCharacterEncoding()).thenReturn("UTF-8");
        // Export all pages in the "Space" space
        when(request.getParameterValues("pages")).thenReturn(new String[] {
            "Space.%25", "Foo.%25", "Bar.Baz.%25"});
        when(request.getParameterValues("excludes")).thenReturn(new String[] {
            "Space.Page1&Space.Page2", "", "Bar.Baz.WebHome" });

        // Make the current user have programming rights
        when(oldcore.getMockRightService().hasWikiAdminRights(context)).thenReturn(true);

        // Query Manager-related mocking
        QueryManager queryManager = oldcore.getMocker().registerMockComponent(QueryManager.class);
        Query query = mock(Query.class);
        String where = "where ( doc.fullName like ? and doc.fullName not like ? and doc.fullName not like ? ) "
            + "or ( doc.fullName like ? ) or ( doc.fullName like ? and doc.fullName not like ? ) ";
        when(queryManager.createQuery(anyString(), eq(Query.HQL))).thenReturn(query);
        when(query.setWiki("xwiki")).thenReturn(query);

        when(query.bindValues(anyList())).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("Space.Page3"));

        // Register some mock resolver to resolve passed page references
        AuthorizationManager authorizationManager = oldcore.getMocker().
            registerMockComponent(AuthorizationManager.class);

        DocumentReference page1Ref = new DocumentReference("xwiki", "Space", "Page1");
        DocumentReference page2Ref = new DocumentReference("xwiki", "Space", "Page2");
        DocumentReference page3Ref = new DocumentReference("xwiki", "Space", "Page3");
        when(authorizationManager.hasAccess(Right.VIEW, context.getUserReference(), page1Ref)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, context.getUserReference(), page2Ref)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, context.getUserReference(), page3Ref)).thenReturn(true);

        DocumentReferenceResolver<String> resolver =
                oldcore.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");

        WikiReference wikiReference = new WikiReference("xwiki");
        when(resolver.resolve("Space.Page1", wikiReference)).thenReturn(page1Ref);
        when(resolver.resolve("Space.Page2", wikiReference)).thenReturn(page2Ref);
        when(resolver.resolve("Space.Page3", wikiReference)).thenReturn(page3Ref);

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

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(queryManager).createQuery(argument.capture(), eq(Query.HQL));

        assertEquals(where, argument.getValue());
        assertFalse(properties.getValue().isVerbose());
        assertFalse(properties.getValue().isWithJRCSRevisions());
        assertFalse(properties.getValue().isWithRevisions());
        assertTrue(properties.getValue().getEntities().matches(page3Ref));
        assertFalse(properties.getValue().getEntities().matches(page1Ref));
        assertFalse(properties.getValue().getEntities().matches(page2Ref));
    }
}
