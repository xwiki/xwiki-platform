import { tryFallibleOrError as ge, assertUnreachable as Ie } from "@xwiki/platform-fn-utils";
function Xe(C) {
  return C && C.__esModule && Object.prototype.hasOwnProperty.call(C, "default") ? C.default : C;
}
var me = { exports: {} }, ie = {};
/**
 * @license React
 * react-jsx-runtime.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
var Le;
function Ve() {
  if (Le) return ie;
  Le = 1;
  var C = Symbol.for("react.transitional.element"), o = Symbol.for("react.fragment");
  function p(R, v, T) {
    var g = null;
    if (T !== void 0 && (g = "" + T), v.key !== void 0 && (g = "" + v.key), "key" in v) {
      T = {};
      for (var k in v)
        k !== "key" && (T[k] = v[k]);
    } else T = v;
    return v = T.ref, {
      $$typeof: C,
      type: R,
      key: g,
      ref: v !== void 0 ? v : null,
      props: T
    };
  }
  return ie.Fragment = o, ie.jsx = p, ie.jsxs = p, ie;
}
var le = {}, ye = { exports: {} }, f = {};
/**
 * @license React
 * react.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
var Ye;
function Je() {
  if (Ye) return f;
  Ye = 1;
  var C = Symbol.for("react.transitional.element"), o = Symbol.for("react.portal"), p = Symbol.for("react.fragment"), R = Symbol.for("react.strict_mode"), v = Symbol.for("react.profiler"), T = Symbol.for("react.consumer"), g = Symbol.for("react.context"), k = Symbol.for("react.forward_ref"), P = Symbol.for("react.suspense"), N = Symbol.for("react.memo"), I = Symbol.for("react.lazy"), x = Symbol.for("react.activity"), Y = Symbol.iterator;
  function W(t) {
    return t === null || typeof t != "object" ? null : (t = Y && t[Y] || t["@@iterator"], typeof t == "function" ? t : null);
  }
  var G = {
    isMounted: function() {
      return !1;
    },
    enqueueForceUpdate: function() {
    },
    enqueueReplaceState: function() {
    },
    enqueueSetState: function() {
    }
  }, te = Object.assign, K = {};
  function q(t, n, a) {
    this.props = t, this.context = n, this.refs = K, this.updater = a || G;
  }
  q.prototype.isReactComponent = {}, q.prototype.setState = function(t, n) {
    if (typeof t != "object" && typeof t != "function" && t != null)
      throw Error(
        "takes an object of state variables to update or a function which returns an object of state variables."
      );
    this.updater.enqueueSetState(this, t, n, "setState");
  }, q.prototype.forceUpdate = function(t) {
    this.updater.enqueueForceUpdate(this, t, "forceUpdate");
  };
  function Q() {
  }
  Q.prototype = q.prototype;
  function re(t, n, a) {
    this.props = t, this.context = n, this.refs = K, this.updater = a || G;
  }
  var X = re.prototype = new Q();
  X.constructor = re, te(X, q.prototype), X.isPureReactComponent = !0;
  var D = Array.isArray;
  function ne() {
  }
  var w = { H: null, A: null, T: null, S: null }, ue = Object.prototype.hasOwnProperty;
  function $(t, n, a) {
    var c = a.ref;
    return {
      $$typeof: C,
      type: t,
      key: n,
      ref: c !== void 0 ? c : null,
      props: a
    };
  }
  function V(t, n) {
    return $(t.type, n, t.props);
  }
  function oe(t) {
    return typeof t == "object" && t !== null && t.$$typeof === C;
  }
  function b(t) {
    var n = { "=": "=0", ":": "=2" };
    return "$" + t.replace(/[=:]/g, function(a) {
      return n[a];
    });
  }
  var J = /\/+/g;
  function z(t, n) {
    return typeof t == "object" && t !== null && t.key != null ? b("" + t.key) : n.toString(36);
  }
  function U(t) {
    switch (t.status) {
      case "fulfilled":
        return t.value;
      case "rejected":
        throw t.reason;
      default:
        switch (typeof t.status == "string" ? t.then(ne, ne) : (t.status = "pending", t.then(
          function(n) {
            t.status === "pending" && (t.status = "fulfilled", t.value = n);
          },
          function(n) {
            t.status === "pending" && (t.status = "rejected", t.reason = n);
          }
        )), t.status) {
          case "fulfilled":
            return t.value;
          case "rejected":
            throw t.reason;
        }
    }
    throw t;
  }
  function M(t, n, a, c, h) {
    var E = typeof t;
    (E === "undefined" || E === "boolean") && (t = null);
    var l = !1;
    if (t === null) l = !0;
    else
      switch (E) {
        case "bigint":
        case "string":
        case "number":
          l = !0;
          break;
        case "object":
          switch (t.$$typeof) {
            case C:
            case o:
              l = !0;
              break;
            case I:
              return l = t._init, M(
                l(t._payload),
                n,
                a,
                c,
                h
              );
          }
      }
    if (l)
      return h = h(t), l = c === "" ? "." + z(t, 0) : c, D(h) ? (a = "", l != null && (a = l.replace(J, "$&/") + "/"), M(h, n, a, "", function(F) {
        return F;
      })) : h != null && (oe(h) && (h = V(
        h,
        a + (h.key == null || t && t.key === h.key ? "" : ("" + h.key).replace(
          J,
          "$&/"
        ) + "/") + l
      )), n.push(h)), 1;
    l = 0;
    var j = c === "" ? "." : c + ":";
    if (D(t))
      for (var A = 0; A < t.length; A++)
        c = t[A], E = j + z(c, A), l += M(
          c,
          n,
          a,
          E,
          h
        );
    else if (A = W(t), typeof A == "function")
      for (t = A.call(t), A = 0; !(c = t.next()).done; )
        c = c.value, E = j + z(c, A++), l += M(
          c,
          n,
          a,
          E,
          h
        );
    else if (E === "object") {
      if (typeof t.then == "function")
        return M(
          U(t),
          n,
          a,
          c,
          h
        );
      throw n = String(t), Error(
        "Objects are not valid as a React child (found: " + (n === "[object Object]" ? "object with keys {" + Object.keys(t).join(", ") + "}" : n) + "). If you meant to render a collection of children, use an array instead."
      );
    }
    return l;
  }
  function H(t, n, a) {
    if (t == null) return t;
    var c = [], h = 0;
    return M(t, c, "", "", function(E) {
      return n.call(a, E, h++);
    }), c;
  }
  function Z(t) {
    if (t._status === -1) {
      var n = t._result;
      n = n(), n.then(
        function(a) {
          (t._status === 0 || t._status === -1) && (t._status = 1, t._result = a);
        },
        function(a) {
          (t._status === 0 || t._status === -1) && (t._status = 2, t._result = a);
        }
      ), t._status === -1 && (t._status = 0, t._result = n);
    }
    if (t._status === 1) return t._result.default;
    throw t._result;
  }
  var B = typeof reportError == "function" ? reportError : function(t) {
    if (typeof window == "object" && typeof window.ErrorEvent == "function") {
      var n = new window.ErrorEvent("error", {
        bubbles: !0,
        cancelable: !0,
        message: typeof t == "object" && t !== null && typeof t.message == "string" ? String(t.message) : String(t),
        error: t
      });
      if (!window.dispatchEvent(n)) return;
    } else if (typeof process == "object" && typeof process.emit == "function") {
      process.emit("uncaughtException", t);
      return;
    }
    console.error(t);
  }, se = {
    map: H,
    forEach: function(t, n, a) {
      H(
        t,
        function() {
          n.apply(this, arguments);
        },
        a
      );
    },
    count: function(t) {
      var n = 0;
      return H(t, function() {
        n++;
      }), n;
    },
    toArray: function(t) {
      return H(t, function(n) {
        return n;
      }) || [];
    },
    only: function(t) {
      if (!oe(t))
        throw Error(
          "React.Children.only expected to receive a single React element child."
        );
      return t;
    }
  };
  return f.Activity = x, f.Children = se, f.Component = q, f.Fragment = p, f.Profiler = v, f.PureComponent = re, f.StrictMode = R, f.Suspense = P, f.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE = w, f.__COMPILER_RUNTIME = {
    __proto__: null,
    c: function(t) {
      return w.H.useMemoCache(t);
    }
  }, f.cache = function(t) {
    return function() {
      return t.apply(null, arguments);
    };
  }, f.cacheSignal = function() {
    return null;
  }, f.cloneElement = function(t, n, a) {
    if (t == null)
      throw Error(
        "The argument must be a React element, but you passed " + t + "."
      );
    var c = te({}, t.props), h = t.key;
    if (n != null)
      for (E in n.key !== void 0 && (h = "" + n.key), n)
        !ue.call(n, E) || E === "key" || E === "__self" || E === "__source" || E === "ref" && n.ref === void 0 || (c[E] = n[E]);
    var E = arguments.length - 2;
    if (E === 1) c.children = a;
    else if (1 < E) {
      for (var l = Array(E), j = 0; j < E; j++)
        l[j] = arguments[j + 2];
      c.children = l;
    }
    return $(t.type, h, c);
  }, f.createContext = function(t) {
    return t = {
      $$typeof: g,
      _currentValue: t,
      _currentValue2: t,
      _threadCount: 0,
      Provider: null,
      Consumer: null
    }, t.Provider = t, t.Consumer = {
      $$typeof: T,
      _context: t
    }, t;
  }, f.createElement = function(t, n, a) {
    var c, h = {}, E = null;
    if (n != null)
      for (c in n.key !== void 0 && (E = "" + n.key), n)
        ue.call(n, c) && c !== "key" && c !== "__self" && c !== "__source" && (h[c] = n[c]);
    var l = arguments.length - 2;
    if (l === 1) h.children = a;
    else if (1 < l) {
      for (var j = Array(l), A = 0; A < l; A++)
        j[A] = arguments[A + 2];
      h.children = j;
    }
    if (t && t.defaultProps)
      for (c in l = t.defaultProps, l)
        h[c] === void 0 && (h[c] = l[c]);
    return $(t, E, h);
  }, f.createRef = function() {
    return { current: null };
  }, f.forwardRef = function(t) {
    return { $$typeof: k, render: t };
  }, f.isValidElement = oe, f.lazy = function(t) {
    return {
      $$typeof: I,
      _payload: { _status: -1, _result: t },
      _init: Z
    };
  }, f.memo = function(t, n) {
    return {
      $$typeof: N,
      type: t,
      compare: n === void 0 ? null : n
    };
  }, f.startTransition = function(t) {
    var n = w.T, a = {};
    w.T = a;
    try {
      var c = t(), h = w.S;
      h !== null && h(a, c), typeof c == "object" && c !== null && typeof c.then == "function" && c.then(ne, B);
    } catch (E) {
      B(E);
    } finally {
      n !== null && a.types !== null && (n.types = a.types), w.T = n;
    }
  }, f.unstable_useCacheRefresh = function() {
    return w.H.useCacheRefresh();
  }, f.use = function(t) {
    return w.H.use(t);
  }, f.useActionState = function(t, n, a) {
    return w.H.useActionState(t, n, a);
  }, f.useCallback = function(t, n) {
    return w.H.useCallback(t, n);
  }, f.useContext = function(t) {
    return w.H.useContext(t);
  }, f.useDebugValue = function() {
  }, f.useDeferredValue = function(t, n) {
    return w.H.useDeferredValue(t, n);
  }, f.useEffect = function(t, n) {
    return w.H.useEffect(t, n);
  }, f.useEffectEvent = function(t) {
    return w.H.useEffectEvent(t);
  }, f.useId = function() {
    return w.H.useId();
  }, f.useImperativeHandle = function(t, n, a) {
    return w.H.useImperativeHandle(t, n, a);
  }, f.useInsertionEffect = function(t, n) {
    return w.H.useInsertionEffect(t, n);
  }, f.useLayoutEffect = function(t, n) {
    return w.H.useLayoutEffect(t, n);
  }, f.useMemo = function(t, n) {
    return w.H.useMemo(t, n);
  }, f.useOptimistic = function(t, n) {
    return w.H.useOptimistic(t, n);
  }, f.useReducer = function(t, n, a) {
    return w.H.useReducer(t, n, a);
  }, f.useRef = function(t) {
    return w.H.useRef(t);
  }, f.useState = function(t) {
    return w.H.useState(t);
  }, f.useSyncExternalStore = function(t, n, a) {
    return w.H.useSyncExternalStore(
      t,
      n,
      a
    );
  }, f.useTransition = function() {
    return w.H.useTransition();
  }, f.version = "19.2.0", f;
}
var fe = { exports: {} };
/**
 * @license React
 * react.development.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
fe.exports;
var De;
function Ze() {
  return De || (De = 1, function(C, o) {
    process.env.NODE_ENV !== "production" && function() {
      function p(e, r) {
        Object.defineProperty(T.prototype, e, {
          get: function() {
            console.warn(
              "%s(...) is deprecated in plain JavaScript React classes. %s",
              r[0],
              r[1]
            );
          }
        });
      }
      function R(e) {
        return e === null || typeof e != "object" ? null : (e = Re && e[Re] || e["@@iterator"], typeof e == "function" ? e : null);
      }
      function v(e, r) {
        e = (e = e.constructor) && (e.displayName || e.name) || "ReactClass";
        var s = e + "." + r;
        Te[s] || (console.error(
          "Can't call %s on a component that is not yet mounted. This is a no-op, but it might indicate a bug in your application. Instead, assign to `this.state` directly or define a `state = {};` class property with the desired state in the %s component.",
          r,
          e
        ), Te[s] = !0);
      }
      function T(e, r, s) {
        this.props = e, this.context = r, this.refs = ve, this.updater = s || be;
      }
      function g() {
      }
      function k(e, r, s) {
        this.props = e, this.context = r, this.refs = ve, this.updater = s || be;
      }
      function P() {
      }
      function N(e) {
        return "" + e;
      }
      function I(e) {
        try {
          N(e);
          var r = !1;
        } catch {
          r = !0;
        }
        if (r) {
          r = console;
          var s = r.error, u = typeof Symbol == "function" && Symbol.toStringTag && e[Symbol.toStringTag] || e.constructor.name || "Object";
          return s.call(
            r,
            "The provided key is an unsupported type %s. This value must be coerced to a string before using it here.",
            u
          ), N(e);
        }
      }
      function x(e) {
        if (e == null) return null;
        if (typeof e == "function")
          return e.$$typeof === Be ? null : e.displayName || e.name || null;
        if (typeof e == "string") return e;
        switch (e) {
          case t:
            return "Fragment";
          case a:
            return "Profiler";
          case n:
            return "StrictMode";
          case l:
            return "Suspense";
          case j:
            return "SuspenseList";
          case we:
            return "Activity";
        }
        if (typeof e == "object")
          switch (typeof e.tag == "number" && console.error(
            "Received an unexpected object in getComponentNameFromType(). This is likely a bug in React. Please file an issue."
          ), e.$$typeof) {
            case se:
              return "Portal";
            case h:
              return e.displayName || "Context";
            case c:
              return (e._context.displayName || "Context") + ".Consumer";
            case E:
              var r = e.render;
              return e = e.displayName, e || (e = r.displayName || r.name || "", e = e !== "" ? "ForwardRef(" + e + ")" : "ForwardRef"), e;
            case A:
              return r = e.displayName || null, r !== null ? r : x(e.type) || "Memo";
            case F:
              r = e._payload, e = e._init;
              try {
                return x(e(r));
              } catch {
              }
          }
        return null;
      }
      function Y(e) {
        if (e === t) return "<>";
        if (typeof e == "object" && e !== null && e.$$typeof === F)
          return "<...>";
        try {
          var r = x(e);
          return r ? "<" + r + ">" : "<...>";
        } catch {
          return "<...>";
        }
      }
      function W() {
        var e = _.A;
        return e === null ? null : e.getOwner();
      }
      function G() {
        return Error("react-stack-top-frame");
      }
      function te(e) {
        if (pe.call(e, "key")) {
          var r = Object.getOwnPropertyDescriptor(e, "key").get;
          if (r && r.isReactWarning) return !1;
        }
        return e.key !== void 0;
      }
      function K(e, r) {
        function s() {
          Ae || (Ae = !0, console.error(
            "%s: `key` is not a prop. Trying to access it will result in `undefined` being returned. If you need to access the same value within the child component, you should pass it as a different prop. (https://react.dev/link/special-props)",
            r
          ));
        }
        s.isReactWarning = !0, Object.defineProperty(e, "key", {
          get: s,
          configurable: !0
        });
      }
      function q() {
        var e = x(this.type);
        return je[e] || (je[e] = !0, console.error(
          "Accessing element.ref was removed in React 19. ref is now a regular prop. It will be removed from the JSX Element type in a future release."
        )), e = this.props.ref, e !== void 0 ? e : null;
      }
      function Q(e, r, s, u, i, m) {
        var d = s.ref;
        return e = {
          $$typeof: B,
          type: e,
          key: r,
          props: s,
          _owner: u
        }, (d !== void 0 ? d : null) !== null ? Object.defineProperty(e, "ref", {
          enumerable: !1,
          get: q
        }) : Object.defineProperty(e, "ref", { enumerable: !1, value: null }), e._store = {}, Object.defineProperty(e._store, "validated", {
          configurable: !1,
          enumerable: !1,
          writable: !0,
          value: 0
        }), Object.defineProperty(e, "_debugInfo", {
          configurable: !1,
          enumerable: !1,
          writable: !0,
          value: null
        }), Object.defineProperty(e, "_debugStack", {
          configurable: !1,
          enumerable: !1,
          writable: !0,
          value: i
        }), Object.defineProperty(e, "_debugTask", {
          configurable: !1,
          enumerable: !1,
          writable: !0,
          value: m
        }), Object.freeze && (Object.freeze(e.props), Object.freeze(e)), e;
      }
      function re(e, r) {
        return r = Q(
          e.type,
          r,
          e.props,
          e._owner,
          e._debugStack,
          e._debugTask
        ), e._store && (r._store.validated = e._store.validated), r;
      }
      function X(e) {
        D(e) ? e._store && (e._store.validated = 1) : typeof e == "object" && e !== null && e.$$typeof === F && (e._payload.status === "fulfilled" ? D(e._payload.value) && e._payload.value._store && (e._payload.value._store.validated = 1) : e._store && (e._store.validated = 1));
      }
      function D(e) {
        return typeof e == "object" && e !== null && e.$$typeof === B;
      }
      function ne(e) {
        var r = { "=": "=0", ":": "=2" };
        return "$" + e.replace(/[=:]/g, function(s) {
          return r[s];
        });
      }
      function w(e, r) {
        return typeof e == "object" && e !== null && e.key != null ? (I(e.key), ne("" + e.key)) : r.toString(36);
      }
      function ue(e) {
        switch (e.status) {
          case "fulfilled":
            return e.value;
          case "rejected":
            throw e.reason;
          default:
            switch (typeof e.status == "string" ? e.then(P, P) : (e.status = "pending", e.then(
              function(r) {
                e.status === "pending" && (e.status = "fulfilled", e.value = r);
              },
              function(r) {
                e.status === "pending" && (e.status = "rejected", e.reason = r);
              }
            )), e.status) {
              case "fulfilled":
                return e.value;
              case "rejected":
                throw e.reason;
            }
        }
        throw e;
      }
      function $(e, r, s, u, i) {
        var m = typeof e;
        (m === "undefined" || m === "boolean") && (e = null);
        var d = !1;
        if (e === null) d = !0;
        else
          switch (m) {
            case "bigint":
            case "string":
            case "number":
              d = !0;
              break;
            case "object":
              switch (e.$$typeof) {
                case B:
                case se:
                  d = !0;
                  break;
                case F:
                  return d = e._init, $(
                    d(e._payload),
                    r,
                    s,
                    u,
                    i
                  );
              }
          }
        if (d) {
          d = e, i = i(d);
          var S = u === "" ? "." + w(d, 0) : u;
          return Oe(i) ? (s = "", S != null && (s = S.replace(Ne, "$&/") + "/"), $(i, r, s, "", function(ee) {
            return ee;
          })) : i != null && (D(i) && (i.key != null && (d && d.key === i.key || I(i.key)), s = re(
            i,
            s + (i.key == null || d && d.key === i.key ? "" : ("" + i.key).replace(
              Ne,
              "$&/"
            ) + "/") + S
          ), u !== "" && d != null && D(d) && d.key == null && d._store && !d._store.validated && (s._store.validated = 2), i = s), r.push(i)), 1;
        }
        if (d = 0, S = u === "" ? "." : u + ":", Oe(e))
          for (var y = 0; y < e.length; y++)
            u = e[y], m = S + w(u, y), d += $(
              u,
              r,
              s,
              m,
              i
            );
        else if (y = R(e), typeof y == "function")
          for (y === e.entries && (Pe || console.warn(
            "Using Maps as children is not supported. Use an array of keyed ReactElements instead."
          ), Pe = !0), e = y.call(e), y = 0; !(u = e.next()).done; )
            u = u.value, m = S + w(u, y++), d += $(
              u,
              r,
              s,
              m,
              i
            );
        else if (m === "object") {
          if (typeof e.then == "function")
            return $(
              ue(e),
              r,
              s,
              u,
              i
            );
          throw r = String(e), Error(
            "Objects are not valid as a React child (found: " + (r === "[object Object]" ? "object with keys {" + Object.keys(e).join(", ") + "}" : r) + "). If you meant to render a collection of children, use an array instead."
          );
        }
        return d;
      }
      function V(e, r, s) {
        if (e == null) return e;
        var u = [], i = 0;
        return $(e, u, "", "", function(m) {
          return r.call(s, m, i++);
        }), u;
      }
      function oe(e) {
        if (e._status === -1) {
          var r = e._ioInfo;
          r != null && (r.start = r.end = performance.now()), r = e._result;
          var s = r();
          if (s.then(
            function(i) {
              if (e._status === 0 || e._status === -1) {
                e._status = 1, e._result = i;
                var m = e._ioInfo;
                m != null && (m.end = performance.now()), s.status === void 0 && (s.status = "fulfilled", s.value = i);
              }
            },
            function(i) {
              if (e._status === 0 || e._status === -1) {
                e._status = 2, e._result = i;
                var m = e._ioInfo;
                m != null && (m.end = performance.now()), s.status === void 0 && (s.status = "rejected", s.reason = i);
              }
            }
          ), r = e._ioInfo, r != null) {
            r.value = s;
            var u = s.displayName;
            typeof u == "string" && (r.name = u);
          }
          e._status === -1 && (e._status = 0, e._result = s);
        }
        if (e._status === 1)
          return r = e._result, r === void 0 && console.error(
            `lazy: Expected the result of a dynamic import() call. Instead received: %s

Your code should look like: 
  const MyComponent = lazy(() => import('./MyComponent'))

Did you accidentally put curly braces around the import?`,
            r
          ), "default" in r || console.error(
            `lazy: Expected the result of a dynamic import() call. Instead received: %s

Your code should look like: 
  const MyComponent = lazy(() => import('./MyComponent'))`,
            r
          ), r.default;
        throw e._result;
      }
      function b() {
        var e = _.H;
        return e === null && console.error(
          `Invalid hook call. Hooks can only be called inside of the body of a function component. This could happen for one of the following reasons:
1. You might have mismatching versions of React and the renderer (such as React DOM)
2. You might be breaking the Rules of Hooks
3. You might have more than one copy of React in the same app
See https://react.dev/link/invalid-hook-call for tips about how to debug and fix this problem.`
        ), e;
      }
      function J() {
        _.asyncTransitions--;
      }
      function z(e) {
        if (de === null)
          try {
            var r = ("require" + Math.random()).slice(0, 7);
            de = (C && C[r]).call(
              C,
              "timers"
            ).setImmediate;
          } catch {
            de = function(u) {
              Me === !1 && (Me = !0, typeof MessageChannel > "u" && console.error(
                "This browser does not have a MessageChannel implementation, so enqueuing tasks via await act(async () => ...) will fail. Please file an issue at https://github.com/facebook/react/issues if you encounter this warning."
              ));
              var i = new MessageChannel();
              i.port1.onmessage = u, i.port2.postMessage(void 0);
            };
          }
        return de(e);
      }
      function U(e) {
        return 1 < e.length && typeof AggregateError == "function" ? new AggregateError(e) : e[0];
      }
      function M(e, r) {
        r !== he - 1 && console.error(
          "You seem to have overlapping act() calls, this is not supported. Be sure to await previous act() calls before making a new one. "
        ), he = r;
      }
      function H(e, r, s) {
        var u = _.actQueue;
        if (u !== null)
          if (u.length !== 0)
            try {
              Z(u), z(function() {
                return H(e, r, s);
              });
              return;
            } catch (i) {
              _.thrownErrors.push(i);
            }
          else _.actQueue = null;
        0 < _.thrownErrors.length ? (u = U(_.thrownErrors), _.thrownErrors.length = 0, s(u)) : r(e);
      }
      function Z(e) {
        if (!Ee) {
          Ee = !0;
          var r = 0;
          try {
            for (; r < e.length; r++) {
              var s = e[r];
              do {
                _.didUsePromise = !1;
                var u = s(!1);
                if (u !== null) {
                  if (_.didUsePromise) {
                    e[r] = s, e.splice(0, r);
                    return;
                  }
                  s = u;
                } else break;
              } while (!0);
            }
            e.length = 0;
          } catch (i) {
            e.splice(0, r + 1), _.thrownErrors.push(i);
          } finally {
            Ee = !1;
          }
        }
      }
      typeof __REACT_DEVTOOLS_GLOBAL_HOOK__ < "u" && typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStart == "function" && __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStart(Error());
      var B = Symbol.for("react.transitional.element"), se = Symbol.for("react.portal"), t = Symbol.for("react.fragment"), n = Symbol.for("react.strict_mode"), a = Symbol.for("react.profiler"), c = Symbol.for("react.consumer"), h = Symbol.for("react.context"), E = Symbol.for("react.forward_ref"), l = Symbol.for("react.suspense"), j = Symbol.for("react.suspense_list"), A = Symbol.for("react.memo"), F = Symbol.for("react.lazy"), we = Symbol.for("react.activity"), Re = Symbol.iterator, Te = {}, be = {
        isMounted: function() {
          return !1;
        },
        enqueueForceUpdate: function(e) {
          v(e, "forceUpdate");
        },
        enqueueReplaceState: function(e) {
          v(e, "replaceState");
        },
        enqueueSetState: function(e) {
          v(e, "setState");
        }
      }, Se = Object.assign, ve = {};
      Object.freeze(ve), T.prototype.isReactComponent = {}, T.prototype.setState = function(e, r) {
        if (typeof e != "object" && typeof e != "function" && e != null)
          throw Error(
            "takes an object of state variables to update or a function which returns an object of state variables."
          );
        this.updater.enqueueSetState(this, e, r, "setState");
      }, T.prototype.forceUpdate = function(e) {
        this.updater.enqueueForceUpdate(this, e, "forceUpdate");
      };
      var L = {
        isMounted: [
          "isMounted",
          "Instead, make sure to clean up subscriptions and pending requests in componentWillUnmount to prevent memory leaks."
        ],
        replaceState: [
          "replaceState",
          "Refactor your code to use setState instead (see https://github.com/facebook/react/issues/3236)."
        ]
      };
      for (ce in L)
        L.hasOwnProperty(ce) && p(ce, L[ce]);
      g.prototype = T.prototype, L = k.prototype = new g(), L.constructor = k, Se(L, T.prototype), L.isPureReactComponent = !0;
      var Oe = Array.isArray, Be = Symbol.for("react.client.reference"), _ = {
        H: null,
        A: null,
        T: null,
        S: null,
        actQueue: null,
        asyncTransitions: 0,
        isBatchingLegacy: !1,
        didScheduleLegacyUpdate: !1,
        didUsePromise: !1,
        thrownErrors: [],
        getCurrentStack: null,
        recentlyCreatedOwnerStacks: 0
      }, pe = Object.prototype.hasOwnProperty, Ce = console.createTask ? console.createTask : function() {
        return null;
      };
      L = {
        react_stack_bottom_frame: function(e) {
          return e();
        }
      };
      var Ae, ke, je = {}, Fe = L.react_stack_bottom_frame.bind(
        L,
        G
      )(), Ge = Ce(Y(G)), Pe = !1, Ne = /\/+/g, $e = typeof reportError == "function" ? reportError : function(e) {
        if (typeof window == "object" && typeof window.ErrorEvent == "function") {
          var r = new window.ErrorEvent("error", {
            bubbles: !0,
            cancelable: !0,
            message: typeof e == "object" && e !== null && typeof e.message == "string" ? String(e.message) : String(e),
            error: e
          });
          if (!window.dispatchEvent(r)) return;
        } else if (typeof process == "object" && typeof process.emit == "function") {
          process.emit("uncaughtException", e);
          return;
        }
        console.error(e);
      }, Me = !1, de = null, he = 0, _e = !1, Ee = !1, xe = typeof queueMicrotask == "function" ? function(e) {
        queueMicrotask(function() {
          return queueMicrotask(e);
        });
      } : z;
      L = Object.freeze({
        __proto__: null,
        c: function(e) {
          return b().useMemoCache(e);
        }
      });
      var ce = {
        map: V,
        forEach: function(e, r, s) {
          V(
            e,
            function() {
              r.apply(this, arguments);
            },
            s
          );
        },
        count: function(e) {
          var r = 0;
          return V(e, function() {
            r++;
          }), r;
        },
        toArray: function(e) {
          return V(e, function(r) {
            return r;
          }) || [];
        },
        only: function(e) {
          if (!D(e))
            throw Error(
              "React.Children.only expected to receive a single React element child."
            );
          return e;
        }
      };
      o.Activity = we, o.Children = ce, o.Component = T, o.Fragment = t, o.Profiler = a, o.PureComponent = k, o.StrictMode = n, o.Suspense = l, o.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE = _, o.__COMPILER_RUNTIME = L, o.act = function(e) {
        var r = _.actQueue, s = he;
        he++;
        var u = _.actQueue = r !== null ? r : [], i = !1;
        try {
          var m = e();
        } catch (y) {
          _.thrownErrors.push(y);
        }
        if (0 < _.thrownErrors.length)
          throw M(r, s), e = U(_.thrownErrors), _.thrownErrors.length = 0, e;
        if (m !== null && typeof m == "object" && typeof m.then == "function") {
          var d = m;
          return xe(function() {
            i || _e || (_e = !0, console.error(
              "You called act(async () => ...) without await. This could lead to unexpected testing behaviour, interleaving multiple act calls and mixing their scopes. You should - await act(async () => ...);"
            ));
          }), {
            then: function(y, ee) {
              i = !0, d.then(
                function(ae) {
                  if (M(r, s), s === 0) {
                    try {
                      Z(u), z(function() {
                        return H(
                          ae,
                          y,
                          ee
                        );
                      });
                    } catch (Qe) {
                      _.thrownErrors.push(Qe);
                    }
                    if (0 < _.thrownErrors.length) {
                      var Ke = U(
                        _.thrownErrors
                      );
                      _.thrownErrors.length = 0, ee(Ke);
                    }
                  } else y(ae);
                },
                function(ae) {
                  M(r, s), 0 < _.thrownErrors.length && (ae = U(
                    _.thrownErrors
                  ), _.thrownErrors.length = 0), ee(ae);
                }
              );
            }
          };
        }
        var S = m;
        if (M(r, s), s === 0 && (Z(u), u.length !== 0 && xe(function() {
          i || _e || (_e = !0, console.error(
            "A component suspended inside an `act` scope, but the `act` call was not awaited. When testing React components that depend on asynchronous data, you must await the result:\n\nawait act(() => ...)"
          ));
        }), _.actQueue = null), 0 < _.thrownErrors.length)
          throw e = U(_.thrownErrors), _.thrownErrors.length = 0, e;
        return {
          then: function(y, ee) {
            i = !0, s === 0 ? (_.actQueue = u, z(function() {
              return H(
                S,
                y,
                ee
              );
            })) : y(S);
          }
        };
      }, o.cache = function(e) {
        return function() {
          return e.apply(null, arguments);
        };
      }, o.cacheSignal = function() {
        return null;
      }, o.captureOwnerStack = function() {
        var e = _.getCurrentStack;
        return e === null ? null : e();
      }, o.cloneElement = function(e, r, s) {
        if (e == null)
          throw Error(
            "The argument must be a React element, but you passed " + e + "."
          );
        var u = Se({}, e.props), i = e.key, m = e._owner;
        if (r != null) {
          var d;
          e: {
            if (pe.call(r, "ref") && (d = Object.getOwnPropertyDescriptor(
              r,
              "ref"
            ).get) && d.isReactWarning) {
              d = !1;
              break e;
            }
            d = r.ref !== void 0;
          }
          d && (m = W()), te(r) && (I(r.key), i = "" + r.key);
          for (S in r)
            !pe.call(r, S) || S === "key" || S === "__self" || S === "__source" || S === "ref" && r.ref === void 0 || (u[S] = r[S]);
        }
        var S = arguments.length - 2;
        if (S === 1) u.children = s;
        else if (1 < S) {
          d = Array(S);
          for (var y = 0; y < S; y++)
            d[y] = arguments[y + 2];
          u.children = d;
        }
        for (u = Q(
          e.type,
          i,
          u,
          m,
          e._debugStack,
          e._debugTask
        ), i = 2; i < arguments.length; i++)
          X(arguments[i]);
        return u;
      }, o.createContext = function(e) {
        return e = {
          $$typeof: h,
          _currentValue: e,
          _currentValue2: e,
          _threadCount: 0,
          Provider: null,
          Consumer: null
        }, e.Provider = e, e.Consumer = {
          $$typeof: c,
          _context: e
        }, e._currentRenderer = null, e._currentRenderer2 = null, e;
      }, o.createElement = function(e, r, s) {
        for (var u = 2; u < arguments.length; u++)
          X(arguments[u]);
        u = {};
        var i = null;
        if (r != null)
          for (y in ke || !("__self" in r) || "key" in r || (ke = !0, console.warn(
            "Your app (or one of its dependencies) is using an outdated JSX transform. Update to the modern JSX transform for faster performance: https://react.dev/link/new-jsx-transform"
          )), te(r) && (I(r.key), i = "" + r.key), r)
            pe.call(r, y) && y !== "key" && y !== "__self" && y !== "__source" && (u[y] = r[y]);
        var m = arguments.length - 2;
        if (m === 1) u.children = s;
        else if (1 < m) {
          for (var d = Array(m), S = 0; S < m; S++)
            d[S] = arguments[S + 2];
          Object.freeze && Object.freeze(d), u.children = d;
        }
        if (e && e.defaultProps)
          for (y in m = e.defaultProps, m)
            u[y] === void 0 && (u[y] = m[y]);
        i && K(
          u,
          typeof e == "function" ? e.displayName || e.name || "Unknown" : e
        );
        var y = 1e4 > _.recentlyCreatedOwnerStacks++;
        return Q(
          e,
          i,
          u,
          W(),
          y ? Error("react-stack-top-frame") : Fe,
          y ? Ce(Y(e)) : Ge
        );
      }, o.createRef = function() {
        var e = { current: null };
        return Object.seal(e), e;
      }, o.forwardRef = function(e) {
        e != null && e.$$typeof === A ? console.error(
          "forwardRef requires a render function but received a `memo` component. Instead of forwardRef(memo(...)), use memo(forwardRef(...))."
        ) : typeof e != "function" ? console.error(
          "forwardRef requires a render function but was given %s.",
          e === null ? "null" : typeof e
        ) : e.length !== 0 && e.length !== 2 && console.error(
          "forwardRef render functions accept exactly two parameters: props and ref. %s",
          e.length === 1 ? "Did you forget to use the ref parameter?" : "Any additional parameter will be undefined."
        ), e != null && e.defaultProps != null && console.error(
          "forwardRef render functions do not support defaultProps. Did you accidentally pass a React component?"
        );
        var r = { $$typeof: E, render: e }, s;
        return Object.defineProperty(r, "displayName", {
          enumerable: !1,
          configurable: !0,
          get: function() {
            return s;
          },
          set: function(u) {
            s = u, e.name || e.displayName || (Object.defineProperty(e, "name", { value: u }), e.displayName = u);
          }
        }), r;
      }, o.isValidElement = D, o.lazy = function(e) {
        e = { _status: -1, _result: e };
        var r = {
          $$typeof: F,
          _payload: e,
          _init: oe
        }, s = {
          name: "lazy",
          start: -1,
          end: -1,
          value: null,
          owner: null,
          debugStack: Error("react-stack-top-frame"),
          debugTask: console.createTask ? console.createTask("lazy()") : null
        };
        return e._ioInfo = s, r._debugInfo = [{ awaited: s }], r;
      }, o.memo = function(e, r) {
        e == null && console.error(
          "memo: The first argument must be a component. Instead received: %s",
          e === null ? "null" : typeof e
        ), r = {
          $$typeof: A,
          type: e,
          compare: r === void 0 ? null : r
        };
        var s;
        return Object.defineProperty(r, "displayName", {
          enumerable: !1,
          configurable: !0,
          get: function() {
            return s;
          },
          set: function(u) {
            s = u, e.name || e.displayName || (Object.defineProperty(e, "name", { value: u }), e.displayName = u);
          }
        }), r;
      }, o.startTransition = function(e) {
        var r = _.T, s = {};
        s._updatedFibers = /* @__PURE__ */ new Set(), _.T = s;
        try {
          var u = e(), i = _.S;
          i !== null && i(s, u), typeof u == "object" && u !== null && typeof u.then == "function" && (_.asyncTransitions++, u.then(J, J), u.then(P, $e));
        } catch (m) {
          $e(m);
        } finally {
          r === null && s._updatedFibers && (e = s._updatedFibers.size, s._updatedFibers.clear(), 10 < e && console.warn(
            "Detected a large number of updates inside startTransition. If this is due to a subscription please re-write it to use React provided hooks. Otherwise concurrent mode guarantees are off the table."
          )), r !== null && s.types !== null && (r.types !== null && r.types !== s.types && console.error(
            "We expected inner Transitions to have transferred the outer types set and that you cannot add to the outer Transition while inside the inner.This is a bug in React."
          ), r.types = s.types), _.T = r;
        }
      }, o.unstable_useCacheRefresh = function() {
        return b().useCacheRefresh();
      }, o.use = function(e) {
        return b().use(e);
      }, o.useActionState = function(e, r, s) {
        return b().useActionState(
          e,
          r,
          s
        );
      }, o.useCallback = function(e, r) {
        return b().useCallback(e, r);
      }, o.useContext = function(e) {
        var r = b();
        return e.$$typeof === c && console.error(
          "Calling useContext(Context.Consumer) is not supported and will cause bugs. Did you mean to call useContext(Context) instead?"
        ), r.useContext(e);
      }, o.useDebugValue = function(e, r) {
        return b().useDebugValue(e, r);
      }, o.useDeferredValue = function(e, r) {
        return b().useDeferredValue(e, r);
      }, o.useEffect = function(e, r) {
        return e == null && console.warn(
          "React Hook useEffect requires an effect callback. Did you forget to pass a callback to the hook?"
        ), b().useEffect(e, r);
      }, o.useEffectEvent = function(e) {
        return b().useEffectEvent(e);
      }, o.useId = function() {
        return b().useId();
      }, o.useImperativeHandle = function(e, r, s) {
        return b().useImperativeHandle(e, r, s);
      }, o.useInsertionEffect = function(e, r) {
        return e == null && console.warn(
          "React Hook useInsertionEffect requires an effect callback. Did you forget to pass a callback to the hook?"
        ), b().useInsertionEffect(e, r);
      }, o.useLayoutEffect = function(e, r) {
        return e == null && console.warn(
          "React Hook useLayoutEffect requires an effect callback. Did you forget to pass a callback to the hook?"
        ), b().useLayoutEffect(e, r);
      }, o.useMemo = function(e, r) {
        return b().useMemo(e, r);
      }, o.useOptimistic = function(e, r) {
        return b().useOptimistic(e, r);
      }, o.useReducer = function(e, r, s) {
        return b().useReducer(e, r, s);
      }, o.useRef = function(e) {
        return b().useRef(e);
      }, o.useState = function(e) {
        return b().useState(e);
      }, o.useSyncExternalStore = function(e, r, s) {
        return b().useSyncExternalStore(
          e,
          r,
          s
        );
      }, o.useTransition = function() {
        return b().useTransition();
      }, o.version = "19.2.0", typeof __REACT_DEVTOOLS_GLOBAL_HOOK__ < "u" && typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStop == "function" && __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStop(Error());
    }();
  }(fe, fe.exports)), fe.exports;
}
var Ue;
function We() {
  return Ue || (Ue = 1, process.env.NODE_ENV === "production" ? ye.exports = Je() : ye.exports = Ze()), ye.exports;
}
/**
 * @license React
 * react-jsx-runtime.development.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
var He;
function et() {
  return He || (He = 1, process.env.NODE_ENV !== "production" && function() {
    function C(t) {
      if (t == null) return null;
      if (typeof t == "function")
        return t.$$typeof === oe ? null : t.displayName || t.name || null;
      if (typeof t == "string") return t;
      switch (t) {
        case K:
          return "Fragment";
        case Q:
          return "Profiler";
        case q:
          return "StrictMode";
        case ne:
          return "Suspense";
        case w:
          return "SuspenseList";
        case V:
          return "Activity";
      }
      if (typeof t == "object")
        switch (typeof t.tag == "number" && console.error(
          "Received an unexpected object in getComponentNameFromType(). This is likely a bug in React. Please file an issue."
        ), t.$$typeof) {
          case te:
            return "Portal";
          case X:
            return t.displayName || "Context";
          case re:
            return (t._context.displayName || "Context") + ".Consumer";
          case D:
            var n = t.render;
            return t = t.displayName, t || (t = n.displayName || n.name || "", t = t !== "" ? "ForwardRef(" + t + ")" : "ForwardRef"), t;
          case ue:
            return n = t.displayName || null, n !== null ? n : C(t.type) || "Memo";
          case $:
            n = t._payload, t = t._init;
            try {
              return C(t(n));
            } catch {
            }
        }
      return null;
    }
    function o(t) {
      return "" + t;
    }
    function p(t) {
      try {
        o(t);
        var n = !1;
      } catch {
        n = !0;
      }
      if (n) {
        n = console;
        var a = n.error, c = typeof Symbol == "function" && Symbol.toStringTag && t[Symbol.toStringTag] || t.constructor.name || "Object";
        return a.call(
          n,
          "The provided key is an unsupported type %s. This value must be coerced to a string before using it here.",
          c
        ), o(t);
      }
    }
    function R(t) {
      if (t === K) return "<>";
      if (typeof t == "object" && t !== null && t.$$typeof === $)
        return "<...>";
      try {
        var n = C(t);
        return n ? "<" + n + ">" : "<...>";
      } catch {
        return "<...>";
      }
    }
    function v() {
      var t = b.A;
      return t === null ? null : t.getOwner();
    }
    function T() {
      return Error("react-stack-top-frame");
    }
    function g(t) {
      if (J.call(t, "key")) {
        var n = Object.getOwnPropertyDescriptor(t, "key").get;
        if (n && n.isReactWarning) return !1;
      }
      return t.key !== void 0;
    }
    function k(t, n) {
      function a() {
        M || (M = !0, console.error(
          "%s: `key` is not a prop. Trying to access it will result in `undefined` being returned. If you need to access the same value within the child component, you should pass it as a different prop. (https://react.dev/link/special-props)",
          n
        ));
      }
      a.isReactWarning = !0, Object.defineProperty(t, "key", {
        get: a,
        configurable: !0
      });
    }
    function P() {
      var t = C(this.type);
      return H[t] || (H[t] = !0, console.error(
        "Accessing element.ref was removed in React 19. ref is now a regular prop. It will be removed from the JSX Element type in a future release."
      )), t = this.props.ref, t !== void 0 ? t : null;
    }
    function N(t, n, a, c, h, E) {
      var l = a.ref;
      return t = {
        $$typeof: G,
        type: t,
        key: n,
        props: a,
        _owner: c
      }, (l !== void 0 ? l : null) !== null ? Object.defineProperty(t, "ref", {
        enumerable: !1,
        get: P
      }) : Object.defineProperty(t, "ref", { enumerable: !1, value: null }), t._store = {}, Object.defineProperty(t._store, "validated", {
        configurable: !1,
        enumerable: !1,
        writable: !0,
        value: 0
      }), Object.defineProperty(t, "_debugInfo", {
        configurable: !1,
        enumerable: !1,
        writable: !0,
        value: null
      }), Object.defineProperty(t, "_debugStack", {
        configurable: !1,
        enumerable: !1,
        writable: !0,
        value: h
      }), Object.defineProperty(t, "_debugTask", {
        configurable: !1,
        enumerable: !1,
        writable: !0,
        value: E
      }), Object.freeze && (Object.freeze(t.props), Object.freeze(t)), t;
    }
    function I(t, n, a, c, h, E) {
      var l = n.children;
      if (l !== void 0)
        if (c)
          if (z(l)) {
            for (c = 0; c < l.length; c++)
              x(l[c]);
            Object.freeze && Object.freeze(l);
          } else
            console.error(
              "React.jsx: Static children should always be an array. You are likely explicitly calling React.jsxs or React.jsxDEV. Use the Babel transform instead."
            );
        else x(l);
      if (J.call(n, "key")) {
        l = C(t);
        var j = Object.keys(n).filter(function(F) {
          return F !== "key";
        });
        c = 0 < j.length ? "{key: someKey, " + j.join(": ..., ") + ": ...}" : "{key: someKey}", se[l + c] || (j = 0 < j.length ? "{" + j.join(": ..., ") + ": ...}" : "{}", console.error(
          `A props object containing a "key" prop is being spread into JSX:
  let props = %s;
  <%s {...props} />
React keys must be passed directly to JSX without using spread:
  let props = %s;
  <%s key={someKey} {...props} />`,
          c,
          l,
          j,
          l
        ), se[l + c] = !0);
      }
      if (l = null, a !== void 0 && (p(a), l = "" + a), g(n) && (p(n.key), l = "" + n.key), "key" in n) {
        a = {};
        for (var A in n)
          A !== "key" && (a[A] = n[A]);
      } else a = n;
      return l && k(
        a,
        typeof t == "function" ? t.displayName || t.name || "Unknown" : t
      ), N(
        t,
        l,
        a,
        v(),
        h,
        E
      );
    }
    function x(t) {
      Y(t) ? t._store && (t._store.validated = 1) : typeof t == "object" && t !== null && t.$$typeof === $ && (t._payload.status === "fulfilled" ? Y(t._payload.value) && t._payload.value._store && (t._payload.value._store.validated = 1) : t._store && (t._store.validated = 1));
    }
    function Y(t) {
      return typeof t == "object" && t !== null && t.$$typeof === G;
    }
    var W = We(), G = Symbol.for("react.transitional.element"), te = Symbol.for("react.portal"), K = Symbol.for("react.fragment"), q = Symbol.for("react.strict_mode"), Q = Symbol.for("react.profiler"), re = Symbol.for("react.consumer"), X = Symbol.for("react.context"), D = Symbol.for("react.forward_ref"), ne = Symbol.for("react.suspense"), w = Symbol.for("react.suspense_list"), ue = Symbol.for("react.memo"), $ = Symbol.for("react.lazy"), V = Symbol.for("react.activity"), oe = Symbol.for("react.client.reference"), b = W.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE, J = Object.prototype.hasOwnProperty, z = Array.isArray, U = console.createTask ? console.createTask : function() {
      return null;
    };
    W = {
      react_stack_bottom_frame: function(t) {
        return t();
      }
    };
    var M, H = {}, Z = W.react_stack_bottom_frame.bind(
      W,
      T
    )(), B = U(R(T)), se = {};
    le.Fragment = K, le.jsx = function(t, n, a) {
      var c = 1e4 > b.recentlyCreatedOwnerStacks++;
      return I(
        t,
        n,
        a,
        !1,
        c ? Error("react-stack-top-frame") : Z,
        c ? U(R(t)) : B
      );
    }, le.jsxs = function(t, n, a) {
      var c = 1e4 > b.recentlyCreatedOwnerStacks++;
      return I(
        t,
        n,
        a,
        !0,
        c ? Error("react-stack-top-frame") : Z,
        c ? U(R(t)) : B
      );
    };
  }()), le;
}
var qe;
function tt() {
  return qe || (qe = 1, process.env.NODE_ENV === "production" ? me.exports = Ve() : me.exports = et()), me.exports;
}
var O = tt(), rt = We();
const ze = /* @__PURE__ */ Xe(rt);
class ot {
  constructor(o, p) {
    this.remoteURLParser = o, this.remoteURLSerializer = p;
  }
  /**
   * Render a macro's AST blocks to JSX elements
   *
   * Will force re-render every time, even if the AST is exactly the same
   * (see the private `generateId` function for more informations)
   *
   * @param blocks - The blocks to render
   * @param editableZoneRef - The macro's editable zone reference
   *
   * @returns The JSX elements
   */
  blocksToReactJSX(o, p) {
    return ge(
      () => this.convertBlocks(o, p, this.generateId())
    );
  }
  /**
   * Render a macro's AST inline contents to JSX elements
   *
   * Will force re-render every time, even if the AST is exactly the same
   * (see the private `generateId` function for more informations)
   *
   * @param inlineContents - The inline contents to render
   * @param editableZoneRef - The macro's editable zone reference
   *
   * @returns The JSX elements
   */
  inlineContentsToReactJSX(o, p) {
    return ge(
      () => this.convertInlineContents(
        o,
        p,
        this.generateId()
      )
    );
  }
  generateId() {
    return Math.random().toString().substring(2);
  }
  convertBlocks(o, p, R) {
    return o.map((v, T) => {
      const g = `${R}.${T}`;
      return /* @__PURE__ */ O.jsx(ze.Fragment, { children: this.convertBlock(v, p, g) }, g);
    });
  }
  convertInlineContents(o, p, R) {
    return o.map((v, T) => {
      const g = `${R}.${T}`;
      return /* @__PURE__ */ O.jsx(ze.Fragment, { children: this.convertInlineContent(v, p, R) }, g);
    });
  }
  convertBlock(o, p, R) {
    switch (o.type) {
      case "paragraph":
        return /* @__PURE__ */ O.jsx("p", { ...this.convertBlockStyles(o.styles), children: this.convertInlineContents(o.content, p, R) });
      case "heading":
        const v = `h${o.level}`;
        return /* @__PURE__ */ O.jsx(v, { ...this.convertBlockStyles(o.styles), children: this.convertInlineContents(o.content, p, R) });
      case "list":
        const T = o.numbered ? "ol" : "ul";
        return /* @__PURE__ */ O.jsx(T, { ...this.convertBlockStyles(o.styles), children: o.items.map((g, k) => {
          const P = `${R}.${k}`;
          return /* @__PURE__ */ O.jsxs("li", { children: [
            g.checked !== void 0 && /* @__PURE__ */ O.jsx("input", { type: "checkbox", checked: g.checked, readOnly: !0 }),
            this.convertInlineContents(
              g.content,
              p,
              P
            )
          ] }, P);
        }) });
      case "quote":
        return /* @__PURE__ */ O.jsx("blockquote", { ...this.convertBlockStyles(o.styles), children: this.convertBlocks(o.content, p, R) });
      case "code":
        return /* @__PURE__ */ O.jsx("code", { children: o.content });
      case "table":
        return /* @__PURE__ */ O.jsxs("table", { ...this.convertBlockStyles(o.styles), children: [
          /* @__PURE__ */ O.jsx("colgroup", { children: o.columns.map((g, k) => {
            const P = {};
            g.widthPx && (P.style = { width: `${g.widthPx}px` });
            const N = `${R}.${k}`;
            return /* @__PURE__ */ O.jsx("col", { ...P }, N);
          }) }),
          o.columns.find((g) => g.headerCell) && /* @__PURE__ */ O.jsx("thead", { children: /* @__PURE__ */ O.jsx("tr", { children: o.columns.map((g, k) => {
            const P = `${R}.${k}`;
            return /* @__PURE__ */ O.jsx(
              "th",
              {
                ...g.headerCell ? this.convertBlockStyles(g.headerCell.styles) : {},
                children: g.headerCell && this.convertInlineContents(
                  g.headerCell.content,
                  p,
                  R
                )
              },
              P
            );
          }) }) }),
          /* @__PURE__ */ O.jsx("tbody", { children: o.rows.map((g, k) => {
            const P = `${R}.${k}`;
            return /* @__PURE__ */ O.jsx("tr", { children: g.map((N, I) => {
              const x = this.convertBlockStyles(N.styles);
              N.colSpan && (x.colSpan = N.colSpan), N.rowSpan && (x.rowSpan = N.rowSpan);
              const Y = `${P}.${I}`;
              return /* @__PURE__ */ O.jsx("td", { ...x, children: this.convertInlineContents(
                N.content,
                p,
                Y
              ) }, Y);
            }) }, P);
          }) })
        ] });
      case "image":
        return /* @__PURE__ */ O.jsx(
          "img",
          {
            src: this.getTargetUrl(o.target),
            alt: o.alt,
            width: o.widthPx,
            height: o.heightPx
          }
        );
      case "rawHtml":
        return /* @__PURE__ */ O.jsx("div", { dangerouslySetInnerHTML: { __html: o.html } });
      case "macroBlock":
        throw new Error("Nested macros are not supported yet");
      case "macroBlockEditableArea":
        if (p.type === "inline")
          throw new Error(
            'Provided editable zone React ref is of type "inline", but macro requests type "block"'
          );
        return /* @__PURE__ */ O.jsx("div", { ref: p.ref });
      default:
        Ie(o);
    }
  }
  convertBlockStyles(o) {
    const p = {};
    return o.backgroundColor && ((p.style ??= {}).backgroundColor = o.backgroundColor), o.textColor && ((p.style ??= {}).color = o.textColor), o.textAlignment && ((p.style ??= {}).textAlign = o.textAlignment), o.cssClasses && (p.className = o.cssClasses.join(" ")), p;
  }
  convertInlineContent(o, p, R) {
    switch (o.type) {
      case "text":
        const v = {};
        o.styles.backgroundColor && (v.backgroundColor = o.styles.backgroundColor), o.styles.textColor && (v.color = o.styles.textColor), o.styles.bold && (v.fontWeight = "bold"), o.styles.italic && (v.fontStyle = "italic"), o.styles.underline && (v.textDecoration = "underline"), o.styles.strikethrough && (v.textDecoration = "line-through");
        const T = Object.values(v).length > 0 ? { style: v } : {};
        return /* @__PURE__ */ O.jsx("span", { ...T, children: o.content });
      case "link":
        return /* @__PURE__ */ O.jsx("a", { href: this.getTargetUrl(o.target), children: this.convertInlineContents(
          o.content,
          p,
          R
        ) });
      case "rawHtml":
        return /* @__PURE__ */ O.jsx("span", { dangerouslySetInnerHTML: { __html: o.html } });
      case "inlineMacro":
        throw new Error("Nested macros are not supported yet");
      case "inlineMacroEditableArea":
        if (p.type === "block")
          throw new Error(
            'Provided editable zone React ref is of type "block", but macro requests type "inline"'
          );
        return /* @__PURE__ */ O.jsx("span", { ref: p.ref });
      default:
        Ie(o);
    }
  }
  getTargetUrl(o) {
    if (o.type === "external")
      return o.url;
    const { rawReference: p } = o, R = ge(
      () => this.remoteURLParser.parse(p)
    );
    if (R instanceof Error)
      throw new Error(
        `Failed to parse reference "${p}": ${R.message}`
      );
    const v = this.remoteURLSerializer.serialize(R);
    if (!v)
      throw new Error(`Failed to serialize reference "${p}"`);
    return v;
  }
}
export {
  ot as MacrosAstToReactJsxConverter
};
//# sourceMappingURL=index.es.js.map
