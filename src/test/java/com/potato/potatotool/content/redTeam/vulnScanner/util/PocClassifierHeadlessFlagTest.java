package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PocClassifierHeadlessFlagTest {

    @Test
    public void testClassifyPocMarksHeadlessWhenProtocolIsHeadlessWithoutPath() {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setProtocol("headless");

        PocClassifier.classifyPoc(poc, null);

        assertTrue(poc.isRequiresHeadless());
        assertEquals(PocObj.PocCategory.HEADLESS, poc.getCategory());
        assertEquals(PocObj.InputType.URL, poc.getInputType());
    }

    @Test
    public void testClassifyPocMarksHeadlessWhenVerifyStepsContainHeadlessStepWithoutPath() {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setVerifySteps(Collections.<PocObj.PocStep>singletonList(new PocObj.HeadlessStep()));

        PocClassifier.classifyPoc(poc, null);

        assertTrue(poc.isRequiresHeadless());
        assertEquals(PocObj.PocCategory.HEADLESS, poc.getCategory());
        assertEquals(PocObj.InputType.URL, poc.getInputType());
    }
}
