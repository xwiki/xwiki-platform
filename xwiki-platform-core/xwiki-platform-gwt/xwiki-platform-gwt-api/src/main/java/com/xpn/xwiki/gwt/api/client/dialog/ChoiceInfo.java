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
package com.xpn.xwiki.gwt.api.client.dialog;

import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;

public class ChoiceInfo {
    private String name;
    private String title;
    private String description;
    private String cSSName;
    private String imageURL;

    public ChoiceInfo(XWikiGWTApp app, String dialogTranslationName, String name) {
        setName(name);
        setCSSName(name);
        setImageURL(app.getSkinFile(dialogTranslationName + "-" + name + ".png"));
        setTitle(app.getTranslation(dialogTranslationName + ".button." + name + ".title"));
        setTitle(app.getTranslation(dialogTranslationName + ".button." + name + ".description"));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCSSName() {
        return cSSName;
    }

    public void setCSSName(String cssName) {
        this.cSSName = cssName;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
