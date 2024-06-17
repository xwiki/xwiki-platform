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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;

/**
 * Sets the effective author of the request to the effective author of the real-time session specified by the request.
 * 
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0
 */
@Component
@Singleton
@Named(EffectiveAuthorSetterListener.HINT)
public class EffectiveAuthorSetterListener extends AbstractLocalEventListener
{
    /**
     * The hint of this listener component.
     */
    public static final String HINT = "org.xwiki.netflux.internal.EffectiveAuthorSetterListener";

    @Inject
    private Logger logger;

    @Inject
    private EntityChannelScriptAuthorTracker scriptAuthorTracker;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Container container;

    /**
     * Default constructor.
     */
    public EffectiveAuthorSetterListener()
    {
        super(HINT, new ActionExecutingEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        Request request = this.container.getRequest();
        getEffectiveAuthor(request).ifPresent(effectiveAuthor -> {
            this.logger.debug("Setting the effective author of the request to [{}].", effectiveAuthor);
            request.setProperty("com.xpn.xwiki.web.XWikiRequest#effectiveAuthor", effectiveAuthor);
        });
    }

    private Optional<UserReference> getEffectiveAuthor(Request request)
    {
        try {
            DocumentReference documentReference = this.documentAccessBridge.getCurrentDocumentReference();
            DocumentModelBridge translatedDocument =
                this.documentAccessBridge.getTranslatedDocumentInstance(documentReference);
            Locale realLocale = LocaleUtils.toLocale(translatedDocument.getRealLanguage());
            DocumentReference documentReferenceWithRealLocale = new DocumentReference(documentReference, realLocale);

            return getChannelsFromRequest(request).stream()
                .map(channel -> this.scriptAuthorTracker.getScriptAuthor(channel)).filter(Optional::isPresent)
                .map(Optional::get)
                .filter(
                    entityChange -> Objects.equals(entityChange.getEntityReference(), documentReferenceWithRealLocale)
                        || entityChange.getEntityReference().hasParent(documentReference))
                .sorted().map(EntityChange::getAuthor).findFirst();
        } catch (Exception e) {
            this.logger.warn("Failed to determine the effective request author. Root cause is [{}].",
                ExceptionUtils.getRootCauseMessage(e));
            return Optional.empty();
        }
    }

    private List<String> getChannelsFromRequest(Request request)
    {
        List<String> channels = request.getProperties("netfluxChannel").stream()
            .map(value -> Objects.toString(value, null)).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        this.logger.debug("Channels from request: [{}].", channels);
        return channels;
    }
}
