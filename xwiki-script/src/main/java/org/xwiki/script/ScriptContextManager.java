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
package org.xwiki.script;

import javax.script.ScriptContext;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Helpers for {@link ScriptContext} management. Like context initialization, etc.
 * 
 * @version $Id$
 */
@ComponentRole
public interface ScriptContextManager
{
    /**
     * @return a clean and initialized {@link ScriptContext}. Clean means that all
     *         {@link org.xwiki.script.ScriptContextInitializer} will have been executed on the returned Script Context
     *         which will thus contain re-initialized values for the values which are set in
     *         {@link org.xwiki.script.ScriptContextInitializer}. The values that are set before calling this method
     *         and for which there are no {@link org.xwiki.script.ScriptContextInitializer} will not be modified.  
     */
    ScriptContext getScriptContext();
}
