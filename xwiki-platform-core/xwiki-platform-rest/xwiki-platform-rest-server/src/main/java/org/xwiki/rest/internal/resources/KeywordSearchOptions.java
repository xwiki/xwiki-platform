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
package org.xwiki.rest.internal.resources;

import java.util.List;

/**
 * Options for keyword search.
 *
 * @param searchScopes the scopes to search in like spaces, objects or page title, name, or content
 * @param wikiName the name of the wiki to search in (optional)
 * @param space the name of the space to search in (optional)
 * @param number the number of results to return
 * @param start the index of the first result to return
 * @param orderField the field to order results by (optional)
 * @param order the order (asc or desc)
 * @param withPrettyNames if the results should contain pretty names
 * @param isLocaleAware if the search should consider the current locale
 *
 * @version $Id$
 * @since 17.5.0
 */
public record KeywordSearchOptions(List<KeywordSearchScope> searchScopes, String wikiName,
                                   String space, int number, int start, String orderField, String order,
                                   Boolean withPrettyNames, Boolean isLocaleAware)
{
    /**
     * @return a new instance of the {@link Builder}
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * @return a new instance of the {@link Builder} initialized with all values of this
     */
    public Builder but()
    {
        return new Builder().searchScopes(this.searchScopes)
            .wikiName(this.wikiName)
            .space(this.space)
            .number(this.number)
            .start(this.start)
            .orderField(this.orderField)
            .order(this.order)
            .withPrettyNames(this.withPrettyNames)
            .isLocaleAware(this.isLocaleAware);
    }

    /**
     * Builder class for constructing instances of {@link KeywordSearchOptions}.
     * Provides a fluent API to set various properties for a keyword search configuration.
     */
    public static class Builder
    {
        private List<KeywordSearchScope> searchScopes;

        private String wikiName;

        private String space;

        private int number;

        private int start;

        private String orderField;

        private String order;

        private Boolean withPrettyNames;

        private Boolean isLocaleAware;

        /**
         * Sets the search scopes for the keyword search.
         *
         * @param searchScopes the list of {@link KeywordSearchScope} to search in
         * @return the current {@link Builder} instance
         */
        public Builder searchScopes(List<KeywordSearchScope> searchScopes)
        {
            this.searchScopes = searchScopes;
            return this;
        }

        /**
         * Sets the wiki name for the keyword search.
         *
         * @param wikiName the name of the wiki
         * @return the current {@link Builder} instance
         */
        public Builder wikiName(String wikiName)
        {
            this.wikiName = wikiName;
            return this;
        }

        /**
         * Sets the space name for the keyword search.
         *
         * @param space the name of the space
         * @return the current {@link Builder} instance
         */
        public Builder space(String space)
        {
            this.space = space;
            return this;
        }

        /**
         * Sets the number of results to return.
         *
         * @param number the number of results
         * @return the current {@link Builder} instance
         */
        public Builder number(int number)
        {
            this.number = number;
            return this;
        }

        /**
         * Sets the index of the first result to return.
         *
         * @param start the starting index
         * @return the current {@link Builder} instance
         */
        public Builder start(int start)
        {
            this.start = start;
            return this;
        }

        /**
         * Sets the field to order results by.
         *
         * @param orderField the field name
         * @return the current {@link Builder} instance
         */
        public Builder orderField(String orderField)
        {
            this.orderField = orderField;
            return this;
        }

        /**
         * Sets the order of results (ascending or descending).
         *
         * @param order the order ("asc" or "desc")
         * @return the current {@link Builder} instance
         */
        public Builder order(String order)
        {
            this.order = order;
            return this;
        }

        /**
         * Specifies whether the results should contain pretty names.
         *
         * @param withPrettyNames true if pretty names should be included, false otherwise
         * @return the current {@link Builder} instance
         */
        public Builder withPrettyNames(Boolean withPrettyNames)
        {
            this.withPrettyNames = withPrettyNames;
            return this;
        }

        /**
         * Specifies whether the search should consider the current locale.
         *
         * @param isLocaleAware true if locale-aware search is enabled, false otherwise
         * @return the current {@link Builder} instance
         */
        public Builder isLocaleAware(Boolean isLocaleAware)
        {
            this.isLocaleAware = isLocaleAware;
            return this;
        }

        /**
         * @return a new instance of {@link KeywordSearchOptions} initialized with the values set on this builder
         */
        public KeywordSearchOptions build()
        {
            return new KeywordSearchOptions(this.searchScopes, this.wikiName, this.space, this.number, this.start,
                this.orderField, this.order, this.withPrettyNames, this.isLocaleAware);
        }
    }
}
