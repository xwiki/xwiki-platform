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
 *
 */
package org.xwiki.security.internal;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import org.xwiki.security.Right;

import java.util.List;
import java.util.LinkedList;

import com.xpn.xwiki.web.Utils;

public class MockDocument extends XWikiDocument
{
    private List<BaseObject> globalRights = new LinkedList();
    private List<BaseObject> localRights = new LinkedList();
    private List<BaseObject> groups = new LinkedList();

    public MockDocument(DocumentReference docRef, String creator)
    {
        super(docRef);
        super.setCreator(creator);
    }

    public MockDocument(String docName, String creator)
    {
        this(Utils.getComponent(DocumentReferenceResolver.class).resolve(docName), creator);
    }

    @Override
    public List<BaseObject> getXObjects(DocumentReference classRef)
    {
        if (classRef.getName().equals("XWikiGroups")) {
            return groups;
        }
        if (classRef.getName().equals("XWikiGlobalRights")) {
            return globalRights;
        } else {
            return localRights;
        }
    }

    public MockDocument allowLocal(List<Right> levels, List<String> users, List<String> groups)
    {
        localRights.add(MockObject.getAllow(levels, users, groups));
        return this;
    }

    public MockDocument allowGlobal(List<Right> levels, List<String> users, List<String> groups)
    {
        globalRights.add(MockObject.getAllow(levels, users, groups));
        return this;
    }

    public MockDocument denyLocal(List<Right> levels, List<String> users, List<String> groups)
    {
        localRights.add(MockObject.getDeny(levels, users, groups));
        return this;
    }

    public MockDocument denyGlobal(List<Right> levels, List<String> users, List<String> groups)
    {
        globalRights.add(MockObject.getDeny(levels, users, groups));
        return this;
    }

    public MockDocument addMember(String name)
    {
        MockObject o = new MockObject();
        o.setMember(name);
        groups.add(o);
        return this;
    }

    public static MockDocument newGroupDocument(String name, String[] members)
    {
        MockDocument doc = new MockDocument(name, "xwiki:XWiki.Admin");
        for (String member : members) {
            doc.addMember(member);
        }
        return doc;
    }
}