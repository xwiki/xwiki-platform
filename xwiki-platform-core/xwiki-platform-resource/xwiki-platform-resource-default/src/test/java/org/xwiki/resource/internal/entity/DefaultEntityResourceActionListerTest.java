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
package org.xwiki.resource.internal.entity;

import java.io.IOException;
import java.io.StringReader;

import javax.inject.Provider;

import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.environment.Environment;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultEntityResourceActionLister}.
 *
 * @version $Id$
 * @since 7.2M1
 */
public class DefaultEntityResourceActionListerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultEntityResourceActionLister> mocker =
        new MockitoComponentMockingRule<>(DefaultEntityResourceActionLister.class);

    @Before
    public void configure() throws Exception
    {
        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);
    }

    @Test
    public void listActions() throws Exception
    {
        Environment environment = this.mocker.getInstance(Environment.class);
        when(environment.getResourceAsStream("/WEB-INF/struts-config.xml")).thenReturn(
            getClass().getClassLoader().getResourceAsStream("WEB-INF/struts-config.xml"));

        this.mocker.registerMockComponent(new DefaultParameterizedType(null, ResourceReferenceHandler.class,
            EntityResourceAction.class), "testaction");

        assertThat(this.mocker.getComponentUnderTest().listActions(), hasItems("view", "edit", "get", "testaction"));
    }

    @Test
    public void createSAXBuilder() throws Exception
    {
        SAXBuilder saxBuilder = this.mocker.getComponentUnderTest().createSAXBuilder();
        assertNotNull(saxBuilder.getEntityResolver().resolveEntity("foo", "bar"));
    }
}
