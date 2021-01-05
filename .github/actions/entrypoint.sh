#!/bin/sh -l

echo "validating helm charts"
helm dependency update $1
helm lint --strict $1
helm template $1
