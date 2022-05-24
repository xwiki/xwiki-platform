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
package org.xwiki.activeinstalls2.internal.data;

import java.util.List;

/**
 * Represents document-related Ping data.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class DocumentsPing
{
    private List<Long> all;

    private long main;

    private long total;

    /**
     * @return the number of documents in the main wiki
     */
    public long getMain()
    {
        return this.main;
    }

    /**
     * @param documentCount see {@link #getMain()}
     */
    public void setMain(long documentCount)
    {
        this.main = documentCount;
    }

    /**
     * @return the total number of documents in the main wiki and all the sub wikis
     */
    public long getTotal()
    {
        return this.total;
    }

    /**
     * @param documentCount see {@link #getTotal()}
     */
    public void setTotal(long documentCount)
    {
        this.total = documentCount;
    }

    /**
     * @return the number of documents in each subwiki, excluding the main wiki, as an array
     *         (we don't want to expose wiki names since that would contain private information)
     */
    public List<Long> getWikis()
    {
        return this.all;
    }

    /**
     * @param documentCount see {@link #getWikis()}
     */
    public void setWikis(List<Long> documentCount)
    {
        this.all = documentCount;
    }

}
