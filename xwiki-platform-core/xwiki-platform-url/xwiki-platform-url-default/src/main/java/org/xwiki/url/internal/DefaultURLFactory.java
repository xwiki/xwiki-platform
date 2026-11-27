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
package org.xwiki.url.internal;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.URLFactory;
import org.xwiki.url.URLFactoryException;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@Component
@Singleton
public class DefaultURLFactory implements URLFactory
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource cfgConfiguration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public URL getServerURL() throws URLFactoryException
    {
        return getServerURL(this.contextProvider.get().getWikiReference());
    }

    private boolean isDaemon(XWikiRequest request)
    {
        return request.getHttpServletRequest() instanceof XWikiServletRequestStub
            && ((XWikiServletRequestStub) request.getHttpServletRequest()).isDaemon();
    }

    private String getWikiProtocol(WikiDescriptor wikiDescriptor) throws WikiManagerException
    {
        // Try wiki descriptor
        Boolean secure = wikiDescriptor.isSecure();
        if (secure != null) {
            return wikiDescriptor.isSecure() == Boolean.TRUE ? "https" : "http";
        }

        // Try configuration
        String protocol = cfgConfiguration.getProperty("xwiki.url.protocol");
        if (protocol != null) {
            return protocol;
        }

        // Try main wiki
        secure = wikiDescriptorManager.getMainWikiDescriptor().isSecure();

        if (secure != null) {
            return secure ? "https" : "http";
        }
        // FIXME: double check that it's the actual default fallback or not...
        return "https";
    }

    private int getWikiPort(WikiDescriptor wikiDescriptor) throws WikiManagerException
    {
        // Try wiki descriptor
        int port = wikiDescriptor.getPort();
        if (port != -1) {
            return port;
        }

        return wikiDescriptorManager.getMainWikiDescriptor().getPort();
    }

    public URL getServerURL(WikiReference wikiReference) throws URLFactoryException
    {
        // TODO: current logic coming mostly from XWiki#getServerURL but with removing some fallbacks
        // We need to put in that code the logic from XWikiServletURLFactory#getServerURL, which ultimately fallback
        // to XWiki#getServerURL
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();

        WikiReference actualWikiReference = (wikiReference != null) ? wikiReference : context.getMainWikiReference();


        // In path based the base URL is the same for all wikis
        if (!context.isMainWiki(actualWikiReference.getName()) && wiki.isPathBased()) {
            actualWikiReference = context.getMainWikiReference();
        }

        // If main wiki check the main wiki home page configuration
        if (context.isMainWiki(actualWikiReference.getName())) {
            String homepage = cfgConfiguration.getProperty("xwiki.home");
            if (StringUtils.isNotEmpty(homepage)) {
                try {
                    return new URL(homepage);
                } catch (MalformedURLException e) {
                    throw new URLFactoryException(
                        String.format("Invalid main wiki home page URL [%s] configured: {}", homepage),
                        e);
                }
            }
        }
        try {
            WikiDescriptor wikiDescriptor = wikiDescriptorManager.getById(actualWikiReference.getName());
            if (wikiDescriptor != null) {
                String server = wikiDescriptor.getDefaultAlias();
                if (server != null) {
                    String protocol = getWikiProtocol(wikiDescriptor);
                    int port = getWikiPort(wikiDescriptor);
                    return new URL(protocol, server, port, "");
                } else {
                    throw new URLFactoryException(
                        String.format("Missing default alias in descriptor for wiki [%s]",
                            actualWikiReference.getName()));
                }
            } else {
                throw new URLFactoryException(
                    String.format("Missing descriptor for wiki [%s]", actualWikiReference.getName()));
            }
        } catch (WikiManagerException e) {
            throw new URLFactoryException(
                String.format("Failed to get descriptor for wiki [%s]", actualWikiReference.getName()), e);
        } catch (MalformedURLException e) {
            throw new URLFactoryException(
                String.format("Error while building URL for wiki [%s]", actualWikiReference.getName()), e);
        }
    }
}
