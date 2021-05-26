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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.oro.text.PatternCache;
import org.apache.oro.text.PatternCacheLRU;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.web.Utils;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.util.Util} class.
 * 
 * @version $Id$
 */
public privileged aspect UtilCompatibilityAspect
{
    private Perl5Matcher Util.matcher = new Perl5Matcher();

    private Perl5Util Util.p5util = new Perl5Util(getPatterns());

    private static PatternCache Util.patterns = new PatternCacheLRU(200);

    /**
     * @deprecated use standard Java pattern matching instead
     */
    @Deprecated
    public String substitute(String pattern, String substitution, String text)
    {
        WikiSubstitution subst = new WikiSubstitution(this, pattern);
        subst.setSubstitution(substitution);
        return subst.substitute(text);
    }

    /**
     * @deprecated use standard Java pattern matching instead
     */
    @Deprecated
    public static PatternCache Util.getPatterns()
    {
        return Util.patterns;
    }

    /**
     * @deprecated use {@link String#split(String)} instead
     */
    @Deprecated
    public Vector<String> Util.split(String pattern, String text)
    {
        Vector<String> results = new Vector<String>();
        Util.getP5util().split(results, pattern, text);

        return results;
    }

    /**
     * @deprecated use standard Java pattern matching instead
     */
    @Deprecated
    public String Util.substitute(String pattern, String text)
    {
        return Util.getP5util().substitute(pattern, text);
    }

    /**
     * @deprecated use standard Java pattern matching instead
     */
    @Deprecated
    public boolean Util.match(String pattern, String text)
    {
        return Util.getP5util().match(pattern, text);
    }

    /**
     * @deprecated use standard Java pattern matching instead
     */
    @Deprecated
    public boolean Util.matched()
    {
        return (Util.getP5util().getMatch() != null);
    }

    /**
     * @deprecated use standard Java pattern matching instead
     */
    @Deprecated
    public Perl5Matcher Util.getMatcher()
    {
        return Util.matcher;
    }

    /**
     * @deprecated use standard Java pattern matching instead
     */
    @Deprecated
    public Perl5Util Util.getP5util()
    {
        return Util.p5util;
    }

    /**
     * @deprecated use {@link #getUniqueMatches(String, String, int)} instead
     */
    @Deprecated
    public List<String> Util.getMatches(String content, String spattern, int group)
        throws MalformedPatternException
    {
        return Util.getUniqueMatches(content, spattern, group);
    }

    /** @deprecated Use {@link org.apache.commons.io.FileUtils#readFileToString(File, String)} */
    @Deprecated
    public static String Util.getFileContent(File file) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return FileUtils.readFileToString(file, XWiki.DEFAULT_ENCODING);
    }

    /** @deprecated Use {@link org.apache.commons.io.IOUtils#toString(Reader)} */
    @Deprecated
    public static String Util.getFileContent(Reader reader) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return IOUtils.toString(reader);
    }

    /** @deprecated Use {@link org.apache.commons.io.FileUtils#readFileToByteArray(File)} */
    @Deprecated
    public static byte[] Util.getFileContentAsBytes(File file) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return FileUtils.readFileToByteArray(file);
    }

    /** @deprecated Use {@link org.apache.commons.io.IOUtils#toByteArray(InputStream)} */
    @Deprecated
    public static byte[] Util.getFileContentAsBytes(InputStream is) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return IOUtils.toByteArray(is);
    }
}
