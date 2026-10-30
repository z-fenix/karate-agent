# Error paths — every endpoint needs a 4xx scenario

A suite that only tests the happy path proves the API works when everything is right — not that it *fails
correctly*, which is where real bugs hide (a validation that doesn't fire, a 500 where a 400 belongs, a
missing-resource that returns 200 with an empty body). **The rule: every endpoint gets at least one 4xx
scenario, and it asserts the error body contract, not just the status.**

## The four that matter most

```gherkin
# 400 — validation. A required field is missing / malformed.
Scenario: creating a todo without a title is rejected
  Given path 'todos'
  And request { complete: false }
  When method post
  Then status 400
  And match response == { error: 'title is required' }     # assert the contract, not just the 400

# 404 — missing resource. Ask for something that isn't there.
Scenario: fetching an unknown todo is a 404
  Given path 'todos', 'does-not-exist'
  When method get
  Then status 404
  And match response == { error: 'not found', id: 'does-not-exist' }

# 401 / 403 — auth. Call a protected endpoint with no / a bad token.
Scenario: listing without a token is unauthorized
  Given path 'todos'
  When method get
  Then status 401
  And match response contains { error: '#string' }

# 409 — conflict. Create a duplicate, or act on a resource in the wrong state.
Scenario: creating a duplicate slug conflicts
  Given path 'todos'
  And request { title: 'x', slug: 'taken' }
  When method post
  Then status 409
```

## Notes

- **Assert the body.** `Then status 400` alone can't tell a real validation error from a generic 400 — pin
  the error shape (`match response == { error: '#string' }` at least, the exact message where it's stable).
  *Exception: no-content statuses (204, 304) carry no body — `response` is `null`, so assert the status
  only; `match response == ''` fails NULL:STRING.*
- **Don't `try/catch` a 4xx.** A 4xx is a normal, assertable outcome — a dedicated scenario, not JS
  error-handling. (Karate does not throw on a non-2xx status.)
- **Negative before edge.** Missing/invalid input (4xx) is different from a boundary happy-path (an empty
  list, a max-length title that still succeeds) — cover both.
- **Ground it in the live API.** Probe the real failure (`Http.post('/todos', {})` → see the real 400 body)
  rather than guessing the error shape from the endpoint name.

A worked negative rides alongside the CRUD chain in the `todo-api` kit's `checks/todos-crud.feature`.
