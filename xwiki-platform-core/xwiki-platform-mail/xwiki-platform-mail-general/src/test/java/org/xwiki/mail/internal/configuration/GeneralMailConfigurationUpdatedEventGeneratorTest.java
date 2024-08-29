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
package org.xwiki.mail.internal.configuration;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.mail.GeneralMailConfigurationUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GeneralMailConfigurationUpdatedEventGenerator}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    DefaultObservationManager.class})
@ReferenceComponentList
class GeneralMailConfigurationUpdatedEventGeneratorTest
{
    private static final String MAIN_WIKI = "main";

    private static final String CURRENT_WIKI_CACHE_ID = "configuration.document.mail.general";

    private static final String MAIN_WIKI_CACHE_ID = "configuration.document.mail.general.mainwiki";

    @MockComponent
    private CacheManager cacheManager;

    /**
     * The mail configuration for the current wiki, needed by the eventGenerator, injected here to make
     * sure its members are all mocks. A real instance is used here as otherwise the cache invalidation cannot be
     * tested.
     */
    @InjectMockComponents
    private GeneralMailConfigClassDocumentConfigurationSource currentWikiConfigurationSource;

    @InjectMockComponents
    private MainWikiGeneralMailConfigClassDocumentConfigurationSource mainWikiConfigurationSource;

    @InjectMockComponents
    private GeneralMailConfigurationUpdatedEventGenerator eventGenerator;

    @Inject
    private ObservationManager observationManager;

    @MockComponent
    private XWikiContext context;

    private Cache<Object> mainWikiCache;

    private Cache<Object> currentWikiCache;

    @BeforeComponent
    private void mockCache(MockitoComponentManager componentManager) throws CacheException
    {
        // Can't use @Mock annotation as mocks are injected too late.
        this.mainWikiCache = mock();
        this.currentWikiCache = mock();
        when(this.cacheManager.createNewCache(any())).then(invocation -> {
            CacheConfiguration configuration = invocation.getArgument(0);
            if (configuration.getConfigurationId().equals(CURRENT_WIKI_CACHE_ID)) {
                return this.currentWikiCache;
            } else if (MAIN_WIKI_CACHE_ID.equals(configuration.getConfigurationId())) {
                return this.mainWikiCache;
            }

            throw new IllegalArgumentException("Unknown cache configuration ID ["
                + configuration.getConfigurationId() + "]");
        });

        when(this.context.isMainWiki(MAIN_WIKI)).thenReturn(true);
        Utils.setComponentManager(componentManager);
    }

    @ParameterizedTest
    @MethodSource("eventParametersSource")
    void onEvent(String wiki, Callable<Event> eventCallable, Object source, String newEventSource) throws Exception
    {
        Event event = eventCallable.call();

        // Unregister the listeners of the configuration sources themselves to check that cache invalidation is
        // really triggered independently.
        this.observationManager.removeListener(CURRENT_WIKI_CACHE_ID);
        this.observationManager.removeListener(MAIN_WIKI_CACHE_ID);
        // Register the listener explicitly as it seems that it isn't registered automatically.
        this.observationManager.addListener(this.eventGenerator);

        EventListener allWikiListener = mock();
        when(allWikiListener.getEvents()).thenReturn(List.of(new GeneralMailConfigurationUpdatedEvent()));
        when(allWikiListener.getName()).thenReturn("allWiki");

        EventListener currentWikiListener = mock();
        when(currentWikiListener.getEvents()).thenReturn(List.of(new GeneralMailConfigurationUpdatedEvent(wiki)));
        when(currentWikiListener.getName()).thenReturn("currentWiki");

        EventListener wikiListener = mock();
        when(wikiListener.getEvents()).thenReturn(List.of(new GeneralMailConfigurationUpdatedEvent("wiki")));
        when(wikiListener.getName()).thenReturn("wiki");

        this.observationManager.addListener(allWikiListener);
        this.observationManager.addListener(currentWikiListener);
        this.observationManager.addListener(wikiListener);

        this.observationManager.notify(event, source);

        verify(this.currentWikiCache).removeAll();
        if (MAIN_WIKI.equals(wiki)) {
            verify(this.mainWikiCache).removeAll();
        } else {
            verify(this.mainWikiCache, never()).removeAll();
        }

        if (List.of(MAIN_WIKI, "wiki").contains(wiki)) {
            verify(wikiListener).onEvent(any(), eq(newEventSource), eq(null));
        } else {
            verify(wikiListener, never()).onEvent(any(), any(), any());
        }

        verify(allWikiListener).onEvent(any(), eq(newEventSource), eq(null));
        verify(currentWikiListener).onEvent(any(), eq(newEventSource), eq(null));
    }

    private static ObjectReference getConfigurationObjectReference(String wikiId)
    {
        DocumentReference documentReference = new DocumentReference(
            AbstractGeneralMailConfigClassDocumentConfigurationSource.MAILCONFIG_REFERENCE, new WikiReference(wikiId));
        DocumentReference configClassReference = new DocumentReference(
            AbstractGeneralMailConfigClassDocumentConfigurationSource.GENERAL_MAILCONFIGCLASS_REFERENCE,
            new WikiReference(wikiId));
        return new BaseObjectReference(configClassReference, 0, documentReference);
    }

    static Stream<Arguments> eventParametersSource()
    {
        // We cannot call getConfigurationObjectReference() here as the component manager hasn't been set on Utils
        // yet and thus initializing the BaseObjectReference fails. Therefore, this returns callables that do the call.
        return Stream.of("wiki", "otherwiki", MAIN_WIKI).flatMap(wiki -> {
            DocumentReference documentReference =
                new DocumentReference(
                    AbstractGeneralMailConfigClassDocumentConfigurationSource.MAILCONFIG_REFERENCE,
                    new WikiReference(wiki)
                );
            Object source = new XWikiDocument(documentReference);
            String newEventSource = MAIN_WIKI.equals(wiki) ? null : wiki;

            return Stream.of(
                arguments(
                    wiki,
                    named("XObjectUpdatedEvent",
                        (Callable<Event>) () -> new XObjectUpdatedEvent(getConfigurationObjectReference(wiki))
                    ),
                    source,
                    newEventSource
                ),
                arguments(
                    wiki,
                    named("XObjectDeletedEvent",
                        (Callable<Event>) () -> new XObjectDeletedEvent(getConfigurationObjectReference(wiki))
                    ),
                    source,
                    newEventSource
                ),
                arguments(
                    wiki,
                    named("XObjectAddedEvent",
                        (Callable<Event>) () -> new XObjectAddedEvent(getConfigurationObjectReference(wiki))
                    ),
                    source,
                    newEventSource
                )
            );
        });
    }
}
