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
package org.xwiki.platform.wiki.creationjob;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.stability.Unstable;

/**
 * Exception concerning the Wiki Creator module.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Unstable
public class WikiCreationException extends Exception
{
    private static final long serialVersionUID = 2501408208237931530L;

    /**
     * Construct a new exception.
     *
     * @param message message about the error
     */
    public WikiCreationException(String message)
    {
        super(message);
    }

    /**
     * Construct a new exception.
     *
     * @param message message about the error
     * @param cause cause of the error
     */
    public WikiCreationException(String message, Throwable cause)
    {
        super(message, cause);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof WikiCreationException) {
            WikiCreationException other = (WikiCreationException) o;
            return new EqualsBuilder().append(getMessage(), other.getMessage()).append(getCause(), other.getCause())
                    .isEquals();
        }
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return getMessage().hashCode() + (getCause() != null ? getCause().hashCode() : 0);
    }
}
