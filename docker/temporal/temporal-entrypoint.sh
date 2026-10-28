#!/bin/sh
# Temporal start-dev binds to 127.0.0.1 only, breaking inter-container comms.
# Run temporal on 127.0.0.1:7234 and forward 0.0.0.0:7233 -> 127.0.0.1:7234 via socat.
temporal server start-dev --port 7234 &
sleep 5
socat TCP-LISTEN:7233,fork,reuseaddr TCP:127.0.0.1:7234
