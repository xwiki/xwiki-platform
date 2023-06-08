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
package org.xwiki.extension.security.notifications;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(NewSecurityIssueEventDescriptor.TYPE)
public class NewSecurityIssueEventDescriptor implements RecordableEventDescriptor
{
    /**
     * Event type identifier.
     */
    public static final String TYPE = "NewSecurityIssueEvent";

    @Override
    public String getEventType()
    {
        return NewSecurityIssueEvent.class.getCanonicalName();
    }

    @Override
    public String getApplicationName()
    {
        return "extension.security.applicationName";
    }

    @Override
    public String getDescription()
    {
        return "extension.security.events.newSecurityIssue.description";
    }

    @Override
    public String getApplicationIcon()
    {
        return "warning";
    }
}
