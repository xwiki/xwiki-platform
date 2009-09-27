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

package com.xpn.xwiki.plugin.applicationmanager.core.api;

import java.lang.reflect.Field;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

/**
 * Permit to manipulate XWikiException in velocity code.
 * 
 * @version $Id$
 */
public class XWikiExceptionApi extends Api
{
    /**
     * No error.
     */
    public static final int ERROR_NOERROR = -1;

    /**
     * Error code that is used when requested error code does not exists.
     */
    public static final int ERROR_XWIKI_ERROR_DOES_NOT_EXIST = -2;

    // ///////////////////////////////////////////////////////////:

    /**
     * Managed exception.
     */
    private XWikiException exception;

    /**
     * XWikiExceptionApi constructor.
     * 
     * @param exception the XWiki exception to manage.
     * @param context the XWiki context.
     */
    public XWikiExceptionApi(XWikiException exception, XWikiContext context)
    {
        super(context);
        this.exception = exception;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.exception.getMessage();
    }

    // ///////////////////////////////////////////////////////////:

    /**
     * Get static field error code value. This name targeting velocity to be able to use like
     * "exception.SOME_ERROR_CODE".
     * 
     * @param error the static field name.
     * @return the static field value.
     * @throws XWikiException ERROR_XWIKI_ERROR_DOES_NOT_EXIST No corresponding error code exist.
     */
    public int get(String error) throws XWikiException
    {
        try {
            Field field = getClass().getField(error);

            if (field.getType() == int.class) {
                return ((Integer) field.get(null)).intValue();
            }
        } catch (Exception e) {
            // Error code is not a XWikiExceptionApi field.
        }

        try {
            Field field = this.exception.getClass().getField(error);

            if (field.getType() == int.class) {
                return ((Integer) field.get(null)).intValue();
            }
        } catch (Exception e) {
            // Error when trying to retrieve error code value.
        }

        throw new XWikiException(XWikiException.MODULE_XWIKI,
            ERROR_XWIKI_ERROR_DOES_NOT_EXIST,
            "Error \"" + error + "\" code does not exist");
    }
}
