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
 *
 */
package org.xwiki.context.internal;

import java.util.Arrays;
import java.util.List;

import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import junit.framework.TestCase;

/**
 * Unit tests for {@link ExecutionContext}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultExecutionContextManagerTest extends TestCase
{
    /**
     * Verify we have different objects in the Execution Context after the clone.
     */
    public void testClone() throws Exception
    {
        ExecutionContext context = new ExecutionContext();
        
        DefaultExecutionContextManager contextManager = new DefaultExecutionContextManager();
        contextManager.addExecutionContextInitializer(new ExecutionContextInitializer() {
            public void initialize(ExecutionContext context) throws ExecutionContextException
            {
                context.setProperty("key", Arrays.asList("value"));
            }
        });
        
        ExecutionContext clonedContext = contextManager.clone(context);
        
        assertEquals("value", ((List<String>) clonedContext.getProperty("key")).get(0));
        assertNotSame(context.getProperty("key"), clonedContext.getProperty("key"));
    }
}
