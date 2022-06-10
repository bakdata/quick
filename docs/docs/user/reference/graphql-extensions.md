# GraphQL extensions

## Enums

### `RestDirectiveMethod`

```graphql
enum RestDirectiveMethod {
    GET, POST
}
```

## Directives

### `@topic`

```graphql
directive @topic(
    name: String!, # Name of the topic.
    keyArgument: String, # The argument which contains the key. This also supports arguments from parents.
    keyField: String # The field which contains the key. This can be used when the key is part of a different mirror.
) on FIELD_DEFINITION
``` 

### `@rest`

```graphql
directive @rest(
    url: String! # url of the rest service
    pathParameter: [String!] # list of the arguments that should be included in the list
    queryParameter: [String!] # list of arguments that should be included as query parameter in the form of `argumentName=value`
    bodyParameter: String # argument which represents a body. This cannot be a scalar
    httpMethod: RestDirectiveMethod = GET #  The HTTP method used when calling the rest service
) on FIELD_DEFINITION
```

