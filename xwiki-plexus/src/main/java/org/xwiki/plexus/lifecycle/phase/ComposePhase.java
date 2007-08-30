/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package org.xwiki.plexus.lifecycle.phase;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.manager.ComponentManager;
import org.codehaus.plexus.lifecycle.phase.AbstractPhase;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.PhaseExecutionException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.PlexusContainerLocator;
import org.xwiki.component.phase.Composable;
import org.xwiki.plexus.manager.PlexusComponentManager;

public class ComposePhase extends AbstractPhase
{
    public void execute(Object object, ComponentManager componentManager, ClassRealm classRealm)
        throws PhaseExecutionException
    {
        if (object instanceof Composable) {
            org.xwiki.component.manager.ComponentManager xwikiManager = new PlexusComponentManager(
                new PlexusContainerLocator(componentManager.getContainer()));
            ((Composable) object).compose(xwikiManager);
        }    
    }
}
