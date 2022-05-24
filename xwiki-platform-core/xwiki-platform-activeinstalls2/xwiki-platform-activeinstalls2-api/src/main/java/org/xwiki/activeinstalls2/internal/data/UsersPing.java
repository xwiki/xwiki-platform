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
 * Represents user-related Ping data.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class UsersPing
{
    private List<Long> all;

    private long main;

    private long total;

    /**
     * @return the number of active users in the main wiki (a.k.a global users)
     */
    public long getMain()
    {
        return this.main;
    }

    /**
     * @param userCount see {@link #getMain()}
     */
    public void setMain(long userCount)
    {
        this.main = userCount;
    }

    /**
     * @return the total number of active users in the main wiki and all the sub wikis (i.e. global users + local
     *         users)
     */
    public long getTotal()
    {
        return this.total;
    }

    /**
     * @param userCount see {@link #getTotal()}
     */
    public void setTotal(long userCount)
    {
        this.total = userCount;
    }

    /**
     * @return the number of active users in each subwiki (a.k.a local users), excluding the main wiki, as an array
     *         (we don't want to expose wiki names since that would contain private information)
     */
    public List<Long> getWikis()
    {
        return this.all;
    }

    /**
     * @param userCount see {@link #getWikis()}
     */
    public void setWikis(List<Long> userCount)
    {
        this.all = userCount;
    }

}
