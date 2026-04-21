package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.CompanyCandidate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompanyCandidateUtils {

    @SafeVarargs
    public static List<CompanyCandidate> mergeCandidates(List<CompanyCandidate>... candidateLists) {
        Map<String, CompanyCandidate> mergedMap = new LinkedHashMap<>();
        if (candidateLists == null) {
            return new ArrayList<>();
        }

        for (List<CompanyCandidate> candidateList : candidateLists) {
            if (candidateList == null) {
                continue;
            }
            for (CompanyCandidate candidate : candidateList) {
                if (candidate == null || candidate.getCompanyName() == null) {
                    continue;
                }
                String normalizedName = normalizeCompanyName(candidate.getCompanyName());
                if (normalizedName.isEmpty()) {
                    continue;
                }
                CompanyCandidate existing = mergedMap.get(normalizedName);
                if (existing == null) {
                    CompanyCandidate copied = new CompanyCandidate();
                    copied.setCompanyName(candidate.getCompanyName().trim());
                    copied.mergeFrom(candidate);
                    mergedMap.put(normalizedName, copied);
                } else {
                    existing.mergeFrom(candidate);
                }
            }
        }

        return new ArrayList<>(mergedMap.values());
    }

    public static CompanyCandidate findExactCandidate(String input, List<CompanyCandidate> candidates) {
        String normalizedInput = normalizeCompanyName(input);
        if (normalizedInput.isEmpty() || candidates == null) {
            return null;
        }

        for (CompanyCandidate candidate : candidates) {
            if (candidate != null && normalizedInput.equals(normalizeCompanyName(candidate.getCompanyName()))) {
                return candidate;
            }
        }
        return null;
    }

    public static String normalizeCompanyName(String companyName) {
        return companyName == null ? "" : companyName.trim();
    }
}
