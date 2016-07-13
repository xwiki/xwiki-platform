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
package org.xwiki.refactoring.splitter;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;

/**
 * Component interface for splitting a {@link WikiDocument} into multiple documents.
 * 
 * @version $Id$
 * @since 1.9M1
 */
@Role
public interface DocumentSplitter
{
    /**
     * Splits the document represented by rootDoc into multiple documents.
     * 
     * @param rootDoc the root {@link WikiDocument} which is being split.
     * @param splittingCriterion the {@link SplittingCriterion} to be used for splitting operation.
     * @param namingCriterion the {@link NamingCriterion} to be used to generate document names.
     * @return a list of documents representing all the resulting documents. Note that rootDoc will be altered by this
     *         method and it will be included in the list of returned documents.
     */
    List<WikiDocument> split(WikiDocument rootDoc, SplittingCriterion splittingCriterion,
        NamingCriterion namingCriterion);
}
