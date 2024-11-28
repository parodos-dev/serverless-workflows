#!/bin/bash

# Set repository (update with your repo)
REPO=$1
WORKFLOW_ID=$2
PR_OR_COMMIT_URL=$3
APPLICATION_UPDATE_COMMAND=$4

CREATE_PR_SCRIPT=$5
USER_EMAIL=$6
USER_NAME=$7
WORKDIR=$8
GH_TOKEN=$9
GITHUB_SHA=${10}

# Get all automated PRs opened by orchestrator-ci for the repository
prs=$(gh pr list --repo "${REPO}" -A orchestrator-ci --json number --jq '.[].number') || exit
echo "${prs}"
STOP=false
# Loop through each PR and check for the version in Chart.yaml
for pr in $prs; do
    echo "Checking PR #$pr..."

    if "${STOP}"; then
        exit 0
    fi
    
    # Fetch the list of changed files for the PR
    files=$(gh pr diff "$pr" --name-only --repo "${REPO}") || exit
    
    # Check if Chart.yaml is in the list of changed files
    if echo "$files" | grep -q "charts/${WORKFLOW_ID}/Chart.yaml"; then
        echo "Chart.yaml found in PR #${pr}"
        STOP=true
        
        # Get the contents of the Chart.yaml file from the PR
        chart_content=$(gh pr diff "$pr" --repo "${REPO}")
        
        # Check if the content has a version entry
        if echo "$chart_content" | grep "[+-]version: .*"; then
            git config --global user.email "${USER_EMAIL}"
            git config --global user.name "${USER_NAME}"
            # Get the PR details (branch)
            pr_labels=$(gh pr view "$pr" --repo "$REPO" --json labels --jq '.labels[].name')
            if [[ ${pr_labels[@]} =~ "do-not-merge" ]]
            then
                echo "PR $pr is labeled with do-not-merge, ignoring it"
                exit 0
            fi

            pr_branch=$(gh pr view "$pr" --repo "$REPO" --json headRefName --jq '.headRefName')

            gh repo clone "${REPO}" config-repo
            cd config-repo || exit
            # Checkout the PR branch
            git checkout "$pr_branch"
                        
            if [ "${APPLICATION_UPDATE_COMMAND}" != "" ]; then
                eval "${APPLICATION_UPDATE_COMMAND}" || exit 1
            else
                cp "${WORKDIR}"/"${WORKFLOW_ID}"/manifests/* charts/"${WORKFLOW_ID}"/templates || exit 1
            fi
            git add -A

            git commit -m "(${WORKFLOW_ID}) Automated PR"
            echo "Automated PR from $PR_OR_COMMIT_URL" | git commit --amend --file=-
  
            git remote set-url origin https://"${GH_TOKEN}"@github.com/"${REPO}" || exit 1
            git push origin HEAD || exit 1
            
            echo "Changes pushed to PR #$pr on branch $pr_branch"
        fi
        exit 0
    fi
done

sh "${CREATE_PR_SCRIPT}" "${USER_EMAIL}" \
           "${USER_NAME}" \
           "${WORKDIR}" \
           "${WORKFLOW_ID}" \
           "${PR_OR_COMMIT_URL}" \
           "${GH_TOKEN}" \
           "${REPO}" \
           "${GITHUB_SHA}"