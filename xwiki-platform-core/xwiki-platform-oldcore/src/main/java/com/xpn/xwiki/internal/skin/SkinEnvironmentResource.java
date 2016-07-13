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
package com.xpn.xwiki.internal.skin;

import java.net.URL;

import javax.inject.Provider;

import org.xwiki.environment.Environment;
import org.xwiki.skin.ResourceRepository;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * @version $Id$
 * @since 6.4M1
 */
public class SkinEnvironmentResource extends AbstractEnvironmentResource
{
    public SkinEnvironmentResource(String path, String resourceName, ResourceRepository repository,
        Environment environment, Provider<XWikiContext> xcontextProvider)
    {
        super(path, resourceName, repository, environment, xcontextProvider);
    }

    @Override
    public String getURL(boolean forceSkinAction) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiURLFactory urlf = xcontext.getURLFactory();

        URL url;

        if (forceSkinAction) {
            url = urlf.createSkinURL(this.resourceName, "skins", getRepository().getId(), xcontext);
        } else {
            url = urlf.createSkinURL(this.resourceName, getRepository().getId(), xcontext);
        }

        return urlf.getURL(url, xcontext);
    }
}
