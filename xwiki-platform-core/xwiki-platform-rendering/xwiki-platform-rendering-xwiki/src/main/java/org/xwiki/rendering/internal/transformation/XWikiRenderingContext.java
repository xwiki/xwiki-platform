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
package org.xwiki.rendering.internal.transformation;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.velocity.VelocityManager;

/**
 * Add some XWiki level stuff to {@link DefaultRenderingContext}.
 * 
 * @version $Id$
 * @since 6.0
 */
@Component
@Singleton
public class XWikiRenderingContext extends DefaultRenderingContext
{
    @Inject
    private VelocityManager velocityManager;

    @Inject
    private Logger logger;

    @Override
    public void push(Transformation transformation, TransformationContext context)
    {
        super.push(transformation, context);

        String namespace = context.getId();
        if (namespace != null) {
            openNamespace(namespace);
        }
    }

    @Override
    public void pop()
    {
        String namespace = peek().getTransformationId();
        if (namespace != null) {
            closeNamespace(namespace);
        }

        super.pop();
    }

    private void openNamespace(String namespace)
    {
        try {
            // Mark that we're starting to use a different Velocity macro name-space.
            velocityManager.getVelocityEngine().startedUsingMacroNamespace(namespace);
            logger.debug("Started using velocity macro namespace [{}].", namespace);
        } catch (Exception e) {
            // Failed to get the Velocity Engine and thus to clear Velocity Macro cache. Log this as a warning but
            // continue since it's not absolutely critical.
            logger.warn("Failed to notify Velocity Macro cache for opening the [{}] namespace. Reason = [{}]",
                namespace, e.getMessage());
        }
    }

    private void closeNamespace(String namespace)
    {
        try {
            velocityManager.getVelocityEngine().stoppedUsingMacroNamespace(namespace);
            logger.debug("Stopped using velocity macro namespace [{}].", namespace);
        } catch (Exception e) {
            // Failed to get the Velocity Engine and thus to clear Velocity Macro cache. Log this as a warning but
            // continue since it's not absolutely critical.
            logger.warn("Failed to notify Velocity Macro cache for closing the [{}] namespace. Reason = [{}]",
                namespace, e.getMessage());
        }
    }
}
