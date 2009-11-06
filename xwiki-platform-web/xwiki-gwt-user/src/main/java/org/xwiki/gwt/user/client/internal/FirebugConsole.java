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
package org.xwiki.gwt.user.client.internal;

import org.xwiki.gwt.user.client.Console;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Node;

/**
 * @see http://getfirebug.com/console.html
 * @version $Id$
 */
public class FirebugConsole extends Console
{
    /**
     * {@inheritDoc}
     * 
     * @see Console#count(String)
     */
    public native void count(String title)
    /*-{
        try {
            console.count(title);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#profile(String)
     */
    public native void profile(String title)
    /*-{
        try {
            console.profile(title);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#profileEnd()
     */
    public native void profileEnd()
    /*-{
        try {
            console.profileEnd();
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#time(String)
     */
    public native void time(String name)
    /*-{
        try {
            console.time(name);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#timeEnd(String)
     */
    public native void timeEnd(String name)
    /*-{
        try {
            console.timeEnd(name);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * Creates a JavaScript array and fills it with the objects from a Java array.
     * 
     * @param array The source Java array.
     * @return The created JavaScript array.
     */
    public static JsArray<JavaScriptObject> toJSArray(Object[] array)
    {
        JsArray<JavaScriptObject> jsArray = JavaScriptObject.createArray().cast();
        for (int i = 0; i < array.length; i++) {
            add(jsArray, array[i]);
        }
        return jsArray;
    }

    /**
     * Adds a Java object to a JavaScript array.
     * 
     * @param jsArray A JavaScript array.
     * @param object A Java object.
     */
    public static native void add(JsArray<JavaScriptObject> jsArray, Object object)
    /*-{
        jsArray.push(object);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#assertTrue(boolean, Object...)
     */
    public native void assertTrue(boolean expression, Object... objects)
    /*-{
        try {
        var args = @org.xwiki.gwt.user.client.internal.FirebugConsole::toJSArray([Ljava/lang/Object;)(objects);
            args.splice(0, 0, expression);
            console.assertTrue.apply(console, args);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#debug(Object, Object...)
     */
    public native void debug(Object object, Object... objects)
    /*-{
        try {
        var args = @org.xwiki.gwt.user.client.internal.FirebugConsole::toJSArray([Ljava/lang/Object;)(objects);
            args.splice(0, 0, object);
            console.debug.apply(console, args);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#dir(Object)
     */
    public native void dir(Object object)
    /*-{
        try {
            console.dir(object);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#dirxml(Node)
     */
    public native void dirxml(Node node)
    /*-{
        try {
            console.dirxml(node);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#error(Object, Object...)
     */
    public native void error(Object object, Object... objects)
    /*-{
        try {
        var args = @org.xwiki.gwt.user.client.internal.FirebugConsole::toJSArray([Ljava/lang/Object;)(objects);
            args.splice(0, 0, object);
            console.error.apply(console, args);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#group(Object, Object...)
     */
    public native void group(Object object, Object... objects)
    /*-{
        try {
        var args = @org.xwiki.gwt.user.client.internal.FirebugConsole::toJSArray([Ljava/lang/Object;)(objects);
            args.splice(0, 0, object);
            console.group.apply(console, args);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#groupEnd()
     */
    public native void groupEnd()
    /*-{
        try {
            console.groupEnd();
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#info(Object, Object...)
     */
    public native void info(Object object, Object... objects)
    /*-{
        try {
        var args = @org.xwiki.gwt.user.client.internal.FirebugConsole::toJSArray([Ljava/lang/Object;)(objects);
            args.splice(0, 0, object);
            console.info.apply(console, args);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#log(Object, Object...)
     */
    public native void log(Object object, Object... objects)
    /*-{
        try {
        var args = @org.xwiki.gwt.user.client.internal.FirebugConsole::toJSArray([Ljava/lang/Object;)(objects);
            args.splice(0, 0, object);
            console.log.apply(console, args);
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#trace()
     */
    public native void trace()
    /*-{
        try {
            console.trace();
        } catch(e) {
            // ignore
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Console#warn(Object, Object...)
     */
    public native void warn(Object object, Object... objects)
    /*-{
        try {
        var args = @org.xwiki.gwt.user.client.internal.FirebugConsole::toJSArray([Ljava/lang/Object;)(objects);
            args.splice(0, 0, object);
            console.warn.apply(console, args);
        } catch(e) {
            // ignore
        }
    }-*/;
}
