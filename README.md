0、备份本地测试config.json文件，替换config.json文件为发版原始文件
1、打包时，先确保当前命令行java版本（jdk8\jdk11+）
2、将对应版本的jdk.xml复制到pom.xml中（jdk8.xml、jdk11+.xml）
3、重新加载mvn配置(pom.xml)
4、运行mvn clean package

MacOs的mvn没环境生效：source ~/.bash_profile

发行版本不支持，查看当前java环境配置是否和pom.xml配置一致


最终混淆后的jar被复制在./outJar/文件夹下

调试时： idea程序实参后面跟上debug    jar运行需要后面跟上debug
webshell解密报错信息才会展示



感谢
https://github.com/alwaystest18/cdnChecker
https://github.com/pen4uin/java-memshell-generator