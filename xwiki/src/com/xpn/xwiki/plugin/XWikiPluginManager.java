/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 21 janv. 2004
 * Time: 10:47:20
 */
package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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

    public void addPlugin(String className,  XWikiContext context) {
        try {
            Class[] classes = new Class[2];
            classes[0] = String.class;
            classes[1] = context.getClass();
            Object[] args = new Object[2] ;
            args[0] = className;
            args[1] = context;
            XWikiPluginInterface plugin = (XWikiPluginInterface) Class.forName(className).getConstructor(classes).newInstance(args);
            if (plugin!=null) {
                plugins.add(className);
                plugins_classes.put(className, plugin);
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
            addPlugin(classNames[i], context);
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

    public String commonTagsHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++)
            text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).commonTagsHandler(text,context);
        return text;
    }

    public String startRenderingHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++)
            text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).startRenderingHandler(text,context);
        return text;
    }

    public String outsidePREHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++)
            text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).outsidePREHandler(text,context);
        return text;
    }

    public String insidePREHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++)
            text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).insidePREHandler(text,context);
        return text;
    }

    public String endRenderingHandler(String text, XWikiContext context) {
        for (int i=0;i<plugins.size();i++)
            text = ((XWikiPluginInterface)plugins_classes.get(plugins.get(i))).endRenderingHandler(text,context);
        return text;
    }

}