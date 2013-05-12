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

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.resources.pages.PageResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.internal.resources.pages.PageResourceImpl")
public class PageResourceImpl extends ModifiablePageResource implements PageResource
{
    @Override
    public Page getPage(String wikiName, String spaceName, String pageName, Boolean withPrettyNames, 
                        Boolean withRenderedContent, Boolean withAttachments, Boolean withObjects)
            throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            Page page = DomainObjectFactory.createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), doc,
                    false, Utils.getXWikiApi(componentManager), withPrettyNames);
            
            // adding rendered content
            if (withRenderedContent) {
                page.setRenderedContent(doc.getRenderedContent());
            }
            
            // adding all attachments
            if (withAttachments) {
                page.setAttachments(getAttachmentsForDocument(doc, withPrettyNames));
            }

            // adding all objects
            if (withObjects) {
                page.setObjects(new Page.Objects());
                page.getObjects().getObjects().addAll(getObjects(wikiName, spaceName, pageName, withPrettyNames));
            }

            return page;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
    


    protected Attachments getAttachmentsForDocument(Document doc, Boolean withPrettyNames) throws XWikiException
    {
        Attachments attachments =  objectFactory.createAttachments();

        List<com.xpn.xwiki.api.Attachment> xwikiAttachments = doc.getAttachmentList();

        for (com.xpn.xwiki.api.Attachment xwikiAttachment : xwikiAttachments) {
            URL url =
                Utils
                .getXWikiContext(componentManager)
                .getURLFactory()
                .createAttachmentURL(xwikiAttachment.getFilename(), doc.getSpace(), doc.getName(),
                    "download",
                    null, doc.getWiki(), Utils.getXWikiContext(componentManager));
            String attachmentXWikiAbsoluteUrl = url.toString();
            String attachmentXWikiRelativeUrl =
                Utils.getXWikiContext(componentManager).getURLFactory()
                .getURL(url, Utils.getXWikiContext(componentManager));

            attachments.getAttachments().add(
                DomainObjectFactory.createAttachment(objectFactory, uriInfo.getBaseUri(), xwikiAttachment,
                    attachmentXWikiRelativeUrl, attachmentXWikiAbsoluteUrl, Utils.getXWikiApi(componentManager),
                    withPrettyNames));
        }

        return attachments;
    }

    
    protected List<Object> getObjects(String wikiName, String spaceName, String pageName,
            Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            List<Object> objects = new ArrayList<Object>();
            List<BaseObject> objectList = getBaseObjects(doc);
            
            for (BaseObject object : objectList) {
                /* By deleting objects, some of them might become null, so we must check for this */
                if (object != null) {
                    objects.add(DomainObjectFactory
                            .createObject(objectFactory, uriInfo.getBaseUri(), Utils.getXWikiContext(
                                    componentManager), doc, object, false, Utils.getXWikiApi(componentManager),
                                    withPrettyNames));
                }
            }

            return objects;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
    
    protected List<BaseObject> getBaseObjects(Document doc) throws XWikiException
    {
        List<BaseObject> objectList = new ArrayList<BaseObject>();

        XWikiDocument xwikiDocument =
            Utils.getXWiki(componentManager).getDocument(doc.getPrefixedFullName(),
                Utils.getXWikiContext(componentManager));

        Map<DocumentReference, List<BaseObject>> classToObjectsMap = xwikiDocument.getXObjects();
        for (DocumentReference classReference : classToObjectsMap.keySet()) {
            List<BaseObject> xwikiObjects = classToObjectsMap.get(classReference);
            for (BaseObject object : xwikiObjects) {
                objectList.add(object);
            }
        }

        return objectList;
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
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, false, true);

            deletePage(documentInfo);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
