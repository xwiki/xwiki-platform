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
package com.xpn.xwiki.api;

public class DocumentSection
{
    private int sectionNumber;

    private int sectionIndex;

    private String sectionLevel;

    private String sectionTitle;

    public DocumentSection(int sectionNumber, int sectionIndex, String sectionLevel,
        String sectionTitle)
    {
        setSectionNumber(sectionNumber);
        setSectionIndex(sectionIndex);
        setSectionLevel(sectionLevel);
        setSectionTitle(sectionTitle);
    }

    public int getSectionNumber()
    {
        return this.sectionNumber;
    }

    public void setSectionNumber(int sectionNumber)
    {
        this.sectionNumber = sectionNumber;
    }

    public int getSectionIndex()
    {
        return this.sectionIndex;
    }

    public void setSectionIndex(int sectionIndex)
    {
        this.sectionIndex = sectionIndex;
    }

    public String getSectionLevel()
    {
        return this.sectionLevel;
    }

    public void setSectionLevel(String sectionLevel)
    {
        this.sectionLevel = sectionLevel;
    }

    public String getSectionTitle()
    {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle)
    {
        this.sectionTitle = sectionTitle;
    }
}
