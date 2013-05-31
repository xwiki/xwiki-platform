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
package com.xpn.xwiki.doc;

public class AttachmentDiff
{
    private String fileName;

    private String origVersion;

    private String newVersion;

    public AttachmentDiff(String fileName, String origVersion, String newVersion)
    {
        setFileName(fileName);
        setOrigVersion(origVersion);
        setNewVersion(newVersion);
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getOrigVersion()
    {
        return this.origVersion;
    }

    public void setOrigVersion(String origVersion)
    {
        this.origVersion = origVersion;
    }

    public String getNewVersion()
    {
        return this.newVersion;
    }

    public void setNewVersion(String newVersion)
    {
        this.newVersion = newVersion;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(this.fileName);
        buf.append(": ");
        if (this.origVersion != null) {
            buf.append(this.origVersion);
        } else {
            buf.append("()");
        }
        buf.append(" \u21E8 ");
        if (this.newVersion != null) {
            buf.append(this.newVersion);
        } else {
            buf.append("()");
        }
        return buf.toString();
    }
}
