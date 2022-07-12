# GraphQL support

This section provides information about the supported and unsupported GraphQL types in Quick.
Furthermore, it describes the GraphQL extensions that are introduced in Quick.

## GraphQL supported types

Currently, Quick supports the following GraphQL types:

1. The mandatory `Query` type,
2. The optional `Mutation` type and the corresponding `Input` type,
3. Basic Types with field declarations as well as type modifiers, for example: `type Character { name:String!, appearsIn: [Episode!] }`,
4. All scalar types: `Int`, `String`, `Float`, `Boolean`, `ID` together with field's arguments, for example: `total(moreThan: Int = 50): Float`,
5. Enumerations (`enum`),
6. Subscriptions (`subscription`).

Thus, the following types are **not** supported:  
 
1. Interfaces (`interface`),  
2. Unions (`union`),  
3. Custom scalars, for example: `scalar MyCustomScalar`.  

## GraphQL extensions

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


