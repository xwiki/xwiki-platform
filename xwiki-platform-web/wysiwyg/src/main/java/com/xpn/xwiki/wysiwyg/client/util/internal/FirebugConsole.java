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
package com.xpn.xwiki.wysiwyg.client.util.internal;

import com.xpn.xwiki.wysiwyg.client.util.Console;

/**
 * @see http://getfirebug.com/console.html
 */
public class FirebugConsole extends Console
{
    /**
     * {@inheritDoc}
     * 
     * @see Console#count(String)
     */
    public native void count(String title) /*-{
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
    public native void profile(String title) /*-{
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
    public native void profileEnd() /*-{
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
    public native void time(String name) /*-{
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
    public native void timeEnd(String name) /*-{
        try {
            console.timeEnd(name);
        } catch(e) {
            // ignore
        }
    }-*/;
}
