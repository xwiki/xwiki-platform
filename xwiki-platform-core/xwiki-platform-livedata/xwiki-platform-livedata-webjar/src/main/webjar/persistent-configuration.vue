<template>
  <div :v-show="false"></div>
</template>


<script>
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
define([
  "Vue",
  "u-node",
  "LZString",
], function (
  Vue,
  u,
  LZString
) {
  Vue.component("persistent-configuration", {

    name: "persistent-configuration",

    template: template,

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
      data: function () { return this.logic.data; },

      $_filters: {
        get: function () { return this.data.query.filters; },
        set: function (value) { this.data.query.filters = value; },
      },

      $_sort: {
        get: function () { return this.data.query.sort; },
        set: function (value) { this.data.query.sort = value; },
      },

      $_offset: {
        get: function () { return this.data.query.offset; },
        set: function (value) { this.data.query.offset = value; },
      },

      $_limit: {
        get: function () { return this.data.query.limit; },
        set: function (value) { this.data.query.limit = value; },
      },

      $_currentLayoutId: {
        get: function () { return this.logic.currentLayoutId; },
        set: function (value) { this.logic.currentLayoutId = value; },
      },

      $_propertyOrder: {
        get: function () { return this.data.query.properties; },
        set: function (value) { this.data.query.properties = value; },
      },

      $_propertyVisibility: {
        get: function () {
          var self = this;
          // only return hidden props
          return this.data.query.properties
            .reduce(function (hiddenProperties, propertyId) {
              return self.logic.isPropertyVisible(propertyId)
                ? hiddenProperties
                : hiddenProperties.concat(propertyId)
            }, []);
        },
        set: function (value) {
          var self = this;
          this.data.query.properties.forEach(function (propertyId) {
            self.logic.setPropertyVisible(propertyId, value.indexOf(propertyId) === -1);
          });
        }
      },

      propertyIds: function () {
        return this.data.query.properties.slice().sort();
      },

      encodingSpecsProperties: function () {
        return ["oneOf"].concat(this.propertyIds);
      },

      encodingSpecsFilterOperators: function () {
        var self = this;
        var operators = [];
        this.data.meta.filters.forEach(function (filterDescriptor) {
          (filterDescriptor.operators || []).forEach(function (operator) {
            if (operators.indexOf(operator.id) === -1) {
              operators.push(operator.id);
            }
          });
        });
        return ["oneOf"].concat(operators);
      },

      encodingSpecsCurrentLayoutId: function () {
        return ["oneOf"].concat(this.logic.getLayoutIds());
      },

      encodingSpecsV1: function () {
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

      coders: function () {
        return [
          // 1 is the spect version
          u.fromJson(1, this.encodingSpecsV1),
        ];
      },

      dataToSave: function () {
        var dataToSave = {};
        for (var key in this.encodingSpecsV1) {
          dataToSave[key] = this[key];
        }
        return dataToSave;
      },

      encodedConfig: function () {
        return this.encodeConfig(this.dataToSave);
      },

      saveKey: function () {
        return "livedata-config-" + this.data.id;
      },

    },

    watch: {
      encodedConfig: function () {
        if (this.encodedConfig) {
          this.saveConfig(this.saveKey);
        } else {
          this.deleteConfig(this.saveKey);
        }
      },
    },


    methods: {

      encodeConfig: function (config) {
        try {
          var encoded = u.encode(this.coders[this.coders.length -1], config);
          var compressed = LZString.compressToEncodedURIComponent(encoded);
          return compressed;
        } catch (err) {
          console.warn(err);
          return "";
        }
      },

      decodeConfig: function (encodedConfig) {
        if (!encodedConfig) { return; }
        var uncompressed = LZString.decompressFromEncodedURIComponent(encodedConfig);
        if (!uncompressed) { return; }
        try {
          var decoded = u.decode(this.coders, uncompressed);
          return decoded;
        } catch (err) {
          console.warn(err);
        }
      },

      saveConfig: function (saveKey) {
        // url search param
        if (this.urlSearchParam) {
          var url = new URL(window.location);
          url.searchParams.set(saveKey, this.encodedConfig);
          history.replaceState(null, "", url.href);
        }
        // local storage
        if (this.localStorage) {
          window.localStorage.setItem(saveKey, this.encodedConfig);
        }
      },

      getConfig: function (saveKey) {
        var config = "";
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

      loadConfig: function (config) {
        var self = this;
        Object.keys(this.dataToSave).forEach(function (key) {
          self[key] = config[key];
        });
      },

      deleteConfig: function (saveKey) {
        // url search param
        if (this.urlSearchParam) {
          var url = new URL(window.location);
          url.searchParams.delete(saveKey);
          history.replaceState(null, "", url.href);
        }
        // local storage
        if (this.localStorage) {
          window.localStorage.removeItem(saveKey);
        }
      },
    },


    mounted: function () {
      var config = this.decodeConfig(this.getConfig(this.saveKey));
      if (!config) {
        this.deleteConfig();
        new XWiki.widgets.Notification("Bad LiveData config given, fall back to default");
        return;
      }
      this.loadConfig(config);
    },


  });
});
</script>


<style>


</style>
