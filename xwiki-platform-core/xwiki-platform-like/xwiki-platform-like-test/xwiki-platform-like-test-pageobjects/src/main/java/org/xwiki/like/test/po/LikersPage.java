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
package org.xwiki.like.test.po;

import java.util.Map;

import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Provide the operations to go to the likers page, and to access the likers UI elements.
 *
 * @version $Id$
 * @since 15.2RC1
 */
public final class LikersPage extends BaseElement
{
    private static final String LIKERS = "likers";

    private TableLayoutElement likersLiveData;

    private LikersPage()
    {
    }

    /**
     * Access to the likers of a given page.
     *
     * @param documentReference the document reference of a page
     * @return the corresponding page object
     */
    public static LikersPage goToLikers(DocumentReference documentReference)
    {
        getUtil().gotoPage(documentReference, "view", Map.of("viewer", LIKERS));
        return new LikersPage();
    }

    /**
     * @return the numbers of currently displayed likers
     */
    public int countDisplayedLikers()
    {
        return getLiveData().countRows();
    }

    /**
     * @param index the index of a liker in the Live Data (the first one is index 1)
     * @return the username of the liker at the given index
     */
    public String getLikerUsername(int index)
    {
        return getLiveData().getCell("User", index).getText();
    }

    private TableLayoutElement getLiveData()
    {
        if (this.likersLiveData == null) {
            this.likersLiveData = new TableLayoutElement(LIKERS);
        }
        return this.likersLiveData;
    }
}
