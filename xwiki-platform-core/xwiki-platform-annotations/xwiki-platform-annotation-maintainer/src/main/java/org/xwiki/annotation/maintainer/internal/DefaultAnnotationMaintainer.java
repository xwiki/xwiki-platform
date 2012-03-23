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
package org.xwiki.annotation.maintainer.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.annotation.maintainer.AbstractAnnotationMaintainer;
import org.xwiki.annotation.maintainer.DiffService;
import org.xwiki.component.annotation.Component;

/**
 * Default implementation of the annotation maintainer, using a default implementation of the diff service.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultAnnotationMaintainer extends AbstractAnnotationMaintainer
{
    /**
     * Diff service to get the differences between content.
     */
    @Inject
    private DiffService diffService;

    @Override
    public DiffService getDiffService()
    {
        return diffService;
    }
}
