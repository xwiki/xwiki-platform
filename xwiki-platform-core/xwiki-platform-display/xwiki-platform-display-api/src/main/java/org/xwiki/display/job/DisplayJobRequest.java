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
package org.xwiki.display.job;

import org.xwiki.display.internal.job.DisplayJob;
import org.xwiki.job.AbstractRequest;
import org.xwiki.rendering.syntax.Syntax;

/**
 * The request of the {@link DisplayJob}.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
public class DisplayJobRequest extends AbstractRequest
{
    private Object source;

    private Syntax targetSyntax;

    public Object getSource()
    {
        return this.source;
    }

    public void setSource(Object source)
    {
        this.source = source;
    }
}
