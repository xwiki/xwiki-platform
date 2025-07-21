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
import { componentStore } from "@/components/store.js";

(function populateStore() {
  componentStore.register("filter", "boolean", async () => {
    return (await import("@/components/filters/FilterBoolean.vue")).default;
  });
  componentStore.register("filter", "date", async () => {
    return (await import("@/components/filters/FilterDate.vue")).default;
  });
  componentStore.register("filter", "list", async () => {
    return (await import("@/components/filters/FilterList.vue")).default;
  });
  componentStore.register("filter", "number", async () => {
    return (await import("@/components/filters/FilterNumber.vue")).default;
  });
  componentStore.register("filter", "text", async () => {
    return (await import("@/components/filters/FilterText.vue")).default;
  });

  componentStore.register("layout", "table", async () => {
    return (await import("@/components/layouts/table/LayoutTable.vue")).default;
  });
  componentStore.register("layout", "cards", async () => {
    return (await import("@/components/layouts/cards/LayoutCards.vue")).default;
  });

  componentStore.register("displayer", "actions", async () => {
    return (await import("@/components/displayers/DisplayerActions.vue")).default;
  });
  componentStore.register("displayer", "boolean", async () => {
    return (await import("@/components/displayers/DisplayerBoolean.vue")).default;
  });
  componentStore.register("displayer", "date", async () => {
    return (await import("@/components/displayers/DisplayerDate.vue")).default;
  });
  componentStore.register("displayer", "docTitle", async () => {
    return (await import("@/components/displayers/DisplayerDocTitle.vue")).default;
  });
  componentStore.register("displayer", "html", async () => {
    return (await import("@/components/displayers/DisplayerHtml.vue")).default;
  });
  componentStore.register("displayer", "link", async () => {
    return (await import("@/components/displayers/DisplayerLink.vue")).default;
  });
  componentStore.register("displayer", "number", async () => {
    return (await import("@/components/displayers/DisplayerNumber.vue")).default;
  });
  componentStore.register("displayer", "text", async () => {
    return (await import("@/components/displayers/DisplayerText.vue")).default;
  });
  componentStore.register("displayer", "xObjectProperty", async () => {
    return (await import("@/components/displayers/DisplayerXObjectProperty.vue")).default;
  });
})();
