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
package org.xwiki.wysiwyg.plugin.alfresco.server;

import java.io.InputStream;
import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.gwt.wysiwyg.client.plugin.alfresco.AlfrescoEntity;

/**
 * Parses the responses received for Alfresco REST requests.
 * 
 * @version $Id$
 */
@ComponentRole
public interface AlfrescoResponseParser
{
    /**
     * Parses the authentication ticket from the authentication response.
     * 
     * @param responseStream the response received for the authentication request
     * @return the authentication ticket
     */
    String parseAuthTicket(InputStream responseStream);

    /**
     * Parses the parent response and extracts the corresponding Alfresco entity.
     * 
     * @param responseStream the response received for the parent request
     * @return the Alfresco entity describing the parent
     */
    AlfrescoEntity parseParent(InputStream responseStream);

    /**
     * Extracts the list of children from the response stream.
     * 
     * @param responseStream the response received for the children request
     * @return the list of child entities
     */
    List<AlfrescoEntity> parseChildren(InputStream responseStream);
}
