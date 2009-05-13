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
package org.xwiki.velocity;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.ComponentRole;

/**
 * Provides access to the main XWiki Velocity objects.
 * 
 * @since 1.5M2
 * @version $Id$
 */
@ComponentRole
public interface VelocityManager
{
    /**
     * @return the current Velocity Context retrieved from the Execution Context
     */
    VelocityContext getVelocityContext();

    /**
     * Get the current Velocity Engine or create one if none has been created.
     * 
     * @return the current Velocity Engine retrieved from the Execution Context
     * @throws XWikiVelocityException if the Velocity Engine cannot be created
     * @todo Move the engine creation to some initialization method instead and remove the need for throwing an
     *       exception
     */
    VelocityEngine getVelocityEngine() throws XWikiVelocityException;
}
