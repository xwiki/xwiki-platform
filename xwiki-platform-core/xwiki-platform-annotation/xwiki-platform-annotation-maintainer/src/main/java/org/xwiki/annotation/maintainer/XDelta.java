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
package org.xwiki.annotation.maintainer;

/**
 * This interface models a difference between two pieces of content.
 *
 * @version $Id$
 * @since 2.3M1
 */
public interface XDelta
{
    /**
     * @return the position in the original content where the edit takes place
     */
    int getOffset();

    /**
     * @return the length difference between the old content and the new content
     */
    int getSignedDelta();

    /**
     * Returns the original string at the position returned by {@link #getOffset()}. If this string is void and the one
     * returned by {@link #getChanged()} is not, it means that this difference is an addition at position
     * {@link #getOffset()}.
     *
     * @return the original string at the position returned by {@link #getOffset()}
     */
    String getOriginal();

    /**
     * Returns the modified string at the position returned by {@link #getOffset()}. If this string is void and the one
     * returned by {@link #getOriginal()} is not, it means that this difference is a deletion at position
     * {@link #getOffset()}.
     *
     * @return the modified string at the position returned by {@link #getOffset()}
     */
    String getChanged();
}
