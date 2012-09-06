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
package org.xwiki.model;

/**
 * An XWiki Server is made of one or several {@link org.xwiki.model.Wiki}s. This is the top most
 * concept of the XWiki Model.
 */
public interface Server extends Persistable
{
    /**
     * @return the list of all Wiki objects inside this Server
     */
    EntityIterator<Wiki> getWikis();

    Wiki getWiki(String wikiName);

    Wiki addWiki(String wikiName);

    void removeWiki(String wikiName);

    boolean hasWiki(String wikiName);
}
