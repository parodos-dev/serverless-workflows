#!/bin/bash

# Set repository (update with your repo)
REPO=$1
WORKFLOW_ID=$2
PR_OR_COMMIT_URL=$3
INPUT_VALUES_FILEPATH=$4

CREATE_PR_SCRIPT=$5
USER_EMAIL=$6
USER_NAME=$7
WORKDIR=$8
GH_TOKEN=$9
WF_CONFIG_REPO=${10}

# Get all open PRs for the repository
prs=$(gh pr list --repo "${REPO}" --json number --jq '.[].number')

# Loop through each PR and check for the version in Chart.yaml
for pr in $prs; do
    echo "Checking PR #$pr..."
    
    # Fetch the list of changed files for the PR
    files=$(gh pr diff "$pr" --name-only --repo "${REPO}")
    
    # Check if Chart.yaml is in the list of changed files
    if echo "$files" | grep -q "charts/${WORKFLOW_ID}/Chart.yaml"; then
        echo "Chart.yaml found in PR #${pr}"
        
        # Get the contents of the Chart.yaml file from the PR
        chart_content=$(gh pr diff "$pr" --repo "${REPO}" -- "charts/${WORKFLOW_ID}/Chart.yaml")
        
        # Check if the content has a version entry
        if echo "$chart_content" | grep -q "^version: .*"; then
            # Get the PR details (branch)
            pr_branch=$(gh pr view "$pr" --repo "$REPO" --json headRefName --jq '.headRefName')
            
            # Checkout the PR branch
            git fetch origin "$pr_branch"
            git checkout "$pr_branch"
            
            # Create the new file
            cp "${WORKDIR}/${WORKFLOW_ID}/manifests/*" "charts/${WORKFLOW_ID}/templates"
            if [ "${INPUT_VALUES_FILEPATH}" != "" ]; then
                cp "${INPUT_VALUES_FILEPATH}" "charts/${WORKFLOW_ID}/values.yaml"
            fi
            git add -A

            git commit -m "(${WORKFLOW_ID}) Automated PR"
            echo "Automated PR from $PR_OR_COMMIT_URL" | git commit --amend --file=-
  
            git push -f origin "$pr_branch"
            
            echo "Changes pushed to PR #$pr on branch $pr_branch"

            exit 0
        fi
    fi
done

sh "${CREATE_PR_SCRIPT}" "${USER_EMAIL}" \
           "${USER_NAME}" \
           "${WORKDIR}" \
           "${WORKFLOW_ID}" \
           "${PR_OR_COMMIT_URL}" \
           "${GH_TOKEN}" \
           "${WF_CONFIG_REPO}" 
