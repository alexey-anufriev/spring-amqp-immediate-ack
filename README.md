# `ImmediateAcknowledgeAmqpException` keeps the message in the queue

## Environment

- Docker

## Problem Description

When `ImmediateAcknowledgeAmqpException` is thrown from the code of the listener
it is expected that the message that has just been consumed by the listener
will be automatically acknowledged and removed from the queue.

### Actual Result

The message stays in the queue in "unacknowledge" state.

## Reproducing Steps

`./mvnw clean test`
