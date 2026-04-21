package com.potato.potatotool.content.redTeam.infoGathering.classObj;

public class CompanyCandidateSelectionResult {
    public enum Action {
        CONFIRM,
        NONE_OF_ABOVE,
        TIMEOUT,
        CANCEL
    }

    private final Action action;
    private final CompanyCandidate selectedCandidate;

    public CompanyCandidateSelectionResult(Action action, CompanyCandidate selectedCandidate) {
        this.action = action;
        this.selectedCandidate = selectedCandidate;
    }

    public Action getAction() {
        return action;
    }

    public CompanyCandidate getSelectedCandidate() {
        return selectedCandidate;
    }
}
