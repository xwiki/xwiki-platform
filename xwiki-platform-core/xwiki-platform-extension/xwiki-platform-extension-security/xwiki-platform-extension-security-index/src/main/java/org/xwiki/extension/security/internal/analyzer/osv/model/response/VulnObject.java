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
package org.xwiki.extension.security.internal.analyzer.osv.model.response;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * See the <a href="https://ossf.github.io/osv-schema/">Open Source Vulnerability format API documentation</a>.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class VulnObject
{
    private List<AffectObject> affected;

    private String id;

    private List<VulnReferenceObject> references;

    private List<SeverityObject> severity;

    private List<String> aliases;

    /**
     * @return the affected field
     * @see <a href="https://ossf.github.io/osv-schema/#affected-fields">affected doc</a>
     */
    public List<AffectObject> getAffected()
    {
        return this.affected;
    }

    /**
     * @param affected the affected field
     * @see <a href="https://ossf.github.io/osv-schema/#affected-fields">affected doc</a>
     */
    public void setAffected(List<AffectObject> affected)
    {
        this.affected = affected;
    }

    /**
     * @return the id field
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the id field
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the references field
     */
    public List<VulnReferenceObject> getReferences()
    {
        return this.references;
    }

    /**
     * @param references references field
     */
    public void setReferences(List<VulnReferenceObject> references)
    {
        this.references = references;
    }

    /**
     * @return the severity field
     */
    public List<SeverityObject> getSeverity()
    {
        return this.severity;
    }

    /**
     * @param severity the severity field
     */
    public void setSeverity(List<SeverityObject> severity)
    {
        this.severity = severity;
    }

    /**
     * @return the {@link VulnReferenceObject#getUrl()} value of the first {@link #getReferences()} with a
     *     {@link VulnReferenceObject#getType()} equals to {@code "WEB"}, the empty string otherwise
     */
    public String getMainURL()
    {
        return this.references.stream()
            .filter(reference -> Objects.equals(reference.getType(), "WEB"))
            .findFirst()
            .map(VulnReferenceObject::getUrl)
            .orElse("");
    }

    /**
     * @return the CVSS V3 {@link #getSeverity()}
     */
    public String getSeverityCCSV3()
    {
        if (this.severity == null) {
            return "";
        }
        return this.severity.stream()
            .filter(s -> Objects.equals("CVSS_V3", s.getType()))
            .map(SeverityObject::getScore)
            .findFirst()
            .orElse(null);
    }

    /**
     * @param currentVersion the currently analyzed version, the fix version cannot be before the current version
     * @return the most recent fix version for the ranges related to this vulnerability
     */
    public Optional<Version> getMaxFixVersion(Version currentVersion)
    {
        return this.affected.stream()
            .flatMap(affect -> affect.getRanges().stream())
            .flatMap(range -> range.getEvents().stream())
            .map(EventObject::getFixed)
            .filter(Objects::nonNull)
            .<Version>map(DefaultVersion::new)
            .filter(it -> it.compareTo(currentVersion) > 0)
            .min(Comparator.naturalOrder());
    }

    /**
     * @return the list of aliases associated with this vulnerability
     */
    public List<String> getAliases()
    {
        if (this.aliases == null) {
            this.aliases = new ArrayList<>();
        }
        return this.aliases;
    }

    /**
     * Sets the list of aliases associated with this vulnerability.
     *
     * @param aliases the list of aliases to be set
     */
    public void setAliases(List<String> aliases)
    {
        this.aliases = aliases;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("affected", getAffected())
            .append("id", getId())
            .append("references", getReferences())
            .append("severity", getSeverity())
            .append("aliases", getAliases())
            .toString();
    }
}
