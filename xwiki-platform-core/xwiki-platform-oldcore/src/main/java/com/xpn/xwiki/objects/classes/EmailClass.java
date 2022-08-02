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
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * Email Field Class allows to create a field for email values. This will allow using a custom displayer assigned to
 * that field by default. The field also includes a default regexp for validation.
 *
 * @version $Id$
 * @since 4.2M2
 */
public class EmailClass extends StringClass
{
    /**
     * Constant defining the field name.
     **/
    protected static final String XCLASSNAME = "email";

    private static final long serialVersionUID = 1L;

    /**
     * Default value for the email validation regular expression.
     */
    private static final String DEFAULT_EMAIL_VALIDATION_REGEXP =
        "/^(([^@\\s]+)@((?:[-a-zA-Z0-9]+\\.)+[a-zA-Z]{2,}))?$/";

    /**
     * Constructor for Email Class.
     *
     * @param wclass Meta Class
     **/
    public EmailClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Email", wclass);
    }

    /**
     * Constructor for Email Class.
     **/
    public EmailClass()
    {
        super();
        setValidationRegExp(DEFAULT_EMAIL_VALIDATION_REGEXP);
    }
}
