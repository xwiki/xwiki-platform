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
package org.xwiki.gwt.useragent.rebind;

import java.util.SortedSet;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ConfigurationProperty;

/**
 * Extends the default {@link com.google.gwt.useragent.rebind.UserAgentPropertyGenerator} to add support for IE11. This
 * is only a temporary hack before GWT adds support for IE11.
 * 
 * @version $Id$
 */
public class UserAgentPropertyGenerator extends com.google.gwt.useragent.rebind.UserAgentPropertyGenerator
{
    @Override
    public String generate(TreeLogger logger, SortedSet<String> possibleValues, String fallback,
        SortedSet<ConfigurationProperty> configProperties)
    {
        String code = super.generate(logger, possibleValues, fallback, configProperties);
        int insertionPoint = code.lastIndexOf("if ((function() { ");
        StringBuilder ie11Test = new StringBuilder();
        ie11Test.append("if ((function() {\n");
        ie11Test.append("  var result = /trident\\/(\\d+)/.exec(ua);\n");
        ie11Test.append("  return result && parseInt(result[1]) >= 7;");
        ie11Test.append("})()) return 'ie11';\n");
        return code.substring(0, insertionPoint) + ie11Test + code.substring(insertionPoint);
    }
}
