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
import java.util.Set;

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

    /** @see #getTarget() */
    private Set<String> target;

    private boolean hidden;
    
    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public String getGroupId()
    {
        return this.groupId;
    }

    @Override
    public void setGroupId(String id)
    {
        this.groupId = id;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    @Override
    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public Date getDate()
    {
        return this.date;
    }

    @Override
    public void setDate(Date date)
    {
        this.date = date;
    }

    @Override
    public Importance getImportance()
    {
        return this.importance;
    }

    @Override
    public void setImportance(Importance importance)
    {
        if (importance == null) {
            this.importance = Importance.MEDIUM;
        } else {
            this.importance = importance;
        }
    }

    @Override
    public String getApplication()
    {
        return this.application;
    }

    @Override
    public void setApplication(String application)
    {
        this.application = application;
    }

    @Override
    public String getStream()
    {
        return this.stream;
    }

    @Override
    public void setStream(String stream)
    {
        this.stream = stream;
    }

    @Override
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

    @Override
    public void setWiki(WikiReference wiki)
    {
        this.wiki = wiki;
    }

    @Override
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

    @Override
    public void setSpace(SpaceReference space)
    {
        this.space = space;
    }

    @Override
    public DocumentReference getDocument()
    {
        return this.document;
    }

    @Override
    public void setDocument(DocumentReference document)
    {
        this.document = document;
    }

    @Override
    public String getDocumentVersion()
    {
        return this.documentVersion;
    }

    @Override
    public void setDocumentVersion(String version)
    {
        this.documentVersion = version;
    }

    @Override
    public EntityReference getRelatedEntity()
    {
        return this.relatedEntity;
    }

    @Override
    public void setRelatedEntity(EntityReference entity)
    {
        this.relatedEntity = entity;
    }

    @Override
    public DocumentReference getUser()
    {
        return this.user;
    }

    @Override
    public void setUser(DocumentReference user)
    {
        this.user = user;
    }

    @Override
    public URL getUrl()
    {
        return this.url;
    }

    @Override
    public void setUrl(URL url)
    {
        this.url = url;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    @Override
    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public String getBody()
    {
        return this.body;
    }

    @Override
    public void setBody(String body)
    {
        this.body = body;
    }

    @Override
    public String getDocumentTitle()
    {
        return this.documentTitle;
    }

    @Override
    public void setDocumentTitle(String title)
    {
        this.documentTitle = title;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return this.parameters == null ? Collections.<String, String> emptyMap()
            : Collections.unmodifiableMap(this.parameters);
    }

    @Override
    public void setParameters(Map<String, String> parameters)
    {
        if (parameters != null) {
            this.parameters = new HashMap<String, String>(parameters);
        } else {
            // Fallback to empty parameters map.
            this.parameters = new HashMap<String, String>();
        }
    }

    @Override
    public void setTarget(Set<String> target)
    {
        this.target = target;
    }

    @Override
    public Set<String> getTarget()
    {
        return this.target != null ? Collections.unmodifiableSet(this.target)
                : Collections.emptySet();
    }

    @Override
    public String toString()
    {
        return STRING_FORMAT.format(new Object[] {getType(), getDate(), getUser(), getDocument()});
    }

    @Override
    public boolean getHidden()
    {
        return this.hidden;
    }

    @Override
    public void setHidden(boolean isHidden)
    {
        this.hidden = isHidden;
    }
}
