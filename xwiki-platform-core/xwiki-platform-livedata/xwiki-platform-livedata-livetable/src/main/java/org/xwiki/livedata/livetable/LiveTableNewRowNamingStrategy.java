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
package org.xwiki.livedata.livetable;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;

/**
 * Strategy to generate the document reference of a new livetable entry.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@Role
@Unstable
public interface LiveTableNewRowNamingStrategy
{
    /**
     * Generates a document reference for a new livetable entry.
     *
     * @param parameters the livedata source parameters
     * @return the generated document reference
     * @throws LiveDataException if the reference cannot be generated
     * @throws XWikiException if there is a wiki-level error
     */
    DocumentReference generate(Map<String, Object> parameters) throws LiveDataException, XWikiException;

    /**
     * Checks if the current user is allowed to create a new entry based on the provided source parameters.
     *
     * @param parameters the live data source parameters
     * @return whether the current user is allowed to create a new entry with this strategy
     */
    boolean isCreationAllowed(Map<String, Object> parameters);
}
