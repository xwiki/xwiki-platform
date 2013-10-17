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
package com.xpn.xwiki.plugin.activitystream.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;

import com.xpn.xwiki.plugin.activitystream.api.IgnoredEvents;
import com.xpn.xwiki.web.Utils;

/**
 * Default implementation for {@link IgnoredEvents}.
 * @version $Id$
 */
@Component
@Singleton
public class DefaultIgnoredEvents implements IgnoredEvents, Initializable
{
    private static final String CONFIGURATION_PROPERTY = "activitystream.ignoredEvents";

    private static final String EXECUTION_CONTEXT_PROPERTY = CONFIGURATION_PROPERTY;

    private List<String> ignoredEventClassNames;

    @Inject
    private Execution execution;

    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public void initialize()
    {
        // load the ignored class names in the configuration, once for all
        ignoredEventClassNames = configurationSource.getProperty(CONFIGURATION_PROPERTY);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        // Try to get the ignored class list in the execution context.
        List<Class<?>> ignoredEventClasses =
                (List<Class<?>>) execution.getContext().getProperty(EXECUTION_CONTEXT_PROPERTY);

        // If the list does not exist, then create it
        if (ignoredEventClasses == null) {
            ignoredEventClasses = new ArrayList<Class<?>>();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            for (String className : ignoredEventClassNames) {
                try {
                    ignoredEventClasses.add(classLoader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    Logger logger = Utils.getComponent(Logger.class);
                    logger.error(String.format("Failed to load the class %s.", className));
                }
            }
            // Add the list to the execution context, in order to not have to create it at every call
            // but to stay dynamic
            execution.getContext().setProperty(EXECUTION_CONTEXT_PROPERTY, ignoredEventClasses);
        }

        // Check if otherEvent is an instance of one ignored event class
        for (Class<?> cls : ignoredEventClasses) {
            if (cls.isInstance(otherEvent)) {
                return true;
            }
        }

        // No match
        return false;
    }
}
