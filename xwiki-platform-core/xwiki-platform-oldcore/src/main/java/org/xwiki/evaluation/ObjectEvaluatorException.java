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
package org.xwiki.evaluation;

import org.xwiki.stability.Unstable;

/**
 * Exception raised during evaluation of XObjects properties.
 *
 * @version $Id$
 * @since 14.10.21
 * @since 15.5.5
 * @since 15.10.2
 */
@Unstable
public class ObjectEvaluatorException extends Exception
{
    /**
     * Creates an instance of ObjectEvaluatorException with a message and a cause.
     *
     * @param message the message detailing the issue
     * @param cause the cause of the exception
     */
    public ObjectEvaluatorException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Creates an instance of ObjectEvaluatorException with a message.
     *
     * @param message the message detailing the issue
     */
    public ObjectEvaluatorException(String message)
    {
        this(message, null);
    }
}
