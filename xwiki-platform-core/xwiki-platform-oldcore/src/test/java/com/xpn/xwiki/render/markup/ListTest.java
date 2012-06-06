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
package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

public class ListTest extends AbstractSyntaxTest
{
    public void testSimpleStarList()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("* a list item");
        expects.add("<ul class=\"star\">\n<li>a list item</li>\n</ul>\n");
        tests.add("*\ta list item");
        expects.add("<ul class=\"star\">\n<li>a list item</li>\n</ul>\n");
        tests.add("    * a list item");
        expects.add("<ul class=\"star\">\n<li>a list item</li>\n</ul>\n");
        tests.add("    *   a list item  ");
        expects.add("<ul class=\"star\">\n<li>a list item</li>\n</ul>\n");
        test(tests, expects);
    }
}
