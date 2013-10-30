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
package org.xwiki.wikistream.instance.internal.output;

import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 * @since 5.3M2
 */
public class UserOutputProperties extends InstanceOutputProperties
{
    private String saveComment = "Import";

    private DocumentReference author = null;

    private boolean authorSet = false;

    private boolean preserveVersion = false;

    private String groupPrefix = "";

    private String groupSuffix = "";

    public String getSaveComment()
    {
        return this.saveComment;
    }

    public void setSaveComment(String saveComment)
    {
        this.saveComment = saveComment;
    }

    public DocumentReference getAuthor()
    {
        return this.author;
    }

    public void setAuthor(DocumentReference author)
    {
        this.author = author;
        this.authorSet = true;
    }

    public boolean isAuthorSet()
    {
        return this.authorSet;
    }

    public boolean isPreserveVersion()
    {
        return this.preserveVersion;
    }

    public void setPreserveVersion(boolean preserveVersion)
    {
        this.preserveVersion = preserveVersion;
    }

    public String getGroupPrefix()
    {
        return this.groupPrefix;
    }

    public void setGroupPrefix(String groupPrefix)
    {
        this.groupPrefix = groupPrefix;
    }

    public String getGroupSuffix()
    {
        return this.groupSuffix;
    }

    public void setGroupSuffix(String groupSuffix)
    {
        this.groupSuffix = groupSuffix;
    }
}
