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
package com.xpn.xwiki.util;

import java.io.Reader;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import com.xpn.xwiki.XWiki;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.util.Util} class.
 * 
 * @version $Id$
 */
public privileged aspect UtilCompatibilityAspect
{
    /**
     * @deprecated use {@link #getUniqueMatches(String, String, int)} instead
     */
    @Deprecated
    public List<String> Util.getMatches(String content, String spattern, int group)
        throws MalformedPatternException
    {
        return this.getUniqueMatches(content, spattern, group);
    }

    /** @deprecated Use {@link org.apache.commons.io.FileUtils#readFileToString(File, String)} */
    @Deprecated
    public static String Util.getFileContent(File file) throws IOException
    {
        return FileUtils.readFileToString(file, XWiki.DEFAULT_ENCODING);
    }

    /** @deprecated Use {@link org.apache.commons.io.IOUtils#toString(Reader)} */
    @Deprecated
    public static String Util.getFileContent(Reader reader) throws IOException
    {
        return IOUtils.toString(reader);
    }

    /** @deprecated Use {@link org.apache.commons.io.FileUtils#readFileToByteArray(File)} */
    @Deprecated
    public static byte[] Util.getFileContentAsBytes(File file) throws IOException
    {
        return FileUtils.readFileToByteArray(file);
    }

    /** @deprecated Use {@link org.apache.commons.io.IOUtils#toByteArray(InputStream)} */
    @Deprecated
    public static byte[] Util.getFileContentAsBytes(InputStream is) throws IOException
    {
        return IOUtils.toByteArray(is);
    }
}
