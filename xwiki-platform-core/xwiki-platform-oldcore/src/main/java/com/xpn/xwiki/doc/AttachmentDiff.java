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

import org.xwiki.diff.Delta;

public class AttachmentDiff
{
    private String fileName;

    private Delta.Type type;

    private XWikiAttachment origAttachment;

    private XWikiAttachment newAttachment;

    @Deprecated
    private String origVersion;

    @Deprecated
    private String newVersion;

    @Deprecated
    public AttachmentDiff(String fileName, String origVersion, String newVersion)
    {
        this(fileName, newVersion == null ? Delta.Type.DELETE : (origVersion == null ? Delta.Type.INSERT
            : Delta.Type.CHANGE), null, null);

        setOrigVersion(origVersion);
        setNewVersion(newVersion);
    }

    /**
     * @since 5.4M1
     */
    public AttachmentDiff(String fileName, Delta.Type type, XWikiAttachment origAttachment,
        XWikiAttachment newAttachment)
    {
        this.fileName = fileName;
        this.type = type;
        this.origAttachment = origAttachment;
        this.newAttachment = newAttachment;

        this.origVersion = origAttachment != null ? origAttachment.getVersion() : null;
        this.newVersion = newAttachment != null ? newAttachment.getVersion() : null;
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * @since 5.4M1
     */
    public Delta.Type getType()
    {
        return this.type;
    }

    /**
     * @since 5.4M1
     */
    public XWikiAttachment getOrigAttachment()
    {
        return this.origAttachment;
    }

    /**
     * @since 5.4M1
     */
    public XWikiAttachment getNewAttachment()
    {
        return this.newAttachment;
    }

    @Deprecated
    public String getOrigVersion()
    {
        return this.origVersion;
    }

    @Deprecated
    public void setOrigVersion(String origVersion)
    {
        this.origVersion = origVersion;
    }

    @Deprecated
    public String getNewVersion()
    {
        return this.newVersion;
    }

    @Deprecated
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
