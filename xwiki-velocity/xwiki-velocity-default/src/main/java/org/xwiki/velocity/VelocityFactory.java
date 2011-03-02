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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Allows creating different Velocity engine instances. This is useful when you need to have different Velocity engines
 * running in the same JVM but with different configurations. Since global templates in Velocity are part of the
 * configuration the only solution to use different global templates is to use different Velocity engines. As an example
 * we need this in XWiki since we want to allow each Skin to provide its own global macros.
 * 
 * @version $Id$
 */
@ComponentRole
public interface VelocityFactory
{
    /**
     * Check if an engine was already created for a certain key.
     * 
     * @param key the key under which the Velocity engine has been saved in cache. This is the key used when the
     *            Velocity engine was created using {@link #createVelocityEngine(String, java.util.Properties)}
     * @return <code>true</code> if there is a cached Velocity Engine matching the passed key, <code>false</code>
     *         otherwise.
     */
    boolean hasVelocityEngine(String key);

    /**
     * Retrieves from the cache the Velocity engine corresponding to a specific key, if such an engine was already
     * created.
     * 
     * @param key the key under which the Velocity engine has been saved in cache. This is the key used when the
     *            Velocity engine was created using {@link #createVelocityEngine(String, java.util.Properties)}
     * @return the cached Velocity engine instance corresponding to the passed key or <code>null</code> if not found
     */
    VelocityEngine getVelocityEngine(String key);

    /**
     * Creates a new Velocity engine instance, which will be cached using a specific key for later reuse. The key allows
     * having different Velocity Engines, so that each one can have its own special configuration. This is especially
     * handy for having different sets of global Velocity libraries (such as for different XWiki Skins for example). If
     * another engine was previously created for the same key, then that instance is returned instead, without creating
     * any other instance.
     * 
     * @param key the key used to cache the Velocity engine instance to return
     * @param properties the list of properties that will override the default properties when creating the engine. For
     *            example it's possible to define a list of global velocimacros by passing the
     *            <code>RuntimeConstants.VM_LIBRARY</code> property key.
     * @return the newly created Velocity Engine, or an existing one, if an engine was previously created for the same
     *         key.
     * @throws XWikiVelocityException if the Velocity Engine cannot be initialized for some reason
     * @todo How to create a new engine instance when parameters have changed? Shouldn't this discard the old instance
     *       and create a new one, instead?
     */
    VelocityEngine createVelocityEngine(String key, Properties properties) throws XWikiVelocityException;
}
