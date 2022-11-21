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
package org.xwiki.extension.xar.job.diff;

/**
 * Add a backward compatibility layer to the {@link DocumentUnifiedDiff} class.
 *
 * @version $Id$
 * @since 14.8RC1
 */
public privileged aspect DocumentUnifiedDiffAspect
{
    /**
     * Creates a new instance to hold the differences between the specified document versions.
     *
     * @param previousReference the reference to the previous version of the document
     * @param nextReference the reference to the next version of the document
     * @deprecated now use {@link #DocumentUnifiedDiff(org.xwiki.model.reference.DocumentVersionReference, org.xwiki.model.reference.DocumentVersionReference)}
     */
    @Deprecated(since = "14.8RC1")
    public DocumentUnifiedDiff.new(DocumentVersionReference previousReference, DocumentVersionReference nextReference)
    {
        this((org.xwiki.model.reference.DocumentVersionReference) previousReference,
            (org.xwiki.model.reference.DocumentVersionReference) nextReference);
    }
}