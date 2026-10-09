# ADR-0001: Avro with Confluent Schema Registry for Kafka messages

**Status**: Accepted

**Date**: 2026-10-06

**Tags**: kafka, avro, schema-registry, message-format

## Context

Every signal source will send its data through Kafka (see [walking-skeleton.md](../walking-skeleton.md)). Kafka accepts any bytes, so a producer can send a message that no consumer can read.

## Decision

Messages use Avro, and the schema is registered in Confluent Schema Registry. The producer serializer checks each message against the registered schema and refuses a message that does not match, before the message is sent. The first schema is [square_count.avsc](../../src/main/avro/square_count.avsc).

## Alternatives considered

### Plain bytes, no registry

Kafka accepts this by default.

**Why rejected**: a wrong message enters the topic and fails only in the consumer.

Other formats (for example JSON or Protobuf): not recorded in the sources.

## Consequences

### Positive

- A message that breaks the schema never leaves the project producer. A client that does not use the serializer can still write bytes to the topic.
- The schema is in one place for all later sources.

### Negative

- Schema Registry is one more service to run, and the tests need a container for it.
- Each consumer, including a later Spark job, needs the registry to read the messages.

## Related

- [walking-skeleton.md](../walking-skeleton.md): row "Message format" of the technical design.
