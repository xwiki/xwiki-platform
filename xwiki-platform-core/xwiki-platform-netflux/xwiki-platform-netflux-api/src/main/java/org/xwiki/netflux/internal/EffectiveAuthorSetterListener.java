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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;

/**
 * Sets the effective author of the request to the effective author of the real-time session specified by the request.
 * 
 * @version $Id$
 * @since 15.10.12
 * @since 16.4.1
 * @since 16.6.0RC1
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

    /**
     * If the request has content that was synchronized through Netflux channels (in a real-time session), then the
     * author of that content is not necessarily the current user. For each Netflux channel, that is used to synchronize
     * content that may contain scripts, we keep track on the last author with the least script rights. We consider the
     * request effective author to be the script author with the least script rights among all script authors of the
     * Netflux channels that have contributed content to this request.
     * <p>
     * Note that the Netflux channels specified on the request could be associated with different XWiki documents (or
     * translations). We don't filter the channels that are associated with the current document (targeted by this
     * request) because we want the effective author to be responsible for the entire content submitted by this request
     * (not just the content of the current document).
     * 
     * @param request the request for which to determine the effective author
     * @return the user that is responsible in terms of access rights for the <strong>entire</strong> content submitted
     *         by this request and the effects it has on the server-side
     */
    private Optional<UserReference> getEffectiveAuthor(Request request)
    {
        return getChannelsFromRequest(request).stream()
            .map(channel -> this.scriptAuthorTracker.getScriptAuthor(channel)).filter(Optional::isPresent)
            .map(Optional::get).sorted().map(EntityChange::getAuthor).findFirst();
    }

    /**
     * @param request the request from which to extract the Netflux channels
     * @return the Netflux channels that have contributed content to the request, as indicated by the
     *         {@code netfluxChannel} request parameter
     */
    private List<String> getChannelsFromRequest(Request request)
    {
        List<String> channels = request.getProperties("netfluxChannel").stream()
            .map(value -> Objects.toString(value, null)).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        this.logger.debug("Channels from request: [{}].", channels);
        return channels;
    }
}
