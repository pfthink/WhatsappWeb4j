package it.auties.github;

import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.model.Node;
import it.auties.whatsapp.socket.WhatsappSocket;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import static java.util.Map.of;

@Log
public class WhatsappAPITest {
    @Test
    public void login() {
        var whatsapp = new WhatsappSocket(WhatsappConfiguration.defaultOptions(), new WhatsappStore(), new WhatsappKeys());
        whatsapp.connect();
    }
}
