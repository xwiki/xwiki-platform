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
package org.xwiki.plexus.lifecycle.phase;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.manager.ComponentManager;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.lifecycle.phase.AbstractPhase;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.PhaseExecutionException;
import org.xwiki.component.phase.LogEnabled;
import org.xwiki.plexus.logging.PlexusLogger;

public class LogEnablePhase extends AbstractPhase
{
    public void execute(Object object, ComponentManager componentManager, ClassRealm classRealm)
        throws PhaseExecutionException
    {
        if ( object instanceof LogEnabled)
        {
            LoggerManager loggerManager = componentManager.getContainer().getLoggerManager();

            ComponentDescriptor descriptor = componentManager.getComponentDescriptor();

            org.codehaus.plexus.logging.Logger logger =
                loggerManager.getLoggerForComponent( descriptor.getRole(), descriptor.getRoleHint() );

            ( (LogEnabled) object ).enableLogging( new PlexusLogger(logger) );
        }
    }
}
