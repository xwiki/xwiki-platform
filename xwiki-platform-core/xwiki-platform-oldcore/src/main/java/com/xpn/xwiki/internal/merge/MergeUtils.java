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

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.qarks.util.files.diff.Diff;
import com.xpn.xwiki.doc.merge.CollisionException;
import com.xpn.xwiki.doc.merge.MergeResult;

/**
 * Provide some 3 ways merging related methods.
 * 
 * @version $Id$
 * @since 4.1M1
 */
public final class MergeUtils
{
    /**
     * Utility class.
     */
    private MergeUtils()
    {

    }

    /**
     * Merge a String.
     * 
     * @param previousStr previous version of the string
     * @param newStr new version of the string
     * @param currentStr current version of the string
     * @param mergeResult the merge report
     * @return the merged string or the provided current string if the merge fail
     */
    // TODO: add support for line merge
    public static String mergeString(String previousStr, String newStr, String currentStr, MergeResult mergeResult)
    {
        if (StringUtils.equals(previousStr, newStr)) {
            // No change so nothing to do
            return currentStr;
        }

        String resultStr;

        if (StringUtils.isEmpty(currentStr)) {
            // The current version is empty
            if (StringUtils.equals(previousStr, currentStr)) {
                // Simply because the previous version was empty too
                resultStr = newStr;
            } else {
                // The current version has been replaced by an empty string
                mergeResult.getErrors().add(
                    new CollisionException("The current value has been replaced by empty string"));
                resultStr = currentStr;
            }
        } else {

            com.qarks.util.files.diff.MergeResult result = Diff.quickMerge(previousStr, newStr, currentStr, false);

            if (result.isConflict()) {
                mergeResult.getErrors().add(
                    new CollisionException(String.format(
                        "Failed to merge with previous string [%s], new string [%s] and current string [%s]",
                        previousStr, newStr, currentStr)));
                resultStr = currentStr;
            } else {
                resultStr = result.getDefaultMergedResult();
                mergeResult.setModified(true);
            }
        }

        return resultStr;
    }

    /**
     * Merge a {@link Collection}.
     * 
     * @param <T> the type of the lists elements
     * @param previousList previous version of the collection
     * @param newList new version of the collection
     * @param currentList current version of the collection to modify
     * @param mergeResult the merge report
     */
    public static <T> void mergeCollection(Collection<T> previousList, Collection<T> newList,
        Collection<T> currentList, MergeResult mergeResult)
    {
        for (T previousElement : previousList) {
            if (!newList.contains(previousElement)) {
                currentList.remove(previousElement);
            }
        }

        for (T newElement : newList) {
            if (!previousList.contains(newElement)) {
                currentList.add(newElement);
            }
        }
    }

    // TODO: add mergeList method which respect better the position of the elements than mergeCollection
}
