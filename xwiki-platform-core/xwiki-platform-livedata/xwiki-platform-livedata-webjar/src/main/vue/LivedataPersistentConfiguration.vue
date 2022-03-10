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

<!--
  The LivedataPersistentConfiguration is a component that does not has
  an Html part.
  As it is not possible to use Vue 2.x reactivity API in order to create
  reactive objects without creating a whole a component,
  we simply create a component with a template that is always hidden,
  that we insert in the parent template.

  However we cannot use v-if to hide the component, as it will be simply
  ignored and would not respond to changes.
  So we are using v-show instead, that will just set `display: none` as css.
-->
<template>
  <div
    v-if="data.id"
    v-show="false"
  ></div>
</template>


<script>
// u-node is used to encode and decode the config
import { fromJson, encode, decode } from "u-node";
// lz-string is used to compress / decrompress the encoded config
import LZString from "lz-string";

export default {

  name: "LivedataPersistentConfiguration",

  inject: ["logic"],

  props: {
    // Whether to store the config in the url
    urlSearchParam: {
      type: Boolean,
      default: true,
    },
    // Whether to store the config in the localStorage
    localStorage: {
      type: Boolean,
      default: false,
    },
  },


  computed: {
    data () { return this.logic.data; },

    // The following computed properties, prefixed by "$_",
    // are the values that are going to be stored.
    // They also have setters so that it can easily be set
    // and update the current config on load

    /* eslint camelcase: ["error", { allow: ["^\\$_"] }] */

    // Filters
    $_filters: {
      get () { return this.data.query.filters; },
      set (value) { this.data.query.filters = value; },
    },
    // Sort
    $_sort: {
      get () { return this.data.query.sort; },
      set (value) { this.data.query.sort = value; },
    },
    // Pagination offset
    $_offset: {
      get () { return this.data.query.offset; },
      set (value) { this.data.query.offset = value; },
    },
    // Pagination limit
    $_limit: {
      get () { return this.data.query.limit; },
      set (value) { this.data.query.limit = value; },
    },
    // Current layout id
    $_currentLayoutId: {
      get () { return this.logic.currentLayoutId; },
      set (value) { this.logic.currentLayoutId = value; },
    },
    // Property order
    $_propertyOrder: {
      get () { return this.data.query.properties; },
      set (value) { this.data.query.properties = value; },
    },
    // Property visibility
    $_propertyVisibility: {
      get () {
        // Return an array of hidden properties
        // We use hidden properties because they are more likely to be
        // less numerous than visible ones
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


    // Return the u-node domain used to encode properties
    encodingSpecsProperties () {
      // The whole list of property ids
      const propertyIds = this.data.query.properties.slice().sort();
      return ["oneOf"].concat(propertyIds);
    },

    // Return the u-node domain used to encode operators
    encodingSpecsFilterOperators () {
      // The whole list of operators ids
      const operatorIds = [];
      this.data.meta.filters.forEach(filterDescriptor => {
        (filterDescriptor.operators || []).forEach(operator => {
          if (!operatorIds.includes(operator.id)) {
            operatorIds.push(operator.id);
          }
        });
      });
      return ["oneOf"].concat(operatorIds);
    },

    // Return the u-node domain used to encode layouts
    encodingSpecsCurrentLayoutId () {
      return ["oneOf"].concat(this.logic.getLayoutIds());
    },

    // The whole specs used to encode the config
    // More detail about u-node can be found at:
    // https://github.com/ananthakumaran/u
    // The keys of the properties are the computed property names defined above,
    // so that we can retreive the computed property and use it to get and set
    // the values during encoding and decoding
    encodingSpecsV1 () {
      return {
        $_filters: ["array", {
          property: this.encodingSpecsProperties,
          matchAll: ["boolean"],
          constraints: ["array", {
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

    // The coders that are going to be used for encoding the config
    // It is an array so that we can specify several encoding specs.
    // Using several encoding specs can be usefull to keep backward
    // compability between two different versions of encodings.
    coders () {
      return [
        // the first parameter is the encoding version
        fromJson(1, this.encodingSpecsV1),
      ];
    },


    // Get the whole object of data that need to be saved
    // Use the encodingSpecs object and the computed properties getters
    // to map the specs keys to the values to save
    dataToSave () {
      const dataToSave = {};
      for (const key in this.encodingSpecsV1) {
        dataToSave[key] = this[key];
      }
      return dataToSave;
    },

    // The data to save fully encoded
    encodedConfig () {
      return this.encodeConfig(this.dataToSave);
    },

    // The key where to save the encoded config
    // (in the url query param and localStorage)
    saveKey () {
      return "livedata-config-" + this.data.id;
    },

  },

  watch: {
    // Watch for any changes from the data we want to save
    // We are watching for the encoded config chagnes, because
    // it means that the data to save has changed too
    // On changes, either save or delete config
    encodedConfig () {
      // If the encodedConfig exist save it
      if (this.encodedConfig) {
        this.saveConfig(this.saveKey, this.encodedConfig);
      // If the encodedConfig is falsy, delete saved config
      } else {
        this.deleteConfig(this.saveKey);
      }
    },
  },


  methods: {

    // Fully encode the provided config
    // - First: encode it using u-node encode function, so that it takes
    //   much less space
    // - Second: compresse the encoded config using lz-string. The resulted
    //   string might be a bit longer, but any plain text is now hashed
    // On error, return emty string, that will delete existing config.
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

    // Fully decode the provided encoded config
    // First: uncompress the encoded config using lz-string
    // Second: decode the uncompressed config using u-node
    // On error, return undefined
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

    // Save the specified config at the specified saveKey
    // This is used to save `this.encodedConfig` at `this.saveKey`
    saveConfig (saveKey, config) {
      // url search param
      if (this.urlSearchParam) {
        const url = new URL(window.location);
        url.searchParams.set(saveKey, config);
        history.replaceState(null, "", url.href);
      }
      // local storage
      if (this.localStorage) {
        window.localStorage.setItem(saveKey, config);
      }
    },

    // Get the encoded config saved at the specified saveKey
    getConfig (saveKey) {
      let config = "";
      // url search param
      if (this.urlSearchParam) {
        config = (new URLSearchParams(window.location.search)).get(saveKey);
      }
      // local storage
      if (!config && this.localStorage) {
        config = window.localStorage.getItem(saveKey);
      }
      return config;
    },

    // Return whether a config can be found at the specified save key
    // either in the url or the localstorage
    // It does not verify if the config is valid
    // or if the saveKey is already used for something else
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

    // Load the given config object
    // Update the Livedata config using the computed value setters
    // defined for the properties to save
    loadConfig (config) {
      Object.keys(this.dataToSave).forEach(key => {
        this[key] = config[key];
      });
    },

    // Delete config at the specified saveKey
    // It does not verify if the config is valid
    // or if the saveKey is already used for something else
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


  // On mounted, get config if any, and load it
  mounted () {
    if (!this.hasConfig(this.saveKey)) { return; }
    const config = this.decodeConfig(this.getConfig(this.saveKey));
    if (!config) {
      this.deleteConfig(config);
      new XWiki.widgets.Notification("Bad LiveData config given, fall back to default");
      return;
    }
    this.loadConfig(config);
  },


};
</script>


<style>

</style>
