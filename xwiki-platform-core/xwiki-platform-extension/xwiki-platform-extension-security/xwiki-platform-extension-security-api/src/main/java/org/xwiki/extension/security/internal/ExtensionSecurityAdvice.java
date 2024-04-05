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
package org.xwiki.extension.security.internal;

/**
 * The list of extension security advices. Advices are computed during the extension security indexing.
 *
 * @version $Id$
 * @since 15.8RC1
 */
public enum ExtensionSecurityAdvice
{
    /**
     * Shared constant when the advice is to upgrade the extension from the Extension Manager.
     */
    UPGRADE_FROM_EM_ADVICE("extension.security.analysis.advice.upgradeFromEM"),
    /**
     * Shared constant when the advice is to upgrade the extension that brought the current extension as a dependency.
     */
    TRANSITIVE_DEPENDENCY_ADVICE("extension.security.analysis.advice.transitive"),
    /**
     * Shared constant when the advice is to upgrade the extension XWiki itself.
     */
    UPGRADE_XWIKI_ADVICE("extension.security.analysis.advice.upgradeXWiki"),
    /**
     * Shared constant when the advice is to upgrade something else from the environment.
     */
    UPGRADE_ENVIRONMENT_ADVICE("extension.security.analysis.advice.upgradeEnvironment");

    private final String translationId;

    /**
     * @param translationId the translation id associated with the advice
     */
    ExtensionSecurityAdvice(String translationId)
    {

        this.translationId = translationId;
    }

    /**
     * Returns the translation id associated with the advice.
     *
     * @return the translation id
     */
    public String getTranslationId()
    {
        return this.translationId;
    }
}
