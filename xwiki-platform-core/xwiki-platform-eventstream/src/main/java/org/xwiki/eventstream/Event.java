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
package org.xwiki.eventstream;

import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * A recorded event that occurred at some point in the wiki.
 * <p>
 * Events are identified by:
 * <ul>
 * <li>an {@link #getId() identifier} unique for each event</li>
 * <li>a {@link #getGroupId() group identifier} used for all the events caused during the same action</li>
 * <li>the {@link #getDate() exact date and time} when the event occurred</li>
 * <li>an {@link #getImportance() importance} which allows to order events in the same group, or in the same stream</li>
 * </ul>
 * Events contain:
 * <ul>
 * <li>a {@link #getTitle() title}</li>
 * <li>a {@link #getBody() body}</li>
 * <li>and a map of {@link #getParameters() parameters}</li>
 * </ul>
 * Events are categorized into:
 * <ol>
 * <li>the {@link #getApplication() the application} that created them, like blog, user statuses, or the general wiki</li>
 * <li>an {@link #getStream() eventual sub-stream} inside the application, for example a space for wiki events, or a
 * certain group for user statuses</li>
 * <li>the {@link #getType() particular type} of event, like adding a comment, updating an attachment, setting a new
 * user status</li>
 * </ol>
 * Events target:
 * <ol>
 * <li>the {@link #getWiki() wiki},</li>
 * <li>the {@link #getSpace() space},</li>
 * <li>and the {@link #getDocument() document} affected by the event,</li>
 * <li>and an eventual {@link #getRelatedEntity() entity} inside the document, for example an individual attachment for
 * attachment events, or an individual object for comment events.</li>
 * </ol>
 * Events can store some more information about the context in which they occur:
 * <ul>
 * <li>the {@link #getUser() user} that caused the event</li>
 * <li>the {@link #getDocumentVersion() version} of the target document at the time that the event occurred</li>
 * <li>the {@link #getDocumentTitle() display title} of the target document at the time that the event occurred</li>
 * <li>the {@link #getUrl() requested URL} that caused the event</li>
 * </ul>
 * 
 * @version $Id$
 * @since 3.0M2
 */
public interface Event
{
    /** The importance of an event. */
    enum Importance
    {
        /** An event of very low importance in the current event group, usually a side effect of another event. */
        BACKGROUND,

        /** An event of little importance, not relevant for most users. */
        MINOR,

        /** The default priority for events. */
        MEDIUM,

        /** An important event that stands out in the event stream. */
        MAJOR,

        /** A critical event that signals a major change or problem in the wiki. */
        CRITICAL
    }

    /**
     * Each event has a unique ID.
     * 
     * @return the unique ID of the event
     */
    String getId();

    /**
     * @param id the unique ID of the event to set
     * @see #getId()
     */
    void setId(String id);

    /**
     * Multiple events can correspond to the same activity, so events can be grouped under the same GroupID.
     * 
     * @return the event group ID
     */
    String getGroupId();

    /**
     * @param id the event group ID
     * @see #getGroupId()
     */
    void setGroupId(String id);

    /**
     * An event happens at a precise date.
     * 
     * @return the event date
     */
    Date getDate();

    /**
     * @param date the event date
     * @see #getDate()
     */
    void setDate(Date date);

    /**
     * Different events can have a different importance. This allows to determine which events are more or less
     * important in the same event group, or which events are important in the stream. For example, annotation automatic
     * updates are less important in a group of changes triggered by a document update, while a major version is more
     * important than a minor version.
     * 
     * @return the importance of the event
     */
    Importance getImportance();

    /**
     * @param importance the importance of the event
     * @see #getImportance()
     */
    void setImportance(Importance importance);

    /**
     * A name for the event.
     * 
     * @return the title of the event
     */
    String getTitle();

    /**
     * @param title the title of the event
     * @see #getTitle()
     */
    void setTitle(String title);

    /**
     * A larger section of text where the event can store some data.
     * 
     * @return the body of the event
     */
    String getBody();

    /**
     * @param body the body of the event
     * @see #getBody()
     */
    void setBody(String body);

    /**
     * Events can be triggered by different applications, not only the main wiki update events: user statuses, blog
     * pingbacks, new extension added...
     * 
     * @return the application name
     */
    String getApplication();

    /**
     * @param application the application Name
     * @see #getApplication()
     */
    void setApplication(String application);

    /**
     * Events can happen in different contexts; for example, wiki activity events happen in different spaces, user
     * statuses are targeted to different groups.
     * 
     * @return the stream name
     */
    String getStream();

    /**
     * @param stream the stream Name
     * @see #getStream()
     */
    void setStream(String stream);

    /**
     * Events have different types: adding a comment, updating an attachment, importing an application, etc.
     * 
     * @return The type of the event
     */
    String getType();

    /**
     * @param type The type of the event
     * @see #getType()
     */
    void setType(String type);

    /**
     * In a wiki farm, each event happens in one of the wikis.
     * 
     * @return the wiki name in which the event was created
     */
    WikiReference getWiki();

    /**
     * @param wiki the wiki name in which the event was created
     * @see #getWiki()
     */
    void setWiki(WikiReference wiki);

    /**
     * Document-related events target a certain document, and documents belong to a space; this is the space of the
     * target document.
     * 
     * @return the space in which the event was created
     */
    SpaceReference getSpace();

    /**
     * @param space the space in which the event was created
     * @see #getSpace()
     */
    void setSpace(SpaceReference space);

    /**
     * Document-related events target a certain document.
     * 
     * @return the document related to the event
     */
    DocumentReference getDocument();

    /**
     * @param document the document related to the event
     * @see #getDocument()
     */
    void setDocument(DocumentReference document);

    /**
     * For events related to documents, this field records the version of the document at the time when the event
     * occurred.
     * 
     * @return the document version when the event occurred
     */
    String getDocumentVersion();

    /**
     * @param version the document version when the event occurred
     * @see #getDocumentVersion()
     */
    void setDocumentVersion(String version);

    /**
     * Some events may be related to a more specific entity in the target document, like an attachment or an object, or
     * may relate to another document besides the {@link #getDocument() main related document}. The result of this
     * method depends on the actual event type.
     * 
     * @return the eventual entity related to the event, may be {@code null}
     */
    EntityReference getRelatedEntity();

    /**
     * @param entity the eventual entity related to the event
     * @see #getRelatedEntity()
     */
    void setRelatedEntity(EntityReference entity);

    /**
     * Event usually occur as the result of a registered user activity.
     * 
     * @return the user creating the event
     */
    DocumentReference getUser();

    /**
     * @param user the user creating the event
     * @see #getUser()
     */
    void setUser(DocumentReference user);

    /**
     * If an event happens in an URL-accessible location (a document), or if the event itself can be seen at a given
     * URL, this field stores that URL.
     * 
     * @return the URL related to the event
     */
    URL getUrl();

    /**
     * @param url the URL related to the event
     * @see #getUrl()
     */
    void setUrl(URL url);

    /**
     * The document title is usually more important than the document name. Since getting the title of a document
     * version is a difficult task, store it in the event for faster and safer access.
     * 
     * @return the title of the related document when the event occurred.
     */
    String getDocumentTitle();

    /**
     * @param title the title of the related document when the event occurred.
     * @see #getDocumentTitle()
     */
    void setDocumentTitle(String title);
    
    /**
     * @return the named parameters associated with this event as key/value pairs.
     */
    Map<String, String> getParameters();

    /**
     * @param parameters the parameters to associate to the event.
     * @see #getParameters()
     */
    void setParameters(Map<String, String> parameters);
}
