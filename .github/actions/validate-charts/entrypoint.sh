#!/bin/sh -l

echo "Hello world"
helm dependency update ./helm/
helm lint --strict ./helm/
helm template ./helm/

echo "done"