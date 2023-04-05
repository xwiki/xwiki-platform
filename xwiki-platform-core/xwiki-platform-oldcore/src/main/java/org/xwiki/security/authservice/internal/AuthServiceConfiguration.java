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
package org.xwiki.security.authservice.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Store and read the configured auth service.
 * 
 * @version $Id$
 * @since 15.3RC1
 */
@Component(roles = AuthServiceConfiguration.class)
@Singleton
public class AuthServiceConfiguration
{
    /**
     * The spaces in which the authentication configuration is stored.
     */
    public static final List<String> SPACES = Arrays.asList("XWiki", "AuthService");

    /**
     * The spaces in which the authentication configuration is stored.
     */
    public static final String SPACES_STRING = "XWiki.AuthService";

    /**
     * The reference of the document holding the configuration of the authentication.
     */
    public static final LocalDocumentReference DOC_REFERENCE = new LocalDocumentReference(SPACES, "Configuration");

    /**
     * The name of the property containing the identifier of the authenticator in the xwiki.properties file.
     */
    public static final String CONFIGURATION_INSTANCE_PROPERTY =
        "security.authentication." + AuthServiceConfigurationClassInitializer.FIELD_SERVICE;

    /**
     * Used to store the configured name in the cache. The reason why we don't directly store the name is that we need
     * to differentiate between two use cases:
     * <ul>
     * <li>A given wiki does not have any configured auth service: the cache contains an entry associated with the wiki
     * identifier with a null name in it</li>
     * <li>The configuration of a given wiki hasn't been cache yet: the cache does not contain any entry associated with
     * the wiki identifier</li>
     * </ul>
     */
    private class ServiceCacheEntry
    {
        private final String name;

        ServiceCacheEntry(String name)
        {
            this.name = name;
        }
    }

    private Map<String, ServiceCacheEntry> serviceCache = new ConcurrentHashMap<>();

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    /**
     * @return the hint of the configured authentication service
     * @throws XWikiException when failing to load the configuration
     */
    public String getAuthService() throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Try at current wiki level
        String service = getAuthService(xcontext.getWikiReference(), xcontext);
        if (service != null) {
            return service;
        }

        // Try at main wiki level
        if (!xcontext.isMainWiki()) {
            service = getAuthService(xcontext.getWikiReference(), xcontext);
            if (service != null) {
                return service;
            }
        }

        // Try at xwiki.properties level
        return this.configurationSource.getProperty(CONFIGURATION_INSTANCE_PROPERTY);
    }

    private String getAuthService(WikiReference wiki, XWikiContext xcontext) throws XWikiException
    {
        ServiceCacheEntry service = this.serviceCache.get(wiki.getName());

        if (service == null) {
            service = new ServiceCacheEntry(loadAuthServiceId(wiki, xcontext));

            this.serviceCache.put(wiki.getName(), service);
        }

        return service.name;
    }

    private String loadAuthServiceId(WikiReference wiki, XWikiContext xcontext) throws XWikiException
    {
        XWikiDocument configurationDocument =
            xcontext.getWiki().getDocument(new DocumentReference(DOC_REFERENCE, wiki), xcontext);
        BaseObject configurationObject =
            configurationDocument.getXObject(AuthServiceConfigurationClassInitializer.CLASS_REFERENCE);

        if (configurationObject != null) {
            String serviceId =
                configurationObject.getStringValue(AuthServiceConfigurationClassInitializer.FIELD_SERVICE);

            if (StringUtils.isNotBlank(serviceId)) {
                return serviceId;
            }
        }

        return null;
    }

    private void setAuthServiceId(String id, WikiReference wiki, XWikiContext xcontext) throws XWikiException
    {
        XWikiDocument configurationDocument =
            xcontext.getWiki().getDocument(new DocumentReference(DOC_REFERENCE, wiki), xcontext);
        BaseObject configurationObject =
            configurationDocument.getXObject(AuthServiceConfigurationClassInitializer.CLASS_REFERENCE, true, xcontext);

        configurationObject.setStringValue(AuthServiceConfigurationClassInitializer.FIELD_SERVICE,
            StringUtils.defaultString(id));

        xcontext.getWiki().saveDocument(configurationDocument, "Change authenticator service", xcontext);
    }

    /**
     * @param id the identifier of the authenticator
     * @throws XWikiException when failing to update the configuration
     */
    public void setAuthServiceId(String id) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        setAuthServiceId(id, new WikiReference(xcontext.getWikiId()), xcontext);
    }

    /**
     * @param wikiId the identifier of the wiki to remove from the cache
     */
    public void invalidate(String wikiId)
    {
        this.serviceCache.remove(wikiId);
    }
}
