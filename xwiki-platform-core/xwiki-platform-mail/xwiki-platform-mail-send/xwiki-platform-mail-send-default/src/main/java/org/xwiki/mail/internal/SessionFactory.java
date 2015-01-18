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
package org.xwiki.mail.internal;

import java.util.Map;

import javax.mail.Session;

import org.xwiki.component.annotation.Role;

/**
 * Create a Java Mail {@link javax.mail.Session} object, taking its properties from some configuration location but
 * allowing to pass additional properties (for example to reuse an existing Batch Id).
 *
 * @version $Id$
 * @since 6.4
 */
@Role
public interface SessionFactory
{
    /**
     * @param additionProperties some additional properties overriding the ones from the configuration
     * @return the Session object to use when performing JavaMail operations
     */
    Session create(Map<String, String> additionProperties);
}
