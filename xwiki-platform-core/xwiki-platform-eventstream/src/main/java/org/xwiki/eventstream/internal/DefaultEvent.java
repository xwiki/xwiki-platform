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
package org.xwiki.eventstream.internal;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * A recorded event that occurred at some point in the wiki.
 * 
 * @version $Id$
 */
public class DefaultEvent implements Event
{
    /** The format of the {@link #toString()} result. */
    private static final MessageFormat STRING_FORMAT =
        new MessageFormat("{0} at {1,time,yyyy-MM-dd HH:mm:ss} by {2} on {3}");

    /** @see #getId() */
    private String id;

    /** @see #getGroupId() */
    private String groupId;

    /** @see #getType() */
    private String type;

    /** @see #getDate() */
    private Date date;

    /** @see #getImportance() */
    private Importance importance = Importance.MEDIUM;

    /** @see #getApplication() */
    private String application;

    /** @see #getStream() */
    private String stream;

    /** @see #getWiki() */
    private WikiReference wiki;

    /** @see #getSpace() */
    private SpaceReference space;

    /** @see #getDocument() */
    private DocumentReference document;

    /** @see #getRelatedEntity() */
    private EntityReference relatedEntity;

    /** @see #getDocumentVersion() */
    private String documentVersion;

    /** @see #getDocumentTitle() */
    private String documentTitle;

    /** @see #getUser() */
    private DocumentReference user;

    /** @see #getUrl() */
    private URL url;

    /** @see #getTitle() */
    private String title;

    /** @see #getBody() */
    private String body;

    /** @see #getParameters() */
    private Map<String, String> parameters;
    
    /**
     * {@inheritDoc}
     * 
     * @see Event#getId()
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setId(String)
     * @see #getId()
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getGroupId()
     */
    public String getGroupId()
    {
        return this.groupId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setGroupId(String)
     * @see #getGroupId()
     */
    public void setGroupId(String id)
    {
        this.groupId = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getType()
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setType(String)
     * @see #getType()
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getDate()
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setDate(Date)
     * @see #getDate()
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getImportance()
     */
    public Importance getImportance()
    {
        return this.importance;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setImportance(org.xwiki.eventstream.Event.Importance)
     * @see #getImportance()
     */
    public void setImportance(Importance importance)
    {
        if (importance == null) {
            this.importance = Importance.MEDIUM;
        } else {
            this.importance = importance;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getApplication()
     */
    public String getApplication()
    {
        return this.application;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setApplication(String)
     * @see #getApplication()
     */
    public void setApplication(String application)
    {
        this.application = application;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getStream()
     */
    public String getStream()
    {
        return this.stream;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setStream(String)
     * @see #getStream()
     */
    public void setStream(String stream)
    {
        this.stream = stream;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getWiki()
     */
    public WikiReference getWiki()
    {
        WikiReference result = null;
        if (this.wiki != null) {
            result = this.wiki;
        } else if (this.space != null) {
            result = (WikiReference) this.space.getRoot();
        } else if (this.document != null) {
            result = this.document.getWikiReference();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setWiki(WikiReference)
     * @see #getWiki()
     */
    public void setWiki(WikiReference wiki)
    {
        this.wiki = wiki;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getSpace()
     */
    public SpaceReference getSpace()
    {
        SpaceReference result = null;
        if (this.space != null) {
            result = this.space;
        } else if (this.document != null) {
            result = this.document.getLastSpaceReference();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setSpace(SpaceReference)
     * @see #getSpace()
     */
    public void setSpace(SpaceReference space)
    {
        this.space = space;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getDocument()
     */
    public DocumentReference getDocument()
    {
        return this.document;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setDocument(DocumentReference)
     * @see #getDocument()
     */
    public void setDocument(DocumentReference document)
    {
        this.document = document;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getDocumentVersion()
     */
    public String getDocumentVersion()
    {
        return this.documentVersion;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setDocumentVersion(String)
     * @see #getDocumentVersion()
     */
    public void setDocumentVersion(String version)
    {
        this.documentVersion = version;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getRelatedEntity()
     */
    public EntityReference getRelatedEntity()
    {
        return this.relatedEntity;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setRelatedEntity(EntityReference)
     * @see #getRelatedEntity()
     */
    public void setRelatedEntity(EntityReference entity)
    {
        this.relatedEntity = entity;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getUser()
     */
    public DocumentReference getUser()
    {
        return this.user;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setUser(DocumentReference)
     * @see #getUser()
     */
    public void setUser(DocumentReference user)
    {
        this.user = user;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getUrl()
     */
    public URL getUrl()
    {
        return this.url;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setUrl(URL)
     * @see #getUrl()
     */
    public void setUrl(URL url)
    {
        this.url = url;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getTitle()
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setTitle(String)
     * @see #getTitle()
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getBody()
     */
    public String getBody()
    {
        return this.body;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setBody(String)
     * @see #getBody()
     */
    public void setBody(String body)
    {
        this.body = body;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getDocumentTitle()
     */
    public String getDocumentTitle()
    {
        return this.documentTitle;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setDocumentTitle(String)
     * @see #getDocumentTitle()
     */
    public void setDocumentTitle(String title)
    {
        this.documentTitle = title;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#getParameters()
     */
    public Map<String, String> getParameters()
    {
        return this.parameters == null ? Collections.<String, String> emptyMap()
            : Collections.unmodifiableMap(this.parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#setParameters(Map)
     * @see #getParameters()
     */
    public void setParameters(Map<String, String> parameters)
    {
        this.parameters = new HashMap<String, String>(parameters);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return STRING_FORMAT.format(new Object[] {getType(), getDate(), getUser(), getDocument()});
    }
}
