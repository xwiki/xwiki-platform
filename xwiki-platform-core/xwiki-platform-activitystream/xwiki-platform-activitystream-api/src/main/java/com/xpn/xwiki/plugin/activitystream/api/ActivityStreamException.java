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
package com.xpn.xwiki.plugin.activitystream.api;

import com.xpn.xwiki.XWikiException;

/**
 * Exception to use in the activity stream plugin. It extends {@link XWikiException}.
 * 
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ActivityStreamException extends XWikiException
{
    /**
     * Error code for the activity stream.
     */
    public static final int MODULE_PLUGIN_ACTIVITY_STREAM = 102;

    /**
     * @see XWikiException#XWikiException()
     */
    public ActivityStreamException()
    {
    }

    /**
     * @see XWikiException#XWikiException(int, int, String)
     * @param module source of the exception
     * @param code error code to use
     * @param message message to display
     */
    public ActivityStreamException(int module, int code, String message)
    {
        super(module, code, message);
    }

    /**
     * @see XWikiException#XWikiException(int, int, String, Throwable)
     * @param module source of the exception
     * @param code error code to use
     * @param message message to display
     * @param e wrapped exception
     */
    public ActivityStreamException(int module, int code, String message, Exception e)
    {
        super(module, code, message, e);
    }

    /**
     * @see Exception#Exception(Throwable)
     * @param e wrapped exception
     */
    public ActivityStreamException(XWikiException e)
    {
        setModule(e.getModule());
        setCode(e.getCode());
        setException(e.getException());
        setArgs(e.getArgs());
        setMessage(e.getMessage());
    }
}
