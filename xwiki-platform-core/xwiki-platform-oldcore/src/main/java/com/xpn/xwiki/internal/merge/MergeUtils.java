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
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
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
    private static final DiffManager DIFFMANAGER = Utils.getComponent(DiffManager.class);

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
            result = DIFFMANAGER.merge(toLines(previousStr), toLines(newStr), toLines(currentStr), null);

            mergeResult.getLog().addAll(result.getLog());

            String resultStr = fromLines(result.getMerged());

            if (!StringUtils.equals(resultStr, currentStr)) {
                mergeResult.setModified(true);
            }

            return resultStr;
        } catch (MergeException e) {
            mergeResult.getLog().error("Failed to execute merge lines", e);
        }

        return currentStr;
    }

    /**
     * Merge an Object. Use Object#equals to find conflicts.
     * 
     * @param previousObject previous version of the object
     * @param newObject new version of the object
     * @param currentObject current version of the object
     * @param mergeResult the merge report
     * @param <T> the type of the objects to merge
     * @return the merged object or the provided current object if the merge fail
     */
    public static <T> T mergeOject(T previousObject, T newObject, T currentObject, MergeResult mergeResult)
    {
        if (ObjectUtils.notEqual(previousObject, newObject)) {
            if (ObjectUtils.equals(previousObject, currentObject)) {
                mergeResult.setModified(true);
                return newObject;
            } else if (ObjectUtils.equals(newObject, currentObject)) {
                return currentObject;
            }

            mergeResult.getLog().error("Failed to merge objects: previous=[{}] new=[{}] current=[{}]", previousObject,
                newObject, currentObject);
        }

        return currentObject;
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
            result = DIFFMANAGER.merge(toCharacters(previousStr), toCharacters(newStr), toCharacters(currentStr), null);

            mergeResult.getLog().addAll(result.getLog());

            String resultStr = fromCharacters(result.getMerged());

            if (!StringUtils.equals(resultStr, currentStr)) {
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
            result = DIFFMANAGER.merge(commonAncestor, next, current, null);

            mergeResult.getLog().addAll(result.getLog());

            List<T> merged = result.getMerged();
            
            if (!ObjectUtils.equals(merged, current)) {
                current.clear();
                current.addAll(result.getMerged());
                mergeResult.setModified(true);
            }
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
        List<String> result;
        try {
            result = IOUtils.readLines(new StringReader(str));

            // Handle special case where the string ends with a new line
            if (str.endsWith("\n") || str.endsWith("\r") || str.endsWith("\r\n")) {
                result.add("");
            }

        } catch (IOException e) {
            // Should never happen
            result = null;
        }
        return result;
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
        List<Character> characters;

        if (str != null) {
            characters = new ArrayList<Character>(str.length());

            for (char c : str.toCharArray()) {
                characters.add(c);
            }
        } else {
            characters = Collections.emptyList();
        }

        return characters;
    }
}
