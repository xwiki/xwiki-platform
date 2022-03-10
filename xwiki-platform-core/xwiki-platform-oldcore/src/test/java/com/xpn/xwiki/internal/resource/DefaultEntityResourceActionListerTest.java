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
package com.xpn.xwiki.internal.resource;

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.internal.web.LegacyAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultEntityResourceActionLister}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultEntityResourceActionListerTest
{
    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @InjectMockComponents
    private DefaultEntityResourceActionLister listener;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @Test
    void listActions() throws Exception
    {
        this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, ResourceReferenceHandler.class, EntityResourceAction.class),
            "testaction");
        this.componentManager.registerMockComponent(LegacyAction.class, "view");

        assertEquals(Arrays.asList("view", "testaction"), this.listener.listActions());
    }
}
