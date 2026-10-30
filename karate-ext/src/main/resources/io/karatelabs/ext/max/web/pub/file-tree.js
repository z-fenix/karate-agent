/*
 * file-tree.js — kaFileTree(config): a reusable, lazy, VS Code-style project-explorer Alpine factory.
 *
 * It owns ALL filesystem CRUD for one project tree and stays decoupled from whatever renders the opened
 * file: selecting a file dispatches a window `ka-file-open` event carrying a resolved VIEW id, so a host
 * page switches its right-pane view (editor / markdown / a future rules or requirement view) with a
 * `switch` — the tree never changes. This is the first pane of the converging 3-pane IDE shell.
 *
 * Lazy: it lists one folder level at a time via `File.ls` (config.lsUrl) — it never walks the whole tree.
 *
 * Use it as the x-data root of components/file-tree.html; the host passes a config FUNCTION name via a
 * data-cfg attribute (so the fragment stays generic):
 *     x-data="kaFileTree(window[$el.dataset.cfg]())"
 *
 * config (all urls hit the /ui/checks data channel — D98, never /api/eval):
 *   lsUrl readUrl writeUrl deleteUrl moveUrl mkdirUrl uploadUrl metaUrl
 *   rootLabel    — header label (e.g. the project name)
 *   initialOpen  — folder paths auto-expanded on first load, e.g. ['checks']
 *   storeKey     — localStorage namespace for the persisted expanded-set
 *   openEvent    — window event dispatched on file open (default 'ka-file-open')
 *   runEvent     — window event dispatched by the context-menu Run (default 'ka-file-run')
 *   viewFor(node)— resolve a node to a right-pane VIEW id (the forward hook). Default: editor/markdown/binary.
 *   lens         — true to host the LENS badge + popover on the header (D193). The badge markup lives in
 *                  the fragment; the lens STATE (lens/lensLabel/lenses/lensOpen/setLens) is the host
 *                  page's, resolved up the Alpine scope chain — this factory only consumes the filter.
 *
 * Events OUT (window):  openEvent {path,name,kind,binary,view} · runEvent {path,dir,kind}
 * Events IN  (window):  ka-file-tree-refresh {dir?,select?} — reload a dir (or all loaded dirs) + optional reselect
 *                       ka-lens-filter {files:{path:1}, dirs:{path:1}, hint?} | null — the LENS filter
 *                       (D193): the ONE tree stays (folders intact, every file op identical); non-matching
 *                       rows are hidden and matching ancestors auto-expand. null clears (the All lens).
 */
