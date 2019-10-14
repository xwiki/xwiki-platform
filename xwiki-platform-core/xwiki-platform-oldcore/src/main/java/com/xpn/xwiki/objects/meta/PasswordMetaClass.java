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
package com.xpn.xwiki.objects.meta;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;

/**
 * Defines the meta properties of a boolean XClass property.
 *
 * @version $Id$
 */
@Component
@Named("Password")
@Singleton
public class PasswordMetaClass extends StringMetaClass
{
    /**
     * The name of the meta property that specifies how the password is stored.
     */
    public static final String STORAGE_TYPE = "storageType";

    /**
     * Indicates that the password should be stored in clean.
     */
    public static final String CLEAR = "Clear";

    /**
     * Indicates that the password should be stored encrypted.
     */
    public static final String ENCRYPTED = "Encrypted";

    /**
     * Indicates that the password hash should be store instead of the pass itself.
     */
    public static final String HASH = "Hash";

    /**
     * The string used to separate the possible values of a static list. It is used for instance to separate the various
     * storage types for {@link #ALGORITHM_KEY}.
     */
    public static final String SEPARATOR = "|";

    /**
     * The name of the meta property that specifies which algorithm should be used for password storage.
     */
    public static final String ALGORITHM_KEY = "algorithm";

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor. Initializes the default meta properties of a Password XClass property.
     */
    public PasswordMetaClass()
    {
        setPrettyName("Password");
        setName(getClass().getAnnotation(Named.class).value());

        StaticListClass storageTypeClass = new StaticListClass(this);
        storageTypeClass.setName(STORAGE_TYPE);
        storageTypeClass.setPrettyName("Storage type");
        storageTypeClass.setValues(HASH + SEPARATOR + CLEAR);
        storageTypeClass.setRelationalStorage(false);
        storageTypeClass.setDisplayType("select");
        storageTypeClass.setMultiSelect(false);
        storageTypeClass.setSize(1);
        safeput(storageTypeClass.getName(), storageTypeClass);

        StringClass encryptAlgorithmClass = new StringClass(this);
        encryptAlgorithmClass.setName(ALGORITHM_KEY);
        encryptAlgorithmClass.setPrettyName("Encryption/hash algorithm");
        encryptAlgorithmClass.setSize(20);
        safeput(encryptAlgorithmClass.getName(), encryptAlgorithmClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new PasswordClass();
    }
}
