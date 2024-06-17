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
package org.xwiki.netflux.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.netflux.internal.EntityChange.ScriptLevel;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Keeps track of the script author associated with entity channels that are used to synchronize content that may
 * contain scripts.
 *
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0RC1
 */
@Component(roles = EntityChannelScriptAuthorTracker.class)
@Singleton
public class EntityChannelScriptAuthorTracker
{
    @Inject
    private Logger logger;

    @Inject
    private EntityChannelStore entityChannels;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserReferenceSerializer;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> explicitEntityReferenceResolver;

    private final Map<String, EntityChange> scriptAuthors = new ConcurrentHashMap<>();

    /**
     * @param channelId identifies the entity channel for which to return the script author
     * @return the most recent entity change (pushed through the specified entity channel) that updated the script
     *         author associated with the entity channel
     */
    public Optional<EntityChange> getScriptAuthor(String channelId)
    {
        Optional<EntityChange> returnValue = Optional.empty();
        EntityChange channelScriptAuthor = this.scriptAuthors.get(channelId);
        if (channelScriptAuthor != null) {
            UserReference currentUserReference = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
            ScriptLevel currentUserScriptLevel =
                getUserScriptLevel(currentUserReference, channelScriptAuthor.getEntityReference());
            if (currentUserScriptLevel.compareTo(channelScriptAuthor.getScriptLevel()) > 0) {
                // We take into account the script author from the entity channel only if it has less script rights
                // than the current user. Otherwise the current user will be used as script author.
                this.logger.debug("The script author associated with the entity channel [{}] is [{}].", channelId,
                    channelScriptAuthor);
                returnValue = Optional.of(channelScriptAuthor);
            }
            if (this.entityChannels.getChannel(channelId).isEmpty()) {
                // The entity channel has been removed. We can safely remove the script author associated with it.
                // Note that we still return the script author that was associated with the removed entity channel
                // because it can happen that the script author is needed after the entity channel is removed (e.g. when
                // you save or preview while editing alone; the entity channel may be removed as soon as you leave the
                // edit mode, but the script author is needed in the save and preview action which is executed after).
                this.scriptAuthors.remove(channelId);
            }
        }

        return returnValue;
    }

    void maybeUpdateScriptAuthor(EntityChannel entityChannel, UserReference scriptAuthor)
    {
        EntityReference targetEntityReference = getTargetEntityReference(entityChannel);
        EntityChange channelScriptAuthor =
            this.scriptAuthors.getOrDefault(entityChannel.getKey(), getEntityScriptAuthor(targetEntityReference));
        ScriptLevel userScriptLevel = getUserScriptLevel(scriptAuthor, entityChannel.getEntityReference());
        if (channelScriptAuthor == null || userScriptLevel.compareTo(channelScriptAuthor.getScriptLevel()) <= 0) {
            // The user making the change has a lower or equal script level than the channel script level. We
            // need to update the channel script level in order to prevent privilege escalation.
            EntityChange scriptAuthorChange = new EntityChange(targetEntityReference, scriptAuthor, userScriptLevel);
            this.scriptAuthors.put(entityChannel.getKey(), scriptAuthorChange);
            this.logger.debug("Updated the script author associated with the entity channel [{}] to [{}].",
                entityChannel, scriptAuthorChange);
        }
    }

    private EntityChange getEntityScriptAuthor(EntityReference entityReference)
    {
        try {
            if (entityReference instanceof DocumentReference) {
                DocumentReference documentReference = (DocumentReference) entityReference;
                DocumentModelBridge document =
                    this.documentAccessBridge.getTranslatedDocumentInstance(documentReference);
                UserReference contentAuthor = document.getAuthors().getContentAuthor();
                return new EntityChange(documentReference, contentAuthor,
                    getUserScriptLevel(contentAuthor, documentReference));
            } else if (entityReference instanceof ObjectPropertyReference) {
                ObjectPropertyReference objectPropertyReference = (ObjectPropertyReference) entityReference;
                DocumentReference documentReference =
                    new DocumentReference(objectPropertyReference.extractReference(EntityType.DOCUMENT));
                // Metadata is stored on the default document translation.
                DocumentModelBridge document = this.documentAccessBridge.getDocumentInstance(documentReference);
                UserReference metadataAuthor = document.getAuthors().getEffectiveMetadataAuthor();
                return new EntityChange(documentReference, metadataAuthor,
                    getUserScriptLevel(metadataAuthor, documentReference));
            }
        } catch (Exception e) {
            this.logger.warn("Failed to compute the script level for entity [{}]. Root cause is [{}].", entityReference,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return null;
    }

    private ScriptLevel getUserScriptLevel(UserReference userReference, EntityReference entityReference)
    {
        DocumentReference documentUserReference = this.documentUserReferenceSerializer.serialize(userReference);
        if (this.authorizationManager.hasAccess(Right.PROGRAM, documentUserReference, entityReference)) {
            return ScriptLevel.PROGRAMMING;
        } else if (this.authorizationManager.hasAccess(Right.SCRIPT, documentUserReference, entityReference)) {
            return ScriptLevel.SCRIPT;
        } else {
            return ScriptLevel.NO_SCRIPT;
        }
    }

    private EntityReference getTargetEntityReference(EntityChannel entityChannel)
    {
        EntityReference targetEntityReference = entityChannel.getEntityReference();
        List<String> path = entityChannel.getPath();
        if (targetEntityReference.getType() == EntityType.DOCUMENT && !path.isEmpty()) {
            try {
                Locale locale = LocaleUtils.toLocale(path.get(0));
                if (path.size() > 1) {
                    // The path indicates the document property that is synchronized.
                    String property = path.get(1);
                    if (!"content".equals(property)) {
                        // The entity channel is used to synchronize document metadata, which is shared by all document
                        // translations (stored on the default translation).
                        return new ObjectPropertyReference(this.explicitEntityReferenceResolver.resolve(property,
                            EntityType.OBJECT_PROPERTY, targetEntityReference));
                    }
                }
                // The entity channel is used to synchronize the value of a document property that is not shared between
                // document translations. We need to take the script author from the specified document translation.
                return new DocumentReference(targetEntityReference.extractReference(EntityType.DOCUMENT), locale);
            } catch (Exception e) {
                // The document reference is probably relative but we can't resolve it because there's no currrent
                // document in the context where this method is called.
                this.logger.warn("Failed to compute the target entity reference"
                    + " associated with the entity channel [{}] with path [{}].", entityChannel.getKey(), path);
            }
        }
        return targetEntityReference;
    }
}
