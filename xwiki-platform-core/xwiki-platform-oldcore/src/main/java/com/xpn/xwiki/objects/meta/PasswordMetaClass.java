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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class PasswordMetaClass extends StringMetaClass
{
    public static final String CLEAR = "Clear";

    public static final String ENCRYPTED = "Encrypted";

    public static final String HASH = "Hash";

    public static final String SEPARATOR = "|";

    public static final String ALGORITHM_KEY = "algorithm";

    public PasswordMetaClass()
    {
        super();
        setPrettyName("Password");
        setName(PasswordClass.class.getName());

        StaticListClass storageType_class = new StaticListClass(this);
        storageType_class.setName("storageType");
        storageType_class.setPrettyName("Storage type");
        storageType_class.setValues(HASH + SEPARATOR + CLEAR);// + SEPARATOR + ENCRYPTED
        storageType_class.setRelationalStorage(false);
        storageType_class.setDisplayType("select");
        storageType_class.setMultiSelect(false);
        storageType_class.setSize(1);
        safeput("storageType", storageType_class);

        StringClass encryptAlgorithm_class = new StringClass(this);
        encryptAlgorithm_class.setName(ALGORITHM_KEY);
        encryptAlgorithm_class.setPrettyName("Encryption/hash algorithm");
        encryptAlgorithm_class.setSize(20);
        safeput(ALGORITHM_KEY, encryptAlgorithm_class);
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new PasswordClass();
    }
}
