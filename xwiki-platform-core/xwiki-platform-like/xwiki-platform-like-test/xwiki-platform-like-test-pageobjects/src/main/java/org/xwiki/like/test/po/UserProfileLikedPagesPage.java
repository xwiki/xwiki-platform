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

import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.BaseElement;

import static java.util.Collections.singletonMap;

/**
 * Provides the operations to interact with the Liked Pages tab of a user profile.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class UserProfileLikedPagesPage extends BaseElement
{
    /**
     * The Title column name.
     */
    public static final String TITLE_COLUMN_NAME = "Title";

    /**
     * The Location column name.
     */
    public static final String LOCATION_COLUMN_NAME = "Location";

    /**
     * The Likes column name.
     */
    public static final String LIKES_COLUMN_NAME = "Likes";

    private final String userName;

    /**
     * Default constructor.
     *
     * @param userName a user name, for instance {@code User1}
     */
    public UserProfileLikedPagesPage(String userName)
    {
        this.userName = userName;
    }

    /**
     * Go to the Liked Pages tab of the user profile of the user passed in the constructor.
     */
    public void gotoPage()
    {
        getUtil().gotoPage(new DocumentReference("xwiki", "XWiki", this.userName), "view",
            singletonMap("category", "org.xwiki.platform.like.userProfileMenu"));
    }

    /**
     * @return the page object of the Liked Pages tab live data
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("likedPages");
    }
}
