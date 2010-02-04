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
package org.xwiki.script.internal.service;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.script.service.ScriptServiceNotFoundException;

/**
 * Locate Script Services by name dynamically at runtime by looking them up agains the Component Manager.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component
public class DefaultScriptServiceManager implements ScriptServiceManager
{
    /**
     * Used to locate Script Services dynamically. Note that since the lookup is done dynamically new Script Services
     * can be added on the fly in the classloader and they'll be found (after they've been registered against the
     * component manager obviously). 
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * @see org.xwiki.script.service.ScriptServiceManager#get(String)
     */
    public ScriptService get(String serviceName) throws ScriptServiceNotFoundException
    {
        ScriptService scriptService;
        try {
            scriptService = this.componentManager.lookup(ScriptService.class, serviceName);
        } catch (ComponentLookupException e) {
            throw new ScriptServiceNotFoundException("Failed to locate Script Service [" + serviceName + "]", e);
        }
        return scriptService;
    }
}
