mvn clean package -f pom-jdk11+.xml 打包
mvn clean package -f jdk8.xml 打包

打包失败请先运行mvn的插件-assembly:single看报错


mvn没环境生效：source ~/.bash_profile

发行版本不支持，查看当前java环境配置是否和pom.xml配置一致


最终混淆后的jar被复制在./outJar/文件夹下