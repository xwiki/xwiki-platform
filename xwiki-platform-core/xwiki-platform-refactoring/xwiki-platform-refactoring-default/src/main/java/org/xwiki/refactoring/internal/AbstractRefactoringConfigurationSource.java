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
package org.xwiki.refactoring.internal;

import java.util.Arrays;
import java.util.List;

import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Provides configuration from the {@code Refactoring.Code.RefactoringConfiguration} document.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public abstract class AbstractRefactoringConfigurationSource extends AbstractDocumentConfigurationSource
{
    protected static final List<String> SPACE_NAMES = Arrays.asList("Refactoring", "Code");

    protected static final LocalDocumentReference DOCUMENT_REFERENCE =
        new LocalDocumentReference(SPACE_NAMES, "RefactoringConfiguration");

    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(SPACE_NAMES, "RefactoringConfigurationClass");

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return CLASS_REFERENCE;
    }
}
