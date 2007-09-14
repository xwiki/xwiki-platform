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
 *
 */

package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class XWikiPluginManager {
    private Vector plugins = new Vector();
    private Map plugins_classes = new HashMap();
    private Map functionList = new HashMap();



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
            Class pluginClass = Class.forName(className);
            XWikiPluginInterface plugin = (XWikiPluginInterface) pluginClass.getConstructor(classes).newInstance(args);
            if (plugin!=null) {
                plugins.add(plugin.getName());
                plugins_classes.put(plugin.getName(), plugin);
                initPlugin(plugin, pluginClass, context);
            }
        } catch (Exception e) {
            // Log an error but do not fail..
            e.printStackTrace();
        }

    }

    public void removePlugin(String className) {
        plugins.remove(className);
        Object plugin = plugins_classes.get(className);
        plugins_classes.remove(className);

        Iterator it = functionList.keySet().iterator();
        while (it.hasNext())
        {
            String name = (String) it.next();
            Vector pluginList = (Vector) functionList.get(name);
            pluginList.remove(plugin);
        }
    }

    public void addPlugins(String[] classNames,  XWikiContext context) {
        initInterface();
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

    public void initInterface(){
        Method[] methods = XWikiPluginInterface.class.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            String name = method.getName();
            functionList.put(name, new Vector());
        }
    }
    
    public void initPlugin(Object plugin, Class pluginClass, XWikiContext context) throws XWikiException {
        Method[] methods = pluginClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            String name = method.getName();
            if (functionList.containsKey(name))
                ((Vector)functionList.get(name)).add(plugin);
        }
        ((XWikiPluginInterface)plugin).init(context);
    }

    public Vector getPlugins(String functionName){
        if (functionList.containsKey(functionName)){
            return (Vector) functionList.get(functionName);
        }
        return null;
    }

    public void virtualInit(XWikiContext context) {
        Vector plugins = getPlugins("virtualInit");
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins.get(i)).virtualInit(context);
            } catch (Exception e)
            {}
        }
    }

    public void flushCache(XWikiContext context) {
        Vector plugins = getPlugins("flushCache");
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins.get(i)).flushCache(context);
            } catch (Exception e)
            {}
        }
    }

    public String commonTagsHandler(String text, XWikiContext context) {
        Vector plugins = getPlugins("commonTagsHandler");
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins.get(i)).commonTagsHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String startRenderingHandler(String text, XWikiContext context) {
        Vector plugins = getPlugins("startRenderingHandler");
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins.get(i)).startRenderingHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String outsidePREHandler(String text, XWikiContext context) {
        Vector plugins = getPlugins("outsidePREHandler");
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins.get(i)).outsidePREHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String insidePREHandler(String text, XWikiContext context) {
        Vector plugins = getPlugins("insidePREHandler");
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins.get(i)).insidePREHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public String endRenderingHandler(String text, XWikiContext context) {
        Vector plugins = getPlugins("endRenderingHandler");
        for (int i=0;i<plugins.size();i++) {
            try {
                text = ((XWikiPluginInterface)plugins.get(i)).endRenderingHandler(text,context);
            } catch (Exception e)
            {}
        }
        return text;
    }

    public void beginRendering(XWikiContext context) {
        Vector plugins = getPlugins("beginRendering");
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins.get(i)).beginRendering(context);
            } catch (Exception e)
            {}
        }
    }

    public void endRendering(XWikiContext context) {
        Vector plugins = getPlugins("endRendering");
        for (int i=0;i<plugins.size();i++) {
            try {
                ((XWikiPluginInterface)plugins.get(i)).endRendering(context);
            } catch (Exception e)
            {}
        }
    }

    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context) {
        Vector plugins = getPlugins("downloadAttachment");
        XWikiAttachment attach = attachment;
        for (int i=0;i<plugins.size();i++) {
            try {
                attach = ((XWikiPluginInterface)plugins.get(i)).downloadAttachment(attach, context);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return attach;
    }
}
