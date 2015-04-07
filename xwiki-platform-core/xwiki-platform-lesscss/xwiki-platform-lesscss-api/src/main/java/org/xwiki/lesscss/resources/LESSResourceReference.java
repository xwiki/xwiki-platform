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
package org.xwiki.lesscss.resources;

import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.stability.Unstable;

/**
 * A reference to a LESS resource.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Unstable
public interface LESSResourceReference
{
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    /**
     * @param skin skin from which the content should be get
     * @return the content holding by the resources pointed by the reference
     * @throws LESSCompilerException if problem occurs
     * 
     * @since 7.0RC1
     */
    String getContent(String skin) throws LESSCompilerException;

    /**
     * @return a serialized form of the resource
     * 
     * @since 7.0RC1
     */
    String serialize();
}
