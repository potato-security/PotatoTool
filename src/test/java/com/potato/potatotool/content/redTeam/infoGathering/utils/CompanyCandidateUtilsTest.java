package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.CompanyCandidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompanyCandidateUtilsTest {

    @Test
    public void mergeCandidates_shouldMergeSameExactCompanyNameAcrossSources() {
        CompanyCandidate chinazCandidate = new CompanyCandidate();
        chinazCandidate.setCompanyName("中国交通建设股份有限公司");
        chinazCandidate.setChinazCompanyId("chinaz-1");
        chinazCandidate.setLegalRepresentative("张三");
        chinazCandidate.addSource(CompanyCandidate.SOURCE_CHINAZ);

        CompanyCandidate aiqichaCandidate = new CompanyCandidate();
        aiqichaCandidate.setCompanyName("中国交通建设股份有限公司");
        aiqichaCandidate.setAiqichaPid("aiqicha-1");
        aiqichaCandidate.setCompanyStatus("存续");
        aiqichaCandidate.addSource(CompanyCandidate.SOURCE_AIQICHA);

        List<CompanyCandidate> merged = CompanyCandidateUtils.mergeCandidates(
                asList(chinazCandidate),
                asList(aiqichaCandidate)
        );

        assertEquals(1, merged.size());
        CompanyCandidate candidate = merged.get(0);
        assertEquals("chinaz-1", candidate.getChinazCompanyId());
        assertEquals("aiqicha-1", candidate.getAiqichaPid());
        assertEquals("张三", candidate.getLegalRepresentative());
        assertEquals("存续", candidate.getCompanyStatus());
        assertTrue(candidate.getSources().contains(CompanyCandidate.SOURCE_CHINAZ));
        assertTrue(candidate.getSources().contains(CompanyCandidate.SOURCE_AIQICHA));
    }

    @Test
    public void findExactCandidate_shouldOnlyMatchExactCompanyName() {
        CompanyCandidate candidate = new CompanyCandidate();
        candidate.setCompanyName("中国交通建设股份有限公司");
        candidate.addSource(CompanyCandidate.SOURCE_CHINAZ);

        List<CompanyCandidate> candidates = asList(candidate);

        assertNotNull(CompanyCandidateUtils.findExactCandidate("中国交通建设股份有限公司", candidates));
        assertNull(CompanyCandidateUtils.findExactCandidate("中交建", candidates));
        assertNull(CompanyCandidateUtils.findExactCandidate("中国交通建设集团有限公司", candidates));
    }

    private static List<CompanyCandidate> asList(CompanyCandidate candidate) {
        List<CompanyCandidate> result = new ArrayList<>();
        result.add(candidate);
        return result;
    }
}
