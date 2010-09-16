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
package com.xpn.xwiki.wysiwyg.server.wiki;

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Converts between client-side entity references and server-side entity references.
 * 
 * @version $Id$
 */
public class EntityReferenceConverter
{
    /**
     * Converts an entity reference received from the client to an entity reference to be used on the server.
     * 
     * @param clientEntityReference a client-side entity reference
     * @return a server-side entity reference
     */
    public EntityReference convert(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference clientEntityReference)
    {
        if (clientEntityReference == null) {
            return null;
        }
        EntityReference serverEntityReference = null;
        if (!StringUtils.isEmpty(clientEntityReference.getWikiName())) {
            serverEntityReference = new EntityReference(clientEntityReference.getWikiName(), EntityType.WIKI);
        }
        if (!StringUtils.isEmpty(clientEntityReference.getSpaceName())) {
            serverEntityReference =
                new EntityReference(clientEntityReference.getSpaceName(), EntityType.SPACE, serverEntityReference);
        }
        if (!StringUtils.isEmpty(clientEntityReference.getPageName())) {
            serverEntityReference =
                new EntityReference(clientEntityReference.getPageName(), EntityType.DOCUMENT, serverEntityReference);
        }
        if (!StringUtils.isEmpty(clientEntityReference.getFileName())) {
            serverEntityReference =
                new EntityReference(clientEntityReference.getFileName(), EntityType.ATTACHMENT, serverEntityReference);
        }
        return serverEntityReference;
    }

    /**
     * Converts an entity reference used on the server side to an entity reference to be sent to the client.
     * 
     * @param serverEntityReference a server-side entity reference
     * @return the corresponding client-side entity reference
     */
    public org.xwiki.gwt.wysiwyg.client.wiki.EntityReference convert(EntityReference serverEntityReference)
    {
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference clientEntityReference =
            new org.xwiki.gwt.wysiwyg.client.wiki.EntityReference();
        switch (serverEntityReference.getType()) {
            case DOCUMENT:
                clientEntityReference.setType(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.DOCUMENT);
                break;
            case ATTACHMENT:
                clientEntityReference.setType(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.ATTACHMENT);
                break;
            default:
                break;
        }
        EntityReference child = serverEntityReference;
        while (child != null) {
            switch (child.getType()) {
                case WIKI:
                    clientEntityReference.setWikiName(child.getName());
                    break;
                case SPACE:
                    clientEntityReference.setSpaceName(child.getName());
                    break;
                case DOCUMENT:
                    clientEntityReference.setPageName(child.getName());
                    break;
                case ATTACHMENT:
                    clientEntityReference.setFileName(child.getName());
                    break;
                default:
                    break;
            }
            child = child.getParent();
        }
        return clientEntityReference;
    }

    /**
     * @param clientEntityType an entity type received from the client
     * @return the corresponding server-side entity type
     */
    public EntityType convert(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType clientEntityType)
    {
        switch (clientEntityType) {
            case DOCUMENT:
                return EntityType.DOCUMENT;
            case ATTACHMENT:
            case IMAGE:
                return EntityType.ATTACHMENT;
            default:
                return null;
        }
    }
}
