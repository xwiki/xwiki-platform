<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
-->
<template>
  <component :is="component" v-for="component in components" />
</template>
<script lang="ts">
import { inject, Component } from 'vue'
import { Logger, CristalApp } from '@cristal/api';

let comps : Array<Component>;
let logger : Logger;

export default {
    name: "UIX",
    props: ['uixname'],
    setup(props) {
      let cristal = inject<CristalApp>("cristal");
      if (cristal) {
         comps = cristal.getUIXTemplates(props.uixname);
        logger = cristal.getLogger("skin.vue.template");
      }
      logger?.debug("Extension Point name", props.uixname);
    },
    data() {
      logger?.debug("UIX components are ", comps);
      if (!comps || comps.length==0) 
        return {}
      else {
        logger?.debug("Found UIX comps ", comps)
        return {
          components: comps
        };
      }
    }
}
</script>