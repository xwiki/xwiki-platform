/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
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
 * @author Christian Gmeiner
 */
package com.xpn.xwiki.wysiwyg.server.converter.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is our own implementation of a StringInputStream, based on InputStream.
 * 
 * @version $Id: $
 */
public class StringInputStream extends InputStream
{

    /**
     * String,, which is used as source for this InputStream.
     */
    private String data;

    /**
     * aviable chars in this InputStream.
     */
    private int aviable;

    /**
     * @param in String which is used for this InputStream
     */
    public StringInputStream(String in)
    {
        data = in;
        aviable = in.length();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException
    {
        // look if there is data aviable
        if (aviable == 0) {
            return -1;
        }

        // calculate new index
        int index = data.length() - aviable;

        aviable--;

        // return char at index
        return data.charAt(index);
    }

}
