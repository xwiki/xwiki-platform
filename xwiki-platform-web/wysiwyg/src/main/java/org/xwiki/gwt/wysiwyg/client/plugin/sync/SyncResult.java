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
package org.xwiki.gwt.wysiwyg.client.plugin.sync;

import org.xwiki.gwt.wysiwyg.client.diff.Revision;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The synchronization result.
 * 
 * @version $Id$
 */
public class SyncResult implements IsSerializable
{
    /**
     * The revision that needs to be applied on the client in order to update its content to the latest version.
     */
    protected Revision revision;

    /**
     * The status.
     */
    protected boolean status;

    /**
     * The new version.
     */
    protected int version;

    /**
     * @return {@link #revision}
     */
    public Revision getRevision()
    {
        return revision;
    }

    /**
     * Sets the {@link #revision}.
     * 
     * @param revision a revision
     */
    public void setRevision(Revision revision)
    {
        this.revision = revision;
    }

    /**
     * @return {@link #status}
     */
    public boolean isStatus()
    {
        return status;
    }

    /**
     * Sets the {@link #status}.
     * 
     * @param status the status
     */
    public void setStatus(boolean status)
    {
        this.status = status;
    }

    /**
     * @return {@link #version}
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Sets the {@link #version}.
     * 
     * @param version the version number
     */
    public void setVersion(int version)
    {
        this.version = version;
    }
}
