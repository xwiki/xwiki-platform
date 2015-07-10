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
package org.xwiki.rest.internal.url;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.XWikiRestException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Abstract class for ParametrizedRestURLGenerator.
 *
 * @param <T> the type of the resource for which the URL are created for. Must inherit from
 * {@link org.xwiki.model.reference.EntityReference}.
 * @version $Id$
 * @since 7.2M1 
 */
public abstract class AbstractParametrizedRestURLGenerator<T> implements ParametrizedRestURLGenerator<T>
{
    @Inject
    protected Provider<XWikiContext> contextProvider;

    protected URI getBaseURI() throws XWikiRestException
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();

            String url = context.getURLFactory().getServerURL(context).toString() +  "/"
                + xwiki.getWebAppPath(context) + "rest";

            return new URI(url);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new XWikiRestException("Failed to generate a proper base URI.", e);
        }
    }

    protected List<String> getSpaceList(SpaceReference spaceReference)
    {
        List<String> spaces = new ArrayList<>();
        for (EntityReference ref = spaceReference; ref != null && ref.getType() == EntityType.SPACE;
                ref = ref.getParent()) {
            spaces.add(ref.getName());
        }
        Collections.reverse(spaces);
        return spaces;
    }

    @Override
    public abstract URL getURL(T reference) throws XWikiRestException;
}
