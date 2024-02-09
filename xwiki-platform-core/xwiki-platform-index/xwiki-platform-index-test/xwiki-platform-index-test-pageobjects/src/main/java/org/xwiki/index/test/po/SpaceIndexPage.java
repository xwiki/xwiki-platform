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
package org.xwiki.index.test.po;

import java.util.Collections;

import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the Main.SpaceIndex page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class SpaceIndexPage extends ViewPage
{
    /**
     * The live table that lists the documents from the space.
     */
    private final LiveDataElement liveDataElement = new LiveDataElement("spaceindex");

    /**
     * Opens the document index for the specified page.
     * 
     * @param spaceName the name of the space to view the index of
     * @return the space index page listing document from the specified space
     */
    public static SpaceIndexPage gotoPage(String spaceName)
    {
        getUtil().gotoPage("Main", "SpaceIndex", "view", Collections.singletonMap("space", spaceName));
        return new SpaceIndexPage();
    }

    /**
     * @return the document index live table
     */
    public LiveDataElement getLiveData()
    {
        return this.liveDataElement;
    }
}
