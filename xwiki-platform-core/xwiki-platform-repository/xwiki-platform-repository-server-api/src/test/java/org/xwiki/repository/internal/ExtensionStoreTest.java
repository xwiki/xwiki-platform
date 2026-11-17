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
package org.xwiki.repository.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExtensionStore}.
 *
 * @version $Id$
 */
@ComponentTest
class ExtensionStoreTest
{
    private static final String ID = "org.xwiki.contrib:project";

    private static final DocumentReference PROJECT_REFERENCE =
        new DocumentReference("wiki", List.of("Extension", "Project"), "WebHome");

    private static final String PROJECT_FULLNAME = "Extension.Project.WebHome";

    @InjectMockComponents
    private ExtensionStore extensionStore;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private ObservationManager observation;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentStringResolver;

    private Query extensionQuery;

    private Query projectQuery;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        // The cache is created when the component is initialized
        when(this.cacheManager.createNewCache(any())).thenReturn(new MapCache<>());
    }

    @BeforeEach
    void beforeEach() throws Exception
    {

        XWikiContext xcontext = mock();
        when(xcontext.getWikiId()).thenReturn("wiki");
        when(this.xcontextProvider.get()).thenReturn(xcontext);

        this.extensionQuery = mock();
        when(this.queryManager.createQuery(contains(XWikiRepositoryModel.EXTENSION_CLASSNAME), eq(Query.XWQL)))
            .thenReturn(this.extensionQuery);
        when(this.extensionQuery.bindValue(any(), any())).thenReturn(this.extensionQuery);
        when(this.extensionQuery.setLimit(anyInt())).thenReturn(this.extensionQuery);

        this.projectQuery = mock();
        when(this.queryManager.createQuery(contains(XWikiRepositoryModel.PROJECT_CLASSNAME), eq(Query.XWQL)))
            .thenReturn(this.projectQuery);
        when(this.projectQuery.bindValue(any(), any())).thenReturn(this.projectQuery);
        when(this.projectQuery.setLimit(anyInt())).thenReturn(this.projectQuery);

        when(this.currentStringResolver.resolve(PROJECT_FULLNAME)).thenReturn(PROJECT_REFERENCE);
    }

    @Test
    void getExistingProjectDocumentReferenceByIdAfterMissingExtension() throws Exception
    {
        when(this.extensionQuery.execute()).thenReturn(List.of());
        when(this.projectQuery.execute()).thenReturn(List.of(PROJECT_FULLNAME));

        // The project has no extension page with the same id: the cached miss must not hide the project page
        assertNull(this.extensionStore.getExistingExtensionDocumentReferenceById(ID));
        assertEquals(PROJECT_REFERENCE, this.extensionStore.getExistingProjectDocumentReferenceById(ID));
        assertNull(this.extensionStore.getExistingExtensionDocumentReferenceById(ID));
    }

    @Test
    void getExistingProjectDocumentReferenceByIdAfterProjectCreated() throws Exception
    {
        when(this.projectQuery.execute()).thenReturn(List.of(), List.of(PROJECT_FULLNAME));

        assertNull(this.extensionStore.getExistingProjectDocumentReferenceById(ID));

        // Creating the project page sets the id of its project object
        ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        verify(this.observation).addListener(listenerCaptor.capture(), anyInt());
        EventListener listener = listenerCaptor.getValue();
        XObjectPropertyUpdatedEvent event = new XObjectPropertyUpdatedEvent(new ObjectPropertyReference(
            XWikiRepositoryModel.PROP_EXTENSION_ID,
            new ObjectReference(XWikiRepositoryModel.PROJECT_CLASSNAME + "[0]", PROJECT_REFERENCE)));
        assertTrue(listener.getEvents().stream().anyMatch(e -> e.matches(event)));
        listener.onEvent(event, null, null);

        assertEquals(PROJECT_REFERENCE, this.extensionStore.getExistingProjectDocumentReferenceById(ID));
    }
}
