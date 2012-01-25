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
package org.xwiki.extension.job;

import java.io.Serializable;
import java.util.Collection;

/**
 * A {@link Job} request.
 * 
 * @version $Id$
 */
public interface Request extends Serializable
{
    /**
     * @see #isRemote()
     */
    String PROPERTY_REMOTE = "remote";

    /**
     * @return indicate if the job has been triggered by a remote event
     */
    boolean isRemote();

    /**
     * @param key the name of the property
     * @param <T> the type of the value
     * @return the value of the property
     */
    <T> T getProperty(String key);

    /**
     * @param key the name of the property
     * @param def the default value of the property
     * @param <T> the type of the value
     * @return the value of the property
     */
    <T> T getProperty(String key, T def);

    /**
     * @return the names of all the properties
     */
    Collection<String> getPropertyNames();
}
