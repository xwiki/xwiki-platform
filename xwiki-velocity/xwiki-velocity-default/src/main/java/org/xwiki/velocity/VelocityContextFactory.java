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
 * Creates Velocity Context instances, pre-populated with the Velocity Tools enabled in the component's configuration,
 * and by all the enabled {@link VelocityContextInitializer} components. The returned context is supposed to be used
 * only once, as otherwise data may leak between different requests, and the parsing/rendering process would be affected
 * by previous data.
 * 
 * @version $Id$
 */
@ComponentRole
public interface VelocityContextFactory
{
    /**
     * Creates and initializes a new Velocity Context.
     * 
     * @return a new Velocity Context, populated with Tools and data pushed by {@link VelocityContextInitializer}s
     * @throws XWikiVelocityException if the context failed to be created or initialized properly
     */
    VelocityContext createContext() throws XWikiVelocityException;
}
