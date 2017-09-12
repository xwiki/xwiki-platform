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
package org.xwiki.notifications.filters.watch.internal;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/***
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("XWiki.Notifications.Code.AutomaticWatchModeClass")
@Singleton
public class AutomaticWatchModeClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The path to the class parent document.
     */
    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(
            Arrays.asList("XWiki", "Notifications", "Code"), "AutomaticWatchModeClass");

    private static final String SELECT = "select";

    private static final String SEPARATOR = "|";

    /**
     * Default constructor.
     */
    public AutomaticWatchModeClassDocumentInitializer()
    {
        super(CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        String values = Stream.of(AutomaticWatchMode.values()).map(AutomaticWatchMode::name).collect(
                Collectors.joining(SEPARATOR));

        xclass.addStaticListField("automaticWatchMode", "Automatic Watch Mode", 1,
                false, false, values, SELECT, SEPARATOR);
    }
}
