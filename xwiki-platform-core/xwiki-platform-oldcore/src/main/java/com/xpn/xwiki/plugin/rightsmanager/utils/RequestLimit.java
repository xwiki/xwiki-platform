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
package com.xpn.xwiki.plugin.rightsmanager.utils;

/**
 * Contains maximum number of result to return and index of the first element.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class RequestLimit
{
    /**
     * The maximum number of result to return.
     */
    private int nb;

    /**
     * The index of the first found element to return.
     */
    private int start;

    /**
     * Construct new instance of RequestLimit with provided nb and start.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found element to return.
     */
    public RequestLimit(int nb, int start)
    {
        this.setNb(nb);
        this.setStart(start);
    }

    /**
     * @param nb the maximum number of result to return.
     */
    public void setNb(int nb)
    {
        this.nb = nb;
    }

    /**
     * @return the maximum number of result to return.
     */
    public int getNb()
    {
        return this.nb;
    }

    /**
     * @param start the index of the first found element to return.
     */
    public void setStart(int start)
    {
        this.start = start;
    }

    /**
     * @return the index of the first found element to return.
     */
    public int getStart()
    {
        return this.start;
    }
}
