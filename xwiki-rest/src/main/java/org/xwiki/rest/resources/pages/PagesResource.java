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
package org.xwiki.rest.resources.pages;

import java.util.Collections;
import java.util.List;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Pages;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class PagesResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        try {
            String spaceName = (String) getRequest().getAttributes().get(Constants.SPACE_NAME_PARAMETER);

            Pages pages = new Pages();
            List<String> pageNames = xwikiApi.getSpaceDocsName(spaceName);
            Collections.sort(pageNames);

            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            RangeIterable<String> ri =
                new RangeIterable<String>(pageNames, Utils.parseInt(queryForm.getFirstValue(Constants.START_PARAMETER),
                    0), Utils.parseInt(queryForm.getFirstValue(Constants.NUMBER_PARAMETER), -1));

            for (String pageName : ri) {
                String pageFullName = String.format("%s.%s", spaceName, pageName);

                if (!xwikiApi.exists(pageFullName)) {
                    getLogger().warning(
                        String.format("[Page '%s' appears to be in space '%s' but no information is available.]",
                            pageName, spaceName));
                } else {
                    Document doc = xwikiApi.getDocument(pageFullName);

                    /* We only add pages we have the right to access */
                    if (doc != null) {
                        pages.addPageSummary(DomainObjectFactory.createPageSummary(getRequest(), resourceClassRegistry,
                            doc));
                    }
                }
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), pages);
        } catch (XWikiException e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}
