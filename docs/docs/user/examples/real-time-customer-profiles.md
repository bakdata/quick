# Real-time customer profiles

This example demonstrates how you can use Quick to create real-time customer profiles for a music streaming service.
The profiles build user metrics,
record charts of the most-streamed albums, artists and tracks,
and create recommendations based on the user's playlist.

You can find the complete code in [the Quick's example repository](https://github.com/bakdata/quick-examples).
The example uses the real world data set LFM-1b.
The Kafka Streams application is written with
our [streams-bootstrap library](https://github.com/bakdata/streams-bootstrap).

---

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

---

For modeling and querying data in this example, you first define a schema with GraphQL.
The query called `getUserProfile` combines six metrics of the customer profile:

- total listening events
- first and the last time a user listened to a song 
- charts with user’s most listened albums, artists and tracks.

Those charts, however, contain only ids and not the names of the corresponding music data.
Therefore, you can let Quick resolve the chart's ids from topics like `topartists` with names from topic artists in our GraphQL schema an call the corresponding type NamedArtistCount.


??? "The GraphQL schema"
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
