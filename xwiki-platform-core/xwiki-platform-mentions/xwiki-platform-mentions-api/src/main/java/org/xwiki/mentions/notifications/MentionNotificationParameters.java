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
package org.xwiki.mentions.notifications;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Contains the mentions for a change (creation or modification) on a given entity. The object contains two maps of
 * mentions, storing respectively the list of all mentions of the entity, and the list of new mentions introduced by the
 * change. The keys of the maps are the types of the mentioned users stored in the values. The value of the maps
 * contains the list of mentions of a given type. In addition, the object contains the reference of the author of the
 * change, the location of the entity (e.g., in a document or a comment), the reference of the changed entity, and the
 * version of the entity after the change.
 *
 * @version $Id$
 * @since 12.6
 */
public class MentionNotificationParameters implements Serializable
{
    private static final long serialVersionUID = -8847694983380889549L;

    private final Map<String, Set<MentionNotificationParameter>> newMentions;

    private final Map<String, Set<MentionNotificationParameter>> unmodifiableNewMentions;

    private final Map<String, Set<MentionNotificationParameter>> mentions;

    private final Map<String, Set<MentionNotificationParameter>> unmodifiableMentions;

    private final String authorReference;

    private final EntityReference entityReference;

    private final MentionLocation location;

    private final String version;

    /**
     * @param authorReference the reference of the author of the change that produced the mentions
     * @param entityReference the entity holding the mentions (a page content, a comment...)
     * @param location the pre-calculated location of the entity
     * @param version version of the document where the mention occurred
     */
    public MentionNotificationParameters(String authorReference,
        EntityReference entityReference, MentionLocation location, String version)
    {
        this.authorReference = authorReference;
        this.entityReference = entityReference;
        this.location = location;
        this.version = version;
        this.newMentions = new HashMap<>();
        this.mentions = new HashMap<>();
        this.unmodifiableNewMentions = Collections.unmodifiableMap(this.newMentions);
        this.unmodifiableMentions = Collections.unmodifiableMap(this.mentions);
    }

    /**
     * @return the reference of the author of the mention
     */
    public String getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * @return the document in which the mention has been done
     */
    public EntityReference getEntityReference()
    {
        return this.entityReference;
    }

    /**
     * @return the location of the mention
     */
    public MentionLocation getLocation()
    {
        return this.location;
    }

    /**
     * Add a mention to the map of mentions.
     *
     * @param type the type of the mentioned actor
     * @param mentionedActorReference the mention notification parameters
     * @return the current object
     */
    public MentionNotificationParameters addMention(String type,
        MentionNotificationParameter mentionedActorReference)
    {
        addToMap(type, mentionedActorReference, this.mentions);
        return this;
    }

    /**
     * Add a mention to the map of new mentions.
     *
     * @param type the type of the mentioned actor
     * @param mentionedActorReference the mention notification parameter
     * @return the current object
     */
    public MentionNotificationParameters addNewMention(String type,
        MentionNotificationParameter mentionedActorReference)
    {
        addToMap(type, mentionedActorReference, this.newMentions);
        return this;
    }

    /**
     * Returns an unmodifable map of the new mentions. The type of the mentioned actors are used as keys, and the values
     * are {@link MentionNotificationParameter}, identifying a unique mention in a page by its actor reference and its
     * anchor.
     *
     * @return the map of new mentions
     */
    public Map<String, Set<MentionNotificationParameter>> getNewMentions()
    {
        return this.unmodifiableNewMentions;
    }

    /**
     * Returns an unmodifiable map of all the mentions, including the new ones. The type of the mentioned actors are
     * used as keys, and the values are {@link MentionNotificationParameter}, identifying a unique mention in a page by
     * its actor reference and its anchor.
     *
     * @return the map of all the mentions
     */
    public Map<String, Set<MentionNotificationParameter>> getMentions()
    {
        return this.unmodifiableMentions;
    }

    /**
     * @return the version of the document where the mentions occurred
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * Add new mention to a map.
     *
     * @param type the type of the mentioned actor
     * @param mentionedActorReference the mention notification parameter
     * @param mentionsMap the map of mentions to update
     */
    private void addToMap(String type, MentionNotificationParameter mentionedActorReference,
        Map<String, Set<MentionNotificationParameter>> mentionsMap)
    {
        if (!mentionsMap.containsKey(type)) {
            mentionsMap.put(type, new HashSet<>());
        }

        mentionsMap.get(type).add(mentionedActorReference);
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

        MentionNotificationParameters that = (MentionNotificationParameters) o;

        return new EqualsBuilder()
            .append(this.newMentions, that.newMentions)
            .append(this.mentions, that.mentions)
            .append(this.authorReference, that.authorReference)
            .append(this.entityReference, that.entityReference)
            .append(this.location, that.location)
            .append(this.version, that.version)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.newMentions)
            .append(this.mentions)
            .append(this.authorReference)
            .append(this.entityReference)
            .append(this.location)
            .append(this.version)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("authorReference", this.getAuthorReference())
            .append("entityReference", this.getEntityReference())
            .append("version", this.getVersion())
            .append("location", this.getLocation())
            .append("mentions", this.mentions)
            .append("newMentions", this.newMentions)
            .build();
    }
}
