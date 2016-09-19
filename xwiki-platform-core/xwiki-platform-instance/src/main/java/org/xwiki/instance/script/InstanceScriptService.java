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
package org.xwiki.instance.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Provides instance-specific scripting APIs.
 * 
 * @version $Id$
 * @since 8.3RC1
 */
@Component
@Named("instance")
@Singleton
@Unstable
public class InstanceScriptService implements ScriptService
{
    @Inject
    private InstanceIdManager instanceIdManager;

    /**
     * @return the XWiki instance id or null if not set, see {@link InstanceIdManager#getInstanceId()}
     */
    public InstanceId getInstanceId()
    {
        return instanceIdManager.getInstanceId();
    }
}
