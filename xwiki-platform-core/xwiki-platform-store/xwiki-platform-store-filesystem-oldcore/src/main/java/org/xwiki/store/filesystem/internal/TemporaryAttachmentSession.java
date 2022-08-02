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
package org.xwiki.store.filesystem.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Contains all information for a user editing session when performing temporary attachment uploads.
 * This class manipulates a map of map of {@link XWikiAttachment} since a user might edit several documents and add
 * several attachments in each of them.
 *
 * @see DefaultTemporaryAttachmentSessionsManager
 * @version $Id$
 * @since 14.3RC1
 */
public class TemporaryAttachmentSession implements HttpSessionBindingListener
{
    private final String sessionId;

    private final Map<DocumentReference, Map<String, XWikiAttachment>> editionsMap;

    /**
     * Default constructor.
     *
     * @param sessionId the identifier of the session.
     */
    public TemporaryAttachmentSession(String sessionId)
    {
        this.sessionId = sessionId;
        this.editionsMap = new ConcurrentHashMap<>();
    }

    /**
     * Add a new attachment for the given document reference.
     *
     * @param documentReference the reference of the document for which the attachment should be added.
     * @param attachment the temporary attachment to add
     */
    public void addAttachment(DocumentReference documentReference, XWikiAttachment attachment)
    {
        Map<String, XWikiAttachment> attachmentMap;
        if (!this.editionsMap.containsKey(documentReference)) {
            attachmentMap = new ConcurrentHashMap<>();
            this.editionsMap.put(documentReference, attachmentMap);
        } else {
            attachmentMap = this.editionsMap.get(documentReference);
        }
        attachmentMap.put(attachment.getFilename(), attachment);
    }

    /**
     * Dispose all attachments created for this session.
     */
    public void dispose()
    {
        this.editionsMap.values().forEach(
            stringXWikiAttachmentMap -> stringXWikiAttachmentMap.values()
                .forEach(attachment -> attachment.getAttachment_content().dispose()));
        this.editionsMap.clear();
    }


    /**
     * Retrieve the set of filenames of uploaded attachments for the given document reference.
     *
     * @param documentReference the reference for which to retrieve the set of filenames.
     * @return the set of filenames of uploaded attachments, or {@link Collections#emptySet()}
     */
    public Set<String> getFilenames(DocumentReference documentReference)
    {
        if (this.editionsMap.containsKey(documentReference)) {
            return this.editionsMap.get(documentReference).keySet();
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Retrieve the attachment added for the given document reference and with the given filename.
     *
     * @param documentReference the reference for which to retrieve the attachment
     * @param filename the name of the attachment to retrieve
     * @return an optional containing the attachment instance or an {@link Optional#empty()} if it cannot be find
     */
    public Optional<XWikiAttachment> getAttachment(DocumentReference documentReference, String filename)
    {
        Optional<XWikiAttachment> result = Optional.empty();

        if (this.editionsMap.containsKey(documentReference)) {
            Map<String, XWikiAttachment> attachmentMap = this.editionsMap.get(documentReference);
            if (attachmentMap.containsKey(filename)) {
                result = Optional.of(attachmentMap.get(filename));
            }
        }
        return result;
    }

    /**
     * Retrieve the set of attachments added for the given document reference.
     *
     * @param documentReference the reference for which to retrieve added attachments
     * @return the set of added attachments or an empty set.
     */
    public Collection<XWikiAttachment> getAttachments(DocumentReference documentReference)
    {
        Collection<XWikiAttachment> result = Collections.emptySet();
        if (this.editionsMap.containsKey(documentReference)) {
            result = new HashSet<>(this.editionsMap.get(documentReference).values());
        }
        return result;
    }

    /**
     * Remove the attachment added to the given reference and identified by the given name and dispose its content.
     *
     * @param documentReference the reference for which the attachment have been added
     * @param filename the name of the attachment
     * @return {@code true} if the attachment has been found, removed and disposed, {@code false} if it cannot be found
     */
    public boolean removeAttachment(DocumentReference documentReference, String filename)
    {
        boolean result = false;
        if (this.editionsMap.containsKey(documentReference)) {
            Map<String, XWikiAttachment> attachmentMap = this.editionsMap.get(documentReference);
            if (attachmentMap.containsKey(filename)) {
                XWikiAttachment attachment = attachmentMap.remove(filename);
                attachment.getAttachment_content().dispose();
                result = true;
            }
        }
        return result;
    }

    /**
     * Remove and dispose all attachments added for the given reference.
     *
     * @param documentReference the reference for which to retrieve all the attachments.
     * @return {@code true} if there was attachments to remove, {@code false} otherwise
     */
    public boolean removeAttachments(DocumentReference documentReference)
    {
        boolean result = false;
        if (this.editionsMap.containsKey(documentReference)) {
            Map<String, XWikiAttachment> attachmentMap = this.editionsMap.get(documentReference);
            if (!attachmentMap.isEmpty()) {
                result = true;
                Collection<XWikiAttachment> attachments = new ArrayList<>(attachmentMap.values());
                attachmentMap.clear();
                attachments.forEach(attachment -> attachment.getAttachment_content().dispose());
            }
        }
        return result;
    }

    /**
     * @return the session identifier.
     */
    public String getSessionId()
    {
        return sessionId;
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

        TemporaryAttachmentSession that = (TemporaryAttachmentSession) o;

        return new EqualsBuilder().append(sessionId, that.sessionId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 63).append(sessionId).toHashCode();
    }

    /**
     * Retrieve the editions map, mainly for testing purpose.
     *
     * @return the map of maps containing all attachment information
     */
    protected Map<DocumentReference, Map<String, XWikiAttachment>> getEditionsMap()
    {
        return editionsMap;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event)
    {
        // Nothing to do.
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event)
    {
        this.dispose();
    }
}
