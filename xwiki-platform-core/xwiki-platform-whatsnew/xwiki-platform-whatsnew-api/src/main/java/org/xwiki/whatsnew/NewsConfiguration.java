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
package org.xwiki.whatsnew;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Configuration options for the What's New extension.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Role
public interface NewsConfiguration
{
    /**
     * @return the list of news sources to use
     */
    List<NewsSourceDescriptor> getNewsSourceDescriptors();

    /**
     * @return the time after which a check for new news should be performed (in seconds)
     */
    long getNewsRefreshRate();

    /**
     * @return the maximum number of news items to display at once, from each source
     */
    int getNewsDisplayCount();

    /**
     * @return true if the feature is active or false otherwise (not configuring any News source makes the feature
     *         inactive)
     */
    boolean isActive();
}
