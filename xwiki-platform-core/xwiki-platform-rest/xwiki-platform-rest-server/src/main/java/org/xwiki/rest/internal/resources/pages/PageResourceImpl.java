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
package org.xwiki.rest.internal.resources.pages;

import java.net.URI;

import javax.inject.Named;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.pages.PageResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.pages.PageResourceImpl")
public class PageResourceImpl extends ModifiablePageResource implements PageResource
{
    @Override
    public Page getPage(String wikiName, String spaceName, String pageName, Boolean withPrettyNames,
        Boolean withObjects, Boolean withXClass, Boolean withAttachments) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            URI baseUri = uriInfo.getBaseUri();

            Page page =
                this.factory.toRestPage(baseUri, uriInfo.getAbsolutePath(), doc, false, withPrettyNames, withObjects,
                    withXClass, withAttachments);

            return page;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Response putPage(String wikiName, String spaceName, String pageName, Page page) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, false, true);

            return putPage(documentInfo, page);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public void deletePage(String wikiName, String spaceName, String pageName) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

            deletePage(documentInfo);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
