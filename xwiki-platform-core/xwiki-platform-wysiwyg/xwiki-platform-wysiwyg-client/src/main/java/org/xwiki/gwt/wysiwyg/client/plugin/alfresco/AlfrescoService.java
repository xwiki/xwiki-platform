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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The service used to access an Alfresco content management system.
 * 
 * @version $Id$
 */
@ComponentRole
@RemoteServiceRelativePath("AlfrescoService.gwtrpc")
public interface AlfrescoService extends RemoteService
{
    /**
     * @param parentReference specifies the parent entity
     * @return the list of children of the specified Alfresco entity
     */
    List<AlfrescoEntity> getChildren(EntityReference parentReference);

    /**
     * @param childReference the Alfresco entity whose parent needs to be retrieved
     * @return the parent of the specified Alfresco entity
     */
    AlfrescoEntity getParent(EntityReference childReference);
}
