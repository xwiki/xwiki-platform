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
package org.xwiki.extension.repository.aether.internal.plexus;

import javax.inject.Inject;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.repository.aether.internal.XWikiLoggerManager;

@Component
public class DefaultPlexusComponentManager implements PlexusComponentManager, Initializable
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * In-process maven runtime.
     */
    private MutablePlexusContainer plexusContainer;

    public void initialize() throws InitializationException
    {
        try {
            initializePlexus();
        } catch (PlexusContainerException e) {
            throw new InitializationException("Failed to initialize Maven", e);
        }
    }

    private void initializePlexus() throws PlexusContainerException
    {
        final String mavenCoreRealmId = "plexus.core";
        ContainerConfiguration mavenCoreCC =
            new DefaultContainerConfiguration().setClassWorld(
                new ClassWorld(mavenCoreRealmId, ClassWorld.class.getClassLoader())).setName("mavenCore");

        this.plexusContainer = new DefaultPlexusContainer(mavenCoreCC);
        this.plexusContainer.setLoggerManager(new XWikiLoggerManager(this.logger));
    }

    public PlexusContainer getPlexus()
    {
        return plexusContainer;
    }
}
