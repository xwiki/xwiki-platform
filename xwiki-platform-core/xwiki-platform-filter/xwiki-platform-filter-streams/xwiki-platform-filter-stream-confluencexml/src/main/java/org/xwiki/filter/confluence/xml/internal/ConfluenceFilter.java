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
package org.xwiki.filter.confluence.xml.internal;

import org.xwiki.filter.event.model.WikiFarmFilter;
import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.model.WikiClassFilter;
import org.xwiki.filter.event.model.WikiClassPropertyFilter;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.event.model.WikiFilter;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.event.model.WikiObjectPropertyFilter;
import org.xwiki.filter.event.model.WikiSpaceFilter;
import org.xwiki.filter.event.user.GroupFilter;
import org.xwiki.filter.event.user.UserFilter;
import org.xwiki.rendering.listener.Listener;

/**
 * All events supported by Confluence module.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public interface ConfluenceFilter extends WikiFarmFilter, WikiFilter, WikiSpaceFilter, WikiDocumentFilter,
    WikiAttachmentFilter, WikiClassFilter, WikiClassPropertyFilter, WikiObjectFilter, WikiObjectPropertyFilter,
    UserFilter, GroupFilter, Listener
{

}
