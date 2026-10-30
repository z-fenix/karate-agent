// The run page's 🤖 AI prompt (D403): a run whose record holds captured traffic asks an outside agent for one task, a
// verified API test of the recorded calls; every other run gets the general share. The prompt restates no API (P26).
window.kaSharePrompt = function (base, id, meta) {
  if (meta && meta.traffic) {
    return "Make a verified API test of the calls recorded in karate-agent run " + id + " at " + base + ".\n\n"
      + "Drive the console: POST JavaScript as the request body to " + base + "/api/eval (or the MCP `eval` tool).\n"
      + "Start with Traffic.read('" + id + "'); the route and its rules: Skill.help('record-to-api').\n";
  }
  const dir = base + '/api/artifacts/runs/' + encodeURIComponent(id);
  return "Here's a settled karate-agent run (run " + id + " at " + base + "). Help me with it "
    + "— e.g. troubleshoot a failure, turn the recorded flow into a deterministic checks/*.js, or "
    + "explain what happened.\n\n"
    + "Fetch the run:\n"
    + "  curl -s " + dir + "/meta.json\n"
    + "  curl -s " + dir + "/transcript.jsonl\n";
};
