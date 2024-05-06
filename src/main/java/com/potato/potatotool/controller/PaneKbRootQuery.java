package com.potato.potatotool.controller;

import com.dlsc.gemsfx.FilterView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.*;

import static com.potato.potatotool.content.kbCheck.*;

/**
 * @author Potato
 * @date 2023/3/21 17:14
 */
public class PaneKbRootQuery {

    private static List<Map<String, String>> csvData;
    static {
        csvData = init();
    }

    @FXML
    private StackPane sPane;

    @FXML
    private VBox vBox;

    @FXML
    private TextArea inputText;

    FilterView<KbInfo> filterView;


    FilterView.FilterGroup<KbInfo> productGroup = new FilterView.FilterGroup<>("影响产品");
    FilterView.FilterGroup<KbInfo> componentGroup = new FilterView.FilterGroup<>("影响组件");
    FilterView.FilterGroup<KbInfo> severityGroup = new FilterView.FilterGroup<>("严重性");
    FilterView.FilterGroup<KbInfo> impactGroup = new FilterView.FilterGroup<>("漏洞影响");
    FilterView.FilterGroup<KbInfo> pocGroup = new FilterView.FilterGroup<>("漏洞利用");


    @FXML
    void initialize(){
        filterView = new FilterView<>();
//        filterView.setTextFilterProvider(text -> KbInfo -> KbInfo.getKb().toLowerCase().contains(text) || KbInfo.getCve().toLowerCase().contains(text));
        filterView.setShowHeader(false);

        TableView<KbInfo> tableView = new TableView<>();
        tableView.setSortPolicy(table -> false);

//        FilterView.FilterGroup<KbInfo> productGroup = new FilterView.FilterGroup<>("影响产品");
//        FilterView.FilterGroup<KbInfo> componentGroup = new FilterView.FilterGroup<>("影响组件");
//        FilterView.FilterGroup<KbInfo> severityGroup = new FilterView.FilterGroup<>("严重性");
//        FilterView.FilterGroup<KbInfo> impactGroup = new FilterView.FilterGroup<>("漏洞影响");
//        FilterView.FilterGroup<KbInfo> pocGroup = new FilterView.FilterGroup<>("漏洞利用");

//        dateGroup.getFilters().add(new FilterView.Filter<>("1900 - 2100", true) {
//            @Override
//            public boolean test(KbInfo KbInfo) {
//                if (Integer.parseInt(KbInfo.getDate()) < 1900) {
//                    return false;
//                }
//                return Integer.parseInt(KbInfo.getDate()) <= 21000000;
//            }
//        });


//        firstNameGroup.getFilters().add(new FilterView.Filter<>("Paul, Eric") {
//            @Override
//            public boolean test(KbInfo KbInfo) {
//                String firstName = KbInfo.getFirstName();
//                return firstName.equals("Paul") || firstName.equals("Eric");
//            }
//        });
//
//
//        firstNameGroup.getFilters().add(new FilterView.Filter<>("Elizabeth") {
//            @Override
//            public boolean test(KbInfo KbInfo) {
//                return KbInfo.getFirstName().equals("Elizabeth");
//            }
//        });
//
//
//        roleGroup.getFilters().add(new FilterView.Filter<>("Parent") {
//            @Override
//            public boolean test(KbInfo KbInfo) {
//                return KbInfo.getRole().equals("Parent");
//            }
//        });

        filterView.getFilterGroups().setAll(productGroup, componentGroup, severityGroup, impactGroup, pocGroup);

        SortedList<KbInfo> sortedList = new SortedList<>(filterView.getFilteredItems());
        tableView.setItems(sortedList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        filterView.getItems().add(new KbInfo("20170314","","4014329","Security Update for Adobe Flash Player","Windows 10 Version 1511 for x64-based Systems","Adobe Flash Player","Critical","Remote Code Execution","4010250",""));
        filterView.getItems().add(new KbInfo("20170314","","4014329","Security Update for Adobe Flash Player","Windows 10 Version 1607 for 32-bit Systems","Adobe Flash Player","Critical","Remote Code Execution","4010250",""));


        TableColumn<KbInfo, String> dateColumn = new TableColumn<>("发布日期");
        TableColumn<KbInfo, String> cveColumn = new TableColumn<>("CVE编号");
        TableColumn<KbInfo, String> kbColumn = new TableColumn<>("KB编号");
        TableColumn<KbInfo, String> titleColumn = new TableColumn<>("标题");
        TableColumn<KbInfo, String> productColumn = new TableColumn<>("影响产品");
        TableColumn<KbInfo, String> componentColumn = new TableColumn<>("影响组件");
        TableColumn<KbInfo, String> severityColumn = new TableColumn<>("严重性");
        TableColumn<KbInfo, String> impactColumn = new TableColumn<>("漏洞影响");
        TableColumn<KbInfo, String> repKbColumn = new TableColumn<>("替代KB编号");
        TableColumn<KbInfo, String> pocColumn = new TableColumn<>("漏洞利用");

        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        cveColumn.setCellValueFactory(new PropertyValueFactory<>("cve"));
        kbColumn.setCellValueFactory(new PropertyValueFactory<>("kb"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        componentColumn.setCellValueFactory(new PropertyValueFactory<>("component"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        impactColumn.setCellValueFactory(new PropertyValueFactory<>("impact"));
        repKbColumn.setCellValueFactory(new PropertyValueFactory<>("repKb"));
        pocColumn.setCellValueFactory(new PropertyValueFactory<>("poc"));

        tableView.getColumns().setAll(dateColumn, cveColumn, kbColumn, titleColumn, productColumn, componentColumn, severityColumn, impactColumn, repKbColumn, pocColumn);

        VBox box = new VBox(filterView, tableView);
        box.setPadding(new Insets(0,0,10,0));
        box.setSpacing(10);

        VBox.setVgrow(tableView, Priority.ALWAYS);
        vBox.getChildren().add(box);
    }

    @FXML
    public void searchKb(ActionEvent event) {
        String input = inputText.getText();

        if(!input.toLowerCase().contains("kb")){
            inputText.setText("输入信息有误，请粘贴补丁号或systeminfo信息");
            return;
        }

        Platform.runLater(() -> {
            filterView.getItems().clear();
            productGroup.getFilters().clear();
            componentGroup.getFilters().clear();
            severityGroup.getFilters().clear();
            impactGroup.getFilters().clear();
            pocGroup.getFilters().clear();

            List<Map<String, String>> filteredKB = filterKB(csvData, input);
            List<KbInfo> kbInfo = new ArrayList<>();

            List<FilterView.Filter<KbInfo>> productFilterView = new ArrayList<>();
            List<String> productList = new ArrayList<>();
            List<FilterView.Filter<KbInfo>> componentFilterView = new ArrayList<>();
            List<String> componentList = new ArrayList<>();
            List<FilterView.Filter<KbInfo>> severityFilterView = new ArrayList<>();
            List<String> severityList = new ArrayList<>();
            List<FilterView.Filter<KbInfo>> impactFilterView = new ArrayList<>();
            List<String> impactList = new ArrayList<>();

            filteredKB.forEach(map -> {
                KbInfo info = new KbInfo(map.get("发布日期"), map.get("CVE编号"), map.get("KB编号"), map.get("标题"), map.get("影响产品"), map.get("影响组件"), map.get("严重性"), map.get("漏洞影响"), map.get("替代KB编号"), map.get("漏洞利用"));
                kbInfo.add(info);

                String product = map.get("影响产品");
                String component = map.get("影响组件");
                String severity = map.get("严重性");
                String impact = map.get("漏洞影响");

                if (!productList.contains(product) && !product.isEmpty()) {
                    productList.add(product);
                    FilterView.Filter tmpFilter=new FilterView.Filter<KbInfo>(product) {
                        @Override
                        public boolean test(KbInfo kbinfo) {
                            return kbinfo.getProduct().equals(product);
                        }
                    };
                    productFilterView.add(tmpFilter);
                }
                if(!componentList.contains(component) && !component.isEmpty()){
                    componentList.add(component);
                    FilterView.Filter tmpFilter=new FilterView.Filter<KbInfo>(component) {
                        @Override
                        public boolean test(KbInfo kbinfo) {
                            return kbinfo.getComponent().equals(component);
                        }
                    };
                    componentFilterView.add(tmpFilter);
                }
                if(!severityList.contains(severity) && !severity.isEmpty()){
                    severityList.add(severity);
                    FilterView.Filter tmpFilter=new FilterView.Filter<KbInfo>(severity) {
                        @Override
                        public boolean test(KbInfo kbinfo) {
                            return kbinfo.getComponent().equals(severity);
                        }
                    };
                    severityFilterView.add(tmpFilter);
                }
                if(!impactList.contains(impact) && !impact.isEmpty()){
                    impactList.add(impact);
                    FilterView.Filter tmpFilter=new FilterView.Filter<KbInfo>(impact) {
                        @Override
                        public boolean test(KbInfo kbinfo) {
                            return kbinfo.getComponent().equals(impact);
                        }
                    };
                    impactFilterView.add(tmpFilter);
                }

            });

            filterView.getItems().addAll(kbInfo);

            productGroup.getFilters().addAll(productFilterView);
            componentGroup.getFilters().addAll(componentFilterView);
            severityGroup.getFilters().addAll(severityFilterView);
            impactGroup.getFilters().addAll(impactFilterView);

            pocGroup.getFilters().add(new FilterView.Filter<>("存在漏洞利用") {
                @Override
                public boolean test(KbInfo KbInfo) {
                    return !KbInfo.getPoc().isEmpty();
                }
            });
            pocGroup.getFilters().add(new FilterView.Filter<>("不存在漏洞利用") {
                @Override
                public boolean test(KbInfo KbInfo) {
                    return KbInfo.getPoc().isEmpty();
                }
            });
            filterView.getFilterGroups().setAll(productGroup, componentGroup, severityGroup, impactGroup, pocGroup);

        });
    }


    public static class KbInfo {

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


    public static void main(String []args) {
        List<String> strList = new ArrayList<>();
        strList.add("4014329");
        strList.add("3216916");
        strList.add("4022721");
        strList.add("4022168");

        List<Map<String, String>> filteredKB = filterKB(csvData, strList);
        System.out.println(filteredKB);
        List<Map<String, String>> filteredExp = filterExp(filteredKB);
        System.out.println(filteredExp);
    }

}
