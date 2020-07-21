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
package org.xwiki.mentions.internal.async;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Object storing the minimal data required to analyse the mentions asynchronously.
 *
 * @version $Id$
 * @since 12.6RC1
 */
public class MentionsData
{
    /**
     * Static object used to identify if elements has been persisted 
     * in the current JVM.
     */
    public static final MentionsData STOP = new MentionsData(true);

    private String documentReference;

    private String authorReference;

    private String version;

    private String wikiId;

    private final boolean stop;

    private long attempts;

    /**
     * Default constructor.
     */
    public MentionsData()
    {
        this.stop = false;
    }

    /**
     * Private constructor for the {@link MentionsData#STOP} static object.
     * @param stop stop flag.
     */
    private MentionsData(boolean stop)
    {
        this.stop = stop;
    }

    /**
     *
     * @return the document reference.
     */
    public String getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     * Set the document reference.
     *
     * @param documentReference the document reference
     * @return the current object
     */
    public MentionsData setDocumentReference(String documentReference)
    {
        this.documentReference = documentReference;
        return this;
    }

    /**
     *
     * @return the document version.
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * Set the document version.
     *
     * @param version the document version
     * @return the current object
     */
    public MentionsData setVersion(String version)
    {
        this.version = version;
        return this;
    }

    /**
     *
     * @return the author reference
     */
    public String getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * Set the author reference.
     *
     * @param authorReference the author reference
     * @return the current object
     */
    public MentionsData setAuthorReference(String authorReference)
    {
        this.authorReference = authorReference;
        return this;
    }

    /**
     *
     * @return the wiki id
     */
    public String getWikiId()
    {
        return this.wikiId;
    }

    /**
     *
     * @return true if the current object is {@link MentionsData#STOP}.  
     */
    public boolean isStop()
    {
        return this == STOP;
    }

    /**
     * Check if the stop flag is relevant. 
     * The stop flag is deprecated when the server restarts.
     *
     * @return true if the stop flag is still relevant.
     */
    public boolean isDeprecated()
    {
        return this.stop && this != STOP;
    }

    /**
     * Set the wiki id.
     *
     * @param wikiId the wiki id
     * @return the current object
     */
    public MentionsData setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
        return this;
    }

    /**
     * Increase the attempts counter.
     */
    public void increaseAttempts()
    {
        this.attempts++;
    }

    /**
     *
     * @return true if the number of attempts for the current data has reached 10. 
     */
    public boolean isFailed()
    {
        return this.attempts > 10;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MentionsData that = (MentionsData) o;

        return new EqualsBuilder()
                   .append(this.documentReference, that.documentReference)
                   .append(this.authorReference, that.authorReference)
                   .append(this.version, that.version)
                   .append(this.wikiId, that.wikiId)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.documentReference)
                   .append(this.authorReference)
                   .append(this.version)
                   .append(this.wikiId)
                   .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("documentReference", this.getDocumentReference())
                   .append("version", this.getVersion())
                   .append("authorReference", this.getAuthorReference())
                   .append("wikiId", this.getWikiId())
                   .build();
    }
}
