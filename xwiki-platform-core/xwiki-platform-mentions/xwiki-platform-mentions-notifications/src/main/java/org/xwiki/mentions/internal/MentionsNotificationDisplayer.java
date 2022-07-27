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
package org.xwiki.mentions.internal;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Displays the mentions notifications.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Named("mentions")
public class MentionsNotificationDisplayer implements NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "compositeEvent";

    private static final String EVENT_PARAMS_BINDING_NAME = "compositeEventParams";

    private static final String DEFAULT_ACTION = "view";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private DocumentAccessBridge documentAccess;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private MentionsNotificationsObjectMapper objectMapper;

    @Inject
    private Logger logger;

    @Override
    public Block renderNotification(CompositeEvent compositeEvent) throws NotificationException
    {
        GroupBlock ret = new GroupBlock();
        String template = "mentions/mention.vm";

        ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
        scriptContext.setAttribute(EVENT_BINDING_NAME, compositeEvent, ScriptContext.ENGINE_SCOPE);
        Map<Event, MentionView> paramsMap = buildParamsMap(compositeEvent);
        scriptContext.setAttribute(EVENT_PARAMS_BINDING_NAME, paramsMap, ScriptContext.ENGINE_SCOPE);
        try {
            ret.addChildren(this.templateManager.execute(template).getChildren());
        } catch (Exception e) {
            throw new NotificationException(String.format("Failed to render a template [%s]", template), e);
        }
        return ret;
    }

    private Map<Event, MentionView> buildParamsMap(CompositeEvent compositeEvent)
    {
        Map<Event, MentionView> ret = new HashMap<>();
        compositeEvent.getEvents()
            .forEach(event -> deserializeParam(event)
                                  .flatMap(this::convert)
                                  .ifPresent(it -> ret.put(event, it)));
        return ret;
    }

    private String getExternalURL(XWikiDocument document, String anchor)
    {
        XWikiContext context = this.contextProvider.get();
        // Code inspired by XWikiDocument#getExternalURL
        URL url = context.getURLFactory()
            .createExternalURL(document.getSpace(), document.getName(), DEFAULT_ACTION, null, anchor, context);
        return url.toString();
    }

    private Optional<MentionView> convert(MentionEventParams mentionEventParams)
    {
        DocumentReference userReference =
            this.documentReferenceResolver.resolve(mentionEventParams.getUserReference());
        DocumentReference documentReference =
            this.documentReferenceResolver.resolve(mentionEventParams.getDocumentReference());
        
        try {
            XWikiContext context = this.contextProvider.get();
            XWikiDocument userDocumentInstance = (XWikiDocument) this.documentAccess.getDocumentInstance(userReference);
            XWikiDocument document = (XWikiDocument) this.documentAccess.getDocumentInstance(documentReference);
            String authorURL = userDocumentInstance.getExternalURL(DEFAULT_ACTION, context);
            String documentURL = getExternalURL(document, mentionEventParams.getAnchor());
            MentionView mentionView = new MentionView()
                                          .setAuthorURL(authorURL)
                                          .setDocumentURL(documentURL)
                                          .setDocument(document)
                                          .setLocation(mentionEventParams.getLocation().name())
                                          .setQuote(mentionEventParams.getQuote());
            return Optional.of(mentionView);
        } catch (Exception e) {
            this.logger
                .warn("Error during the conversion of [{}]. Cause [{}].", userReference, getRootCauseMessage(e));
        }

        return Optional.empty();
    }

    /**
     * Retrieve the {@link MentionEventParams} of the notification from the event.
     * @param event the event.
     * @return The unserialized {@link MentionEventParams}.
     */
    private Optional<MentionEventParams> deserializeParam(Event event)
    {
        Map<String, Object> parameters = event.getCustom();
        if (parameters.containsKey(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY)) {
            return this.objectMapper
                .unserialize((String) parameters.get(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return singletonList(MentionEvent.EVENT_TYPE);
    }
}
