# Data-driven tests — `Scenario Outline` + `Examples`

When the same call should be tested across several inputs (valid variations, boundary values, each
validation rule), don't copy-paste near-identical scenarios — parametrize with a `Scenario Outline:` and an
`Examples:` table. One readable definition, N cases in the report.

## Inline Examples

```gherkin
Scenario Outline: creating a todo with a title succeeds
  Given path 'todos'
  And request { title: '<title>', complete: false }
  When method post
  Then status 201
  And match response == { id: '#string', title: '<title>', complete: false }

  Examples:
    | title       |
    | buy milk    |
    | walk the dog|
    | 🚀 launch   |
```

`<placeholder>` substitutes into steps as a **string**. For a typed cell (number/boolean/null/json), suffix
the column name with `!` so it's evaluated, not quoted:

```gherkin
Scenario Outline: validation rejects bad input with the right message
  Given path 'todos'
  And request <body>
  When method post
  Then status 400
  And match response == <expected>

  Examples:
    | body!                  | expected!                        |
    | { complete: false }    | { error: 'title is required' }   |
    | { title: '' }          | { error: 'title is required' }   |
    | { title: null }        | { error: 'title is required' }   |
```

## External data

Keep large / shared tables out of the feature:

```gherkin
Examples:
  | read('todos.csv') |          # a CSV: header row = column names, one case per row
```
```gherkin
* def cases = read('cases.json')  # a JSON array of case objects, then drive over it
Examples:
  | cases |
```

## Dynamic (compute the rows at runtime)

When the cases come from a call — an API list, a generator — use a `@setup` scenario and reference its
result (each row is one object, its keys are the placeholders):

```gherkin
@setup
Scenario: build the cases
  * def data = karate.range(1, 4).map(function(i){ return { title: 'todo-' + i } })

Scenario Outline: create each generated todo
  Given path 'todos'
  And request { title: '<title>', complete: false }
  When method post
  Then status 201
  Examples:
    | karate.setup().data |
```

## When NOT to

An Outline with **2 rows** that differ a lot reads worse than two named scenarios — use it for *the same
assertion over varying data*, not to force two different behaviors into one shape. (See `api` standing
rule 11.)
