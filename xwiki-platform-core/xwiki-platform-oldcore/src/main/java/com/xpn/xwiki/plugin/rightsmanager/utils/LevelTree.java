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
 * Contains all rights for a level right.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class LevelTree
{
    /**
     * List of allow and deny rights for this right level.
     */
    public AllowDeny inherited;

    /**
     * List of inherited allow and deny rights for this right level.
     */
    public AllowDeny direct;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if (this.inherited != null) {
            String heritedString = this.inherited.toString();
            if (heritedString.length() > 0) {
                sb.append('{');
                sb.append("inherited : ");
                sb.append(this.inherited);
                sb.append('}');
            }
        }

        if (this.direct != null) {
            String directString = this.direct.toString();
            if (directString.length() > 0) {
                sb.append('{');
                sb.append("direct : ");
                sb.append(this.direct);
                sb.append('}');
            }
        }

        return sb.toString();
    }
}
