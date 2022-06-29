# Real-time customer profiles

This example demonstrates how you can use Quick to create real-time customer profiles for a music streaming service.
The generated profiles include user metrics,
charts of the most-streamed albums, artists and tracks,
and recommendations based on the user's playlist.

You can then use Quick to visualize the real-time profiles in a front-end. 
To see an example, you can [view the demo website](https://profile-store.d9p.io/dashboard/).

You can find the complete code in [Quick's example repository](https://github.com/bakdata/quick-examples/tree/main/profile-store).
The example uses the real world data set [LFM-1b](http://www.cp.jku.at/datasets/LFM-1b/).
The Kafka Streams application is written with
our [streams-bootstrap library](https://github.com/bakdata/streams-bootstrap).

---

There is also a video, explaining this example:
<div class="video-wrapper">
<iframe allow="accelerometer; autoplay;
                clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen
        src="https://www.youtube-nocookie.com/embed/aitX3hoS5Xc"
        title="YouTube video player" width="900" height="500" ></iframe>
</div>

## Listening events

Every time a customer listens to a song, the system emits a listening event containing the ids of album,
artist and track to an Apache Kafka topic.
The system further attaches metadata like the timestamp to the event.
Then, a Kafka Streams application processes it for the customer profile creation.

```json title="Exemplary listening events"
{"userId": 402, "artistId": 7, "albumId": 17147, "trackId": 44975, "timestamp": 1568052379}
{"userId": 703, "artistId": 64, "albumId": 17148, "trackId": 44982, "timestamp": 1568052379}
{"userId": 4234, "artistId": 3744, "albumId": 34424, "trackId": 105501, "timestamp": 1568052382}
{"userId": 2843, "artistId": 71, "albumId": 315, "trackId": 2425, "timestamp": 1568052383}
{"userId": 1335, "artistId": 13866, "albumId": 29007, "trackId": 83201, "timestamp": 1568052385}
```

## Set up

### Designing the GraphQL schema

For modeling and querying data in this example, you first define a schema with GraphQL.
The query called `getUserProfile` combines six metrics of the customer profile:

- total listening events
- the first and last time a user listened to a song
- charts with user's most listened albums, artists and tracks

Those charts, however, contain only ids and not the names of the corresponding music data.
You can let Quick resolve those ids with names.
For that, you use topics containing the mapping from id to names and then reference them in the GraphQL schema.

??? "The GraphQL user profile schema (`schema-user-profile.gql`)" 
    ```graphql
    type Query {
        getUserProfile(userId: Long!): UserProfile
    }

    type UserProfile {
        totalListenCount: Long! @topic(name: "counts", keyArgument: "userId")
        firstListenEvent: Long! @topic(name: "firstlisten", keyArgument: "userId")
        lastListenEvent: Long! @topic(name: "lastlisten", keyArgument: "userId")
        artistCharts: NamedArtistCharts! @topic(name: "topartists", keyArgument: "userId")
        albumCharts: NamedAlbumCharts! @topic(name: "topalbums", keyArgument: "userId")
        trackCharts: NamedTrackCharts! @topic(name: "toptracks", keyArgument: "userId")
    }

    type NamedArtistCharts {
        topK: [NamedArtistCount!]!
    }

    type NamedAlbumCharts {
        topK: [NamedAlbumCount!]!
    }

    type NamedTrackCharts {
        topK: [NamedTrackCount!]!
    }

    type Item {
        id: Long!
        name: String!
    }

    type NamedArtistCount {
        id: Long!
        artist: Item! @topic(name: "artists", keyField: "id")
        countPlays: Long!
    }

    type NamedAlbumCount {
        id: Long!
        album: Item! @topic(name: "albums", keyField: "id")
        countPlays: Long!
    }

    type NamedTrackCount {
        id: Long!
        track: Item! @topic(name: "tracks", keyField: "id")
        countPlays: Long!
    }
    ...
    ```

### Quick 

You are now ready to process and query our data with Quick.
To start a Quick instance, you can refer to the [getting started guide](../../getting-started/setup-quick).

To avoid redundancy, we show the setup of the integral parts.
You find the steps for a complete deployment in the `justfile` in the [example repository](https://github.com/bakdata/quick-examples/tree/main/profile-store/deployment/justfile). 

#### Gateway

Create a new gateway and apply the GraphQL schema.

```shell
quick gateway create profiles
```
```shell
quick gateway apply profiles -f schema-user-profile.gql
```

#### Topics

Then, you create the input topics for the artist, album and track data.  

```shell
quick topic create albums  --key long --value schema -s profiles.Item
quick topic create artists --key long --value schema -s profiles.Item
quick topic create tracks  --key long --value schema -s profiles.Item

quick topic create listeningevents --key long --value schema \
  --schema profiles.ListeningEvent
```

The command expects the topic name and the type or schema of key and value.
Since the topics contain complex values, you define them in the global GraphQL schema and apply that to the gateway.
For topic creation, you won't need to specify a file, but reference them with `<gateway name>.<type name>` .


## Creating user profiles

With gateway and input topics in place, the next step is the creation of profiles.
Kafka Streams applications process the data and compute the respective parts of the profiles.

### Metrics

The user profile has the following metrics:

- first listening event
- last listening event
- number of listening events 

1. Create topics that later store the corresponding data:

    ```shell
    quick topic create firstlisten --key long --value long
    quick topic create lastlisten --key long --value long
    quick topic create counts --key long --value long
    ```
  
2. Deploy the applications:

    ```shell
    quick app deploy firstlisten \
    --registry bakdata \
    --image quick-demo-profile-listenings-activity \
    --tag "1.0.0" \
    --args input-topics=listeningevents, output-topic=firstlisten, kind=FIRST, productive=false
    ```

Quick supports running dockerized applications.
You can deploy those applications with the command `quick app deploy [...]`.
For more detailed information, call `quick app deploy -h`
or [see reference](../reference/cli-commands.md#quick-app-deploy).

### Charts

Similar to the metrics, you can now add support for the user charts of the profile.

1. Create the topics:
    ```shell
    quick topic create topartists --key long \
    --value schema -s profiles.Charts

    quick topic create topalbums --key long \
    --value schema -s profiles.Charts

    quick topic create toptracks --key long \
    --value schema -s profiles.Charts
    ```

2. Deploy the applications:
    ```shell
    quick app deploy topartists \
    --registry bakdata \
    --image quick-demo-profile-listenings-charts \
    --tag "1.0.0" \
    --args input-topics=listeningevents output-topic=topartists productive=false
    ```


## Building recommendations

The user profiles are missing the last part: the recommendations.
You can add to the GraphQL schema the example query `getArtistRecommendations`.

??? "Schema extension with `getArtistRecommendations`"
    ```graphql
    type Query {
        getArtistRecommendations(
            userId: Long!,
            field: FieldType! = ARTIST,
            limit: Int,
            walks: Int,
            walkLength: Int,
            resetProbability: Float
        ):  Recommendations @rest(
                url: "http://recommender/recommendation",
                pathParameter: ["userId", "field"],
                queryParameter: ["limit", "walks", "walkLength", "resetProbability"]
            )
    }
    
    enum FieldType {
        ARTIST
        ALBUM
        TRACK
    }
    
    type Recommendations {
        recommendations: [Recommendation!]!
    }
    
    type Recommendation {
        id: Long!
        artist: Item @topic(name: "artists", keyField: "id")
    }
    ```

It takes a couple parameters:

- mandatory is only `userId`.
  It tells the recommendation service for which user it should compute the recommendations.
- `field` defines which type of recommendation you expect: `ARTIST`, `ALBUM` or `TRACK`.
  The schema assumes this to be `ARTIST`.
- The remaining parameters come from the underlying recommendation algorithm [SALSA](https://github.com/twitter/GraphJet) 
  and have sensible default values.

Further, you can see the `@rest` of Quick directive.
It lets you include any type of REST service in your GraphQL schema.
In this example,
the recommendation service returns the result for a particular user id as a list of ids via REST.
Since the idea is to recommend artist names,
you can resolve the ids from the REST service with the name from the `artistis` topic in the type `Recommendation`.

You can deploy the recommendation service via Quick as well:
```shell
quick app deploy recommender \
  --registry bakdata \
  --image quick-demo-profile-recommender \
  --tag "1.0.0" \
  --port 8080 \
  --args input-topics=listeningevents productive=false 
```


### Querying recommendations

Now everything is in place to query the artist recommendation.

```shell
query{
  getArtistRecommendations(userId: 32226961){
    recommendations{
      id
      name
    }
  }
}
```

```json title="Exemplary results"
{
  "data": {
    "getArtistRecommendations": {
      "recommendations": [
        {
          "id": 2041,
          "name": "Leevi and the Leavings"
        },
        {
          "id": 1825,
          "name": "Neil Young"
        },
        {
          "id": 1871,
          "name": "Mogwai"
        },
        {
          "id": 2353,
          "name": "The National"
        },
          ...     
      ]
    }
  }
}
```
