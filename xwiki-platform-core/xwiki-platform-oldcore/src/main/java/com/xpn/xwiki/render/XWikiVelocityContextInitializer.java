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
 *
 */
package com.xpn.xwiki.render;

import org.xwiki.velocity.VelocityContextInitializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.apache.velocity.VelocityContext;

@Component("xwiki")
public class XWikiVelocityContextInitializer implements VelocityContextInitializer
{
    @Requirement
    private Execution execution;

    public void initialize(VelocityContext context)
    {
        // TODO: Move the Velocity Context initialization code currently located in
        // VelocityManager.getVelocityContext() here. This requires some refactoring as
        // it means the XWiki object must be initialized before this code is called.
    }
}
