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
package com.xpn.xwiki.store.migration.hibernate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-10092. An object can be stored in the database without some of its properties.
 * 
 * @since 10.8.1
 * @since 10.9RC1
 * @version $Id$
 */
@Component
@Named("R1008010XWIKI10092")
@Singleton
public class R1008010XWIKI10092DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String getDescription()
    {
        return "Add missing properties to existing objects.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(1008010);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        List<String> classNames = xcontext.getWiki().getClassList(xcontext);
        for (String className : classNames) {
            DocumentReference classReference = this.documentReferenceResolver.resolve(className);
            BaseClass xclass = xcontext.getWiki().getXClass(classReference, xcontext);
            // There's no missing object property if the class is empty.
            if (!xclass.getPropertyList().isEmpty()) {
                // Pass the class name so that we don't have to serialize the class reference.
                migrateObjectsOfType(className, xclass);
            }
        }
    }

    private void migrateObjectsOfType(String className, BaseClass xclass) throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Void>()
        {
            @Override
            public Void doInHibernate(Session session) throws HibernateException, XWikiException
            {
                List<BaseObject> objects =
                    getObjectsWithMissingProperties(className, xclass.getPropertyList(), session);
                for (BaseObject object : objects) {
                    addMissingProperties(object, xclass, session);
                }

                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<BaseObject> getObjectsWithMissingProperties(String className, Set<String> expectedProperties,
        Session session)
    {
        // Get all the objects that have less properties than what their class declares (the expected property count).
        // Note that we count only the expected properties (those declared by the class).
        Query query = session.createQuery("select obj from BaseObject as obj, BaseProperty as prop "
            + "where obj.id = prop.id.id and obj.className = :className and prop.id.name in :expectedProperties "
            + "group by obj, BaseObject having count(prop) < :expectedPropertyCount");
        query.setString("className", className);
        query.setParameterList("expectedProperties", expectedProperties);
        query.setLong("expectedPropertyCount", Integer.valueOf(expectedProperties.size()).longValue());
        return query.list();
    }

    private void addMissingProperties(BaseObject object, BaseClass xclass, Session session)
    {
        for (PropertyClass propertyClass : getMissingProperties(object, xclass, session)) {
            // Add missing property.
            BaseProperty<?> property = propertyClass.newProperty();
            // The property has a composite id made of the object id and the property name.
            // We don't set the property name because newProperty() does it.
            property.setId(object.getId());
            session.save(property);
        }
    }

    private List<PropertyClass> getMissingProperties(BaseObject object, BaseClass xclass, Session session)
    {
        // Copy the property list so that we don't modify the cached class.
        Set<String> missingProperties = new HashSet<>(xclass.getPropertyList());
        missingProperties.removeAll(getCurrentProperties(object, session));
        return missingProperties.stream().map(propertyName -> (PropertyClass) xclass.get(propertyName))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<String> getCurrentProperties(BaseObject object, Session session)
    {
        // The object was loaded from the database without its properties so we need to make a second query to get them.
        Query query = session.createQuery("select prop.id.name from BaseObject as obj, BaseProperty as prop "
            + "where obj.id = prop.id.id and obj.id = :objectId");
        query.setLong("objectId", object.getId());
        return query.list();
    }
}
