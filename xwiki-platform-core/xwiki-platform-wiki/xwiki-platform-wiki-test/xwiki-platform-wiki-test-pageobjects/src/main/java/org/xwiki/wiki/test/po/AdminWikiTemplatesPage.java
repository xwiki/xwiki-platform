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
package org.xwiki.wiki.test.po;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Provides the operations to interact with the Wiki Templates administration page.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class AdminWikiTemplatesPage extends ViewPage
{
    /**
     * Goes to the Wiki Template administration page.
     *
     * @return a Wiki Template administration page object
     */
    public static AdminWikiTemplatesPage goToPage()
    {
        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("editor", "globaladmin");
        queryParameters.put("section", "wikis.templates");
        getUtil().gotoPage(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"), "admin", queryParameters);
        return new AdminWikiTemplatesPage();
    }

    /**
     * @return the live date of the Wiki Template administration page
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("wikis");
    }
}
