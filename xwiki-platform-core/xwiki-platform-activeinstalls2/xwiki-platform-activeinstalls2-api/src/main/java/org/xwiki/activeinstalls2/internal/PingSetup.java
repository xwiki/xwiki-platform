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
package org.xwiki.activeinstalls2.internal;

import org.xwiki.component.annotation.Role;

/**
 * Prepare the ElasticSearch server to be ready to accept pings (e.g. create the ingest pipelines, create the
 * index and mapping, etc). Note that this is not done inside {@link PingSender#sendPing()} since it's only needed
 * to be done once, and it requires appropriate permissions on the ES instance side.
 *
 * @version $Id$
 * @since 14.6
 */
@Role
public interface PingSetup
{
    /**
     * @throws Exception in case an error happened during the preparation
     */
    void setup() throws Exception;
}
