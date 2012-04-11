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

import java.net.URL;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.url.InvalidURLException;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLFactory;

/**
 * URL Factory that delegates the work to the URL Factory specified in the XWiki Configuration
 * (see {@link org.xwiki.url.URLConfiguration#getURLFormatId()}.
 *
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Singleton
public class DefaultXWikiURLFactory implements XWikiURLFactory<URL>
{
    /**
     * Used to get the hint of the {@link XWikiURLFactory} to use.
     */
    @Inject
    private URLConfiguration configuration;

    /**
     * Used to lookup the correct {@link XWikiURLFactory} component.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public XWikiURL createURL(URL urlRepresentation, Map<String, Object> parameters) throws InvalidURLException
    {
        XWikiURLFactory factory;
        try {
            factory = this.componentManager.getInstance(XWikiURLFactory.class, this.configuration.getURLFormatId());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Invalid configuration hint [" + this.configuration.getURLFormatId()
                + "]. Cannot create XWiki URL.", e);
        }
        return factory.createURL(urlRepresentation, parameters);
    }
}