function kaFileTree(config) {
  var cfg = Object.assign({
    lsUrl: '/ui/checks/ls', readUrl: '/ui/checks/read', writeUrl: '/ui/checks/write',
    deleteUrl: '/ui/checks/delete', moveUrl: '/ui/checks/move', mkdirUrl: '/ui/checks/mkdir',
    uploadUrl: '/ui/checks/upload', metaUrl: '/ui/checks/meta',
    rootLabel: 'PROJECT', initialOpen: [], storeKey: 'ka-file-tree',
    openEvent: 'ka-file-open', runEvent: 'ka-file-run', viewFor: null,
  }, config || {});

  // extension → file kind. One source of truth for the icon, the colour, and editability (binary or not).
  var BINARY = { pdf: 1, png: 1, jpg: 1, jpeg: 1, gif: 1, webp: 1, ico: 1, bmp: 1, svg: 0, /* svg is text */
    zip: 1, gz: 1, tar: 1, jar: 1, woff: 1, woff2: 1, ttf: 1, otf: 1, eot: 1,
    mp4: 1, mov: 1, webm: 1, mp3: 1, wav: 1, png8: 1, jpe: 1, tif: 1, tiff: 1, exe: 1, bin: 1, dat: 1 };
  var IMAGE = { png: 1, jpg: 1, jpeg: 1, gif: 1, webp: 1, ico: 1, bmp: 1, svg: 1, tif: 1, tiff: 1 };

  function ext(name) { var i = name.lastIndexOf('.'); return i < 0 ? '' : name.slice(i + 1).toLowerCase(); }

  return {
    cfg: cfg,
    nodes: {},            // dir-path → [child nodes]; '' = project root. The lazy cache.
    expanded: {},         // path → bool
    selected: '',         // currently-selected file path
    error: '',
    menu: { open: false, x: 0, y: 0, node: null },
    props: { open: false, x: 0, y: 0, node: null, meta: null, loading: false, copied: false },
    rename: { path: null, name: '' },
    create: { open: false, parent: '', kind: 'file', name: '' },
    drag: { src: null },
    dropTarget: null,     // path of the folder hovered as a drop target ('' = root)
    uploadTarget: '',     // folder the hidden file-input drops into (set just before .click())
    filter: null,         // the active lens filter ({files, dirs, hint?}) or null = All (D193)

    // ---- lifecycle --------------------------------------------------------------------------
    async init() {
      // register the lens-filter listener FIRST (synchronously, before any await) — the host's
      // init() may dispatch the restored lens while this init is still loading the root dir
      window.addEventListener('ka-lens-filter', function (e) { this.applyFilter(e.detail || null); }.bind(this));
      this.loadExpanded();
      await this.loadDir('');
      // auto-open the configured folders + any restored-expanded ones (load their children)
      var toOpen = (cfg.initialOpen || []).slice();
      Object.keys(this.expanded).forEach(function (p) { if (this.expanded[p]) toOpen.push(p); }.bind(this));
      for (var i = 0; i < toOpen.length; i++) { this.expanded[toOpen[i]] = true; await this.loadDir(toOpen[i]); }
      // deep-link: ?file=<project-root-relative path> opens that file (the View control-plane raw route)
      var dl = new URLSearchParams(location.search).get('file');
      if (dl) { await this.openPath(dl); }
      window.addEventListener('ka-file-tree-refresh', function (e) {
        var d = (e && e.detail) || {};
        if (d.dir != null) { this.loadDir(d.dir); } else { this.reloadAll(); }
        if (d.select) { this.openPath(d.select); }
      }.bind(this));
      // a click anywhere dismisses the context menu (capture so a row click closes it first)
      window.addEventListener('click', function () { this.menu.open = false; }.bind(this));
      window.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') { this.menu.open = false; this.props.open = false; }
      }.bind(this));
    },

    // ---- data -------------------------------------------------------------------------------
    async _get(url) {
      try { return await (await fetch(url)).json(); }
      catch (e) { this.error = String(e); return { error: String(e) }; }
    },
    async _post(url, body) {
      try {
        return await (await fetch(url, {
          method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body || {}),
        })).json();
      } catch (e) { this.error = String(e); return { error: String(e) }; }
    },
    async loadDir(dir) {
      var out = await this._get(cfg.lsUrl + '?dir=' + encodeURIComponent(dir));
      if (out && out.error) {
        // a stale expanded path — the folder was deleted elsewhere (e.g. runs purged on the Runs page).
        // Drop it quietly: collapse + forget it, don't alarm with a banner. Only a failing ROOT listing
        // is a genuine error worth surfacing.
        if (dir) { delete this.nodes[dir]; this.expanded[dir] = false; this.persistExpanded(); }
        else { this.error = out.error; }
        return;
      }
      this.error = '';
      var list = Array.isArray(out) ? out : [];
      // decorate with the full project-root-relative path
      this.nodes[dir] = list.map(function (e) {
        return { name: e.name, dir: !!e.dir, size: e.size || 0, modified: e.modified || 0,
                 path: dir ? dir + '/' + e.name : e.name };
      });
    },
    async reloadAll() { for (var dir in this.nodes) { if (this.nodes.hasOwnProperty(dir)) await this.loadDir(dir); } },

    // ---- the lens filter (D193) — hide non-matching rows, keep the tree + every file op ------
    async applyFilter(f) {
      this.filter = f && f.files ? f : null;
      if (this.filter) {
        // auto-expand + lazy-load every ancestor folder so the matches are visible
        var dirs = Object.keys(this.filter.dirs || {});
        for (var i = 0; i < dirs.length; i++) {
          this.expanded[dirs[i]] = true;
          if (!this.nodes[dirs[i]]) await this.loadDir(dirs[i]);
        }
      }
    },

    // ---- the visible (flattened) tree -------------------------------------------------------
    get flatTree() {
      var out = [];
      var self = this;
      var c = this.create;
      (function walk(dir, depth) {
        // the inline new-file/new-folder input renders AS A ROW at the top of its target folder's
        // children — so it visibly lands inside the selected folder (not adrift at the tree top)
        if (c.open && c.parent === dir) out.push({ __create: true, depth: depth, path: '__create__' });
        var kids = self.nodes[dir];
        if (!kids) return;
        for (var i = 0; i < kids.length; i++) {
          var n = kids[i];
          // lens filter: a dir shows only when it holds matches; a file only when it matches
          if (self.filter && (n.dir ? !self.filter.dirs[n.path] : !self.filter.files[n.path])) continue;
          out.push({ name: n.name, dir: n.dir, size: n.size, modified: n.modified, path: n.path, depth: depth });
          if (n.dir && self.expanded[n.path]) walk(n.path, depth + 1);
        }
      })('', 0);
      return out;
    },
    get empty() { var r = this.nodes['']; return !!r && r.length === 0; },
    /** A lens is active but nothing in the project matches it — the fragment shows a soft hint. */
    get filterEmpty() {
      if (!this.filter) return false;
      return this.flatTree.filter(function (n) { return !n.__create; }).length === 0;
    },

    // ---- classification / icons -------------------------------------------------------------
    kindOf(name) {
      var e = ext(name);
      if (e === 'js') return 'js';
      if (e === 'md') return 'goal';
      if (e === 'feature') return 'suite';
      if (IMAGE[e]) return 'image';
      if (e === 'pdf') return 'pdf';
      if (BINARY[e]) return 'binary';
      return 'text';
    },
    isBinary(name) { return !!BINARY[ext(name)]; },
    isImage(name) { return !!IMAGE[ext(name)]; },
    // symbol id in components/icons.html
    iconKey(n) {
      if (n.dir) return this.expanded[n.path] ? 'folder-open' : 'folder';
      var k = this.kindOf(n.name);
      return k === 'js' ? 'code' : k === 'goal' || k === 'suite' || k === 'text' ? 'doc'
        : k === 'image' ? 'photo' : 'file';   // pdf/binary → the plain file glyph
    },
    iconClass(n) {
      if (n.dir) return 'text-slate-400 dark:text-slate-500';
      var c = { js: 'text-amber-500', goal: 'text-violet-500 dark:text-violet-400',
        suite: 'text-sky-500', text: 'text-slate-400 dark:text-slate-500',
        image: 'text-rose-400', pdf: 'text-rose-500', binary: 'text-slate-400 dark:text-slate-500' };
      return c[this.kindOf(n.name)] || 'text-slate-400';
    },

    // ---- selection / expand -----------------------------------------------------------------
    onRow(n) {
      if (n.dir) { this.toggleExpand(n.path); this.selected = n.path; }
      else { this.openFile(n); }
    },
    async toggleExpand(path) {
      if (this.expanded[path]) { this.expanded[path] = false; }
      else { this.expanded[path] = true; if (!this.nodes[path]) await this.loadDir(path); }
      this.persistExpanded();
    },
    openFile(n) {
      this.selected = n.path;
      var view = cfg.viewFor ? cfg.viewFor(n) : (this.isBinary(n.name) ? 'binary' : ext(n.name) === 'md' ? 'markdown' : 'editor');
      this.dispatch(cfg.openEvent, { path: n.path, name: n.name, kind: this.kindOf(n.name),
        binary: this.isBinary(n.name), image: this.isImage(n.name), view: view });
    },
    // open a path that may be deep — load + expand every ancestor folder, then select the leaf
    async openPath(path) {
      var parts = path.split('/'); var dir = '';
      for (var i = 0; i < parts.length - 1; i++) {
        dir = dir ? dir + '/' + parts[i] : parts[i];
        this.expanded[dir] = true;
        if (!this.nodes[dir]) await this.loadDir(dir);
      }
      var parent = parts.slice(0, -1).join('/');
      var kids = this.nodes[parent] || [];
      var leaf = kids.find(function (k) { return k.path === path; });
      if (leaf && !leaf.dir) { this.openFile(leaf); }
      else { this.selected = path; }   // a folder (or not-yet-listed) — at least select it
      this.persistExpanded();
    },

    // ---- the folder new items default into (selected folder / selected file's parent / root) -
    activeParent() {
      if (!this.selected) return '';
      var node = this.findNode(this.selected);
      if (node && node.dir) return this.selected;
      return this.parentOf(this.selected);
    },
    findNode(path) {
      var parent = this.parentOf(path); var kids = this.nodes[parent] || [];
      return kids.find(function (k) { return k.path === path; }) || null;
    },

    // ---- create (file / folder) -------------------------------------------------------------
    startCreate(kind, parent) {
      var p = parent != null ? parent : this.activeParent();
      this.create = { open: true, kind: kind, parent: p, name: '' };
      this.menu.open = false;
      // make sure the target folder is open + loaded so the inline input renders INSIDE it
      if (p) { this.expanded[p] = true; if (!this.nodes[p]) this.loadDir(p); this.persistExpanded(); }
      this.$nextTick(function () { if (this.$refs.createInput) this.$refs.createInput.focus(); }.bind(this));
    },
    cancelCreate() { this.create.open = false; },
    async submitCreate() {
      var raw = (this.create.name || '').trim().replace(/^\/+/, '');
      if (!raw) { this.create.open = false; return; }
      var parent = this.create.parent;
      if (this.create.kind === 'folder') {
        var fp = parent ? parent + '/' + raw : raw;
        var mk = await this._post(cfg.mkdirUrl, { name: fp });
        if (mk && mk.error) { this.error = mk.error; return; }
        if (parent) { this.expanded[parent] = true; await this.loadDir(parent); } else { await this.loadDir(''); }
        this.expanded[fp] = true; await this.loadDir(fp);
        this.selected = fp; this.create.open = false; this.persistExpanded();
        return;
      }
      // file — honour an explicit .md/.feature, else default to a .js check
      var name = raw;
      if (parent && name.indexOf('/') < 0) name = parent + '/' + name;
      if (!/\.(js|md|feature|json|txt|yaml|yml|csv|html|css)$/i.test(name)) name += '.js';
      var base = name.replace(/^.*\//, '').replace(/\.[^.]+$/, '');
      var tmpl = name.endsWith('.md')
        ? '# ' + base + '\n\nGoal / knowledge note — plain-language steps that steer the autonomous loop.\n\n## Steps\n1. \n'
        : name.endsWith('.feature')
        ? 'Feature: ' + base + '\n\n  Scenario: \n    * print \'todo\'\n'
        : /\.js$/.test(name)
        ? '// @description: \n// @params: \n// drives `bot` deterministically — no LLM\nbot.go(\'about:blank\')\n'
        : '';
      var w = await this._post(cfg.writeUrl, { name: name, content: tmpl });
      if (w && w.error) { this.error = w.error; return; }
      if (parent) { this.expanded[parent] = true; await this.loadDir(parent); } else { await this.loadDir(''); }
      this.create.open = false; this.persistExpanded();
      await this.openPath(name);
    },

    // ---- rename (inline) --------------------------------------------------------------------
    startRename(n) {
      this.rename = { path: n.path, name: n.name };
      this.menu.open = false;
      this.$nextTick(function () { if (this.$refs.renameInput) { this.$refs.renameInput.focus(); this.$refs.renameInput.select(); } }.bind(this));
    },
    cancelRename() { this.rename.path = null; },
    async submitRename() {
      var path = this.rename.path; if (!path) return;
      var to = (this.rename.name || '').trim().replace(/^\/+|\/+$/g, '');
      this.rename.path = null;
      if (!to || to === this.basename(path)) return;       // unchanged / empty
      var parent = this.parentOf(path);
      var dst = parent ? parent + '/' + to : to;
      var r = await this._post(cfg.moveUrl, { from: path, to: dst });
      if (r && r.error) { this.error = r.error; return; }
      await this.loadDir(parent);
      // keep selection + the open editor following the rename
      var wasSelected = this.selected === path;
      if (wasSelected) { this.selected = dst; }
      this.remapExpanded(path, dst);
      if (wasSelected) { var node = this.findNode(dst); if (node && !node.dir) this.openFile(node); }
      this.persistExpanded();
    },

    // ---- delete -----------------------------------------------------------------------------
    async del(n) {
      this.menu.open = false;
      var label = n.dir ? 'folder ' + n.path + '/ and everything in it' : n.path;
      if (!confirm('Delete ' + label + '? This cannot be undone.')) return;
      var r = await this._post(cfg.deleteUrl, { name: n.path });
      if (r && r.error) { this.error = r.error; return; }
      await this.loadDir(this.parentOf(n.path));
      if (this.selected === n.path || (this.selected && this.selected.indexOf(n.path + '/') === 0)) {
        this.selected = ''; this.dispatch(cfg.openEvent, { path: '', closed: true });
      }
    },

    // ---- upload (binary) into a folder ------------------------------------------------------
    async uploadInto(parent, fileList) {
      var files = Array.from(fileList || []);
      if (!files.length) return;
      for (var i = 0; i < files.length; i++) {
        var f = files[i];
        var buf = await f.arrayBuffer();
        var target = (parent ? parent + '/' : '') + f.name;
        var r = await (await fetch(cfg.uploadUrl + '?name=' + encodeURIComponent(target), {
          method: 'POST', headers: { 'Content-Type': 'application/octet-stream' }, body: buf,
        })).json();
        if (r && r.error) { this.error = String(r.error); break; }
      }
      if (parent) { this.expanded[parent] = true; await this.loadDir(parent); } else { await this.loadDir(''); }
      this.persistExpanded();
    },

    // ---- move (drag & drop) -----------------------------------------------------------------
    canDrop(src, destDir) {
      if (src == null) return false;
      if (destDir === this.parentOf(src)) return false;       // already there
      if (destDir === src) return false;                      // into itself
      if (destDir.indexOf(src + '/') === 0) return false;     // into its own descendant
      return true;
    },
    async moveTo(src, destDir) {
      if (!this.canDrop(src, destDir)) return;
      var to = (destDir ? destDir + '/' : '') + this.basename(src);
      var r = await this._post(cfg.moveUrl, { from: src, to: to });
      if (r && r.error) { this.error = r.error; return; }
      await this.loadDir(this.parentOf(src));
      if (destDir) { this.expanded[destDir] = true; await this.loadDir(destDir); } else { await this.loadDir(''); }
      var wasSelected = this.selected === src;
      this.remapExpanded(src, to);
      if (wasSelected) { this.selected = to; var node = this.findNode(to); if (node && !node.dir) this.openFile(node); }
      this.persistExpanded();
    },
    onDragStart(n, ev) { this.drag.src = n.path; ev.dataTransfer.effectAllowed = 'move'; try { ev.dataTransfer.setData('text/plain', n.path); } catch (e) {} },
    onDragOver(n, ev) {
      var destDir = n.dir ? n.path : this.parentOf(n.path);
      if (this.canDrop(this.drag.src, destDir)) { ev.preventDefault(); this.dropTarget = n.dir ? n.path : destDir; }
    },
    onDrop(n, ev) {
      ev.preventDefault();
      var destDir = n.dir ? n.path : this.parentOf(n.path);
      var src = this.drag.src; this.drag.src = null; this.dropTarget = null;
      this.moveTo(src, destDir);
    },
    onDragOverRoot(ev) { if (this.canDrop(this.drag.src, '')) { ev.preventDefault(); this.dropTarget = ''; } },
    onDropRoot(ev) { ev.preventDefault(); var src = this.drag.src; this.drag.src = null; this.dropTarget = null; this.moveTo(src, ''); },
    onDragEnd() { this.drag.src = null; this.dropTarget = null; },

    // ---- context menu -----------------------------------------------------------------------
    openMenu(n, ev) {
      this.selected = n.path;
      // position within viewport; the menu is fixed-positioned
      this.menu = { open: true, x: Math.min(ev.clientX, window.innerWidth - 200), y: Math.min(ev.clientY, window.innerHeight - 280), node: n };
    },
    // a folder is a set (D281) — the engine resolves its members (features recursively, checks/ .js)
    isRunnable(n) { return n.dir || /\.(js|md|feature)$/i.test(n.name); },
    runNode(n) { this.menu.open = false; this.dispatch(cfg.runEvent, { path: n.path, dir: !!n.dir, kind: n.dir ? 'folder' : this.kindOf(n.name) }); },
    async copyPath(n) { this.menu.open = false; try { await navigator.clipboard.writeText(n.path); } catch (e) {} },

    // the PROJECT-ROOT-absolute path (leading '/') — the form a check references, e.g. read('/checks/foo.txt').
    // node.path is already root-relative, so just prefix the slash (guard against a double slash).
    rootPath(n) { return n && n.path ? (n.path.charAt(0) === '/' ? n.path : '/' + n.path) : ''; },
    async copyRef(n) {
      var self = this;
      try {
        await navigator.clipboard.writeText(this.rootPath(n));
        self.props.copied = true;
        setTimeout(function () { self.props.copied = false; }, 1200);
      } catch (e) {}
    },

    // ---- properties popover -----------------------------------------------------------------
    async openProps(n, ev) {
      ev.stopPropagation();
      var r = ev.currentTarget.getBoundingClientRect();
      this.props = { open: true, x: Math.min(r.left, window.innerWidth - 280), y: r.bottom + 4, node: n, meta: null, loading: false, copied: false };
      // a source file (js/md/feature) may carry @description/@params — fetch lazily
      if (!n.dir && /\.(js|md|feature)$/i.test(n.name)) {
        this.props.loading = true;
        var m = await this._get(cfg.metaUrl + '?name=' + encodeURIComponent(n.path));
        if (this.props.node === n) { this.props.meta = (m && !m.error) ? m : null; this.props.loading = false; }
      }
    },
    closeProps() { this.props.open = false; },
    fmtSize(b) {
      if (!b) return '0 B';
      var u = ['B', 'KB', 'MB', 'GB']; var i = Math.floor(Math.log(b) / Math.log(1024));
      return (b / Math.pow(1024, i)).toFixed(i ? 1 : 0) + ' ' + u[i];
    },
    fmtTime(ms) { if (!ms) return '—'; try { return new Date(ms).toLocaleString(); } catch (e) { return '—'; } },

    // ---- persistence + helpers --------------------------------------------------------------
    persistExpanded() {
      try {
        var open = Object.keys(this.expanded).filter(function (k) { return this.expanded[k]; }.bind(this));
        localStorage.setItem('katree:' + cfg.storeKey + ':expanded', JSON.stringify(open));
      } catch (e) {}
    },
    loadExpanded() {
      try {
        var raw = localStorage.getItem('katree:' + cfg.storeKey + ':expanded');
        if (raw) { JSON.parse(raw).forEach(function (p) { this.expanded[p] = true; }.bind(this)); }
      } catch (e) {}
    },
    // when a path moves, carry its expanded state (and that of its descendants) to the new path
    remapExpanded(from, to) {
      var next = {};
      Object.keys(this.expanded).forEach(function (k) {
        if (!this.expanded[k]) return;
        if (k === from) next[to] = true;
        else if (k.indexOf(from + '/') === 0) next[to + k.slice(from.length)] = true;
        else next[k] = true;
      }.bind(this));
      this.expanded = next;
    },
    basename(p) { var i = p.lastIndexOf('/'); return i < 0 ? p : p.slice(i + 1); },
    parentOf(p) { var i = p.lastIndexOf('/'); return i < 0 ? '' : p.slice(0, i); },
    dispatch(name, detail) { window.dispatchEvent(new CustomEvent(name, { detail: detail })); },
  };
}
