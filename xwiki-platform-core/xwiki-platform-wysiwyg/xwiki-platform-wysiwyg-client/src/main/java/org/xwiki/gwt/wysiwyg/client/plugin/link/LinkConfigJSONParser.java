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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable.ConfigJSONParser;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

/**
 * Creates {@link LinkConfig} instances from JSON.
 * 
 * @version $Id$
 */
public class LinkConfigJSONParser implements ConfigJSONParser<LinkConfig>
{
    @Override
    public LinkConfig parse(String json)
    {
        JavaScriptObject jsObj = JavaScriptObject.fromJson(json);
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.setReference((String) jsObj.get("reference"));
        linkConfig.setUrl((String) jsObj.get("url"));
        linkConfig.setLabel((String) jsObj.get("label"));
        linkConfig.setLabelText((String) jsObj.get("labelText"));
        linkConfig.setReadOnlyLabel(Boolean.valueOf((String) jsObj.get("readOnlyLabel")));
        linkConfig.setOpenInNewWindow(Boolean.valueOf((String) jsObj.get("openInNewWindow")));
        linkConfig.setTooltip((String) jsObj.get("tooltip"));
        String type = (String) jsObj.get("type");
        if (type != null) {
            linkConfig.setType(LinkType.valueOf(type));
        }
        return linkConfig;
    }
}
