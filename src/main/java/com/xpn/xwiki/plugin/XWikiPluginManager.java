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
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

public class XWikiPluginManager {
    private Vector plugins = new Vector();
    private Map plugins_classes = new HashMap();

    public XWikiPluginManager() {
    }

    public XWikiPluginManager(String classList, XWikiContext context) {
        String[] classNames = StringUtils.split(classList, " ,");
        addPlugins(classNames, context);
    }

    public XWikiPluginManager(String[] classNames, XWikiContext context) {
        addPlugins(classNames, context);
    }

    public void addPlugin(String name, String className,  XWikiContext context) {
        try {
            Class[] classes = new Class[3];
            classes[0] = String.class;
            classes[1] = String.class;
            classes[2] = context.getClass();
            Object[] args = new Object[3] ;
            args[0] = name;
            args[1] = className;
            args[2] = context;
            XWikiPluginInterface plugin = (XWikiPluginInterface) Class.forName(className).getConstructor(classes).newInstance(args);
            if (plugin!=null) {
                plugins.add(plugin.getName());
                plugins_classes.put(plugin.getName(), plugin);
            }
        } catch (Exception e) {
            // Log an error but do not fail..
            e.printStackTrace();
        }

    }

    public void removePlugin(String className) {
        plugins.remove(className);
        plugins_classes.remove(className);
    }

    public void addPlugins(String[] classNames,  XWikiContext context) {
        for (int i=0;i<classNames.length;i++) {
            addPlugin(classNames[i], classNames[i], context);
        }
    }

    public XWikiPluginInterface getPlugin(String className) {
        return (XWikiPluginInterface) plugins_classes.get(className);
    }

    public Vector getPlugins() {
        return plugins;
    }

    public void setPlugins(Vector plugins) {
        this.plugins = plugins;
    }

    public void virtualInit(XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).virtualInit(context);
            } catch (Exception e)
            {}
        }
    }

    public void flushCache() {
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).flushCache();
            } catch (Exception e)
            {}
        }
    }

    public String commonTagsHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).commonTagsHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String startRenderingHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).startRenderingHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String outsidePREHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).outsidePREHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String insidePREHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).insidePREHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String endRenderingHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).endRenderingHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public void beginRendering(XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).beginRendering(context);
            } catch (Exception e)
            {}
        }
    }

    public void endRendering(XWikiContext context) {
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).endRendering(context);
            } catch (Exception e)
            {}
        }
    }

    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context) {
	for (int i=0;i<plugins.size();i++) {
	    try {
	    } catch (Exception e)
	    {}
	}
	return attachment;
    }

}
