/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.activitystream.api;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id: $
 */
public class ActivityStreamException extends XWikiException
{
    public static final int MODULE_PLUGIN_ACTIVITY_STREAM = 102;

    /**
     * @see XWikiException#XWikiException()
     */
    public ActivityStreamException()
    {
    }

    /**
     * @see XWikiException#XWikiException(int, int, String)
     */
    public ActivityStreamException(int module, int code, String message)
    {
        super(module, code, message);
    }

    /**
     * @see XWikiException#XWikiException(int, int, String, Throwable)
     */
    public ActivityStreamException(int module, int code, String message, Exception e)
    {
        super(module, code, message, e);
    }

    /**
     * @see Exception#Exception(Throwable)
     */
    public ActivityStreamException(XWikiException e)
    {
        super();
        setModule(e.getModule());
        setCode(e.getCode());
        setException(e.getException());
        setArgs(e.getArgs());
        setMessage(e.getMessage());
    }
}
