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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Velocity context initializer used to add the annotations service on the velocity context.
 * 
 * @version $Id$
 * @since 2.3M1
 * @deprecated starting with 3.0RC1 use directly the Annotation Script Service from scripts
 */
@Component
@Named("annotations")
@Singleton
@Deprecated
public class AnnotationVelocityContextInitializer implements VelocityContextInitializer
{
    /**
     * The key to add to the velocity context.
     */
    public static final String VELOCITY_CONTEXT_KEY = "annotations";
    
    /**
     * The hint under which the annotations script service is registered.
     */
    private static final String ANNOTATION_SCRIPT_SERVICE_HINT = VELOCITY_CONTEXT_KEY;

    /**
     * Component manager to pull all services instances to build the bridge.
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
            // create a wrapper of the annotation service for exposing its methods in velocity
            ScriptService annotationsScriptService = componentManager.getInstance(ScriptService.class,
                ANNOTATION_SCRIPT_SERVICE_HINT);
            context.put(VELOCITY_CONTEXT_KEY, annotationsScriptService);
        } catch (ComponentLookupException e) {
            this.logger.warn(
                "Could not initialize the annotations velocity bridge, "
                    + "annotations service will not be accessible in velocity context.");
        }
    }
}
