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
package org.xwiki.test.ui.po;

/**
 * The page that shows the differences between two versions of a document.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ComparePage extends ViewPage
{
    /**
     * The changes pane.
     */
    private ChangesPane changesPane = new ChangesPane();

    /**
     * @return the pane that displays all the differences between the selected versions
     */
    public ChangesPane getChangesPane()
    {
        return changesPane;
    }
}
