<!--
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
 -->

<template>
  <div
    v-if="data.id"
    v-show="false"
  ></div>
</template>


<script>
import { fromJson, encode, decode } from "u-node";
import LZString from "lz-string";

export default {

  name: "LivedataPersistentConfiguration",

  inject: ["logic"],

  props: {
    urlSearchParam: {
      type: Boolean,
      default: true,
    },
    localStorage: {
      type: Boolean,
      default: false,
    },
  },


  computed: {
    data () { return this.logic.data; },

    $_filters: {
      get () { return this.data.query.filters; },
      set (value) { this.data.query.filters = value; },
    },

    $_sort: {
      get () { return this.data.query.sort; },
      set (value) { this.data.query.sort = value; },
    },

    $_offset: {
      get () { return this.data.query.offset; },
      set (value) { this.data.query.offset = value; },
    },

    $_limit: {
      get () { return this.data.query.limit; },
      set (value) { this.data.query.limit = value; },
    },

    $_currentLayoutId: {
      get () { return this.logic.currentLayoutId; },
      set (value) { this.logic.currentLayoutId = value; },
    },

    $_propertyOrder: {
      get () { return this.data.query.properties; },
      set (value) { this.data.query.properties = value; },
    },

    $_propertyVisibility: {
      get () {
        // only return hidden props
        return this.data.query.properties
          .reduce((hiddenProperties, propertyId) => this.logic.isPropertyVisible(propertyId)
            ? hiddenProperties
            : hiddenProperties.concat(propertyId),
          []);
      },
      set (value) {
        this.data.query.properties.forEach(propertyId => {
          this.logic.setPropertyVisible(propertyId, !value.includes(propertyId));
        });
      }
    },

    propertyIds () {
      return this.data.query.properties.slice().sort();
    },

    encodingSpecsProperties () {
      return ["oneOf"].concat(this.propertyIds);
    },

    encodingSpecsFilterOperators () {
      const operators = [];
      this.data.meta.filters.forEach(filterDescriptor => {
        (filterDescriptor.operators || []).forEach(operator => {
          if (!operators.includes(operator.id)) {
            operators.push(operator.id);
          }
        });
      });
      return ["oneOf"].concat(operators);
    },

    encodingSpecsCurrentLayoutId () {
      return ["oneOf"].concat(this.logic.getLayoutIds());
    },

    encodingSpecsV1 () {
      return {
        $_filters: ["array", {
          property: this.encodingSpecsProperties,
          matchAll: ["boolean"],
          constrains: ["array", {
            operator: this.encodingSpecsFilterOperators,
            value: ["varchar"],
          }],
        }],
        $_sort: ["array", {
          property: this.encodingSpecsProperties,
          descending: ["boolean"],
        }],
        $_offset: ["integer"],
        $_limit: ["integer"],
        $_currentLayoutId: this.encodingSpecsCurrentLayoutId,
        $_propertyOrder: ["array", this.encodingSpecsProperties],
        $_propertyVisibility: ["array", this.encodingSpecsProperties],
      };
    },

    coders () {
      return [
        // 1 is the spect version
        fromJson(1, this.encodingSpecsV1),
      ];
    },

    dataToSave () {
      const dataToSave = {};
      for (const key in this.encodingSpecsV1) {
        dataToSave[key] = this[key];
      }
      return dataToSave;
    },

    encodedConfig () {
      return this.encodeConfig(this.dataToSave);
    },

    saveKey () {
      return "livedata-config-" + this.data.id;
    },

  },

  watch: {
    encodedConfig () {
      if (this.encodedConfig) {
        this.saveConfig(this.saveKey);
      } else {
        this.deleteConfig(this.saveKey);
      }
    },
  },


  methods: {

    encodeConfig (config) {
      try {
        const encoded = encode(this.coders[this.coders.length -1], config);
        const compressed = LZString.compressToEncodedURIComponent(encoded);
        return compressed;
      } catch (err) {
        console.warn(err);
        return "";
      }
    },

    decodeConfig (encodedConfig) {
      if (!encodedConfig) { return; }
      const uncompressed = LZString.decompressFromEncodedURIComponent(encodedConfig);
      if (!uncompressed) { return; }
      try {
        return decode(this.coders, uncompressed);
      } catch (err) {
        console.warn(err);
      }
    },

    saveConfig (saveKey) {
      // url search param
      if (this.urlSearchParam) {
        const url = new URL(window.location);
        url.searchParams.set(saveKey, this.encodedConfig);
        history.replaceState(null, "", url.href);
      }
      // local storage
      if (this.localStorage) {
        window.localStorage.setItem(saveKey, this.encodedConfig);
      }
    },

    getConfig (saveKey) {
      let config = "";
      // url search param
      if (!config && this.urlSearchParam) {
        config = (new URLSearchParams(window.location.search)).get(saveKey);
      }
      // local storage
      if (!config && this.localStorage) {
        config = window.localStorage.getItem(saveKey);
      }
      return config;
    },

    hasConfig (saveKey) {
      // url search param
      if (this.urlSearchParam && new URLSearchParams(window.location.search).has(saveKey)) {
        return true;
      }
      // local storage
      if (this.localStorage && window.localStorage.getItm(saveKey) !== null) {
        return true;
      }
      return false;
    },

    loadConfig (config) {
      Object.keys(this.dataToSave).forEach(key => {
        this[key] = config[key];
      });
    },

    deleteConfig (saveKey) {
      // url search param
      if (this.urlSearchParam) {
        const url = new URL(window.location);
        url.searchParams.delete(saveKey);
        history.replaceState(null, "", url.href);
      }
      // local storage
      if (this.localStorage) {
        window.localStorage.removeItem(saveKey);
      }
    },
  },


  mounted () {
    if (!this.hasConfig(this.saveKey)) { return; }
    const config = this.decodeConfig(this.getConfig(this.saveKey));
    if (!config) {
      this.deleteConfig();
      new XWiki.widgets.Notification("Bad LiveData config given, fall back to default");
      return;
    }
    this.loadConfig(config);
  },


};
</script>


<style>

</style>
