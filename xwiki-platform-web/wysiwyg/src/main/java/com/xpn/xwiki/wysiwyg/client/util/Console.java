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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.core.client.GWT;

public class Console
{
    private static Console instance;

    public static synchronized Console getInstance()
    {
        if (instance == null) {
            instance = GWT.create(Console.class);
        }
        return instance;
    }

    /**
     * Creates a new timer under the given name. Call {@link #timeEnd(String)} with the same name to stop the timer and
     * print the time elapsed.
     * 
     * @param name The name of the timer.
     */
    public void time(String name)
    {
    }

    /**
     * Stops a timer created by a call to {@link #time(String)} and writes the time elapsed.
     * 
     * @param name The name of the timer.
     */
    public void timeEnd(String name)
    {
    }

    /**
     * Turns on the JavaScript profiler.
     * 
     * @param title The text to be printed in the header of the profile report.
     */
    public void profile(String title)
    {
    }

    /**
     * Turns off the JavaScript profiler and prints its report.
     */
    public void profileEnd()
    {
    }

    /**
     * Writes the number of times that the line of code where count was called was executed.
     * 
     * @param title The message printed in addition to the number of the count.
     */
    public void count(String title)
    {
    }
}
