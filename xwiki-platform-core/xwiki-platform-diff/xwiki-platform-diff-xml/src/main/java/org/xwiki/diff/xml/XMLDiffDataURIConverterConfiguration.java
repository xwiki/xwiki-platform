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
package org.xwiki.diff.xml;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration for the data URI converter in the XML diff module.
 *
 * @since 14.10.15
 * @since 15.5.1
 * @since 15.6
 * @version $Id$
 */
@Unstable
@Role
public interface XMLDiffDataURIConverterConfiguration
{
    /**
     * @return the timeout to use when fetching data from the web to embed as data URI
     */
    int getHTTPTimeout();

    /**
     * @return the maximum size of the data to embed as data URI
     */
    long getMaximumContentSize();

    /**
     * @return true if the data URI converter is enabled
     */
    boolean isEnabled();
}
