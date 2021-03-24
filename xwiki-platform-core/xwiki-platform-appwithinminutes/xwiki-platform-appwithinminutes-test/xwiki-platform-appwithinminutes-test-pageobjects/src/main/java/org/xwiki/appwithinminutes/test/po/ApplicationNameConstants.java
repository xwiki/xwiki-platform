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
package org.xwiki.appwithinminutes.test.po;

/**
 * Stores the constant strings related to pages involving application names.
 *
 * @version $Id$
 * @since 13.2
 * @since 12.10.6
 */
public interface ApplicationNameConstants
{
    /**
     * The error message displayed when we try to create an application with an empty name.
     */
    String EMPTY_APP_NAME_ERROR_MESSAGE = "Please enter the application name.";
    /**
     * The warning message displayed when we input the name of an existing application.
     */
    String APP_NAME_USED_WARNING_MESSAGE = "This application already exists.";
}
