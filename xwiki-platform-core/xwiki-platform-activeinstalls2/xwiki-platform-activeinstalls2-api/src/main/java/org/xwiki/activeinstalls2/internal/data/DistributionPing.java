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
package org.xwiki.activeinstalls2.internal.data;

/**
 * Represents distribution-related Ping data.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class DistributionPing
{
    private String instanceId;

    private ExtensionPing extension;

    /**
     * @return the XWiki unique instance id
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @param instanceId see {@link #getInstanceId()}
     */
    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    /**
     * @return the current XWiki distribution (as an Extension)
     */
    public ExtensionPing getExtension()
    {
        return this.extension;
    }

    /**
     * @param extension see {@link #getExtension()}
     */
    public void setExtension(ExtensionPing extension)
    {
        this.extension = extension;
    }
}
