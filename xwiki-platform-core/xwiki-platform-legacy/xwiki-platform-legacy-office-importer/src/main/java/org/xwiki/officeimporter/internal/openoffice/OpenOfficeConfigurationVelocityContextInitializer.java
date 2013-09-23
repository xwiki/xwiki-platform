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
package org.xwiki.officeimporter.internal.openoffice;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfigurationVelocityBridge;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Puts a reference to {@link OpenOfficeConfigurationVelocityBridge} in newly created velocity contexts.
 * 
 * @version $Id$
 * @since 1.9RC1
 */
@Component
@Named("ooconfig")
@Singleton
public class OpenOfficeConfigurationVelocityContextInitializer implements VelocityContextInitializer
{
    /**
     * The key to use for {@link OpenOfficeConfigurationVelocityBridge} in the velocity context.
     */
    public static final String VELOCITY_CONTEXT_KEY = "ooconfig";

    /**
     * The {@link OpenOfficeConfiguration} component.
     */
    @Inject
    private OpenOfficeConfiguration ooConfig;

    @Override
    public void initialize(VelocityContext velocityContext)
    {
        velocityContext.put(VELOCITY_CONTEXT_KEY, new OpenOfficeConfigurationVelocityBridge(this.ooConfig));
    }
}
