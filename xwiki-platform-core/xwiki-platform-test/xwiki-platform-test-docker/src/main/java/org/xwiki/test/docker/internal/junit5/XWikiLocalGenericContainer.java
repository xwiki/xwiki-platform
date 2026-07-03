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
package org.xwiki.test.docker.internal.junit5;

import java.util.concurrent.Future;

import org.testcontainers.containers.GenericContainer;

/**
 * To be used to construct a container built from a local image (that is not available on Dockerhub). Such containers
 * won't have their images pulled when starting them.
 *
 * @param <T> the type of container
 * @version $Id$
 * @since 11.10
 */
public class XWikiLocalGenericContainer<T extends XWikiGenericContainer<T>> extends XWikiGenericContainer<T>
{
    /**
     * @see GenericContainer#GenericContainer(String)
     *
     * @param dockerImageName the name of the docker image to start a container from
     */
    public XWikiLocalGenericContainer(String dockerImageName)
    {
        super(dockerImageName);
    }

    /**
     * @see GenericContainer#GenericContainer(Future)
     *
     * @param image the image to start a container from
     */
    public XWikiLocalGenericContainer(Future<String> image)
    {
        super(image);
    }
}
