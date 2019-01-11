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
package org.xwiki.skin;

import java.util.Map;

import org.xwiki.filter.input.InputSource;

/**
 * @param <I> the type of the {@link InputSource}
 * @version $Id$
 * @since 7.0M1
 */
public interface Resource<I extends InputSource>
{
    /**
     * @return the repository containing the resource
     */
    ResourceRepository getRepository();

    /**
     * @return the unique identifier of the resource
     */
    String getId();

    /**
     * @return the path of the resource
     */
    String getPath();

    /**
     * @return the name of the resource (usually a relative path)
     * @since 8.3RC1
     */
    String getResourceName();

    /**
     * Provide an {@link InputSource} to read the resource.
     * 
     * @return an {@link InputSource} to read the resource
     * @throws Exception when failing to create a {@link InputSource} for the resource
     */
    I getInputSource() throws Exception;

    /**
     * Create a URL for the resource.
     * 
     * @param forceSkinAction true if a dynamic skin URL should be forced
     * @return a URL to the resource
     * @throws Exception when failing to create a URL for the resource
     */
    String getURL(boolean forceSkinAction) throws Exception;

    /**
     * Create a URL for the resource.
     *
     * @param forceSkinAction true if a dynamic skin URL should be forced
     * @param queryParameters add the following parameters to the URL
     * @return a URL to the resource
     * @throws Exception when failing to create a URL for the resource
     */
    default String getURL(boolean forceSkinAction, Map<String, String> queryParameters) throws Exception {
        return getURL(forceSkinAction);
    }
}
