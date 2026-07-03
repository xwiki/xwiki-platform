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

import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Likers page object.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
public class LikersPage extends ViewPage
{
    /**
     * Go to the likers page of a given page.
     *
     * @param documentReference the document reference of the page for which likers are shown
     * @return the likers page object for the given document reference
     */
    public static LikersPage goToLikers(DocumentReference documentReference)
    {
        getUtil().gotoPage(documentReference, "view", Map.of("viewer", "likers"));
        return new LikersPage();
    }

    /**
     * @return the likers Live Data element
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("likersTable");
    }
}
