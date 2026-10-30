/*
 * The auth preset form (components/auth-preset.html) — its Alpine factory. It reads kaWords.authPreset's model from the
 * authPreset island and posts one ApiTest.config change over /ui/apitest/config (api-workspace §6): the preset for the
 * console's env, or for every env. A token, password, key or client secret typed here goes as {value} and the server
 * keeps it in the secret store under the name the preset already uses, else one it derives; a credential left blank keeps
 * the stored one. "No sign-in" removes the preset shown. A save reloads the page, so the sign-in chip shows it.
 */
function kaAuthPreset() {
  var el = document.getElementById('authPreset-data');
  var m = (el && JSON.parse(el.textContent)) || {};
  var refs = m.refs || {};
  var REFUSED = {
    STALE: 'The project settings changed since you opened this page — reload.',
    HAND_EDITED: 'The generated part of karate-config.js was edited by hand, so nothing was saved. Restore it from version control.',
    NOT_EDITABLE: 'karate-config.js is not in a shape this form can change, so nothing was saved.',
    NO_PROJECT: 'Choose a project first.'
  };
  function message(e) { return String(e && e.message ? e.message : e).replace(/^Error: /, ''); }
  return {
    kind: m.kind || 'none', scope: m.from === 'all' || !m.env ? 'all' : 'env', username: m.username || '', name: m.name || '',
    where: m.where || 'header', tokenUrl: m.tokenUrl || '', clientId: m.clientId || '', oscope: m.scope || '', login: m.login || '',
    typed: { token: '', password: '', key: '', clientSecret: '' }, busy: false, said: '', bad: false,

    held(f) { return refs[f] ? 'Kept as the secret ' + refs[f].name + (refs[f].stored ? '. Type a new one to replace it.' : ', not stored yet.') : ''; },
    // a credential: what was typed, else the stored one's name; null when there is neither
    cred(f) {
      var v = this.typed[f];
      if (v) return refs[f] ? { secret: refs[f].name, value: v } : { value: v };
      return refs[f] && refs[f].stored ? { secret: refs[f].name } : null;
    },
    preset() {
      var need = { token: 'the token', password: 'the password', key: 'the key', clientSecret: 'the client secret' };
      var a, missing = [];
      var self = this;
      function cred(f) { var c = self.cred(f); if (!c) missing.push(need[f]); return c; }
      function text(v, what) { v = String(v || '').trim(); if (!v) missing.push(what); return v; }
      if (this.kind === 'bearer') a = { scheme: 'bearer', token: cred('token') };
      else if (this.kind === 'login') a = { scheme: 'bearer', login: text(this.login, 'the sign-in test') };
      else if (this.kind === 'basic') a = { scheme: 'basic', username: text(this.username, 'the username'), password: cred('password') };
      else if (this.kind === 'apikey') {
        a = { scheme: 'apikey', key: cred('key') };
        a[this.where === 'query' ? 'query' : 'header'] = text(this.name, 'the key’s name');
      } else if (this.kind === 'oauth2') {
        a = { scheme: 'oauth2', tokenUrl: text(this.tokenUrl, 'the token address'), clientId: text(this.clientId, 'the client ID'),
          clientSecret: cred('clientSecret') };
        if (String(this.oscope).trim()) a.scope = String(this.oscope).trim();
      }
      if (missing.length) throw new Error('Enter ' + missing.join(' and ') + ' first.');
      return a;
    },
    async save() {
      if (this.busy) return;
      this.said = ''; this.bad = false;
      var body = { ifMatch: m.version, env: this.scope === 'env' ? m.env : null };
      try {
        if (this.kind === 'none') {
          if (!m.from) { this.said = 'There is no sign-in to remove.'; return; }
          body.remove = 'auth';
          body.env = m.from === 'env' ? m.env : null;
        } else {
          body.auth = this.preset();
        }
        this.busy = true;
        var res = await fetch('/ui/apitest/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        var out = await res.json().catch(function () { return null; });
        if (!out) throw new Error('The console did not answer (' + res.status + ').');
        if (out.error) throw new Error(REFUSED[out.code] || 'Nothing was saved: ' + String(out.error).replace(/^ApiTest\.config:\s*/, '') + '.');
        location.reload();
      } catch (e) {
        this.bad = true; this.said = message(e); this.busy = false;
      }
    }
  };
}
