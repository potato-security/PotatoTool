package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker;

public class JavassistClassLoader extends ClassLoader {
    public JavassistClassLoader() {
        super(Thread.currentThread().getContextClassLoader());
    }
}
