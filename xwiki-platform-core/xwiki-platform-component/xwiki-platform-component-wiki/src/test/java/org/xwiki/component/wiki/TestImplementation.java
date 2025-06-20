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

/**
 * Stub wiki component implementation for testing.
 *
 * @version $Id$
 */
public class TestImplementation implements TestRole, WikiComponent
{
    private DocumentReference documentReference;

    private DocumentReference authorReference;

    private WikiComponentScope scope;

    private int roleTypePriority;

    private int roleHintPriority;

    public TestImplementation(DocumentReference documentReference, DocumentReference authorReference,
        WikiComponentScope scope, int roleTypePriority, int roleHintPriority)
    {
        this.documentReference = documentReference;
        this.authorReference = authorReference;
        this.scope = scope;
        this.roleTypePriority = roleTypePriority;
        this.roleHintPriority = roleHintPriority;
    }

    @Override
    public String test()
    {
        return "test";
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
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
        return this.scope;
    }

    @Override
    public int getRoleHintPriority()
    {
        return this.roleHintPriority;
    }

    @Override
    public int getRoleTypePriority()
    {
        return this.roleTypePriority;
    }
}
