package com.redhat.parodos.workflow.assessment;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BasicAssessment {
    public WorkflowOptions execute(String repositoryUrl) {
        WorkflowOptions workflowOptions = new WorkflowOptions();
        if (repositoryUrl.toLowerCase().contains("java")) { // basic check for workflow options recommendation
            workflowOptions.setCurrentVersion(new WorkflowOption("ocpOnbarding", "Ocp Onboarding"));
            workflowOptions.setUpgradeOptions(new ArrayList<>());
            workflowOptions.setMigrationOptions(List.of(new WorkflowOption("move2kube", "Move2Kube")));
            workflowOptions.setNewOptions(List.of(new WorkflowOption("vmOnboarding", "Vm Onboarding")));
            workflowOptions.setContinuationOptions(new ArrayList<>());
            workflowOptions.setOtherOptions(List.of(new WorkflowOption("training", "Training")));
            return workflowOptions;
        }
        workflowOptions.setCurrentVersion(new WorkflowOption("analysis", "Analysis"));
        workflowOptions.setUpgradeOptions(new ArrayList<>());
        workflowOptions.setMigrationOptions(new ArrayList<>());
        workflowOptions.setNewOptions(new ArrayList<>());
        workflowOptions.setContinuationOptions(new ArrayList<>());
        workflowOptions.setOtherOptions(List.of(new WorkflowOption("training", "Training")));
        return workflowOptions;
    }
}
