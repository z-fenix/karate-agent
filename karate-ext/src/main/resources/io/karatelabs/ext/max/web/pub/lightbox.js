/* The shared screenshot lightbox (components/lightbox.html) — open from any page via
 * kaLightboxOpen([{url, title}, ...], startIndex). Loaded globally by layout.html (before the
 * deferred Alpine CDN script, so the kaLightbox factory exists when Alpine boots). */
function kaLightboxOpen(shots, index) {
  window.dispatchEvent(new CustomEvent('ka-lightbox-open', { detail: { shots: shots, index: index || 0 } }));
}

function kaLightbox() {
  return {
    open: false, shots: [], index: -1,
    get current() { return this.shots[this.index] || {}; },
    show(d) {
      if (!d || !d.shots || !d.shots.length) return;
      this.shots = d.shots;
      this.index = Math.min(Math.max(d.index || 0, 0), d.shots.length - 1);
      this.open = true;
    },
    close() { this.open = false; },
    nav(delta) { this.jump(this.index + delta); },
    jump(i) {
      if (!this.open || !this.shots.length) return;
      this.index = Math.min(Math.max(i, 0), this.shots.length - 1);
    }
  };
}
