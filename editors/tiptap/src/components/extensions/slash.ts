import { Editor, Extension, Range } from "@tiptap/core";
import Suggestion from "@tiptap/suggestion";
import { App, createApp } from "vue";
import Selector from "../../vue/c-tiptap-selector.vue";
import { Plugin } from "prosemirror-state";
import { CommandParams } from "./menu-helpers";

const Slash = Extension.create({
  name: "slash",
  addOptions() {
    return {
      suggestion: {
        char: "/",
        startOfLine: true,
        command: ({ editor, range, props }: CommandParams) => {
          props.command({ editor, range, props });
        },
        items: getSuggestionItems,
        render: renderItems,
      },
    };
  },
  addProseMirrorPlugins(): Plugin[] {
    return [
      Suggestion({
        editor: this.editor,
        ...this.options.suggestion,
      }),
    ];
  },
});

/**
 * Define the descriptor for action categories.
 * A category is composed of the action title and a set of actions linked to
 * that category.
 * @since 0.8
 */
export interface ActionCategoryDescriptor {
  actions: ActionDescriptor[];
  title: string;
}

/**
 * Defines the structure of a slash action descriptor.
 *
 * @since 0.8
 */
export interface ActionDescriptor {
  title: string;
  /**
   * An optional sort field to be used instead of the title when sorting the
   * actions.
   */
  sortField?: string;
  command: (commandParams: { editor: Editor; range: Range }) => void;
  icon: string;
  hint: string;
  /**
   * A list of strings that are not expected to be displayed but that are used
   * when filtering for actions in the UI.
   */
  aliases?: string[];
}

function getHeadingAction(level: number): ActionDescriptor {
  return {
    title: `Heading ${level}`,
    aliases: [`h${level}`],
    icon: `type-h${level}`,
    hint: `Toggle Heading level ${level}`,
    command({ editor, range }) {
      editor
        .chain()
        .focus()
        .deleteRange(range)
        .setNode("heading", { level: level })
        .run();
    },
  };
}

function getListActions(): ActionDescriptor[] {
  return [
    {
      title: "Bulleted list",
      icon: "list-ul",
      hint: "Toggle bulleted list",
      sortField: "list-bulleted",
      command({ editor, range }) {
        editor.chain().focus().deleteRange(range).toggleBulletList().run();
      },
    },
    {
      title: "Ordered list",
      icon: "list-ol",
      hint: "Toggle ordered list",
      sortField: "list-ordered",
      command({ editor, range }) {
        editor.chain().focus().deleteRange(range).toggleOrderedList().run();
      },
    },
  ];
}

function getTableAction(): ActionDescriptor {
  return {
    title: "Table",
    icon: "table",
    hint: "Insert a table",
    command({ editor, range }) {
      editor.chain().focus().deleteRange(range).insertTable().run();
    },
  };
}

function getBlockquoteAction(): ActionDescriptor {
  return {
    title: "Blockquote",
    icon: "quote",
    hint: "Toggle blockquote",
    command({ editor, range }) {
      editor.chain().focus().deleteRange(range).toggleBlockquote().run();
    },
  };
}

function getCodeBlockAction(): ActionDescriptor {
  return {
    title: "Code",
    icon: "code",
    hint: "Toggle code block",
    command({ editor, range }) {
      editor.chain().focus().deleteRange(range).toggleCodeBlock().run();
    },
  };
}

function getAllActions(): ActionCategoryDescriptor[] {
  const getHeadingActions = [1, 2, 3, 4, 5, 6].map((level) =>
    getHeadingAction(level),
  );

  // TODO: add image, links and attachments.
  return [
    {
      title: "Layout",
      actions: [
        ...getHeadingActions,
        ...getListActions(),
        getTableAction(),
        getBlockquoteAction(),
        getCodeBlockAction(),
      ],
    },
  ];
}

/**
 * Produces an equality operator based on the current query.
 * The equality operation currently returns true of the query is a sub-string
 * of the provided value, without taking into account the case.
 *
 * @param query the query to apply on the provided value
 * @return a lamba taking a string and returning a true when the value matches
 * the query filter, and false otherwise
 */
function queryEqualityOperator(query: string) {
  return (value: string) => {
    return value.toLowerCase().includes(query.toLowerCase());
  };
}

export function filterActionsByQuery(
  query: string,
  actions: ActionCategoryDescriptor[],
) {
  function filterByQueryString(action: ActionDescriptor) {
    const equalityOperator = queryEqualityOperator(query);
    return (
      equalityOperator(action.title) ||
      equalityOperator(action.hint) ||
      action.aliases?.some(equalityOperator)
    );
  }

  function orderByAction(action0: ActionDescriptor, action1: ActionDescriptor) {
    const title0 = action0.sortField || action0.title;
    const title1 = action1.sortField || action1.title;
    return title0 === title1 ? 0 : title0 > title1 ? 1 : -1;
  }

  const categories: ActionCategoryDescriptor[] = actions.flatMap((category) => {
    const filteredActions = category.actions.filter(filterByQueryString);
    filteredActions.sort(orderByAction);
    if (filteredActions.length > 0) {
      return [
        // Clone the category but replace the actions with a filtered list of
        // actions matching the query.
        {
          ...category,
          actions: filteredActions,
        },
      ];
    } else {
      // If no actions of the group are matched by the filter, skip the category
      return [];
    }
  });

  categories.sort((category0, category1) => {
    return category0.title > category1.title ? 1 : -1;
  });

  return categories;
}

function getSuggestionItems({
  query,
}: {
  query: string;
}): ActionCategoryDescriptor[] {
  return filterActionsByQuery(query, getAllActions());
}

function renderItems() {
  let app: App;
  let elemDiv: HTMLDivElement;

  return {
    onExit() {
      app?.unmount();
    },
    onKeyDown({ event }: { event: KeyboardEvent }) {
      const key = event.key;
      if (key === "Escape") {
        app?.unmount();
        document.body.removeChild(elemDiv);
        return true;
      }

      if (key === "ArrowDown" || key === "ArrowUp" || key === "Enter") {
        return (app._instance?.refs.container as HTMLElement).dispatchEvent(
          new KeyboardEvent("keydown", { key: key }),
        );
      }
      return false;
    },
    onStart(props: unknown) {
      elemDiv = document.createElement("div");
      document.body.appendChild(elemDiv);
      app = createApp(Selector, {
        props,
      });
      app.mount(elemDiv);
    },
    onUpdate(props: unknown) {
      if (app._instance) {
        app._instance.props.props = props;
      }
    },
  };
}

export { Slash, renderItems, getSuggestionItems };
