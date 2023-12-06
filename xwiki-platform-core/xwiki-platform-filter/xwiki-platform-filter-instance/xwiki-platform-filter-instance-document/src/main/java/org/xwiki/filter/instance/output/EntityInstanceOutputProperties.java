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
package org.xwiki.filter.instance.output;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.stability.Unstable;

/**
 * Entity Instance output filter stream properties (shared between DocumentInstanceOutputProperties and
 * UserInstanceOutputProperties).
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@Unstable
public class EntityInstanceOutputProperties extends InstanceOutputProperties
{
    /**
     * @see #getSaveComment()
     */
    private String saveComment = "Import";

    /**
     * @see #getAuthor()
     */
    private DocumentReference author;

    /**
     * @see #isAuthorSet()
     */
    private boolean authorSet;

    /**
     * @see #isVersionPreserved()
     */
    private boolean versionPreserved = true;


    /**
     * @return the comment to use when saving users
     */
    @PropertyName("Save comment")
    @PropertyDescription("The comment to use when saving a user")
    public String getSaveComment()
    {
        return this.saveComment;
    }

    /**
     * @param saveComment the version comment to use when saving users
     */
    public void setSaveComment(String saveComment)
    {
        this.saveComment = saveComment;
    }

    /**
     * @return the author to use when saving users
     */
    @PropertyName("Save author")
    @PropertyDescription("The author to use when saving a user")
    public DocumentReference getAuthor()
    {
        return this.author;
    }

    /**
     * @param author the author to use when saving users
     */
    public void setAuthor(DocumentReference author)
    {
        this.author = author;
        this.authorSet = true;
    }

    /**
     * @return true if the author have been explicitly set
     */
    public boolean isAuthorSet()
    {
        return this.authorSet;
    }

    /**
     * @return Indicate if the version related information coming from the events should be kept
     */
    @PropertyName("Preserve version")
    @PropertyDescription("Indicate if the versions related informations coming from the events should be kept")
    public boolean isVersionPreserved()
    {
        return this.versionPreserved;
    }

    /**
     * @param versionPreserved Indicate if the version related information coming from the events should be kept
     */
    public void setVersionPreserved(boolean versionPreserved)
    {
        this.versionPreserved = versionPreserved;
    }
}
