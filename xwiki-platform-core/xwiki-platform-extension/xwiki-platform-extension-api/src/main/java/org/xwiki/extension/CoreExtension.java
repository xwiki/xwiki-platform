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
package org.xwiki.extension;

import java.net.URL;

/**
 * Represent an extension which can't be modified (uninstalled, upgraded).
 * <p>
 * In practice it's generally all the jars already in the classpath at startup.
 * 
 * @version $Id$
 */
public interface CoreExtension extends Extension
{
    /**
     * @see #getURL()
     */
    String PKEY_URL = "core.url";

    /**
     * @see #isGuessed()
     */
    String PKEY_GUESSED = "core.guessed";

    /**
     * @return the core extension URL
     */
    URL getURL();

    /**
     * @return true if the extension is "guessed" which means that it's id or version are not 100% sure. It generally
     *         indicate that a jar without any technical information or partial information has been found in the
     *         classpath.
     */
    boolean isGuessed();
}
