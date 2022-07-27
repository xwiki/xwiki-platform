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
package org.xwiki.container.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.ApplicationContextListener;
import org.xwiki.container.ApplicationContextListenerManager;

/**
 * Default implementation of {@link ApplicationContextListenerManager}.
 * 
 * @version $Id$
 * @since 1.9M2
 * @deprecated use the notion of Environment instead
 */
@Component
@Singleton
@Deprecated(since = "3.5M1")
public class DefaultApplicationContextListenerManager implements ApplicationContextListenerManager
{
    /**
     * The {@link ComponentManager} used to lookup for all {@link ApplicationContextListener} components.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void initializeApplicationContext(ApplicationContext applicationContext)
    {
        try {
            List<ApplicationContextListener> initializers =
                this.componentManager.getInstanceList(ApplicationContextListener.class);
            for (ApplicationContextListener initializer : initializers) {
                initializer.initializeApplicationContext(applicationContext);
            }
        } catch (ComponentLookupException ex) {
            this.logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void destroyApplicationContext(ApplicationContext applicationContext)
    {
        try {
            List<ApplicationContextListener> initializers =
                this.componentManager.getInstanceList(ApplicationContextListener.class);
            for (ApplicationContextListener initializer : initializers) {
                initializer.destroyApplicationContext(applicationContext);
            }
        } catch (ComponentLookupException ex) {
            this.logger.error(ex.getMessage(), ex);
        }
    }       
}
