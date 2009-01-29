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

import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Page;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class PageVersionResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
        if (documentInfo == null) {
            return null;
        }

        Document doc = documentInfo.getDocument();

        Page page = DomainObjectFactory.createPage(getRequest(), resourceClassRegistry, doc, true);
        if (page == null) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        }

        return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), page);
    }
}
