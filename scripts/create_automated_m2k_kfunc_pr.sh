#!/bin/bash 
USER_EMAIL=$1
USER_NAME=$2
WORKDIR=$3
WORKFLOW_ID=$4
PR_OR_COMMIT_URL=$5
GH_TOKEN=$6
WF_CONFIG_REPO=$7
GITHUB_SHA=$8

TIMESTAMP=$(date +%s)
git config --global user.email "${USER_EMAIL}"
git config --global user.name "${USER_NAME}"
gh repo clone "${WF_CONFIG_REPO}" config-repo
cd config-repo || exit
git switch -c "${WORKFLOW_ID}"-autopr-"${TIMESTAMP}"

./hack/bump_chart_version.sh "${WORKFLOW_ID}" --bump-tag-version
mkdir -p charts/"${WORKFLOW_ID}"/templates
cp "${WORKDIR}"/workflows/"${WORKFLOW_ID}"/manifests/* charts/"${WORKFLOW_ID}"/templates
yq --inplace '.kfunction.image="quay.io/orchestrator/serverless-workflow-m2k-kfunc:'"${GITHUB_SHA}"'"' charts/move2kube/values.yaml
git add -A

git commit -m "(m2k-kfunc) Automated PR"
echo "Automated PR from ${PR_OR_COMMIT_URL}" | git commit --amend --file=-
git remote set-url origin https://"${GH_TOKEN}"@github.com/"${WF_CONFIG_REPO}"
git push origin HEAD
gh pr create -f --title "m2k-kfunc: Automatic manifests generation" \
--body "
Updating generated manifests for m2k-kfunc application

This PR was created automatically as a result of merging ${PR_OR_COMMIT_URL}
"