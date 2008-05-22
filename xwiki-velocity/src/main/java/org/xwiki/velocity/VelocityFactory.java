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

import java.util.Properties;

/**
 * Allows creating different Velocity engine instances. This is useful when you need to have
 * different Velocity engines running in the same JVM but with different configurations. Since
 * global templates in Velocity are part of the configuration the only solution to use different
 * global templates is to use different Velocity engines. As an example we need this in XWiki since
 * we want to allow each Skin to provide its own global macros.
 */
public interface VelocityFactory
{
    /**
     * This component's role, used when code needs to look it up.
     */
    public final static String ROLE = VelocityFactory.class.getName();

    /**
     * @param key the key used to cache the Velocity engine instance to return
     * @return true if there is a cached Velocity Engine matching the passed key
     */
    boolean hasVelocityEngine(String key);

    /**
     * @param key the key used to cache the Velocity engine instance to return
     * @return the cached Velocity engine instance corresponding to the passed or null if not found
     */
    VelocityEngine getVelocityEngine(String key);

    /**
     * Allow having different Velocity Engines so that each one can have its own special
     * configuration. This is especially handy for having different sets of global Velocity
     * libraries (such as for different XWiki Skins for example).
     *
     * @throws XWikiVelocityException if the Velocity Engine cannot be initialized for some reason
     */
    VelocityEngine createVelocityEngine(String key, Properties properties)
        throws XWikiVelocityException;
}
