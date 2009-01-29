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

import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Page;
import org.xwiki.rest.model.XStreamFactory;

import com.thoughtworks.xstream.XStream;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * Resource for a page (and its variants). This resource supports also modifications through PUT and DELETE methods.
 * 
 * @version $Id$
 */
public abstract class ModifiablePageResource extends XWikiResource
{
    @Override
    public boolean allowPut()
    {
        return true;
    }

    @Override
    public boolean allowDelete()
    {
        return true;
    }

    @Override
    public void handlePut()
    {
        MediaType mediaType = getRequest().getEntity().getMediaType();

        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), false, true);
        if (documentInfo == null) {
            return;

        }

        Document doc = documentInfo.getDocument();

        /* Process the entity */
        if (MediaType.TEXT_PLAIN.equals(mediaType)) {
            try {
                doc.setContent(getRequest().getEntity().getText());
                doc.save();
            } catch (IOException e) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                return;
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                } else {
                    getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                }

                return;
            }
        } else if (MediaType.APPLICATION_XML.equals(mediaType)) {
            XStream xstream = XStreamFactory.getXStream();

            Page page = null;

            /* If we receive an XML that is not convertible to a Page object we reject it */
            try {
                page = (Page) xstream.fromXML(getRequest().getEntity().getText());
            } catch (Exception e) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
                return;
            }

            /* We will only save if something changes... */
            boolean save = false;

            if (page.getContent() != null) {
                doc.setContent(page.getContent());
                save = true;
            }

            if (page.getParent() != null) {
                if (!page.getParent().equals(doc.getParent())) {
                    doc.setParent(page.getParent());
                    save = true;
                }
            }

            if (page.getTitle() != null) {
                if (!page.getTitle().equals(doc.getTitle())) {
                    doc.setTitle(page.getTitle());
                    save = true;
                }
            }

            if (save) {
                try {
                    doc.save();
                } catch (XWikiException e) {
                    if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                        getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                    } else {
                        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                    }

                    return;
                }

                /* Set the correct response code, depending whether the document existed or not */
                if (documentInfo.isCreated()) {
                    getResponse().setStatus(Status.SUCCESS_CREATED);
                } else {
                    getResponse().setStatus(Status.SUCCESS_ACCEPTED);
                }

                /* Set the entity as being the new/updated document XML representation */
                getResponse().setEntity(
                    new StringRepresentation(Utils.toXml(DomainObjectFactory.createPage(getRequest(),
                        resourceClassRegistry, doc, false)), MediaType.APPLICATION_XML));
            }
        }
    }

    @Override
    public void handleDelete()
    {
        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, true);
        if (documentInfo == null) {
            return;
        }

        Document doc = documentInfo.getDocument();

        try {
            doc.delete();
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            }

            return;
        }
    }

}
