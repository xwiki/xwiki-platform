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
package org.xwiki.webjars;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.*;
import org.xwiki.action.ActionChain;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ActionId;
import org.xwiki.resource.EntityResource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.webjars.internal.TestableWebJarsAction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.webjars.internal.WebJarsAction}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class WebJarsActionTest
{
    @Rule
    public MockitoComponentMockingRule<TestableWebJarsAction> componentManager =
        new MockitoComponentMockingRule<TestableWebJarsAction>(TestableWebJarsAction.class);

    @Test
    public void executeWhenResourceDoesntExist() throws Exception
    {
        EntityResource resource = new EntityResource(new DocumentReference("wiki", "space", "page"), ActionId.VIEW);
        resource.addParameter("value", "angular/2.1.11/angular.js");
        ActionChain actionChain = mock(ActionChain.class);
        TestableWebJarsAction action = this.componentManager.getComponentUnderTest();

        ClassLoader classLoader = mock(ClassLoader.class);

        action.setClassLoader(classLoader);

        action.execute(resource, actionChain);

        verify(classLoader).getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js");
    }

    @Test
    public void executeWhenResourceExists() throws Exception
    {
        EntityResource resource = new EntityResource(new DocumentReference("wiki", "space", "page"), ActionId.VIEW);
        resource.addParameter("value", "angular/2.1.11/angular.js");
        ActionChain actionChain = mock(ActionChain.class);
        TestableWebJarsAction action = this.componentManager.getComponentUnderTest();

        ClassLoader classLoader = mock(ClassLoader.class);
        ByteArrayInputStream bais = new ByteArrayInputStream("content".getBytes());
        when(classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            bais);

        Container container = this.componentManager.getInstance(Container.class);
        Response response = mock(Response.class);
        when(container.getResponse()).thenReturn(response);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(baos);
        action.setClassLoader(classLoader);

        action.execute(resource, actionChain);

        // Verify that the resource content has been copied to the Response output stream.
        assertEquals("content", baos.toString());
    }
}
