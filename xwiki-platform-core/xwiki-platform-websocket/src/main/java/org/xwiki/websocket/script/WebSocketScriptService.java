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
package org.xwiki.websocket.script;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;

/**
 * Script-related WebSocket APIs.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
@Component
@Named("websocket")
@Singleton
public class WebSocketScriptService implements ScriptService
{
    private static final String UTF8 = "UTF-8";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Computes the URL to access a WebSocket end-point. We support two types of end-points:
     * <ul>
     * <li>statically registered: are registered when the web application is deployed so they can't be removed or added
     * dynamically at runtime (without a server restart); they are mapped to an URL like this:
     * {@code ws://<host>/<webAppContextPath>/websocket/<endPointPath>} (e.g.
     * {@code ws://localhost:8080/xwiki/websocket/echo}); in order to get the URL to such an end-point you need to pass
     * to this method only the {@code /<endPointPath>} (e.g. {@code /echo})</li>
     * <li>dynamically registered: can be added or removed at runtime (e.g. by installing or uninstalling extensions,
     * without a server restart); they are mapped to an URL like this:
     * {@code ws://<host>/<webAppContextPath>/websocket/<wiki>/<endPointRoleHint>} (e.g.
     * {@code ws://localhost:8080/xwiki/websocket/dev/echo}); notice the wiki path parameter which is needed in order to
     * look for the end-point component in the right namespace; in order to get the URL to such an end-point you need to
     * pass to this method the {@code <endPointRoleHint>} (e.g. {@code echo}, if the end-point component has
     * {@code @Named("echo")})</li>
     * </ul>
     * 
     * @param pathOrRoleHint either the path a statically registered WebSocket end-point is mapped to (must start with
     *            slash {@code /}), or the role hint of a dynamically registered end-point (must not start with slash
     *            {@code /})
     * @return the URL to access the specified WebSocket end-point; statically registered end-points are identified by
     *         their path while dynamically registered end-points are identified by their role hint
     */
    public String url(String pathOrRoleHint)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();

            StringBuilder path = new StringBuilder("/");

            // Test if installed with ROOT servlet context and avoid double slash.
            String webAppPath = xcontext.getWiki().getWebAppPath(xcontext);
            if (!"/".equals(webAppPath)) {
                path.append(webAppPath);
            }

            path.append("websocket").append(path(pathOrRoleHint));
            URL serverURL = xcontext.getURLFactory().getServerURL(xcontext);
            String scheme = "https".equals(serverURL.getProtocol()) ? "wss" : "ws";
            // We have to add the path afterwards because the URI constructor double encodes it.
            // See https://bugs.openjdk.java.net/browse/JDK-8151244 (URI Constructor Doesn't Encode Path Correctly)
            return new URI(scheme, null, serverURL.getHost(), serverURL.getPort(), null, null, null).toString()
                + path.toString();
        } catch (Exception e) {
            this.logger.warn("Failed to create WebSocket URL for [{}]. Root cause is [{}].", pathOrRoleHint,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    private String path(String pathOrRoleHint) throws UnsupportedEncodingException
    {
        if (pathOrRoleHint.charAt(0) == '/') {
            return pathOrRoleHint;
        } else {
            String currentWiki = this.xcontextProvider.get().getWikiId();
            return String.format("/%s/%s", URLEncoder.encode(currentWiki, UTF8),
                URLEncoder.encode(pathOrRoleHint, UTF8));
        }
    }
}
