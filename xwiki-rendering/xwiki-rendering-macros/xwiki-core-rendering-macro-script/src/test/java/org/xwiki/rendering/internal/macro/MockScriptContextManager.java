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
package org.xwiki.rendering.internal.macro;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.script.ScriptContextManager;

/**
 * Mock ScriptContextManager implementation used for testing, since we don't want to pull any dependency on the
 * Model/Skin/etc for the Rendering module's unit tests.
 * 
 * @version $Id$
 * @since 1.7M3
 */
public class MockScriptContextManager implements ScriptContextManager
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor getComponentDescriptor()
    {
        DefaultComponentDescriptor componentDescriptor = new DefaultComponentDescriptor();

        componentDescriptor.setRole(ScriptContextManager.ROLE);
        componentDescriptor.setRoleHint("default");
        componentDescriptor.setImplementation(MockScriptContextManager.class.getName());

        return componentDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.script.ScriptContextManager#getScriptContext()
     */
    public ScriptContext getScriptContext()
    {
        return new SimpleScriptContext();
    }
}
