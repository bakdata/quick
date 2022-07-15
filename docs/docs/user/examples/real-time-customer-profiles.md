# Real-time customer profiles

This example uses Quick to create real-time customer profiles for a music streaming service.
These profiles will include user metrics, charts of the most-streamed albums, artists and tracks, and recommendations based on the user's playlist.

## What this will demonstrate
- the use of topics, of course
- analytics on an incoming stream
- integration of a recommendation service
- a global GraphQL schema forming the customer profile

Visit the [demo website](https://profile-store.d9p.io/dashboard/) to see the example up and running.
This visualizes the real-time profiles in a front-end. 


The code can be found in [Quick's example repository](https://github.com/bakdata/quick-examples/tree/main/profile-store).
The example uses the real world data set [LFM-1b](http://www.cp.jku.at/datasets/LFM-1b/).
The Kafka Streams application is written based on our open source [streams-bootstrap library](https://github.com/bakdata/streams-bootstrap).

---

Finally, there is a video explaining this example:
<div class="video-wrapper">
<iframe allow="accelerometer; autoplay;
                clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen
        src="https://www.youtube-nocookie.com/embed/aitX3hoS5Xc"
        title="YouTube video player" width="900" height="500" ></iframe>
</div>

## The Input: Listening Events

Every time a customer listens to a song, the system emits a listening event containing the ids of album, artist and track.
The system additionally attaches metadata such as the timestamp to the event.
Later, a Kafka Streams application processes it for the customer profile creation.

```json title="Exemplary listening events"
{"userId": 402, "artistId": 7, "albumId": 17147, "trackId": 44975, "timestamp": 1568052379}
{"userId": 703, "artistId": 64, "albumId": 17148, "trackId": 44982, "timestamp": 1568052379}
{"userId": 4234, "artistId": 3744, "albumId": 34424, "trackId": 105501, "timestamp": 1568052382}
{"userId": 2843, "artistId": 71, "albumId": 315, "trackId": 2425, "timestamp": 1568052383}
{"userId": 1335, "artistId": 13866, "albumId": 29007, "trackId": 83201, "timestamp": 1568052385}
```

## The Quick Configuration

### The Globel GraphQL Schema

We first define the global schema with GraphQL.
The query called `getUserProfile` combines six metrics of the customer profile:

- total listening events
- the first and last time a user listened to a song
- charts with user's most listened albums, artists and tracks

We retrieve all that data from different topics via the `@topic` directive.
Still, the charts, contain solely ids and not the names of the corresponding music data.
You can let Quick resolve those ids transparently.
For that, we use the topics (artists, albums, tracks) containing the mapping from ids to names and reference them in the GraphQL schema.
The creation of the metrics topics (counts, firstlisten, lastlisten) is described below.

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


This is all we need to do to ingetrage data with Quick.

For the Quick setup, please refer to the [getting started guide](../../getting-started/setup-quick).
To avoid redundancy, we only show the setup for integral parts here.
You find all steps in the [justfile](https://github.com/bakdata/quick-examples/tree/main/profile-store/deployment/justfile). 

### Gateway

Create a new gateway and apply the GraphQL schema.

```shell
quick gateway create profiles
```
```shell
quick gateway apply profiles -f schema-user-profile.gql
```

### Topics

Then, we create the input topics for the artist, album and track data.  

```shell
quick topic create albums  --key long --value schema -s profiles.Item
quick topic create artists --key long --value schema -s profiles.Item
quick topic create tracks  --key long --value schema -s profiles.Item

quick topic create listeningevents --key long --value schema \
  --schema profiles.ListeningEvent
```

The command expects the topic name and the type or schema of key and value.
Since the topics contain complex values, we reference them via `<gateway name>.<type name>`.
This uses the definition in the global GraphQL schema we previously applied to the gateway.

## Analytics

With gateway and input topics in place, we can now take care of the analytics.
Kafka Streams apps will process the data and compute the respective parts of the profiles.

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
We deploy those applications with the command `quick app deploy [...]`.
For details, call `quick app deploy -h` or see the [reference](../reference/cli-commands.md#quick-app-deploy).

### Charts

Similar to the metrics, we now add support for a user's charts in the profile.

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


### Recommendations

Finally, we integrate recommendations into the profiles.
Therefore, we add the `getArtistRecommendations` query backed by an external service to the schema.

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

`getArtistRecommendations` takes several parameters:

- `userId` is mandatory:
  It tells the service for which user it should compute the recommendations.
- `field` defines which type of recommendation to create:
  `ARTIST`, `ALBUM` or `TRACK` is possible.
  The schema sets it to `ARTIST` by default.
- The remaining parameters come from the underlying recommendation algorithm [SALSA](https://github.com/twitter/GraphJet) 
  and have sensible default values.

To leverage the external service, we use the `@rest` directive.
This directive integrates a REST services into your global schema.
In this example, the recommendation service returns the result for a particular user id as a list of ids.
We resolve these ids with the names from the `artistis` topic in the type `recommendation`.

You can deploy the recommendation service via Quick as well:
```shell
quick app deploy recommender \
  --registry bakdata \
  --image quick-demo-profile-recommender \
  --tag "1.0.0" \
  --port 8080 \
  --args input-topics=listeningevents productive=false 
```


Finally, everything is in place to query the artist recommendation.

```shell title="Example query"
query{
  getArtistRecommendations(userId: 32226961){
    recommendations{
      id
      name
    }
  }
}
```

```json title="Example results"
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
