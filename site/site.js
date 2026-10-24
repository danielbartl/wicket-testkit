// Shared by all pages of the wicket-testkit site: theme toggle (auto -> light -> dark), syntax colors and copy
// buttons for code blocks, tabs, the guide's current section, and the "paths break" demo on the start page.
(function () {
  "use strict";

  // ---------- theme ----------
  var KEY = "wicket-testkit-theme";
  var MODES = ["auto", "light", "dark"];
  var themeBtn = document.querySelector(".theme-btn");
  function stored() {
    try { return localStorage.getItem(KEY) || "auto"; } catch (e) { return "auto"; }
  }
  function applyTheme(mode) {
    if (mode === "auto") { document.documentElement.removeAttribute("data-theme"); }
    else { document.documentElement.setAttribute("data-theme", mode); }
    if (themeBtn) {
      themeBtn.innerHTML = "<span class=\"lbl\">Theme: </span>" + mode;
      themeBtn.setAttribute("aria-label", "Color theme: " + mode + ". Change it.");
    }
  }
  applyTheme(stored());
  if (themeBtn) {
    themeBtn.addEventListener("click", function () {
      var next = MODES[(MODES.indexOf(stored()) + 1) % MODES.length];
      try { localStorage.setItem(KEY, next); } catch (e) { /* the choice just isn't remembered */ }
      applyTheme(next);
    });
  }

  // ---------- syntax colors ----------
  var JAVA = new RegExp([
    "(\\/\\/[^\\n]*|\\/\\*[\\s\\S]*?\\*\\/)",                // 1 comment
    "(\"\"\"[\\s\\S]*?\"\"\"|\"(?:[^\"\\\\\\n]|\\\\.)*\")",  // 2 string or text block
    "(@[A-Za-z]\\w*)",                                       // 3 annotation
    "\\b(public|private|protected|static|final|class|record|void|new|return|var|import|package|extends|" +
      "implements|if|else|for|try|catch|finally|throw|throws|this|null|true|false|int|boolean)\\b" // 4 keyword
  ].join("|"), "g");
  var XML = /(<!--[\s\S]*?-->)|(<\/?[\w.:-]+>)/g;
  function escapeHtml(text) {
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  }
  function colorize(text, pattern, classes) {
    var out = "", last = 0, m;
    pattern.lastIndex = 0;
    while ((m = pattern.exec(text)) !== null) {
      out += escapeHtml(text.slice(last, m.index));
      for (var g = 1; g < m.length; g++) {
        if (m[g] !== undefined) { out += "<span class=\"" + classes[g - 1] + "\">" + escapeHtml(m[g]) + "</span>"; break; }
      }
      last = pattern.lastIndex;
    }
    return out + escapeHtml(text.slice(last));
  }
  document.querySelectorAll("pre code[data-lang]").forEach(function (code) {
    var lang = code.getAttribute("data-lang");
    if (lang === "java" || lang === "kotlin") {
      code.innerHTML = colorize(code.textContent, JAVA, ["c", "s", "a", "k"]);
    } else if (lang === "xml") {
      code.innerHTML = colorize(code.textContent, XML, ["c", "k"]);
    }
  });

  // ---------- copy buttons ----------
  document.querySelectorAll(".code .copy").forEach(function (button) {
    button.addEventListener("click", function () {
      var pre = button.closest(".code").querySelector("pre");
      var text = pre.textContent;
      function done(label) {
        button.textContent = label;
        setTimeout(function () { button.textContent = "Copy"; }, 1600);
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(function () { done("Copied"); }, function () { select(pre); done("Selected"); });
      } else {
        select(pre);
        done("Selected");
      }
    });
  });
  function select(element) {
    var range = document.createRange();
    range.selectNodeContents(element);
    var selection = window.getSelection();
    selection.removeAllRanges();
    selection.addRange(range);
  }

  // ---------- tabs ----------
  document.querySelectorAll("[data-tabs]").forEach(function (group) {
    var tabs = Array.prototype.slice.call(group.querySelectorAll("[role=tab]"));
    function show(tab) {
      tabs.forEach(function (t) {
        var selected = t === tab;
        t.setAttribute("aria-selected", String(selected));
        t.tabIndex = selected ? 0 : -1;
        document.getElementById(t.getAttribute("aria-controls")).hidden = !selected;
      });
    }
    tabs.forEach(function (tab, i) {
      tab.addEventListener("click", function () { show(tab); });
      tab.addEventListener("keydown", function (e) {
        var step = e.key === "ArrowRight" ? 1 : e.key === "ArrowLeft" ? -1 : 0;
        if (step) {
          var next = tabs[(i + step + tabs.length) % tabs.length];
          show(next);
          next.focus();
        }
      });
    });
  });

  // ---------- the guide's contents: collapsed on narrow screens, where they sit above the text ----------
  var tocDetails = document.querySelector(".toc details");
  if (tocDetails && window.matchMedia("(max-width: 960px)").matches) {
    tocDetails.open = false;
  }

  // ---------- the guide's current section ----------
  var tocLinks = document.querySelectorAll(".toc a[href^='#']");
  if (tocLinks.length && "IntersectionObserver" in window) {
    var byId = {};
    tocLinks.forEach(function (a) { byId[a.getAttribute("href").slice(1)] = a; });
    var headings = Array.prototype.filter.call(document.querySelectorAll("main h2[id], main h3[id]"),
      function (h) { return byId[h.id]; });
    var visible = {};
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) { visible[entry.target.id] = entry.isIntersecting; });
      var current = null;
      for (var i = 0; i < headings.length; i++) {
        if (visible[headings[i].id]) { current = headings[i]; break; }
      }
      if (current) {
        tocLinks.forEach(function (a) { a.removeAttribute("aria-current"); });
        byId[current.id].setAttribute("aria-current", "true");
      }
    }, { rootMargin: "-70px 0px -60% 0px" });
    headings.forEach(function (h) { observer.observe(h); });
  }

  // ---------- "paths break, locators don't": the start page's component tree ----------
  var demo = document.getElementById("demo");
  if (demo) {
    var ORDERS = [
      { id: 41, customer: "ACME", status: "open" },
      { id: 42, customer: "Globex", status: "open" },
      { id: 43, customer: "ACME", status: "shipped" }
    ];
    var state = { wrapped: false, sorted: false };
    var tree = demo.querySelector(".tree");
    var pathVerdict = demo.querySelector("[data-verdict=path]");
    var locatorVerdict = demo.querySelector("[data-verdict=locator]");

    var line = function (depth, id, type, value, cls, tag) {
      var li = document.createElement("li");
      li.className = cls || "";
      li.style.setProperty("--depth", depth);
      var name = document.createElement("span");
      name.className = "id";
      name.textContent = id;
      li.appendChild(name);
      var kind = document.createElement("span");
      kind.className = "type";
      kind.textContent = type;
      li.appendChild(kind);
      if (value) {
        var v = document.createElement("span");
        v.className = "value";
        v.textContent = value;
        li.appendChild(v);
      }
      if (tag) {
        var t = document.createElement("span");
        t.className = "tag";
        t.textContent = tag;
        li.appendChild(t);
      }
      return li;
    };

    var render = function () {
      var rows = ORDERS.slice();
      if (state.sorted) {
        rows.sort(function (a, b) { return a.customer.localeCompare(b.customer) || a.id - b.id; });
      }
      var prefix = state.wrapped ? "content:" : "";
      var globexRow = rows.findIndex(function (o) { return o.id === 42; });
      var pathTarget = state.wrapped ? null : rows[1];
      var depth = state.wrapped ? 1 : 0;

      tree.textContent = "";
      tree.appendChild(line(0, "OrdersPage", "WebPage", "", "root"));
      tree.appendChild(line(1, "title", "Label", "\"Orders\""));
      if (state.wrapped) { tree.appendChild(line(1, "content", "WebMarkupContainer", "", "added", "new")); }
      tree.appendChild(line(depth + 1, "orders", "ListView", ""));
      rows.forEach(function (order, i) {
        tree.appendChild(line(depth + 2, String(i), "ListItem", "Order " + order.id));
        var cls = "", tag = "";
        if (order.id === 42) { cls = "found"; tag = "locator"; }
        if (pathTarget === order) {
          cls += order.id === 42 ? " path" : " path wrong";
          tag = order.id === 42 ? "path, locator" : "path";
        }
        tree.appendChild(line(depth + 3, "customer", "Label", "\"" + order.customer + "\"", cls, tag));
        tree.appendChild(line(depth + 3, "status", "Label", "\"" + order.status + "\""));
      });

      if (state.wrapped) {
        pathVerdict.className = "verdict bad";
        pathVerdict.innerHTML = "<b>Fails:</b> path: 'orders:1:customer' does not exist for page: OrdersPage";
      } else if (pathTarget.id !== 42) {
        pathVerdict.className = "verdict bad";
        pathVerdict.innerHTML = "<b>Wrong row:</b> it now reads " + pathTarget.customer + "&#8217;s order " +
          pathTarget.id + ". The test fails, and a check that happens to pass checks the wrong order.";
      } else {
        pathVerdict.className = "verdict good";
        pathVerdict.innerHTML = "<b>Passes:</b> row 1 shows Globex, for now.";
      }
      locatorVerdict.className = "verdict good";
      locatorVerdict.innerHTML = "<b>Passes:</b> finds Globex&#8217;s customer label at <code>" + prefix +
        "orders:" + globexRow + ":customer</code>.";
      demo.querySelectorAll("[data-change]").forEach(function (b) {
        b.setAttribute("aria-pressed", String(state[b.getAttribute("data-change")]));
      });
    };

    demo.querySelectorAll("[data-change]").forEach(function (button) {
      button.addEventListener("click", function () {
        var key = button.getAttribute("data-change");
        state[key] = !state[key];
        render();
      });
    });
    var reset = demo.querySelector("[data-reset]");
    if (reset) {
      reset.addEventListener("click", function () {
        state.wrapped = false;
        state.sorted = false;
        render();
      });
    }
    render();
  }
})();
