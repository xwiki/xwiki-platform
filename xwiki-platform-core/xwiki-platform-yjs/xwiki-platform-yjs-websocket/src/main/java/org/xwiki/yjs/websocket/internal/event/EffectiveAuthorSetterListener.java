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
package org.xwiki.yjs.websocket.internal.event;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;
import org.xwiki.yjs.websocket.internal.RoomScriptAuthorTracker;
import org.xwiki.yjs.websocket.internal.ScriptAuthorChange;

/**
 * Sets the effective author of the current request to the effective author of the real-time collaboration session
 * specified by the request.
 * 
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Singleton
@Named(EffectiveAuthorSetterListener.HINT)
public class EffectiveAuthorSetterListener extends AbstractLocalEventListener
{
    /**
     * The hint of this listener component.
     */
    public static final String HINT = "org.xwiki.yjs.websocket.internal.event.EffectiveAuthorSetterListener";

    private static final String SEPARATOR = "/";

    @Inject
    private Logger logger;

    @Inject
    private RoomScriptAuthorTracker scriptAuthorTracker;

    @Inject
    private Container container;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

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
            request.setAttribute(Request.ATTRIBUTE_EFFECTIVE_AUTHOR, effectiveAuthor);
        });
    }

    /**
     * If the request has content that was synchronized through Yjs rooms (in real-time collaboration sessions), then
     * the author of that content is not necessarily the current user. For each Yjs room we keep track of the last
     * author with the least script rights. We consider the request effective author to be the script author with the
     * least script rights among all script authors of the Yjs rooms that have contributed content to this request.
     * <p>
     * Note that the effective author is responsible for the entire content submitted by this request. The request can
     * update multiple document instances (e.g. the request might have object property values that are saved on the
     * default document translation, as well as document title and content which are saved on the document translation
     * matching the current locale).
     * 
     * @param request the request for which to determine the effective author
     * @return the user that is responsible in terms of access rights for the <strong>entire</strong> content submitted
     *         by this request and the effects it has on the server-side
     */
    private Optional<UserReference> getEffectiveAuthor(Request request)
    {
        return getRoomReferencesFromRequest(request).stream().map(this.scriptAuthorTracker::getScriptAuthor)
            .filter(Optional::isPresent).map(Optional::get).sorted().map(ScriptAuthorChange::getAuthor).findFirst();
    }

    /**
     * @param request the request from which to extract the collaboration room references
     * @return the collaboration rooms that have contributed content to the request, as indicated by the
     *         {@code collaboration} request parameter
     */
    private List<DocumentReference> getRoomReferencesFromRequest(Request request)
    {
        String[] roomKeys = request.getParameterValues("collaboration");
        if (roomKeys == null) {
            roomKeys = new String[0];
        }
        List<DocumentReference> roomReferences = Stream.of(roomKeys).map(roomKey -> {
            String[] parts = roomKey.split(SEPARATOR);
            DocumentReference roomReference =
                this.currentDocumentReferenceResolver.resolve(URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
            if (parts.length > 1) {
                roomReference = new DocumentReference(roomReference, LocaleUtils.toLocale(parts[1]));
            } else if (roomKey.endsWith(SEPARATOR)) {
                roomReference = new DocumentReference(roomReference, Locale.ROOT);
            }
            return roomReference;
        }).toList();
        this.logger.debug("Collaboration rooms from request: [{}].", roomReferences);
        return roomReferences;
    }
}
