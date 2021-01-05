#!/bin/sh -l

if [ $1 == "validate" ]; then
    echo "validating helm charts"
    helm dependency update $2
    helm lint --strict $2
    helm template $2

elif [ $1 == "publish" ]; then
    CHART_VERSION=$(echo ${GITHUB_REF} | cut -d/ -f 3)
    CHART_NAME=$(awk '/^name:/ {print $2}' $2/Chart.yaml)
    INPUT_REPOSITORY=${{ inputs.helm-gcs-repository }}
    INPUT_CREDENTIALS=${{ inputs.helm-gcs-credentials }}
    export GOOGLE_APPLICATION_CREDENTIALS=${HOME}/helm-gcs-key.json

    echo ${INPUT_CREDENTIALS:-$HELM_GCS_CREDENTIALS} > ${GOOGLE_APPLICATION_CREDENTIALS}
    helm dependency update $2
    helm repo add helm-gcs ${INPUT_REPOSITORY:-$HELM_GCS_REPOSITORY}
    helm package --version ${CHART_VERSION} --app-version ${CHART_VERSION} $2
    helm gcs push ${CHART_NAME}-${CHART_VERSION}.tgz helm-gcs --public --retry
fi