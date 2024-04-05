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
package org.xwiki.image.style;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;

/**
 * Provides the script service operations for the image styles. In particular, it provides helpers to generate the REST
 * endpoint URLs.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
@Named("imageStyle")
public class ImageStyleScriptService implements ScriptService
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * @return the image styles rest endpoint path
     */
    public String getDefaultImageStyleRestPath()
    {
        return getImageStylesRestPath() + "/default";
    }

    /**
     * @return the default image style rest endpoint path
     */
    public String getImageStylesRestPath()
    {
        return "/rest/wikis/" + this.contextProvider.get().getWikiId() + "/imageStyles";
    }
}
