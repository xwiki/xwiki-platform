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
package org.xwiki.test.integration;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

/**
 * Allow logging Ant messages to the console. This is used by the {@link org.xwiki.test.integration.XWikiTestSetup}
 * class which uses Ant tasks to start/stop XWiki.
 */
public class AntBuildListener implements BuildListener
{
    private boolean isDebugModeOn;

    /**
     * @param isDebugModeOn if true then display debug messages too on the console
     */
    public AntBuildListener(boolean isDebugModeOn)
    {
        this.isDebugModeOn = isDebugModeOn;
    }

    @Override
    public void buildStarted(BuildEvent event)
    {
        // Voluntarily do nothing
    }

    @Override
    public void buildFinished(BuildEvent event)
    {
        // Voluntarily do nothing
    }

    @Override
    public void targetStarted(BuildEvent event)
    {
        // Voluntarily do nothing
    }

    @Override
    public void targetFinished(BuildEvent event)
    {
        // Voluntarily do nothing
    }

    @Override
    public void taskStarted(BuildEvent event)
    {
        // Voluntarily do nothing
    }

    @Override
    public void taskFinished(BuildEvent event)
    {
        // Voluntarily do nothing
    }

    @Override
    public void messageLogged(BuildEvent event)
    {
        if ((event.getPriority() != Project.MSG_DEBUG) || isDebugModeOn) {
            System.out.println(event.getMessage());
        }
    }
}
