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

import junit.framework.TestCase;
import com.xpn.xwiki.XWikiException;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi}.
 * 
 * @version $Id$
 */
public class XWikiExceptionApiTest extends TestCase
{
    /**
     * We only verify here we get the correct error code for some static error field name.
     * 
     * @throws XWikiException error occurred.
     */
    public void testGetStaticErrorCode() throws XWikiException
    {
        XWikiExceptionApi exceptionapi = new XWikiExceptionApi(new XWikiException(), null);

        assertEquals("Error code is incorrect", XWikiException.ERROR_XWIKI_UNKNOWN, exceptionapi
            .get("ERROR_XWIKI_UNKNOWN"));
        assertEquals("Error code is incorrect", XWikiException.ERROR_XWIKI_EXPORT_XSL_FILE_NOT_FOUND, exceptionapi
            .get("ERROR_XWIKI_EXPORT_XSL_FILE_NOT_FOUND"));
        assertEquals("Error code is incorrect", XWikiException.ERROR_XWIKI_CONTENT_LINK_INVALID_URI, exceptionapi
            .get("ERROR_XWIKI_CONTENT_LINK_INVALID_URI"));
    }

    /**
     * Try to get error code that does not exists.
     */
    public void testCantFindErrorCode()
    {
        XWikiExceptionApi exceptionapi = new XWikiExceptionApi(new XWikiException(), null);

        try {
            exceptionapi.get("wrongerrorname");
            fail("XWikiExceptionApi.get(String) did not throwed exception with \"wrongerrorname\" " + "error name.");
        } catch (XWikiException e) {
            assertEquals("Wrong exception throwed.", XWikiExceptionApi.ERROR_XWIKI_ERROR_DOES_NOT_EXIST, e.getCode());
        }
    }

    /**
     * Example of XWikiException overload.
     */
    public class SomeExtendedException extends XWikiException
    {
        public static final int ERROR_EXTENDED_ERROR1 = 80001;

        public static final int ERROR_EXTENDED_ERROR2 = 80002;
    }

    /**
     * Example of XWikiException overload.
     */
    public class SomeOtherExtendedException extends XWikiException
    {
        public static final int ERROR_EXTENDED_ERROR1 = 90001;

        public static final int ERROR_EXTENDED_ERROR2 = 90002;
    }

    /**
     * We only verify here we get the correct error code for some static error field name.
     * 
     * @throws XWikiException error occurred.
     */
    public void testGetExtendedStaticErrorCode() throws XWikiException
    {
        XWikiExceptionApi exceptionapi1 = new XWikiExceptionApi(new SomeExtendedException(), null);

        assertEquals("Error code is incorrect", SomeExtendedException.ERROR_EXTENDED_ERROR1, exceptionapi1
            .get("ERROR_EXTENDED_ERROR1"));
        assertEquals("Error code is incorrect", SomeExtendedException.ERROR_EXTENDED_ERROR2, exceptionapi1
            .get("ERROR_EXTENDED_ERROR2"));

        XWikiExceptionApi exceptionapi2 = new XWikiExceptionApi(new SomeOtherExtendedException(), null);

        assertEquals("Error code is incorrect", SomeOtherExtendedException.ERROR_EXTENDED_ERROR1, exceptionapi2
            .get("ERROR_EXTENDED_ERROR1"));
        assertEquals("Error code is incorrect", SomeOtherExtendedException.ERROR_EXTENDED_ERROR2, exceptionapi2
            .get("ERROR_EXTENDED_ERROR2"));
    }
}
