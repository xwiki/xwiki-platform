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
package org.xwiki.refactoring.job;

import java.util.List;
import java.util.Map;

import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 */
public class ExtensionBreakingQuestion
{
    private Map<XarInstalledExtension, List<DocumentReference>> brokenExtensions;

    private boolean confirm = false;

    public ExtensionBreakingQuestion(
            Map<XarInstalledExtension, List<DocumentReference>> brokenExtensions)
    {
        this.brokenExtensions = brokenExtensions;
    }

    public Map<XarInstalledExtension, List<DocumentReference>> getBrokenExtensions()
    {
        return brokenExtensions;
    }

    public boolean isConfirm()
    {
        return confirm;
    }

    public void setConfirm(boolean confirm)
    {
        this.confirm = confirm;
    }
}
