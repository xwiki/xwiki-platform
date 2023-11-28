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
package org.xwiki.tour.test.po;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * @version $Id$
 * @since 15.9RC1
 */
public class TourFromLivetable
{
    private String name;

    private String targetPage;

    private boolean isActive;

    private String targetClass;

    public TourFromLivetable(String name, String targetPage, boolean isActive, String targetClass)
    {
        this.name = name;
        this.targetPage = targetPage;
        this.isActive = isActive;
        this.targetClass = targetClass;
    }

    public String getName()
    {
        return name;
    }

    public String getTargetPage()
    {
        return targetPage;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public String getTargetClass()
    {
        return targetClass;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof TourFromLivetable) {
            TourFromLivetable t = (TourFromLivetable) o;
            return new EqualsBuilder().append(t.name, name).append(t.targetPage, targetPage)
                .append(t.targetClass, targetClass).append(t.isActive, isActive).isEquals();

        }
        return false;
    }
}
