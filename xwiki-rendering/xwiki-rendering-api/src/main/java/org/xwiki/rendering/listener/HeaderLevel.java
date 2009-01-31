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
package org.xwiki.rendering.listener;

/**
 * @version $Id$
 * @since 1.5M2
 */
public enum HeaderLevel
{
    LEVEL1(1), LEVEL2(2), LEVEL3(3), LEVEL4(4), LEVEL5(5), LEVEL6(6);

    private int value;

    private HeaderLevel(int value)
    {
        this.value = value;
    }

    public int getAsInt()
    {
        return this.value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString()
    {
        return "" + getAsInt();
    }

    public static HeaderLevel parseInt(int value)
    {
        HeaderLevel result;
        switch (value) {
            case 1:
                result = HeaderLevel.LEVEL1;
                break;
            case 2:
                result = HeaderLevel.LEVEL2;
                break;
            case 3:
                result = HeaderLevel.LEVEL3;
                break;
            case 4:
                result = HeaderLevel.LEVEL4;
                break;
            case 5:
                result = HeaderLevel.LEVEL5;
                break;
            case 6:
                result = HeaderLevel.LEVEL6;
                break;
            default:
                throw new IllegalArgumentException("Invalid level [" + value + "]. Only levels 1 to 6 are allowed.");
        }
        return result;
    }
}
