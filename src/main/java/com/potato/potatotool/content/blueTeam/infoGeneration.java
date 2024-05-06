package com.potato.potatotool.content.blueTeam;

import com.github.promeg.pinyinhelper.Pinyin;

import java.time.LocalDate;
import java.util.Random;


/**
 * @author Potato
 * @date 2024/4/19 11:30
 */
public class infoGeneration {

    private static final Random random = new Random();


    /**
     * 姓名生成
     * @return
     */
    public static String name(){

        String[] surname_list  = {
                "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "吴", "周",
                "徐", "孙", "马", "朱", "胡", "郭", "何", "林", "罗", "高",
                "郑", "梁", "谢", "宋", "唐", "许", "韩", "邓", "冯", "曹",
                "彭", "曾", "肖", "田", "董", "潘", "袁", "蔡", "蒋", "余",
                "于", "杜", "叶", "程", "魏", "苏", "吕", "丁", "任", "卢",
                "姚", "沈", "钟", "姜", "崔", "谭", "陆", "范", "汪", "廖",
                "石", "金", "韦", "贾", "夏", "付", "方", "邹", "熊", "白",
                "孟", "秦", "邱", "侯", "江", "尹", "薛", "闫", "段", "雷",
                "龙", "黎", "史", "陶", "贺", "毛", "郝", "顾", "龚", "邵",
                "万", "覃", "武", "钱", "戴", "严", "莫", "孔", "向", "常"
        };
        String[] lastName_list = {"璇","颖","宁","佳","乐","妮","一","华","琴","顺","静","齐","洋","臻","萤","东","宇","颜","君","刚","芳","燕","萌","超","涛","仑","易","鑫","楠","玉","妍","飞","青","琳","琦","莉","讯","斌","彬","勋","亮","烨","葵","极","兆","子璇","淼","国栋","夫子","瑞堂","甜","敏","尚","国贤","贺祥","晨涛","昊轩","易轩","益辰","益帆","益冉","瑾春","瑾昆","春齐","杨","文昊","东东","雄霖","浩晨","熙涵","溶溶","冰枫","欣欣","宜豪","欣慧","建政","美欣","淑慧","文轩","文杰","欣源","忠林","榕润","欣汝","慧嘉","新建","建林","亦菲","林","冰洁","佳欣","涵涵","禹辰","淳美","泽惠","伟洋","涵越","润丽","翔","淑华","晶莹","凌晶","苒溪","雨涵","嘉怡","佳毅","子辰","佳琪","紫轩","瑞辰","昕蕊","萌","明远","欣宜","泽远","欣怡","佳怡","佳惠","晨茜","晨璐","运昊","汝鑫","淑君","晶滢","润莎","榕汕","佳钰","佳玉","晓庆","一鸣","语晨","添池","添昊","雨泽","雅晗","雅涵","清妍","诗悦","嘉乐","晨涵","天赫","玥傲","佳昊","天昊","萌萌","若萌"};

        String surname = surname_list[random.nextInt(surname_list.length)];
        String lastName = lastName_list[random.nextInt(lastName_list.length)];
        String name = surname + lastName;

        return name;

    }


    /**
     * 手机号生成
     * @return
     */
    public static String phone(){

        String[] china_Mobile = {"139","138","137","136","134","135","147","150","151","152","157","158","159","172","178","182","183","184","187","188","195","197","198"};
        String[] china_Unicom = {"130","131","132","140","145","146","155","156","185","186","175","176","196"};
        String[] china_Telecom = {"133","149","153","177","173","180","181","189","191","193","199"};

        String first_phone = null;
        String last_phone = generateRandomDigits(8);

        String[] selectedOperator = china_Mobile;
        String operatorMode = "中国移动";
        switch (random.nextInt(3)) {
            case 0:
                selectedOperator = china_Mobile;
                operatorMode = "中国移动";
                break;
            case 1:
                selectedOperator = china_Unicom;
                operatorMode = "中国联通";
                break;
            case 2:
                selectedOperator = china_Telecom;
                operatorMode = "中国电信";
                break;
        }

        first_phone = selectedOperator[random.nextInt(selectedOperator.length)];
        String phone = first_phone + last_phone;

        return String.join("-", operatorMode, phone);

    }

