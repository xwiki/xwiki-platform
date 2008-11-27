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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the template used by the rich text area to initialize the edited document.
 * 
 * @version $Id$
 */
public class DocumentTemplate
{
    /**
     * The list of style sheet URLs. For each of them the rich text area will add a style sheet declaration in the
     * edited document.
     */
    private List<String> styleSheetURLs = new ArrayList<String>();

    /**
     * The list of script URLs. For each of them the rich text area will add a script declaration in the edited
     * document.<br/>
     * FIXME: right now scripts are added in the head of in line frame's document, but they are not evaluated.
     */
    private List<String> scriptURLs = new ArrayList<String>();

    /**
     * The list of style names to apply to the body element of the edited document.
     */
    private List<String> bodyStyleNames = new ArrayList<String>();

    /**
     * The id of the body element of the edited document.
     */
    private String bodyId;

    /**
     * The base URL of the edited document.
     */
    private String baseURL;

    /**
     * @return The array of style sheet URLs.
     */
    public String[] getStyleSheetURLs()
    {
        return styleSheetURLs.toArray(new String[styleSheetURLs.size()]);
    }

    /**
     * Adds a style sheet URL to the template. The rich text area will add a style sheet declaration with this URL in
     * the edited document.
     * 
     * @param styleSheetURL A style sheet URL.
     */
    public void addStyleSheet(String styleSheetURL)
    {
        this.styleSheetURLs.add(styleSheetURL);
    }

    /**
     * @return The array of script URLs.
     */
    public String[] getScriptURLs()
    {
        return scriptURLs.toArray(new String[scriptURLs.size()]);
    }

    /**
     * Adds a script URL to this template. The rich text area will add a script declaration with this URL in the edited
     * document.
     * 
     * @param scriptURL A script URL.
     */
    public void addScript(String scriptURL)
    {
        this.scriptURLs.add(scriptURL);
    }

    /**
     * @return The computed class name of the body element from the edited document.
     */
    public String getBodyClassName()
    {
        StringBuffer className = new StringBuffer();
        for (String styleName : bodyStyleNames) {
            className.append(" ");
            className.append(styleName);
        }
        return className.toString().trim();
    }

    /**
     * Adds a new style name that will end up on the body element of the edited document.
     * 
     * @param styleName A style name for the body element of the edited document.
     */
    public void addBodyStyleName(String styleName)
    {
        this.bodyStyleNames.add(styleName);
    }

    /**
     * @return The id of the body element from the edited document.
     */
    public String getBodyId()
    {
        return bodyId;
    }

    /**
     * Sets the id that will be set on the body element of the edited document.
     * 
     * @param bodyId The id to be set on the body element.
     */
    public void setBodyId(String bodyId)
    {
        this.bodyId = bodyId;
    }

    /**
     * @return the base URL for the edited document.
     */
    public String getBaseURL()
    {
        return baseURL;
    }

    /**
     * Sets the base URL for the edited document.
     * 
     * @param baseURL the URL to set
     */
    public void setBaseURL(String baseURL)
    {
        this.baseURL = baseURL;
    }
}
