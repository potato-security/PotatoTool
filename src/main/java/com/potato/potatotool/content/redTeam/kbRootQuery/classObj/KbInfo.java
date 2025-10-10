package com.potato.potatotool.content.redTeam.kbRootQuery.classObj;

public class KbInfo {

        private String date;
        private String cve;
        private String kb;
        private String title;
        private String product;
        private String component;
        private String severity;
        private String impact;
        private String repKb;
        private String poc;

        public KbInfo(String date, String cve, String kb, String title, String product, String component, String severity, String impact, String repKb, String poc) {
            this.date = date;
            this.cve = cve;
            this.kb = kb;
            this.title = title;
            this.product = product;
            this.component = component;
            this.severity = severity;
            this.impact = impact;
            this.repKb = repKb;
            this.poc = poc;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getCve() {
            return cve;
        }

        public void setCve(String cve) {
            this.cve = cve;
        }

        public String getKb() {
            return kb;
        }

        public void setKb(String kb) {
            this.kb = kb;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String impact) {
            this.impact = impact;
        }

        public String getRepKb() {
            return repKb;
        }

        public void setRepKb(String repKb) {
            this.repKb = repKb;
        }

        public String getPoc() {
            return poc;
        }

        public void setPoc(String poc) {
            this.poc = poc;
        }
    }