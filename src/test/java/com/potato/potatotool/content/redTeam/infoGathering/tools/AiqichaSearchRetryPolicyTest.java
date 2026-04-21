package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.CompanyCandidate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AiqichaSearchRetryPolicyTest {

    @Test
    public void searchCompanyCandidates_shouldTryNextHeaderProfileAfterAccessRestrictionRetries() {
        TestAiqichaSearch aiqichaSearch = new TestAiqichaSearch();
        aiqichaSearch.enqueue(302, "https://aiqicha.baidu.com/acount/accessrestriction", null);
        aiqichaSearch.enqueue(302, "https://aiqicha.baidu.com/acount/accessrestriction", null);
        aiqichaSearch.enqueue(200, null, validSearchPage("中交建", "中国交通建设股份有限公司", "aqc-1"));

        List<CompanyCandidate> candidates = aiqichaSearch.searchCompanyCandidates("中交建");

        assertEquals(1, candidates.size());
        assertEquals("中国交通建设股份有限公司", candidates.get(0).getCompanyName());
        assertEquals(3, aiqichaSearch.fetchCount);
        assertEquals(
                java.util.Arrays.asList("启动软件时关闭代理，使用中国IP", "启动软件时关闭代理，使用中国IP"),
                aiqichaSearch.tips
        );
    }

    @Test
    public void searchCompanyCandidates_shouldRetryAccountVerificationOnceAndRecover() {
        TestAiqichaSearch aiqichaSearch = new TestAiqichaSearch();
        aiqichaSearch.enqueue(302, "https://wappass.baidu.com/pass", null);
        aiqichaSearch.enqueue(200, null, validSearchPage("中交建", "中国交通建设股份有限公司", "aqc-1"));

        List<CompanyCandidate> candidates = aiqichaSearch.searchCompanyCandidates("中交建");

        assertEquals(1, candidates.size());
        assertEquals("中国交通建设股份有限公司", candidates.get(0).getCompanyName());
        assertEquals("aqc-1", candidates.get(0).getAiqichaPid());
        assertEquals(2, aiqichaSearch.fetchCount);
        assertEquals(1, aiqichaSearch.openedUrls.size());
    }

    @Test
    public void searchCompanyCandidates_shouldTryNextHeaderProfileWhenCookieRedirectOccurs() {
        TestAiqichaSearch aiqichaSearch = new TestAiqichaSearch();
        aiqichaSearch.enqueue(302, "/login?u=https://aiqicha.baidu.com", null);
        aiqichaSearch.enqueue(200, null, validSearchPage("中交建", "中国交通建设股份有限公司", "aqc-1"));

        List<CompanyCandidate> candidates = aiqichaSearch.searchCompanyCandidates("中交建");

        assertEquals(1, candidates.size());
        assertEquals("中国交通建设股份有限公司", candidates.get(0).getCompanyName());
        assertEquals(2, aiqichaSearch.fetchCount);
        assertEquals(Collections.singletonList("请确认[爱企查]Cookie是否有效，跳过部分流程……"), aiqichaSearch.tips);
    }

    @Test
    public void searchCompanyCandidates_shouldIgnoreWeakRecommendationPage() {
        TestAiqichaSearch aiqichaSearch = new TestAiqichaSearch();
        aiqichaSearch.enqueue(200, null, weakRecommendationPage("中交建"));
        aiqichaSearch.enqueue(200, null, weakRecommendationPage("中交建"));

        List<CompanyCandidate> candidates = aiqichaSearch.searchCompanyCandidates("中交建");

        assertTrue(candidates.isEmpty());
        assertEquals(3, aiqichaSearch.fetchCount);
    }

    @Test
    public void searchCompanyCandidates_shouldIgnoreFakeResultPage() {
        TestAiqichaSearch aiqichaSearch = new TestAiqichaSearch();
        aiqichaSearch.enqueue(200, null, fakeResultPage("中交建", "城厢"));
        aiqichaSearch.enqueue(200, null, fakeResultPage("中交建", "城厢"));

        List<CompanyCandidate> candidates = aiqichaSearch.searchCompanyCandidates("中交建");

        assertTrue(candidates.isEmpty());
        assertEquals(3, aiqichaSearch.fetchCount);
    }

    @Test
    public void searchCompanyCandidates_shouldUseBrowserRealPageFallback() {
        TestAiqichaSearch aiqichaSearch = new TestAiqichaSearch();
        aiqichaSearch.browserFallbackResponse =
                new AiqichaSearch.SearchPageFetchResult(200, null, validSearchPage("中交建", "中国交通建设股份有限公司", "aqc-1"),
                        "https://aiqicha.baidu.com/s?q=%E4%B8%AD%E4%BA%A4%E5%BB%BA&t=0");

        List<CompanyCandidate> candidates = aiqichaSearch.searchCompanyCandidates("中交建");

        assertEquals(1, candidates.size());
        assertEquals("中国交通建设股份有限公司", candidates.get(0).getCompanyName());
        assertEquals("aqc-1", candidates.get(0).getAiqichaPid());
        assertEquals(2, aiqichaSearch.fetchCount);
    }

    @Test
    public void buildSearchCookie_shouldPreserveConfiguredCookiesAndRefreshAb() throws Exception {
        TestAiqichaSearch aiqichaSearch = new TestAiqichaSearch();
        String originalCookie = readAiqichaCookie();
        try {
            writeAiqichaCookie("BAIDUID=test-baidu-id; BDUSS=keep-me; ab177500000=stale; ab177500001=stale2; ab_sr=keep-sr");

            String cookie = aiqichaSearch.buildSearchCookie();
            String minimalCookie = aiqichaSearch.buildMinimalSearchCookie();

            assertTrue(cookie.contains("BAIDUID=test-baidu-id"));
            assertTrue(cookie.contains("ab"));
            assertTrue(cookie.contains("BDUSS=keep-me"));
            assertTrue(cookie.contains("ab_sr=keep-sr"));
            assertTrue(!cookie.contains("stale"));
            assertTrue(!cookie.contains("ab177500000="));
            assertTrue(!cookie.contains("ab177500001="));
            assertTrue(minimalCookie.contains("BAIDUID=test-baidu-id"));
            assertTrue(minimalCookie.contains("ab"));
            assertTrue(!minimalCookie.contains("BDUSS"));
        } finally {
            writeAiqichaCookie(originalCookie);
        }
    }

    private static String validSearchPage(String queryWord, String companyName, String pid) {
        return "<html><head><title>爱企查-工商查询_专业企业信息查询平台_公司查询_老板查询_工商信息查询系统</title></head>"
                + "<script>window.pageData = {\"isLogin\":1,\"queryWord\":\""
                + queryWord
                + "\",\"result\":{\"queryStr\":\""
                + queryWord
                + "\",\"title\":\"ok\",\"keywords\":\"ok\",\"description\":\"ok\",\"resultList\":[{\"titleName\":\""
                + companyName
                + "\",\"entName\":\""
                + companyName
                + "\",\"pid\":\""
                + pid
                + "\"}],\"absorbed\":[]}}; window.isSpider = false;</script></html>";
    }

    private static String weakRecommendationPage(String companyName) {
        return "<html><script>window.pageData = {\"isLogin\":1,\"queryWord\":\""
                + companyName
                + "\",\"result\":{\"resultList\":[],\"absorbed\":[{\"pid\":\"aqc-1\",\"name\":\"中国交通建设股份有限公司\"}]}}; window.isSpider = false;</script></html>";
    }

    private static String fakeResultPage(String companyName, String fakeQueryStr) {
        return "<html><script>window.pageData = {\"isLogin\":1,\"queryWord\":\""
                + companyName
                + "\",\"result\":{\"queryStr\":\""
                + fakeQueryStr
                + "\",\"title\":\"ok\",\"keywords\":\"ok\",\"description\":\"ok\",\"resultList\":[{\"titleName\":\"中国交通建设股份有限公司\",\"pid\":\"aqc-1\"}],\"absorbed\":[]}}; window.isSpider = false;</script></html>";
    }

    private static String readAiqichaCookie() throws Exception {
        Field field = AiqichaSearch.class.getDeclaredField("Aiqicha_Cookie");
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private static void writeAiqichaCookie(String value) throws Exception {
        Field field = AiqichaSearch.class.getDeclaredField("Aiqicha_Cookie");
        field.setAccessible(true);
        field.set(null, value);
    }

    private static class TestAiqichaSearch extends AiqichaSearch {
        private final Deque<SearchPageFetchResult> searchPageResponses = new ArrayDeque<>();
        private final List<String> tips = new ArrayList<>();
        private final List<String> openedUrls = new ArrayList<>();
        private SearchPageFetchResult browserFallbackResponse;
        private int fetchCount;

        private TestAiqichaSearch() {
            super(Collections.<Integer>emptyList(), null);
        }

        private void enqueue(int statusCode, String location, String content) {
            searchPageResponses.addLast(new SearchPageFetchResult(statusCode, location, content));
        }

        @Override
        protected SearchPageFetchResult fetchSearchPageResult(String companyName) {
            fetchCount++;
            return searchPageResponses.isEmpty() ? null : searchPageResponses.removeFirst();
        }

        @Override
        protected SearchPageFetchResult fetchSearchPageResultFromExistingBrowserSession(String companyName) {
            return browserFallbackResponse;
        }

        @Override
        protected SearchPageFetchResult fetchSearchPageResultFromManagedBrowser(String companyName) {
            return null;
        }

        @Override
        protected String buildExistingSessionSearchCookie() {
            return "";
        }

        @Override
        protected boolean sleepInterruptibly(long millis) {
            return true;
        }

        @Override
        protected void showTip(String message) {
            tips.add(message);
        }

        @Override
        protected void openDocument(String location) {
            openedUrls.add(location);
        }
    }
}
