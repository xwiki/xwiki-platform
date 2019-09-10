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
package org.xwiki.store.merge;

import java.util.List;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Provides general merge utility methods.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@Role
@Unstable
public interface MergeManager
{
    /**
     * Perform a 3-way merge between the given strings by first splitting them into lines.
     * The comparison is performed line by line.
     *
     * @param previousStr the previous string.
     * @param newStr the new string.
     * @param currentStr the current string.
     * @param configuration the configuration for the merge operation.
     * @return the obtained merged string.
     */
    MergeManagerResult<String, String> mergeLines(String previousStr, String newStr, String currentStr,
        MergeConfiguration configuration);

    /**
     * Perform a 3-way merge between the given objects.
     *
     * @param previousObject the previous object.
     * @param newObject the new object.
     * @param currentObject the current object.
     * @param configuration the configuration for the merge operation.
     * @param <T> the type of the object
     * @return an obtained merged object.
     */
    <T> MergeManagerResult<T, T> mergeObject(T previousObject, T newObject, T currentObject,
        MergeConfiguration configuration);

    /**
     * Perform a 3-way merge between the given strings by first splitting them into arrays of characters.
     * The comparison is performed character by character.
     *
     * @param previousStr the previous string.
     * @param newStr the new string.
     * @param currentStr the current string.
     * @param configuration the configuration for the merge operation.
     * @return the obtained merged string.
     */
    MergeManagerResult<String, Character> mergeCharacters(String previousStr, String newStr, String currentStr,
        MergeConfiguration configuration);

    /**
     * Perform a 3-way merge between the list of elements.
     * The current list is modified during the operation.
     *
     * @param commonAncestor the previous list.
     * @param next the next list.
     * @param current the current list.
     * @param configuration the configuration for the merge operation.
     * @param <T> the type of elements.
     * @return a merge result with the resulting list.
     */
    <T> MergeManagerResult<List<T>, T> mergeList(List<T> commonAncestor, List<T> next, List<T> current,
        MergeConfiguration configuration);

    /**
     * Perform a 3-way merge between documents.
     * Note that if {@link MergeConfiguration#isProvidedVersionsModifiables()} is {@code true} then the current document
     * is cloned before the merge operation, else the given current document is directly modified for performance.
     *
     * @param previousDoc the previous document.
     * @param nextDoc the next document.
     * @param currentDoc the current document.
     * @param mergeConfiguration the configuration.
     * @return a merge result with the resulting document.
     */
    MergeDocumentResult mergeDocument(DocumentModelBridge previousDoc, DocumentModelBridge nextDoc,
        DocumentModelBridge currentDoc, MergeConfiguration mergeConfiguration);
}
