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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link DefaultWikiComponentInvocationHandler}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultWikiComponentInvocationHandlerTest
{
    @MockComponent
    AuthorExecutor authorExecutor;

    @MockComponent
    WikiComponentMethodExecutor methodExecutor;

    @InjectComponentManager
    ComponentManager componentManager;

    public static class TestComponent
    {
        public void foo()
        {

        }
    }

    @Test
    void invokeWithTheRightAuthor() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "document");
        DocumentReference authorReference = new DocumentReference("wiki", "space", "author");

        DefaultWikiComponent component = new DefaultWikiComponent(documentReference, authorReference, null, null, null);
        Map<String, XDOM> methods = new HashMap<>();
        methods.put("foo", null);
        component.setHandledMethods(methods);

        DefaultWikiComponentInvocationHandler handler =
            new DefaultWikiComponentInvocationHandler(component, this.authorExecutor, this.componentManager);

        Method method = TestComponent.class.getMethod("foo");

        handler.invoke(null, method, null);

        verify(this.authorExecutor).call(any(), same(authorReference), same(documentReference));
    }
}
