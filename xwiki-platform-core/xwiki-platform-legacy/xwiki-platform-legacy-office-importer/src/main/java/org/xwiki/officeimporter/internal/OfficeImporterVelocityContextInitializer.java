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
package org.xwiki.officeimporter.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.OfficeImporterVelocityBridge;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Puts a reference to office importer in newly created velocity contexts.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component
@Named("officeimporter")
@Singleton
public class OfficeImporterVelocityContextInitializer implements VelocityContextInitializer
{
    /**
     * The key to use for office importer in the velocity context.
     */
    public static final String VELOCITY_CONTEXT_KEY = "officeimporter";

    /**
     * Used to lookup other components.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void initialize(VelocityContext context)
    {
        try {
            context.put(VELOCITY_CONTEXT_KEY, new OfficeImporterVelocityBridge(this.componentManager));
        } catch (OfficeImporterException ex) {
            this.logger.error("Unrecoverable error, office importer will not be available for velocity scripts.", ex);
        }
    }
}
