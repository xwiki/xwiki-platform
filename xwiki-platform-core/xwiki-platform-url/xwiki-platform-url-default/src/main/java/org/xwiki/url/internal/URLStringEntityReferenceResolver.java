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
package org.xwiki.url.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.SymbolScheme;

/**
 * Special resolver to resolve Entity References when they're used in URLs. This is needed for
 * Tomcat since <a href="http://tomcat.apache.org/tomcat-7.0-doc/config/systemprops.html#Security">Tomcat has some
 * default security settings preventing the usage of URL-encoded {@code /} and {@code \} in URLs</a> (to prevent
 * Directory attacks.
 *
 * @version $Id$
 * @since 8.1M2
 */
@Component
@Named("url")
@Singleton
public class URLStringEntityReferenceResolver extends DefaultStringEntityReferenceResolver
{
    @Inject
    @Named("url")
    private SymbolScheme symbolScheme;

    @Override
    protected SymbolScheme getSymbolScheme()
    {
        return this.symbolScheme;
    }
}
