/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author moghrabix
 */

package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;

public class XWikiDefaultPlugin implements XWikiPluginInterface {
    private String name;
    private String className;

    public XWikiDefaultPlugin(String name, String className, XWikiContext context) {
        setClassName(className);
        setName(name);
    }

    public void init(XWikiContext context) {
    }

    public void virtualInit(XWikiContext context) {
    }

    public void flushCache() {
    }

    public String commonTagsHandler(String line, XWikiContext context) {
        return line;
    }

    public String startRenderingHandler(String line, XWikiContext context) {
        return line;
    }

    public String outsidePREHandler(String line, XWikiContext context) {
        return line;
    }

    public String insidePREHandler(String line, XWikiContext context) {
        return line;
    }

    public String endRenderingHandler(String line, XWikiContext context) {
        return line;
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!name.equals(className))
         this.name = name;
    }

    public String getClassName() {
        return name;
    }

    public void setClassName(String name) {
        this.name = name;
    }

    public void beginRendering(XWikiContext context) {
    }

    public void endRendering(XWikiContext context) {
    }
    
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context) {
    	return attachment;
    }
}
