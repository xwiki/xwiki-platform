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
package com.xpn.xwiki.wysiwyg.client.ui.wrap;

import java.util.ArrayList;
import java.util.List;

public class IFrameConfig
{
    private List<String> styleSheetURLs = new ArrayList<String>();

    /**
     * FIXME: right now scripts are added in the head of in line frame's document, but they are not evaluated.
     */
    private List<String> scriptURLs = new ArrayList<String>();

    private List<String> bodyStyleNames = new ArrayList<String>();

    private String bodyId;

    public String[] getStyleSheetURLs()
    {
        return styleSheetURLs.toArray(new String[styleSheetURLs.size()]);
    }

    public void addStyleSheet(String styleSheetURL)
    {
        this.styleSheetURLs.add(styleSheetURL);
    }

    public String[] getScriptURLs()
    {
        return scriptURLs.toArray(new String[scriptURLs.size()]);
    }

    public void addScript(String scriptURL)
    {
        this.scriptURLs.add(scriptURL);
    }

    public String getBodyClassName()
    {
        StringBuffer className = new StringBuffer();
        for (String styleName : bodyStyleNames) {
            className.append(" ");
            className.append(styleName);
        }
        return className.toString().trim();
    }

    public void addBodyStyleName(String styleName)
    {
        this.bodyStyleNames.add(styleName);
    }

    public String getBodyId()
    {
        return bodyId;
    }

    public void setBodyId(String bodyId)
    {
        this.bodyId = bodyId;
    }
}
