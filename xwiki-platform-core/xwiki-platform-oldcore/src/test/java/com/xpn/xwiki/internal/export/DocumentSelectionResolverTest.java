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
package com.xpn.xwiki.internal.export;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.tree.EntityTreeFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentSelectionResolver}.
 * 
 * @version $Id$
 * @since 11.10
 */
@ComponentTest
class DocumentSelectionResolverTest
{
    @InjectMockComponents
    private DocumentSelectionResolver documentSelectionResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("document")
    private QueryFilter documentQueryFilter;

    @MockComponent
    @Named("hidden/document")
    private QueryFilter hiddenDocumentQueryFilter;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    @Named("test")
    EntityTreeFilter filter;

    private XWikiRequest request = mock(XWikiRequest.class);

    @BeforeEach
    void configure(MockitoComponentManager componentManager)
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWikiId()).thenReturn("test");
        when(xcontext.getWikiReference()).thenReturn(new WikiReference("test"));

        when(xcontext.getRequest()).thenReturn(this.request);
        when(this.request.getCharacterEncoding()).thenReturn("utf-8");

        when(this.contextComponentManagerProvider.get()).thenReturn(componentManager);
    }

    @Test
    void isSelectionSpecified()
    {
        Map<String, String[]> parameterMap = new HashMap<>();
        when(this.request.getParameterMap()).thenReturn(parameterMap);

        assertFalse(this.documentSelectionResolver.isSelectionSpecified());

        parameterMap.put("pages", new String[] {""});
        assertTrue(this.documentSelectionResolver.isSelectionSpecified());

        parameterMap.remove("pages");
        parameterMap.put("filter", new String[] {""});
        assertFalse(this.documentSelectionResolver.isSelectionSpecified());

        parameterMap.put("filter", new String[] {"test"});
        assertTrue(this.documentSelectionResolver.isSelectionSpecified());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getSelectedDocumentsForSpaceWildcard() throws Exception
    {
        createDocumentReference("Shape.%", "test", "Shape", "%");
        when(this.request.getParameterValues("pages")).thenReturn(new String[] {"Shape.%"});

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("where (doc.fullName like ?1)", Query.HQL)).thenReturn(query);
        when(query.setWiki("test")).thenReturn(query);
        when(query.bindValues(any(List.class))).thenReturn(query);

        DocumentReference circleReference = new DocumentReference("test", Arrays.asList("Shape", "2D"), "Circle");
        when(query.execute()).thenReturn(Collections.singletonList(circleReference));

        assertEquals(Collections.singleton(circleReference), this.documentSelectionResolver.getSelectedDocuments(true));

        verify(query).addFilter(this.documentQueryFilter);
        verify(query).addFilter(this.hiddenDocumentQueryFilter);
    }

    private DocumentReference createDocumentReference(String... args)
    {
        if (args.length < 4) {
            throw new RuntimeException("At least 4 arguments are expected");
        }

        String localStringReference = args[0];
        String wikiName = args[1];
        List<String> spaceNames = Arrays.asList(args).subList(2, args.length - 1);
        String pageName = args[args.length - 1];

        DocumentReference documentReference = new DocumentReference(wikiName, spaceNames, pageName);
        when(this.currentDocumentReferenceResolver.resolve(localStringReference)).thenReturn(documentReference);
        when(this.localEntityReferenceSerializer.serialize(documentReference)).thenReturn(localStringReference);

        return documentReference;
    }

    @SuppressWarnings("unchecked")
    @Test
    void getSelectedDocumentsForSpaceWildcardWithExclusions() throws Exception
    {
        createDocumentReference("Shape.%", "test", "Shape", "%");
        when(this.request.getParameterValues("pages")).thenReturn(new String[] {"Shape.%"});

        createDocumentReference("Shape.WebHome", "test", "Shape", "WebHome");
        createDocumentReference("Shape.3D.%", "test", "Shape", "3D", "%");
        when(this.request.getParameterValues("excludes")).thenReturn(new String[] {"Shape.WebHome&Shape.3D.%25"});

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(
            "where (doc.fullName like ?1 and doc.fullName not in (?2) and doc.fullName not like ?3)", Query.HQL))
                .thenReturn(query);
        when(query.setWiki("test")).thenReturn(query);
        when(query.bindValues(any(List.class))).thenReturn(query);

        DocumentReference circleReference = new DocumentReference("test", Arrays.asList("Shape", "2D"), "Circle");
        when(query.execute()).thenReturn(Collections.singletonList(circleReference));

        assertEquals(Collections.singleton(circleReference), this.documentSelectionResolver.getSelectedDocuments());

        verify(query).addFilter(this.documentQueryFilter);
        verify(query, never()).addFilter(this.hiddenDocumentQueryFilter);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getSelectedDocumentsForSpaceWildcardAndExactMatch() throws Exception
    {
        createDocumentReference("Shape.%", "test", "Shape", "%");
        DocumentReference sphereReference = createDocumentReference("Shape.3D.Sphere", "test", "Shape", "3D", "Sphere");
        when(this.request.getParameterValues("pages")).thenReturn(new String[] {"Shape.%", "Shape.3D.Sphere"});

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("where (doc.fullName like ?1)", Query.HQL)).thenReturn(query);
        when(query.setWiki("test")).thenReturn(query);
        when(query.bindValues(any(List.class))).thenReturn(query);

        DocumentReference circleReference = new DocumentReference("test", Arrays.asList("Shape", "2D"), "Circle");
        when(query.execute()).thenReturn(Collections.singletonList(circleReference));

        Set<DocumentReference> result =
            Arrays.asList(sphereReference, circleReference).stream().collect(Collectors.toSet());
        assertEquals(result, this.documentSelectionResolver.getSelectedDocuments());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getSelectedDocumentsFromMultipleWikis() throws Exception
    {
        createDocumentReference("Shape.2D.%", "one", "Shape", "2D", "%");
        createDocumentReference("Shape.3D.%", "two", "Shape", "3D", "%");
        when(this.request.getParameterValues("pages")).thenReturn(new String[] {"Shape.2D.%", "Shape.3D.%"});

        createDocumentReference("Shape.2D.WebHome", "one", "Shape", "2D", "WebHome");
        createDocumentReference("Shape.3D.Cube.%", "three", "Shape", "3D", "Cube", "%");
        when(this.request.getParameterValues("excludes"))
            .thenReturn(new String[] {"Shape.2D.WebHome", "Shape.3D.Cube.%25"});

        Query queryOne = mock(Query.class, "one");
        when(this.queryManager.createQuery("where (doc.fullName like ?1 and doc.fullName not in (?2))", Query.HQL))
            .thenReturn(queryOne);
        when(queryOne.setWiki("one")).thenReturn(queryOne);
        when(queryOne.bindValues(any(List.class))).thenReturn(queryOne);

        DocumentReference circleReference = new DocumentReference("one", Arrays.asList("Shape", "2D"), "Circle");
        when(queryOne.execute()).thenReturn(Collections.singletonList(circleReference));

        Query queryTwo = mock(Query.class, "two");
        // The exclude is ignored because it is from a different wiki.
        when(this.queryManager.createQuery("where (doc.fullName like ?1)", Query.HQL)).thenReturn(queryTwo);
        when(queryTwo.setWiki("two")).thenReturn(queryTwo);
        when(queryTwo.bindValues(any(List.class))).thenReturn(queryTwo);

        DocumentReference sphereReference = new DocumentReference("two", Arrays.asList("Shape", "3D"), "Sphere");
        when(queryTwo.execute()).thenReturn(Collections.singletonList(sphereReference));

        Set<DocumentReference> result =
            Arrays.asList(sphereReference, circleReference).stream().collect(Collectors.toSet());
        assertEquals(result, this.documentSelectionResolver.getSelectedDocuments());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getSelectedDocumentsWithWildcardAndFilter() throws Exception
    {
        createDocumentReference("Shape.%", "test", "Shape", "%");
        when(this.request.getParameterValues("pages")).thenReturn(new String[] {"Shape.%"});

        when(this.request.getParameter("filter")).thenReturn("test");
        DocumentReference squareReference = createDocumentReference("Shape.2D.Square", "test", "Shape", "2D", "Square");
        when(this.filter.getDescendantExclusions(new SpaceReference("test", "Shape")))
            .thenReturn(Collections.singleton(squareReference));

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("where (doc.fullName like ?1 and doc.fullName not in (?2))", Query.HQL))
            .thenReturn(query);
        when(query.setWiki("test")).thenReturn(query);
        when(query.bindValues(any(List.class))).thenReturn(query);

        DocumentReference circleReference = new DocumentReference("test", Arrays.asList("Shape", "2D"), "Circle");
        when(query.execute()).thenReturn(Collections.singletonList(circleReference));

        assertEquals(Collections.singleton(circleReference), this.documentSelectionResolver.getSelectedDocuments());
    }

    @Test
    void getSelectedDocumentsWithFilterOnly() throws Exception
    {
        when(this.request.getParameter("filter")).thenReturn("test");
        DocumentReference shapeReference = createDocumentReference("Shape.WebHome", "test", "Shape", "WebHome");
        when(this.filter.getDescendantExclusions(new WikiReference("test")))
            .thenReturn(Collections.singleton(shapeReference));

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("where (doc.fullName not in (?1))", Query.HQL)).thenReturn(query);
        when(query.setWiki("test")).thenReturn(query);
        when(query.bindValues(Collections.singletonList(Collections.singleton("Shape.WebHome")))).thenReturn(query);

        DocumentReference circleReference = new DocumentReference("test", Arrays.asList("Shape", "2D"), "Circle");
        when(query.execute()).thenReturn(Collections.singletonList(circleReference));

        assertEquals(Collections.singleton(circleReference), this.documentSelectionResolver.getSelectedDocuments());
    }

    @Test
    void getSelectedDocumentsWithFilterWithoutConstraints() throws Exception
    {
        when(this.request.getParameter("filter")).thenReturn("test");

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("", Query.HQL)).thenReturn(query);
        when(query.setWiki("test")).thenReturn(query);
        when(query.bindValues(Collections.emptyList())).thenReturn(query);

        DocumentReference circleReference = new DocumentReference("test", Arrays.asList("Shape", "2D"), "Circle");
        when(query.execute()).thenReturn(Collections.singletonList(circleReference));

        assertEquals(Collections.singleton(circleReference), this.documentSelectionResolver.getSelectedDocuments());
    }
}
