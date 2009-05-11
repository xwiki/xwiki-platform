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
package org.xwiki.configuration;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Contains a list of {@link ConfigurationSource}s that can be used by modules requiring a configuration. The idea
 * is to offer a default list of sources that all modules can use and share in order to have some centralized
 * configuration sources. However since this is implemented as a component it's also possible for a user to override
 * the component implementation and replace it by theirs thus effectively changing the sources of configuration data. 
 *
 * @version $Id$
 * @since 1.6M1
 */
@ComponentRole
public interface ConfigurationSourceCollection
{
    List<ConfigurationSource> getConfigurationSources();
}
