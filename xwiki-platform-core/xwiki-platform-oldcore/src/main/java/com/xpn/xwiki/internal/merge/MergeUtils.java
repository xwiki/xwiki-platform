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
package com.xpn.xwiki.internal.merge;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.diff.DiffManager;
import org.xwiki.diff.MergeException;

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.web.Utils;

/**
 * Provide some 3 ways merging related methods.
 * 
 * @version $Id$
 * @since 4.1M1
 */
public final class MergeUtils
{
    /**
     * Used to do the actual merge.
     */
    private static DiffManager diffManager = Utils.getComponent(DiffManager.class);

    /**
     * Utility class.
     */
    private MergeUtils()
    {

    }

    /**
     * Merge String at lines level.
     * 
     * @param previousStr previous version of the string
     * @param newStr new version of the string
     * @param currentStr current version of the string
     * @param mergeResult the merge report
     * @return the merged string or the provided current string if the merge fail
     */
    public static String mergeLines(String previousStr, String newStr, String currentStr, MergeResult mergeResult)
    {
        org.xwiki.diff.MergeResult<String> result;
        try {
            result = diffManager.merge(toLines(previousStr), toLines(currentStr), toLines(newStr), null);

            mergeResult.getLog().addAll(result.getLog());

            String resultStr = fromLines(result.getMerged());

            if (StringUtils.equals(resultStr, currentStr)) {
                mergeResult.setModified(true);
            }

            return resultStr;
        } catch (MergeException e) {
            mergeResult.getLog().error("Failed to execute merge lines", e);
        }

        return currentStr;
    }

    /**
     * Merge String at characters level.
     * 
     * @param previousStr previous version of the string
     * @param newStr new version of the string
     * @param currentStr current version of the string
     * @param mergeResult the merge report
     * @return the merged string or the provided current string if the merge fail
     */
    public static String mergeCharacters(String previousStr, String newStr, String currentStr, MergeResult mergeResult)
    {
        org.xwiki.diff.MergeResult<Character> result;
        try {
            result = diffManager.merge(toCharacters(previousStr), toCharacters(currentStr), toCharacters(newStr), null);

            mergeResult.getLog().addAll(result.getLog());

            String resultStr = fromCharacters(result.getMerged());

            if (StringUtils.equals(resultStr, currentStr)) {
                mergeResult.setModified(true);
            }

            return resultStr;
        } catch (MergeException e) {
            mergeResult.getLog().error("Failed to execute merge characters", e);
        }

        return currentStr;
    }

    /**
     * Merge a {@link List}.
     * 
     * @param <T> the type of the lists elements
     * @param commonAncestor previous version of the collection
     * @param next new version of the collection
     * @param current current version of the collection to modify
     * @param mergeResult the merge report
     */
    public static <T> void mergeList(List<T> commonAncestor, List<T> next, List<T> current, MergeResult mergeResult)
    {
        org.xwiki.diff.MergeResult<T> result;
        try {
            result = diffManager.merge(commonAncestor, current, next, null);

            current.clear();
            current.addAll(result.getMerged());
        } catch (MergeException e) {
            mergeResult.getLog().error("Failed to execute merge lists", e);
        }
    }

    /**
     * @param lines the lines
     * @return the multilines text
     */
    private static String fromLines(List<String> lines)
    {
        return StringUtils.join(lines, '\n');
    }

    /**
     * @param str the multilines text
     * @return the lines
     */
    private static List<String> toLines(String str)
    {
        try {
            return IOUtils.readLines(new StringReader(str));
        } catch (IOException e) {
            // Should never happen
            return null;
        }
    }

    /**
     * @param characters the characters
     * @return the single line text
     */
    private static String fromCharacters(List<Character> characters)
    {
        return StringUtils.join(characters, null);
    }

    /**
     * @param str the single line text
     * @return the lines
     */
    private static List<Character> toCharacters(String str)
    {
        List<Character> characters = new ArrayList<Character>(str.length());

        for (char c : str.toCharArray()) {
            characters.add(c);
        }

        return characters;
    }
}
