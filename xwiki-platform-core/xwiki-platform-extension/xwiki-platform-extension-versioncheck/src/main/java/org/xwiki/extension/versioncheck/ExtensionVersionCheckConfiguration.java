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
package org.xwiki.extension.versioncheck;

import java.util.regex.Pattern;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.version.Version;

/**
 * Provide configuration options for the extension version checker.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Role
public interface ExtensionVersionCheckConfiguration
{
    /**
     * @return true if updates of the environment extension should be checked
     */
    boolean isEnvironmentCheckEnabled();

    /**
     * @return the number of seconds between each check for a new environment version
     */
    long environmentCheckInterval();

    /**
     * @return A {@link Pattern} used on {@link Version#getValue()} to determine if a given {@link Version} should be
     * considered as a new compatible version. Returns null if no pattern is defined.
     */
    Pattern allowedEnvironmentVersions();
}
