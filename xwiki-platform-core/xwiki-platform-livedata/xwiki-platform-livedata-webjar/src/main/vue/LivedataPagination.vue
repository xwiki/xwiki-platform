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
  <!-- Pagination -->
  <nav class="livedata-pagination">

    <span
      class="pagination-page-size"
      v-if="data.meta.pagination.showPageSizeDropdown"
    >
      Entries per page
      <select
        @change="logic.setPageSize(+$event.target.value)"
      >
        <option
          v-for="pageSize in data.meta.pagination.pageSizes"
          :key="pageSize"
          :value="pageSize"
          :selected="pageSize === data.query.limit"
        >{{ pageSize }}</option>
      </select>
    </span>

    <span class="pagination-current-entries">
      {{ logic.getFirstIndexOfPage() + 1}} - {{ logic.getLastIndexOfPage() + 1}}
      of {{ data.data.count }}
    </span>

    <nav class="pagination-indexes">
      <a
        class="page-nav"
        v-if="data.meta.pagination.showFirstLast"
        href="#"
        @click.prevent="logic.setPageIndex(0)"
      >
        <span class="fa fa-angle-double-left"></span>
      </a>

      <a
        class="page-nav"
        v-if="data.meta.pagination.showNextPrevious"
        href="#"
        @click.prevent="logic.setPageIndex(logic.getPageIndex() - 1)"
      >
        <span class="fa fa-angle-left"></span>
      </a>

      <span
        v-for="pageIndex in paginationIndexesAndDots"
        :key="pageIndex"
      >
        <span v-if="pageIndex === '...'">...</span>
        <a
          v-else
          :class="{
            'page-nav': true,
            'current': pageIndex === logic.getPageIndex(),
          }"
          href="#"
          @click.prevent="logic.setPageIndex(pageIndex)"
        >
          {{ pageIndex + 1 }}
        </a>
      </span>

      <a
        class="page-nav"
        v-if="data.meta.pagination.showNextPrevious"
        href="#"
        @click.prevent="logic.setPageIndex(logic.getPageIndex() + 1)"
      >
        <span class="fa fa-angle-right"></span>
      </a>

      <a
        class="page-nav"
        v-if="data.meta.pagination.showFirstLast"
        href="#"
        @click.prevent="logic.setPageIndex(logic.getPageCount() - 1)"
      >
        <span class="fa fa-angle-double-right"></span>
      </a>

    </nav>

  </nav>
</template>


<script>
export default {

  name: "LivedataPagination",

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },

    // get the page indexes to be displayed in the page nav
    paginationIndexes () {
      const pageCount = this.logic.getPageCount();
      const maxShownPages = this.data.meta.pagination.maxShownPages;
      const currentPageIndex = this.logic.getPageIndex();
      const pageIndexes = [];
      const addPage = pageNumber => {
        if (pageNumber >= 0 && pageNumber < pageCount && !pageIndexes.includes(pageNumber)) {
          pageIndexes.push(pageNumber);
        }
      };

      // pages to display at the very least
      if (maxShownPages >= 1) { addPage(currentPageIndex); }
      if (maxShownPages >= 2) { addPage(0); }
      if (maxShownPages >= 3) { addPage(pageCount - 1); }

      let i = 1;
      const bound = Math.max(currentPageIndex, pageCount - currentPageIndex);
      while (pageIndexes.length < maxShownPages && Math.abs(i) < bound) {
        addPage(currentPageIndex + i);
        if (i > 0) {
          i *= -1;
        } else {
          i = (i * -1) + 1;
        }
      }

      return pageIndexes.sort((a, b) => a - b);
    },

    paginationIndexesAndDots () {
      const indexesAndDots = [];
      this.paginationIndexes.forEach((index, i, indexes) => {
        indexesAndDots.push(index);
        if (indexes[i + 1] && indexes[i + 1] !== index + 1) {
          indexesAndDots.push("...");
        }
      });
      return indexesAndDots;
    },

  },

};
</script>


<style>

.livedata-pagination {
  margin-left: 1rem;
  font-size: 0.9em;
}

.livedata-pagination .pagination-current-entries {
  color: #777777;
  margin-right: 1rem;
}

.livedata-pagination .pagination-indexes {
  display: inline-block;
  user-select: none;
  -webkit-user-select: none;
  -ms-user-select: none;
}

.livedata-pagination .page-nav {
  padding: 0px 3px;
  cursor: pointer;
}
.livedata-pagination .page-nav.current {
  font-weight: bold;
}
.livedata-pagination .page-nav:hover {
  text-decoration: underline;
}
.livedata-pagination .page-nav:not(:hover) {
  text-decoration: none;
}

.livedata-pagination .pagination-page-size {
  margin-right: 1rem;
}
.livedata-pagination .pagination-page-size select {
  height: unset;
  padding: 2px 4px;
}


</style>
