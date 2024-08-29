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
package org.xwiki.security.authentication;

/**
 * This class aims at holding any exception that might occur during a reset password process.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public class ResetPasswordException extends Exception
{
    /**
     * Default constructor.
     *
     * @param message end-user message about the problem.
     */
    public ResetPasswordException(String message)
    {
        super(message);
    }

    /**
     * Constructor in case of parent exception.
     *
     * @param message end-user message about the problem.
     * @param throwable parent exception.
     */
    public ResetPasswordException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
