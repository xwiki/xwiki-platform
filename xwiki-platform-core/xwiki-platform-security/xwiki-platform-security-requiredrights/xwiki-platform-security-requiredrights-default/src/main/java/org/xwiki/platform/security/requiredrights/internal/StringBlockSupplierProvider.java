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
package org.xwiki.platform.security.requiredrights.internal;

import java.io.StringReader;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

/**
 * Provides a way to easily construct a supplier that just displays the given string.
 *
 * @version $Id$
 */
@Component(roles = StringBlockSupplierProvider.class)
@Singleton
public class StringBlockSupplierProvider
{
    @Inject
    @Named("plain/1.0")
    private Parser parser;

    /**
     * @param string the string to display
     * @return a supplier that returns a block that displays the given string
     */
    public Supplier<Block> get(String string)
    {
        return () -> {
            if (StringUtils.isNotBlank(string)) {
                try {
                    return this.parser.parse(new StringReader(string));
                } catch (ParseException e) {
                    // Ignore
                }
            }

            return new CompositeBlock();
        };
    }
}
