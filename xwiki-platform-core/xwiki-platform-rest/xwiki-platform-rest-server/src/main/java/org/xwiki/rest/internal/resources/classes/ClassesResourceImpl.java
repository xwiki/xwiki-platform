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
package org.xwiki.rest.internal.resources.classes;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Classes;
import org.xwiki.rest.resources.classes.ClassesResource;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.classes.ClassesResourceImpl")
public class ClassesResourceImpl extends XWikiResource implements ClassesResource
{
    @Override
    public Classes getClasses(String wikiName, Integer start, Integer number) throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            List<String> classNames = Utils.getXWikiApi(componentManager).getClassList();
            Collections.sort(classNames);

            RangeIterable<String> ri = new RangeIterable<String>(classNames, start, number);

            Classes classes = objectFactory.createClasses();

            for (String className : ri) {
                com.xpn.xwiki.api.Class xwikiClass = Utils.getXWikiApi(componentManager).getClass(className);
                classes.getClazzs().add(
                        DomainObjectFactory.createClass(objectFactory, uriInfo.getBaseUri(), wikiName, xwikiClass));
            }

            return classes;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }
}
