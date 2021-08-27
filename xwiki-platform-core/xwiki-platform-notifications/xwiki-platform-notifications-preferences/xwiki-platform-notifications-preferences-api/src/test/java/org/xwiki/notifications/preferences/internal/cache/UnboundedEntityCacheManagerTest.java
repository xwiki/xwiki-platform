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
package org.xwiki.notifications.preferences.internal.cache;

import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link UnboundedEntityCacheManager} and {@link UnboundedEntityCacheInvalidatorListener}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(UnboundedEntityCacheInvalidatorListener.class)
class UnboundedEntityCacheManagerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "document");

    @MockComponent
    private JMXBeanRegistration jmx;

    @InjectMockComponents
    private UnboundedEntityCacheManager manager;

    @InjectComponentManager
    private ComponentManager componentManager;

    private EventListener listener;

    private void assertCreate(boolean invalidateOnUpdate) throws ComponentLookupException, MalformedObjectNameException,
        IntrospectionException, InstanceNotFoundException, ReflectionException
    {
        Map<EntityReference, String> cache = this.manager.createCache("cache", invalidateOnUpdate);

        assertSame(cache, this.manager.getCache("cache"));

        this.listener =
            this.componentManager.getInstance(EventListener.class, UnboundedEntityCacheInvalidatorListener.NAME);

        cache.put(DOCUMENT_REFERENCE, "value");

        assertEquals("value", cache.get(DOCUMENT_REFERENCE));

        this.listener.onEvent(new DocumentDeletedEvent(DOCUMENT_REFERENCE), new XWikiDocument(DOCUMENT_REFERENCE),
            null);

        assertNull(cache.get(DOCUMENT_REFERENCE));

        cache.put(DOCUMENT_REFERENCE, "value");

        this.listener.onEvent(new DocumentUpdatedEvent(DOCUMENT_REFERENCE), new XWikiDocument(DOCUMENT_REFERENCE),
            null);

        if (invalidateOnUpdate) {
            assertNull(cache.get(DOCUMENT_REFERENCE));
        } else {
            assertEquals("value", cache.get(DOCUMENT_REFERENCE));
        }

        cache.put(DOCUMENT_REFERENCE, "document");
        cache.put(DOCUMENT_REFERENCE.getWikiReference(), "wiki");

        assertEquals("wiki", cache.get(DOCUMENT_REFERENCE.getWikiReference()));

        this.listener.onEvent(new WikiDeletedEvent(DOCUMENT_REFERENCE.getWikiReference().getName()), null, null);

        assertNull(cache.get(DOCUMENT_REFERENCE));
        assertNull(cache.get(DOCUMENT_REFERENCE.getWikiReference()));

        verify(this.jmx).registerMBean(isA(JMXUnboundedEntityCacheMBean.class), eq("type=UnboundedEntityCache,name=cache"));
    }

    @Test
    void createWithoutInvalidationOnUpdate() throws ComponentLookupException, MalformedObjectNameException,
        IntrospectionException, InstanceNotFoundException, ReflectionException
    {
        assertCreate(false);
    }

    @Test
    void createWithInvalidationOnUpdate() throws ComponentLookupException, MalformedObjectNameException,
        IntrospectionException, InstanceNotFoundException, ReflectionException
    {
        assertCreate(true);
    }
}
