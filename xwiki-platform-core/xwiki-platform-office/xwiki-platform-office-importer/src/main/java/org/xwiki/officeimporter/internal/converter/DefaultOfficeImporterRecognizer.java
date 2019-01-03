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
package org.xwiki.officeimporter.internal.converter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.artofsolving.jodconverter.document.DocumentFamily;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.xwiki.component.annotation.Component;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.server.OfficeServer;

/**
 * Recognizes office formats.
 *
 * @version $Id$
 * @since 11.0RC1
 */
@Component
@Singleton
public class DefaultOfficeImporterRecognizer implements OfficeImporterRecognizer
{
    /**
     * Used to query office server status.
     */
    @Inject
    private OfficeServer officeServer;

    @Override
    public boolean isPresentation(String officeFileName)
    {
        String extension = officeFileName.substring(officeFileName.lastIndexOf('.') + 1);
        OfficeConverter officeConverter = this.officeServer.getConverter();
        if (officeConverter != null) {
            DocumentFormat format = officeConverter.getFormatRegistry().getFormatByExtension(extension);
            return format != null && format.getInputFamily() == DocumentFamily.PRESENTATION;
        }
        return false;
    }
}
