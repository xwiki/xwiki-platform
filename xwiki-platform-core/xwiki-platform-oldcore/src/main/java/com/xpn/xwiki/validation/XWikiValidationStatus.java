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
package com.xpn.xwiki.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;

public class XWikiValidationStatus
{

    private List errorObjects = new ArrayList();

    private List propertyErrors = new ArrayList();

    private List errors = new ArrayList();

    private List exceptions = new ArrayList();

    public XWikiValidationStatus()
    {
    }

    public static void addErrorToContext(String className, String propName, String propPrettyName,
        String validationMessage, XWikiContext context)
    {
        XWikiValidationStatus errors = getValidationStatus(context);
        errors.addError(className, propName, propPrettyName, validationMessage, context);

    }

    private static XWikiValidationStatus getValidationStatus(XWikiContext context)
    {
        XWikiValidationStatus errors = context.getValidationStatus();
        if (errors == null) {
            errors = new XWikiValidationStatus();
            context.setValidationStatus(errors);
        }
        return errors;
    }

    public static void addExceptionToContext(String className, String propName, Throwable e, XWikiContext context)
    {
        XWikiValidationStatus errors = getValidationStatus(context);
        errors.addException(className, propName, e, context);
    }

    public void addError(String className, String propName, String propPrettyName, String validationMessage,
        XWikiContext context)
    {
        getErrorObjects().add(className);
        getPropertyErrors().add(propName);

        if ((validationMessage != null) && (!validationMessage.trim().equals("")))
            getErrors().add(validationMessage);
        else {
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            if (vcontext == null)
                getErrors().add("Validation error for property " + propPrettyName + " in class " + className);
            else {
                vcontext.put("className", className);
                vcontext.put("propName", propPrettyName);
                String message = context.getMessageTool().get("validationerror", Collections.singletonList(propName));
                getErrors().add(message);
            }
        }
    }

    public void addException(String className, String propName, Throwable e, XWikiContext context)
    {
        getExceptions().add(e);
    }

    public boolean hasExceptions()
    {
        return (getExceptions().size() > 0);
    }

    public List getExceptions()
    {
        return exceptions;
    }

    public List getErrorObjects()
    {
        return errorObjects;
    }

    public void setErrorObjects(List errorObjects)
    {
        this.errorObjects = errorObjects;
    }

    public List getPropertyErrors()
    {
        return propertyErrors;
    }

    public void setPropertyErrors(List propertyErrors)
    {
        this.propertyErrors = propertyErrors;
    }

    public List getErrors()
    {
        return errors;
    }

    public void setErrors(List errors)
    {
        this.errors = errors;
    }

    public void setExceptions(List exceptions)
    {
        this.exceptions = exceptions;
    }
}
