# Changelog

## [0.8.0](https://github.com/bakdata/quick/tree/0.8.0) (2022-10-14)

[Full Changelog](https://github.com/bakdata/quick/compare/0.7.0...0.8.0)

**Implemented enhancements:**

- Remove zero padding from the key in the range index [\#103](https://github.com/bakdata/quick/issues/103)
- Make range queries exclusive [\#102](https://github.com/bakdata/quick/issues/102)
- Make range queries exclusive [\#96](https://github.com/bakdata/quick/issues/96)
- Define the system behaviour when --no-point is entered and there is no range field [\#82](https://github.com/bakdata/quick/issues/82)
- Check for `--range-field` and `--retention-time` during deployment [\#73](https://github.com/bakdata/quick/issues/73)
- Expose and implement range query API [\#63](https://github.com/bakdata/quick/issues/63)
- Create Range Data Fetcher [\#59](https://github.com/bakdata/quick/issues/59)
- Implement processor for range indexes [\#58](https://github.com/bakdata/quick/issues/58)
- Add `rangeFrom` and `rangeTo` to `@topic` directive [\#57](https://github.com/bakdata/quick/issues/57)
- Deploy range mirror [\#56](https://github.com/bakdata/quick/issues/56)
- Add a mechanism for the dynamic update of partition-host mapping to the PartitionRouter \(PartitionedMirrorClient\) [\#39](https://github.com/bakdata/quick/issues/39)
- Partition-mapping routing in the gateway to avoid selecting a wrong replica in a multi-replica scenario [\#35](https://github.com/bakdata/quick/issues/35)

**Fixed bugs:**

- Fix the broken link in the range query details docs [\#112](https://github.com/bakdata/quick/issues/112)
- Investigate the problem with the flaky behaviour of the MultiSubscriptionFetcherTest in the CI/CD [\#98](https://github.com/bakdata/quick/issues/98)
- Key information validation clashes with the semantics of multi subscriptions [\#88](https://github.com/bakdata/quick/issues/88)
- Partition routing not working properly [\#80](https://github.com/bakdata/quick/issues/80)
- It is not possible to create a gateway when -s flag is used [\#74](https://github.com/bakdata/quick/issues/74)
- Non string type arguments not supported in list arguments [\#65](https://github.com/bakdata/quick/issues/65)

**Updated dependencies:**

- Support Kafka 3.2 [\#105](https://github.com/bakdata/quick/issues/105)
- Micronaut 3.0 upgrade [\#6](https://github.com/bakdata/quick/issues/6)
- Prepare Kafka 3.0 upgrade [\#5](https://github.com/bakdata/quick/issues/5)

**Documentation updates:**

- Roadmap section update [\#107](https://github.com/bakdata/quick/issues/107)
- Update reference section [\#100](https://github.com/bakdata/quick/issues/100)
- Update the range queries documentation with a new example [\#97](https://github.com/bakdata/quick/issues/97)
- Links in the Docu should point to the version being viewed instead of latest [\#93](https://github.com/bakdata/quick/issues/93)
- Extend the docu with multi subscriptions \(intro + example and details\) [\#85](https://github.com/bakdata/quick/issues/85)
- Add documentation for working with range queries [\#68](https://github.com/bakdata/quick/issues/68)

**Miscellaneous:**

- Create a test for MultiSubscriptionFetcher that encompasses complex types [\#87](https://github.com/bakdata/quick/issues/87)
- Adjust manager to the change in the --point / --no-point approach [\#83](https://github.com/bakdata/quick/issues/83)
- Allow a possibility to choose the instance of Python Package Index [\#76](https://github.com/bakdata/quick/issues/76)
- Enforce checkstyle in the CI [\#64](https://github.com/bakdata/quick/issues/64)
- Design: Range queries V1 support [\#55](https://github.com/bakdata/quick/issues/55)
- Investigate high latency ingestion [\#54](https://github.com/bakdata/quick/issues/54)
- Evaluate range queries [\#28](https://github.com/bakdata/quick/issues/28)




## [0.7.0](https://github.com/bakdata/quick/tree/0.7.0) (2022-07-20)

[Full Changelog](https://github.com/bakdata/quick/compare/0.6.0...0.7.0)

**Documentation updates:**

- Document supported GraphQL elements [\#32](https://github.com/bakdata/quick/issues/32)
- Broken links in documentation [\#11](https://github.com/bakdata/quick/issues/11)
- Move solutions to main repository [\#7](https://github.com/bakdata/quick/issues/7)

**Miscellaneous:**

- Expose partition information in mirror [\#30](https://github.com/bakdata/quick/issues/30)
- `TopicTypeService` should be aware of different schema formats [\#24](https://github.com/bakdata/quick/issues/24)
- GraphQL to Protobuf converter [\#23](https://github.com/bakdata/quick/issues/23)
- Ensure Protobuf schema doesn't exist when creating a new topic [\#22](https://github.com/bakdata/quick/issues/22)
- New configuration variable for schema type [\#21](https://github.com/bakdata/quick/issues/21)
- Improve validation checks for the topic directive [\#20](https://github.com/bakdata/quick/issues/20)
- Enable the app deployment from a private container registry [\#18](https://github.com/bakdata/quick/issues/18)
- Design: Protobuf support [\#17](https://github.com/bakdata/quick/issues/17)
- No error message when creating the same gateway, app, or mirror multiple times [\#15](https://github.com/bakdata/quick/issues/15)
- New `TypeResolver` for Protobuf [\#2](https://github.com/bakdata/quick/issues/2)
- Support reading Protobuf data [\#1](https://github.com/bakdata/quick/issues/1)




## [0.6.0](https://github.com/bakdata/quick/tree/0.6.0) (2022-04-24)

Open-Source release 🎉
