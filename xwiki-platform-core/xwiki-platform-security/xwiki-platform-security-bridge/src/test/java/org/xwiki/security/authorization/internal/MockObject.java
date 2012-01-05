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
package org.xwiki.security.authorization.internal;

import java.util.Iterator;

import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.objects.BaseObject;

public class MockObject extends BaseObject
{
    private String users;
    private String groups;
    private String levels;
    private String member;
    private int allow;

    @Override
    public int getIntValue(String name)
    {
        return allow;
    }

    @Override
    public String getStringValue(String name)
    {
        if (name.equals("member")) {
            return member;
        }
        if (name.equals("users")) {
            return users;
        }
        if (name.equals("groups")) {
            return groups;
        }
        return levels;
    }

    private static String join(Iterable<String> strings)
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String s : strings) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append(s);
        }
        return builder.toString();
    }

    public static BaseObject getAllow(Iterable<Right> levels, Iterable<String> users, Iterable<String> groups)
    {
        MockObject o = new MockObject();
        o.allow = 1;
        fillOut(o, levels, users, groups);
        return o;
    }

    public static BaseObject getDeny(Iterable<Right> levels, Iterable<String> users, Iterable<String> groups)
    {
        MockObject o = new MockObject();
        o.allow = 0;
        fillOut(o, levels, users, groups);
        return o;
    }

    private static void fillOut(MockObject o, final Iterable<Right> levels, Iterable<String> users, Iterable<String> groups)
    {
        o.levels = join(new Iterable<String>() {
                @Override
                public Iterator<String> iterator()
                {
                    return new Iterator<String>() {
                        private final Iterator<Right> iterator = levels.iterator();
                        @Override
                        public String next()
                        {
                            return iterator.next().getName();
                        }
                        @Override
                        public boolean hasNext()
                        {
                            return iterator.hasNext();
                        }
                        @Override
                        public void remove()
                        {
                        }
                    };
                }
            });
        o.users = join(users);
        o.groups = join(groups);
    }

    public void setMember(String member)
    {
        this.member = member;
    }
}
