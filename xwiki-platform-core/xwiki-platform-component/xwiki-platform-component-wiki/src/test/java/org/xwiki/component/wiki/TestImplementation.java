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
package org.xwiki.component.wiki;

import java.lang.reflect.Type;

import org.xwiki.model.reference.DocumentReference;

public class TestImplementation implements TestRole, WikiComponent
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @Override
    public String test()
    {
        return "test";
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return DOC_REFERENCE;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return AUTHOR_REFERENCE;
    }

    @Override
    public Type getRoleType()
    {
        return TestRole.class;
    }

    @Override
    public String getRoleHint()
    {
        return "roleHint";
    }

    @Override
    public WikiComponentScope getScope()
    {
        return WikiComponentScope.WIKI;
    }
}
