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
package com.xpn.xwiki.internal.template;

import java.util.HashSet;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

/**
 * Allow registering the templates in the Execution Context object since it's shared during the whole execution of the
 * current request.
 *
 * @version $Id$
 */
@Component
@Named("templateContext")
@Singleton
public class TemplateContextInitializer implements ExecutionContextInitializer
{
    @Override
    public void initialize(ExecutionContext executionContext) throws ExecutionContextException
    {
        if (!executionContext.hasProperty(TemplateContext.TEMPLATES)) {
            executionContext.newProperty(TemplateContext.TEMPLATES)
                .inherited()
                .initial(new HashSet<>())
                .declare();
        }
    }
}
