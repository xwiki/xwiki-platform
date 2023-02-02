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
package org.xwiki.tool.packager.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentManagerInitializer;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.observation.EventListener;

/**
 * Dedicated component manager initializer for the packager plugin.
 * Main role of this initializer is to unregister the components we don't want to have when the importer is used.
 *
 * @version $Id$
 * @since 15.0
 */
@Component
@Singleton
@Named("packager")
public class PackagerComponentManagerInitializer implements ComponentManagerInitializer
{
    @Override
    public void initialize(ComponentManager componentManager)
    {
        // We don't want the solr indexer to be triggered whenever a document is imported.
        componentManager.unregisterComponent(EventListener.class, "solr.update");
        // We don't need the untyped event listener and it requires specific dependencies.
        componentManager.unregisterComponent(EventListener.class, "Untyped Event Listener");
        //  Cancel ThreadClassloaderExecutionContextInitializer to not mess with the Maven classloader.
        componentManager.unregisterComponent(ExecutionContextInitializer.class, "threadclassloader");
    }
}