    // 随机号码生成
    private static String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // 生成随机数字
        }
        return sb.toString();
    }

    /**
     * 身份证号生成
     * @return
     */
    public static String idCard(){
        // 可用区域编号
        String[] can_use_area = {"110100","110101","110102","110103","110104","110105","110107","110108","110109","110111","110112","110113",
                "110114","110115","110116","110228","120100","120102","120103","120104","120106","120107","120108","120109",
                "120110","120111","120112","120113","120114","120115","120221","120223","120225","130100","130101","130102",
                "130103","130104","130105","130107","130108","130121","130123","130124","130125","130126","130127","130128",
                "141126","141127","141128","141129","141181","150100","150101","150102","150103","150104","150105","150121",
                "210100","210101","210102","210103","210104","210105","210111","210112","210113","210114","210122","210123",
                "211000","211001","211002","211003","211004","211011","211021","211100","211101","211102","211103","211121",
                "220100","220101","220102","220103","220104","220105","220106","220122","220181","220182","220183","220200",
                "230100","230101","230103","230104","230105","230106","230108","230121","230123","230124","230125","230126",
                "230400","230401","230402","230403","230404","230405","230407","230421","230422","230500","230501","230502",
                "231000","231001","231002","231003","231004","231024","231081","231083","231084","231085","231100","231101",
                "310100","310101","310103","310105","310106","310107","310108","310109","310110","310112","310113","310114",
                "370405","370406","370481","370500","370501","370502","370521","370523","370600","370601","370602","370611",
                "410222","410223","410224","410225","410301","410302","410303","410304","410305","410306","410307","410322",
                "441284","441300","441301","441303","441322","441323","441324","441400","441401","441402","441421","441422",
                "500110","500111","500112","500114","500115","500200","500222","500223","500224","500225","500226","500227",
                "610101","610102","610103","610104","610111","610112","610113","610114","610115","610116","610122","610124",
                "653124","653125","653126","653127","653128","653129","653130","653131","653200","653201","653221","653222",
                "654022","654023","654024","654025","654026","654027","654028","654200","654201","654202","654221","654223",
                "654224","654225","654226","654300","654301","654321","654322","654323","654324","654325","654326","659000"};

        String[] man = {"1", "3", "5", "7", "9"};
        String[] girl = {"0", "2", "4", "6", "8"};
        // 身份证号码的加权因子
        String[] COEFFICIENT_ARRAY = {"7", "9", "10", "5", "8", "4", "2", "1", "6", "3", "7", "9", "10", "5", "8", "4", "2"};
        // 身份证号码的校验码
        String[] LAST_NUMBER_ARRAY = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};

        String address = can_use_area[random.nextInt(can_use_area.length)];
        String birthday = generateRandomBirthday().toString().replace("-", "");
        int sex = random.nextInt(2);
        String sexCode = random.nextInt(10) + "" + random.nextInt(10) + "" + (((sex == 1) ? man[random.nextInt(man.length)] : girl[random.nextInt(girl.length)]));

        // 合并身份证号码的前17位
        String idcardWithoutLast = address + birthday + sexCode;

        // 计算校验码
        int total = 0;
        for (int j = 0; j < idcardWithoutLast.length(); j++) {
            total += Integer.parseInt(String.valueOf(idcardWithoutLast.charAt(j))) * Integer.parseInt(COEFFICIENT_ARRAY[j]);
        }
        String lastNumber = LAST_NUMBER_ARRAY[total % 11];

        // 合并生成的完整身份证号码
        String idcard = idcardWithoutLast + lastNumber;

        return idcard;

    }


    // 生成随机出生日期
    private static LocalDate generateRandomBirthday() {
        int year = random.nextInt(40) + 1980;
        int month = random.nextInt(12) + 1;
        int day = random.nextInt(28) + 1; // 限制日期在 1 到 28 之间，避免生成无效日期
        return LocalDate.of(year, month, day);
    }


    /**
     * 统一社会信用代码
     * @return
     */
    public static String unifiedSocialCreditCode(){
        String[] orgCodeList = {
                "1|机构编制", "2|外交", "3|教育", "4|公安", "5|民政", "6|司法", "7|交通运输", "8|文化", "9|工商",
                "A|中央军委改革和编制办公室", "N|农业", "Y|其他"
        };
        String[] icCodeList = {
                "1|机关", "2|事业单位", "3|中央编办直接管理机构编制的群众团体", "9|其他",
                "1|外国常驻新闻机构", "9|其他", "1|律师执业机构", "2|公证处", "3|基层法律服务所", "4|司法鉴定机构",
                "5|仲裁委员会", "9|其他", "1|外国在华文化中心", "9|其他", "1|社会团体", "2|民办非企业单位",
                "3|基金会", "9|其他", "1|外国旅游部门常驻机构代表机构", "2|港澳台地区旅游部门常驻内地（大陆）代表机构",
                "9|其他", "1|宗教活动场所", "2|宗教院校", "9|其他", "1|基层工会", "9|其他", "1|企业",
                "2|个体工商户", "3|农民专业社", "1|军队事业单位", "9|其他", "1|组级集体经济组织", "2|村级集体经济组织",
                "3|乡镇集体经济组织", "9|其他", "1|其他"
        };
        String[] areasList = {
                "110000", "110101", "110102", "110103", "110104", "110105", "110106", "110107", "110108", "110109",
                "110111", "110112", "110113", "110114", "110115", "110116", "110117", "110228", "110229", "120000",
                "120101", "120102", "120103", "120104", "120105", "120106", "120107", "120108", "120109", "120110",
                "120111", "120112", "120113", "120114", "120115", "120221", "120223", "120225", "130000", "130100",
                "130102", "130103", "130104", "130105", "130107", "130108", "130121", "130123", "130124", "130125",
                "130126", "130127", "130128", "130129", "130130", "130131", "130132", "130133", "130181", "130182",
                "130183", "130184", "130185", "130200", "130202", "130203", "130204", "130205", "130207", "130208",
                "130223", "130224", "130225", "130227", "130229", "130230", "130281", "130283", "130300", "130302",
                "130303", "130304", "130321", "130322", "130323", "130324", "130400", "130402", "130403", "130404",
                "130406", "130421", "130423", "130424", "130425", "130426", "130427", "130428", "130429", "130430"
        };

        // 登记管理部门代码 第1位
        String[] orgCode = orgCodeList[random.nextInt(orgCodeList.length)].split("\\|");

        // 机构类别代码 第2位
        String[] icCode = icCodeList[random.nextInt(icCodeList.length)].split("\\|");

        // 登记管理机关行政区划码 第3—8位
        String area = areasList[random.nextInt(areasList.length)];

        // 主体标识码(组织机构代码) 第9—17位
        StringBuilder subject = new StringBuilder();
        subject.append(random.nextInt(8) + 1); // 第一位数字为1-9
        for (int i = 1; i < 8; i++) {
            subject.append(random.nextInt(10)); // 后面7位数字为0-9
        }

        // 校验码 第18位
        String code = orgCode[0] + icCode[0] + area + subject.toString();
        String checkCode = generateOrgCheckCode(code);
        return code + checkCode;

    }

    /**
     * 生成组织机构代码
     * @return
     */
    public static String organizationCode() {
        StringBuilder code = new StringBuilder();
        code.append(random.nextInt(8) + 1); // 第一位数字为1-9
        for (int i = 1; i < 8; i++) {
            code.append(random.nextInt(10)); // 后面7位数字为0-9
        }
        String checkCode = generateOrgCheckCode(code.toString());
        return code.toString() + "-" + checkCode;
    }

    // 生成校验码
    private static String generateOrgCheckCode(String code) {
        String s = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int[] wi = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};
        int sum = 0;
        for (int i = 0; i < code.length(); i++) {
            sum += s.indexOf(code.charAt(i)) * wi[i];
        }
        int C18 = 31 - (sum % 31);
        if (C18 == 31) {
            return "0";
        } else {
            return s.substring(C18, C18 + 1);
        }
    }


    /**
     * 银行卡号生成
     * @return
     */
    public static String bankCard(){
        String[] bankList = {
                "621410|工商银行牡丹灵通卡借记卡",
                "621670|中国工商银行普通高中学生资助卡借记卡",
                "548259|工商银行国际借记卡借记卡",
                "451811|工商银行牡丹贷记卡(银联卡)贷记卡",
                "513685|工商银行国航知音牡丹信用卡贷记卡",
                "438126|工商银行牡丹VISA白金卡贷记卡",
                "621284|建设银行中职学生资助卡借记卡",
                "434061|建设银行乐当家金卡VISA借记卡",
                "434062|建设银行乐当家白金卡借记卡",
                "489592|中国建设银行VISA白金信用卡贷记卡",
                "622725|建设银行龙卡准贷记卡准贷记卡",
                "402658|招商银行两地一卡通借记卡",
                "601428|交通银行太平洋万事顺卡借记卡",
                "622181|邮储银行绿卡专用卡借记卡",
                "620059|农业银行银联标准卡借记卡",
                "622618|民生银行钻石卡借记卡",
                "621243|上海银行单位卡借记卡",
                "622993|大连银行人民币借记卡借记卡",
                "622884|渤海银行渤海银行借记卡",
                "621279|宁波银行银联标准卡借记卡",
                "940023|厦门银行股份有限公司借记卡",
        };
        String bankInfo = bankList[random.nextInt(bankList.length)];
        String[] bankInfoParts = bankInfo.split("\\|");
        String bankCode = bankInfoParts[0];
        String bankName = bankInfoParts[1];

        String cardNumber = bankCode + generateRandomDigits(9);

        String code = "";
        int temp_data = 0;

        // 计算奇数位的总和
        for (int i = 1; i < 15; i += 2) {
            int temp = Integer.parseInt(cardNumber.substring(i, i + 1)) * 2;
            if (temp > 9) {
                temp_data += temp - 9;
            } else {
                temp_data += temp;
            }
        }

        // 计算偶数位的总和
        for (int i = 0; i < 15; i += 2) {
            temp_data += Integer.parseInt(cardNumber.substring(i, i + 1));
        }

        // 寻找合法的校验位
        for (int i = 0; i < 10; i++) {
            if ((temp_data + i) % 10 == 0) {
                code = cardNumber + i;
                break; // 找到一个合法的校验位即可退出循环
            }
        }

        return code + "|" + bankName;
    }


    /**
     * 邮箱号生成
     * @return
     */
    public static String email(String name_PinYin){
        String[] type_list = {
                "@yahoo.com",
                "@gmail.com",
                "@sina.com",
                "@163.com",
                "@126.com",
                "@qq.com",
                "@hotmail.com"
        };
        String type = type_list[random.nextInt(type_list.length)];

        return name_PinYin+type;
    }


    /**
     * 用户名中文转拼音 张三-ZhangSan
     * @return
     */
    public static String nameToPinYin(String chineseName){

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < chineseName.length(); i++){
            char currentChar = chineseName.charAt(i);
            String pinyin = Pinyin.toPinyin(currentChar);

            result.append(capitalizeFirstLetter(pinyin.toLowerCase()));
        }

        return result.toString();

    }


    // 拼音开头大写
    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 邮编号生成
     * @return
     */
    public static String postalCode(){
        String[] postal_Code = {
                "110102",
                "110105",
                "110106",
                "110107",
                "110108",
                "110109",
                "110111",
                "110112",
                "110113",
                "110114",
                "110115",
                "110116",
                "110117",
                "110200",
                "110228",
                "110229",
                "340100",
                "340121",
                "130183",
                "130184",
                "130185",
                "130200",
                "130201",
                "130202",
                "130203",
                "130204",
                "130205",
                "130207",
                "130208",
                "130223",
                "130224",
                "130225",
                "130227",
                "130229",
                "130230",
                "130281",
                "130283",
                "130300",
                "130301",
                "130302",
                "130303",
                "130304",
                "130321",
                "130322",
                "130323",
                "130324",
                "130400",
                "130401",
                "130402",
                "130403",
                "130404",
                "130406",
                "130421",
                "130423",
                "130424",
                "130425",
                "130426",
                "130427",
                "130428",
                "130429",
                "130430",
                "130431",
                "130432"
        };

        return postal_Code[random.nextInt(postal_Code.length)];
    }


    /**
     * 地址生成
     * @return
     */
    public static String address(){
        String[] city_list = {
                "北京",
                "上海",
                "天津",
                "重庆",
                "哈尔滨",
                "长春",
                "沈阳",
                "呼和浩特",
                "石家庄",
                "乌鲁木齐",
                "兰州",
                "西宁",
                "西安",
                "银川",
                "郑州",
                "济南",
                "太原",
                "合肥",
                "武汉",
                "长沙",
                "南京",
                "成都",
                "贵阳",
                "昆明",
                "南宁",
                "拉萨",
                "杭州",
                "南昌",
                "广州",
                "福州",
                "台北",
                "海口",
                "香港",
                "澳门",
                "通辽",
                "兴安盟",
                "太原",
                "辛集",
                "邯郸",
                "沈阳",
                "辽阳",
                "兴城",
                "北镇",
                "阜新",
                "哈尔滨",
                "齐齐哈尔",
                "淮安",
                "张家港",
                "海门",
                "六安",
                "巢湖",
                "马鞍山",
                "永安",
                "宁德",
                "嘉禾",
                "荆门",
                "潜江",
                "大冶",
                "宜都",
                "佛山",
                "深圳",
                "潮州",
                "惠州",
                "汕尾",
                "东莞",
                "梧州",
                "柳州",
                "合山",
                "六盘水",
                "关岭"
        };
        String[] district_list ={
                "西夏",
                "永川",
                "秀英",
                "高港",
                "清城",
                "兴山",
                "锡山",
                "清河",
                "龙潭",
                "华龙",
                "海陵",
                "滨城",
                "东丽",
                "高坪",
                "沙湾",
                "平山",
                "城北",
                "海港",
                "沙市",
                "双滦",
                "长寿",
                "山亭",
                "南湖",
                "浔阳",
                "南长",
                "友好",
                "安次",
                "翔安",
                "沈河",
                "魏都",
                "西峰",
                "萧山",
                "金平",
                "沈北新",
                "孝南",
                "上街",
                "城东",
                "牧野",
                "大东",
                "白云",
                "花溪",
                "吉区",
                "新城",
                "怀柔",
                "六枝特",
                "涪城",
                "清浦",
                "南溪",
                "淄川",
                "高明",
                "东城",
                "崇文",
                "朝阳",
                "大兴",
                "房山",
                "门头沟",
                "黄浦",
                "徐汇",
                "静安",
                "普陀",
                "闵行",
                "和平",
                "蓟州",
                "永川",
                "长寿",
                "璧山",
                "合川",
                "梁平",
                "丰都",
                "江北",
        };
        String[] province_list = {
                "北京市",
                "上海市",
                "天津市",
                "重庆市",
                "内蒙古自治区",
                "山西省",
                "河北省",
                "吉林省",
                "江苏省",
                "辽宁省",
                "黑龙江省",
                "安徽省",
                "山东省",
                "浙江省",
                "江西省",
                "福建省",
                "湖南省",
                "湖北省",
                "河南省",
                "广东省",
                "广西壮族自治区",
                "贵州省",
                "海南省",
                "四川省",
                "云南省",
                "陕西省",
                "甘肃省",
                "宁夏回族自治区",
                "青海省",
                "新疆维吾尔自治区",
                "西藏自治区",
                "台湾省",
                "香港特别行政区",
                "澳门特别行政区",
        };

        String[] citySuffixes = {"市", "县"};
        String[] streetSuffixes = {"街", "路"};

        String provincesName = province_list[random.nextInt(province_list.length)];
        String citiesName = city_list[random.nextInt(city_list.length)];
        String citySuffixesName = citySuffixes[random.nextInt(citySuffixes.length)];
        String districtsName = district_list[random.nextInt(district_list.length)];
        String streetSuffixesName = streetSuffixes[random.nextInt(streetSuffixes.length)];
        int randomNumber = random.nextInt(500) + 1;

        return provincesName + citiesName + citySuffixesName + districtsName + streetSuffixesName + randomNumber + "号";

    }



    public static void main(String []args) {

        String name = name();
        System.out.println("姓名："+name);

        String nameToPinYin = nameToPinYin(name);
        System.out.println("拼音："+nameToPinYin);

        String phone = phone();
        System.out.println("手机号："+phone);

        String email = email(nameToPinYin);
        System.out.println("邮箱号："+email);

        String address = address();
        System.out.println("居住地址："+address);

        String idCard = idCard();
        System.out.println("身份证号："+idCard);

        String bankCard = bankCard();
        System.out.println("银行卡号："+bankCard);

        String postalCode = postalCode();
        System.out.println("邮编号："+postalCode);

        String unifiedSocialCreditCode = unifiedSocialCreditCode();
        System.out.println("统一社会信用代码："+unifiedSocialCreditCode);

        String organizationCode = organizationCode();
        System.out.println("组织机构代码："+organizationCode);


    }

}
