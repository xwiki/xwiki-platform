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
package org.xwiki.annotation.maintainer;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.annotation.Annotation;

/**
 * Mock document loaded from test files with that handles updates and updated annotations.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class MockDocument extends org.xwiki.annotation.MockDocument
{
    /**
     * The modified annotations.
     */
    protected List<Annotation> updatedAnnotations = new ArrayList<Annotation>();

    /**
     * @return the modified source of this document
     */
    public String getModifiedSource()
    {
        String updated = (String) properties.get("sourceUpdated");
        return updated != null ? updated : getSource();
    }

    /**
     * @return the list of updated annotations
     */
    public List<Annotation> getUpdatedAnnotations()
    {
        return updatedAnnotations;
    }
}
