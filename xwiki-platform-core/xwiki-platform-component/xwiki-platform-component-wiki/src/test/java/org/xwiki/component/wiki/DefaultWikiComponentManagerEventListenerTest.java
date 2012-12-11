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
package org.xwiki.component.wiki;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.wiki.internal.DefaultWikiComponent;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManagerEventListener;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

@MockingRequirement(DefaultWikiComponentManagerEventListener.class)
public class DefaultWikiComponentManagerEventListenerTest extends AbstractMockingComponentTestCase implements
    WikiComponentConstants
{
    private static final String ROLE_HINT = "roleHint";

    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private DefaultWikiComponentManagerEventListener listener;

    private WikiComponentBuilder provider;

    @Before
    public void configure() throws Exception
    {
        this.provider =
            getComponentManager().registerMockComponent(getMockery(), WikiComponentBuilder.class, "default");
        this.listener =
            getComponentManager().getInstance(EventListener.class, "defaultWikiComponentManagerEventListener");
    }

    @Test
    public void onEventWhenSourceIsNotAXWikiDocument() throws Exception
    {
        final WikiComponentManager wikiComponentManager = getComponentManager().getInstance(WikiComponentManager.class);

        getMockery().checking(new Expectations()
        {
            {
                never(wikiComponentManager).registerWikiComponent(with(any(WikiComponent.class)));
                never(wikiComponentManager).unregisterWikiComponents(with(any(DocumentReference.class)));
            }
        });

        listener.onEvent(null, null, null);
    }

    @Test
    public void onEventWhenDocumentDoesNotContainAComponent() throws Exception
    {
        final WikiComponentManager wikiComponentManager = getComponentManager().getInstance(WikiComponentManager.class);
        final DocumentModelBridge componentDoc = getMockery().mock(DocumentModelBridge.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(componentDoc).getDocumentReference();
                will(returnValue(DOC_REFERENCE));
                never(wikiComponentManager).registerWikiComponent(with(any(WikiComponent.class)));
                never(wikiComponentManager).unregisterWikiComponents(with(any(DocumentReference.class)));
            }
        });

        listener.onEvent(null, componentDoc, null);
    }

    @Test
    public void onDocumentCreated() throws Exception
    {
        final WikiComponentManager manager = getComponentManager().getInstance(WikiComponentManager.class);
        final List<DocumentReference> providerReferences = new ArrayList<DocumentReference>();
        providerReferences.add(DOC_REFERENCE);
        final DocumentModelBridge componentDoc = getMockery().mock(DocumentModelBridge.class);
        final WikiComponent component =
            new DefaultWikiComponent(DOC_REFERENCE, AUTHOR_REFERENCE, TestRole.class, ROLE_HINT,
                WikiComponentScope.WIKI);
        final List<WikiComponent> components = new ArrayList<WikiComponent>();
        components.add(component);

        getMockery().checking(new Expectations()
        {
            {
                allowing(componentDoc).getDocumentReference();
                will(returnValue(DOC_REFERENCE));
                oneOf(manager).unregisterWikiComponents(DOC_REFERENCE);
                oneOf(provider).getDocumentReferences();
                will(returnValue(providerReferences));
                oneOf(provider).buildComponents(DOC_REFERENCE);
                will(returnValue(components));
                oneOf(manager).registerWikiComponent(component);
            }
        });

        listener.onEvent(new DocumentCreatedEvent(DOC_REFERENCE), componentDoc, null);
    }

    @Test
    public void onDocumentUpdated() throws Exception
    {
        final WikiComponentManager manager = getComponentManager().getInstance(WikiComponentManager.class);
        final List<DocumentReference> providerReferences = new ArrayList<DocumentReference>();
        providerReferences.add(DOC_REFERENCE);
        final DocumentModelBridge componentDoc = getMockery().mock(DocumentModelBridge.class);
        final WikiComponent component =
            new DefaultWikiComponent(DOC_REFERENCE, AUTHOR_REFERENCE, TestRole.class, ROLE_HINT,
                WikiComponentScope.WIKI);
        final List<WikiComponent> components = new ArrayList<WikiComponent>();
        components.add(component);

        getMockery().checking(new Expectations()
        {
            {
                allowing(componentDoc).getDocumentReference();
                will(returnValue(DOC_REFERENCE));
                oneOf(manager).unregisterWikiComponents(DOC_REFERENCE);
                oneOf(provider).getDocumentReferences();
                will(returnValue(providerReferences));
                oneOf(provider).buildComponents(DOC_REFERENCE);
                will(returnValue(components));
                oneOf(manager).registerWikiComponent(component);
            }
        });

        Event event = new DocumentUpdatedEvent(DOC_REFERENCE);
        listener.onEvent(event, componentDoc, null);
    }

    @Test
    public void onDocumentDeleted() throws Exception
    {
        final WikiComponentManager manager = getComponentManager().getInstance(WikiComponentManager.class);
        final DocumentModelBridge componentDoc = getMockery().mock(DocumentModelBridge.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(componentDoc).getDocumentReference();
                will(returnValue(DOC_REFERENCE));
                oneOf(manager).unregisterWikiComponents(DOC_REFERENCE);
            }
        });

        listener.onEvent(new DocumentDeletedEvent(DOC_REFERENCE), componentDoc, null);
    }

    @Test
    public void onApplicationReady() throws Exception
    {
        final WikiComponentManager manager = getComponentManager().getInstance(WikiComponentManager.class);
        final DocumentModelBridge componentDoc = getMockery().mock(DocumentModelBridge.class);
        final WikiComponent component
            = new DefaultWikiComponent(DOC_REFERENCE, AUTHOR_REFERENCE, TestRole.class, ROLE_HINT,
            WikiComponentScope.WIKI);
        final List<WikiComponent> components = new ArrayList<WikiComponent>();
        components.add(component);
        final List<DocumentReference> providerReferences = new ArrayList<DocumentReference>();
        providerReferences.add(DOC_REFERENCE);

        getMockery().checking(new Expectations()
        {
            {
                allowing(componentDoc).getDocumentReference();
                will(returnValue(DOC_REFERENCE));
                oneOf(provider).getDocumentReferences();
                will(returnValue(providerReferences));
                oneOf(provider).buildComponents(DOC_REFERENCE);
                will(returnValue(components));
                oneOf(manager).registerWikiComponent(component);
            }
        });

        listener.onEvent(new ApplicationReadyEvent(), null, null);
    }

}
