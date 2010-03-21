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
package org.xwiki.annotation.internal;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Velocity context initializer used to add the annotations service on the velocity context.
 * 
 * @version $Id$
 */
@Component("annotations")
public class AnnotationVelocityContextInitializer extends AbstractLogEnabled implements VelocityContextInitializer
{
    /**
     * The key to add to the velocity context.
     */
    public static final String VELOCITY_CONTEXT_KEY = "annotations";

    /**
     * Component manager to pull all services instances to build the bridge.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.VelocityContextInitializer#initialize(org.apache.velocity.VelocityContext)
     */
    public void initialize(VelocityContext context)
    {
        try {
            // create a wrapper of the annotation service for exposing its methods in velocity
            AnnotationVelocityBridge bridge = new AnnotationVelocityBridge(componentManager);
            context.put(VELOCITY_CONTEXT_KEY, bridge);
        } catch (ComponentLookupException e) {
            getLogger().warn(
                "Could not initialize the annotations velocity bridge, "
                    + "annotations service will not be accessible in velocity context.");
        }
    }
}
