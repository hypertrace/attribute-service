#!/bin/sh -l

echo "validating helm charts"
helm dependency update ./helm/
helm lint --strict ./helm/
helm template ./helm/
