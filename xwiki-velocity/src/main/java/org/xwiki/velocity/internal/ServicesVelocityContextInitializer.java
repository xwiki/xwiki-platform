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
package org.xwiki.velocity.internal;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Registers the Script Service Manager in the Velocity Context so that it's available from Velocity.
 *
 * @version $Id$
 * @since 2.3M1
 */
// TODO: In the future Velocity will be implemented using the JSR 223 API and this class won't be required anymore.
@Component("scriptservices")
public class ServicesVelocityContextInitializer implements VelocityContextInitializer
{
    /**
     * The Script Service Manager to bind in the Script Context.
     */
    @Requirement
    private ScriptServiceManager scriptServiceManager;

    /**
     * {@inheritDoc}
     * @see org.xwiki.velocity.VelocityContextInitializer#initialize(org.apache.velocity.VelocityContext)
     */
    public void initialize(VelocityContext context)
    {
        context.put("services", this.scriptServiceManager);
    }
}
