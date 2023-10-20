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
package org.xwiki.export.pdf.internal.macro;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.internal.macro.toc.AbstractTocTreeBuilderFactory;
import org.xwiki.rendering.internal.macro.toc.TocTreeBuilder;

/**
 * Initialize a {@link TocTreeBuilder} for pdf export.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named("pdf")
public class PdfTocTreeBuilderFactory extends AbstractTocTreeBuilderFactory
{
    @Override
    public TocTreeBuilder build(String resolverHint) throws ComponentLookupException
    {
        return new PDFTocTreeBuilder(getTocBlockFilter(), getTocEntriesResolver(resolverHint), getExtensions());
    }
}
