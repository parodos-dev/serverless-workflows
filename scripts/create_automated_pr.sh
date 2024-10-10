#!/bin/bash 
USER_EMAIL=$1
USER_NAME=$2
WORKDIR=$3
WORKFLOW_ID=$4
PR_OR_COMMIT_URL=$5
GH_TOKEN=$6
WF_CONFIG_REPO=$7

TIMESTAMP=$(date +%s)
git config --global user.email "${USER_EMAIL}"
git config --global user.name "${USER_NAME}"
gh repo clone "${WF_CONFIG_REPO}" config-repo
cd config-repo || exit
git switch -c "${WORKFLOW_ID}"-autopr-"${TIMESTAMP}"

./hack/bump_chart_version.sh "${WORKFLOW_ID}" --bump-tag-version
mkdir -p charts/"${WORKFLOW_ID}"/templates
cp "${WORKDIR}"/"${WORKFLOW_ID}"/manifests/* charts/"${WORKFLOW_ID}"/templates
git add -A

git commit -m "(${WORKFLOW_ID}) Automated PR"
echo "Automated PR from $PR_OR_COMMIT_URL" | git commit --amend --file=-
git remote set-url origin https://"${GH_TOKEN}"@github.com/"${WF_CONFIG_REPO}"
git push origin HEAD
gh pr create -f --title "${WORKFLOW_ID}: Automatic manifests generation" \
--body "
Updating generated manifests for ${WORKFLOW_ID} workflow

This PR was created automatically as a result of merging ${PR_OR_COMMIT_URL}
"