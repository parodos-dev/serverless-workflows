package com.redhat.parodos.workflow.assessment;

import java.util.List;

public class WorkflowOptions {
    private WorkflowOption currentVersion;

    private List<WorkflowOption> upgradeOptions;

    private List<WorkflowOption> migrationOptions;

    private List<WorkflowOption> newOptions;

    private List<WorkflowOption> continuationOptions;

    private List<WorkflowOption> otherOptions;

    public WorkflowOption getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(WorkflowOption currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<WorkflowOption> getUpgradeOptions() {
        return upgradeOptions;
    }

    public void setUpgradeOptions(List<WorkflowOption> upgradeOptions) {
        this.upgradeOptions = upgradeOptions;
    }

    public List<WorkflowOption> getMigrationOptions() {
        return migrationOptions;
    }

    public void setMigrationOptions(List<WorkflowOption> migrationOptions) {
        this.migrationOptions = migrationOptions;
    }

    public List<WorkflowOption> getNewOptions() {
        return newOptions;
    }

    public void setNewOptions(List<WorkflowOption> newOptions) {
        this.newOptions = newOptions;
    }

    public List<WorkflowOption> getContinuationOptions() {
        return continuationOptions;
    }

    public void setContinuationOptions(List<WorkflowOption> continuationOptions) {
        this.continuationOptions = continuationOptions;
    }

    public List<WorkflowOption> getOtherOptions() {
        return otherOptions;
    }

    public void setOtherOptions(List<WorkflowOption> otherOptions) {
        this.otherOptions = otherOptions;
    }
}
