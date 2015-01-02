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
package org.xwiki.mail.internal.script;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.script.ScriptServicePermissionChecker;

/**
 * No check is performed and mails are allowed to be sent. This is useful when setting up XWiki in a secure
 * environment where we want to allow all users to be able to send emails through the Script Service.
 *
 * @version $Id$
 * @since 6.4M2
 */
@Component
@Named("alwaysallow")
@Singleton
public class AlwaysAllowScriptServicePermissionChecker implements ScriptServicePermissionChecker
{
    @Override
    public void check() throws MessagingException
    {
        // Nothing to do, we always authorize!
    }
}
