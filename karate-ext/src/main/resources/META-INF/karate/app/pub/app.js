// Theme cycle — light / dark / system, persisted (the markup skill's tailwind topic, §Dark mode).
function cycleTheme() {
  var stored = localStorage.getItem('theme');
  if (stored === 'light') localStorage.setItem('theme', 'dark');
  else if (stored === 'dark') localStorage.removeItem('theme');
  else localStorage.setItem('theme', 'light');
  applyTheme();
}

function applyTheme() {
  var stored = localStorage.getItem('theme');
  var dark = stored === 'dark' || (stored !== 'light' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  document.documentElement.classList.toggle('dark', dark);
}

document.addEventListener('DOMContentLoaded', applyTheme);

// Copy as curl — the folded exchange's button.
function copyText(button) {
  var text = button.parentElement.querySelector('[data-curl]').textContent;
  if (navigator.clipboard) navigator.clipboard.writeText(text);
  button.textContent = 'Copied';
}

// The status of an action for the world picked — the server's Twin.next reading, per world.
function actionStatus(spec) {
  if (!spec.worlds.length) return spec;
  for (var i = 0; i < spec.worlds.length; i++) {
    if (spec.worlds[i].key === spec.pick) return spec.worlds[i];
  }
  return { enabled: false, badge: 'unavailable', badgeText: 'Not available', reasonText: 'pick a record' };
}

// An option's text: the overlay's label for the member, else the member; the form still sends the member.
function optionLabel(spec, field, opt) {
  var labels = (spec.valueLabels || {})[field] || {};
  var key = typeof opt === 'string' ? opt : JSON.stringify(opt);
  return Object.prototype.hasOwnProperty.call(labels, key) ? labels[key] : SchemaForm.optionText(opt);
}

function badgeClass(badge) {
  return badge === 'available' ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
    : badge === 'unavailable' ? 'bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-400'
    : 'bg-amber-50 text-amber-700 dark:bg-amber-400/10 dark:text-amber-300';
}

// A prefill's D277 source, as the field marker's tooltip names it.
function provenanceTip(source) {
  return 'Prefilled — ' + source + ': ' + ({ example: "the command's authored example",
    schema: 'the first value its domain allows', fallback: 'no declared value' }[source] || source);
}
