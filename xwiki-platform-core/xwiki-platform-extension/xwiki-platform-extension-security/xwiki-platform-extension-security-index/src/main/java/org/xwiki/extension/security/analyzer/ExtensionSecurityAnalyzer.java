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
package org.xwiki.extension.security.analyzer;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.Extension;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.security.internal.ExtensionSecurityException;
import org.xwiki.stability.Unstable;

/**
 * Generic interface to call for the security analysis of an extension.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Role
@Unstable
public interface ExtensionSecurityAnalyzer
{
    /**
     * Analyze the security vulnerabilities for the given extension.
     *
     * @param extension the extension to analyze
     * @return {@code true} if at least one unknown security issue is found for the extension
     * @throws ExtensionSecurityException in case of issue during the analysis
     */
    ExtensionSecurityAnalysisResult analyze(Extension extension) throws ExtensionSecurityException;
}
