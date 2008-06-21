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
 *
 */
package org.xwiki.platform.patchservice.api;

/**
 * An <tt>Originator</tt> identifies the origin of a patch: the place where the changes occured, and the user who made
 * those changes.
 * 
 * @see RWOriginator
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface Originator extends XmlSerializable
{
    /**
     * Gets the local name of the user who caused this patch.
     * 
     * @return The wiki name of the original author.
     */
    String getAuthor();

    /**
     * Gets the name of the virtual wiki where the patch was created.
     * 
     * @return The original name of the wiki.
     */
    String getWikiId();

    /**
     * Gets an identifier of the host where the patch was created.
     * 
     * @return The ID (e.g. IP address) of the original host.
     */
    String getHostId();
}
