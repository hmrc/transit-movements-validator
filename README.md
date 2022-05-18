
# Transit Movements Validator
This is a microservice for an internal API to parse and validate XML payloads. 

This microservice is in [Beta](https://www.gov.uk/help/beta). The signature may change.

## Prerequisites

- Scala 2.12.15
- Java 8
- sbt > 1.5.8
- [Service Manager](https://github.com/hmrc/service-manager)

## Development Setup

Run from the console using: `sbt run`

### Highlighted SBT Tasks
Task | Description | Command
:-------|:------------|:-----
run | Runs the application with the default configured port | ```$ sbt run```
test | Runs the standard unit tests | ```$ sbt test```
it:test  | Runs the integration tests | ```$ sbt it:test ```
dependencyCheck | Runs dependency-check against the current project. It aggregates dependencies and generates a report | ```$ sbt dependencyCheck```
dependencyUpdates |  Shows a list of project dependencies that can be updated | ```$ sbt dependencyUpdates```
dependencyUpdatesReport | Writes a list of project dependencies to a file | ```$ sbt dependencyUpdatesReport```

## Related API documentation

- [Common Transit Convention Traders API specifications](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/common-transit-convention-traders/1.0)

## Helpful information

Guides for the related public Common Transit Convention Traders API are on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub)

## Reporting Issues

If you have any issues relating to the Common Transit Convention Traders API, please raise them through our [public API](https://github.com/hmrc/common-transit-convention-traders#reporting-issues).

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
