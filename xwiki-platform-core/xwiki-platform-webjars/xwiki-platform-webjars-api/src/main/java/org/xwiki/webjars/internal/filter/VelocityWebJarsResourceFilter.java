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
package org.xwiki.webjars.internal.filter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.velocity.VelocityManager;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Evaluates the velocity content inside the requested WebJars resource.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Component
@Named("velocity")
@Singleton
public class VelocityWebJarsResourceFilter implements WebJarsResourceFilter
{
    /**
     * Used to evaluate the Velocity code from the WebJar resources.
     */
    @Inject
    private VelocityManager velocityManager;

    @Override
    public InputStream filter(InputStream resourceStream, String resourceName)
        throws ResourceReferenceHandlerException
    {
        try {
            // Evaluates the given resource using Velocity.
            StringWriter writer = new StringWriter();
            this.velocityManager.getVelocityEngine().evaluate(this.velocityManager.getVelocityContext(), writer,
                resourceName, new InputStreamReader(resourceStream, UTF_8));
            return new ByteArrayInputStream(writer.toString().getBytes(UTF_8));
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to evaluate the Velocity code from WebJar resource [%s]", resourceName),
                e);
        }
    }
}
