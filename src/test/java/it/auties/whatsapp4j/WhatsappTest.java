package it.auties.whatsapp4j;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.response.impl.UserInformationResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * A simple class to check that the library is working
 */
@Log
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class WhatsappTest implements WhatsappListener {
    private static @NotNull CountDownLatch latch;
    private static @NotNull WhatsappAPI whatsappAPI;
    private static @NotNull WhatsappContact contact;
    private static @NotNull WhatsappChat contactChat;
    private static @NotNull WhatsappChat group;

    @BeforeAll
    public static void init(){
        log.info("Initializing api to start testing...");
        whatsappAPI = new WhatsappAPI();
        latch = new CountDownLatch(3);
    }

    @Test
    @Order(1)
    public void registerListener() {
        log.info("Registering listener...");
        whatsappAPI.registerListener(this);
        log.info("Registered listener");
    }

    @Test
    @Order(2)
    public void testConnection() throws InterruptedException {
        log.info("Connecting...");
        whatsappAPI.connect();
        latch.await();
        log.info("Connected!");
    }

    @Test
    @Order(3)
    public void testChangeGlobalPresence() throws ExecutionException, InterruptedException {
        log.info("Changing global presence...");
        var response = whatsappAPI.changePresence(WhatsappContactStatus.AVAILABLE).get();
        Assertions.assertEquals(200, response.status(), "Cannot change individual presence, %s".formatted(response));
        log.info("Changed global presence...");
    }

    @Test
    @Order(4)
    public void testContactLookup() {
        log.info("Looking up a contact...");
        contact = whatsappAPI.manager().findContactByName("Jaaakko").orElseThrow(() -> new AssertionFailedError("Cannot lookup contact"));
        contactChat = whatsappAPI.manager().findChatByJid(contact.jid()).orElseThrow();
        log.info("Looked up: %s".formatted(contact));
    }

    @Test
    @Order(5)
    public void testChangeIndividualPresence() throws ExecutionException, InterruptedException {
        log.info("Changing individual presence...");
        var response = whatsappAPI.changePresence(contactChat, WhatsappContactStatus.AVAILABLE).get();
        Assertions.assertEquals(200, response.status(), "Cannot change individual presence, %s".formatted(response));
        log.info("Changed individual presence...");
    }

    @Test
    @Order(6)
    public void testUserPresenceSubscription() throws ExecutionException, InterruptedException {
        log.info("Subscribing to user presence...");
        var userPresenceResponse = whatsappAPI.subscribeToUserPresence(contact).get();
        Assertions.assertEquals(200, userPresenceResponse.status(), "Cannot subscribe to user presence: %s".formatted(userPresenceResponse));
        log.info("Subscribed to user presence: %s".formatted(userPresenceResponse));
    }

    @Test
    @Order(7)
    public void testPictureQuery() throws IOException, ExecutionException, InterruptedException {
        log.info("Loading picture...");
        var picResponse = whatsappAPI.queryChatPicture(contactChat).get();
        switch (picResponse.status()){
            case 200 -> {
                var file = Files.createTempFile(UUID.randomUUID().toString(), ".jpg");
                Files.write(file, new URL(picResponse.url()).openStream().readAllBytes(), StandardOpenOption.CREATE);
                log.info("Loaded picture: %s".formatted(file.toString()));
            }
            case 404 -> log.info("The contact doesn't have a pic");
            default -> throw new AssertionFailedError("Cannot query pic: %s".formatted(picResponse));
        }
    }

    @Test
    @Order(8)
    public void testStatusQuery() throws ExecutionException, InterruptedException {
        log.info("Querying %s's status...".formatted(contact.bestName()));
        whatsappAPI.queryUserStatus(contact)
                .get()
                .status()
                .ifPresentOrElse(status -> log.info("Queried %s".formatted(status)), () -> log.info("%s doesn't have a status".formatted(contact.bestName())));
    }

    @Test
    @Order(9)
    public void testFavouriteMessagesQuery() throws ExecutionException, InterruptedException {
        log.info("Loading 20 favourite messages...");
        var favouriteMessagesResponse = whatsappAPI.queryFavouriteMessagesInChat(contactChat, 20).get();
        Assertions.assertTrue(favouriteMessagesResponse.data().isPresent(), "Cannot query favourite messages");
        log.info("Loaded favourite messages: %s".formatted(favouriteMessagesResponse.data().get()));
    }

    @Test
    @Order(10)
    public void testGroupsInCommonQuery() throws ExecutionException, InterruptedException {
        log.info("Loading groups in common...");
        var groupsInCommonResponse = whatsappAPI.queryGroupsInCommon(contact).get();
        Assertions.assertEquals(200, groupsInCommonResponse.status(), "Cannot query groups in common: %s".formatted(groupsInCommonResponse));
        log.info("Loaded groups in common: %s".formatted(groupsInCommonResponse.groups()));
    }

    @Test
    @Order(11)
    public void testGroupCreation() throws InterruptedException, ExecutionException {
        log.info("Creating group...");
        group = whatsappAPI.createGroup(BinaryArray.random(6).toHex(), contact).get();
        log.info("Created group: %s".formatted(group));
    }

    @Test
    @Order(12)
    public void testChangeGroupName() throws InterruptedException, ExecutionException {
        log.info("Changing group name...");
        var changeGroupResponse = whatsappAPI.changeGroupName(group, BinaryArray.random(6).toHex()).get();
        Assertions.assertEquals(200, changeGroupResponse.status(), "Cannot change group name: %s".formatted(changeGroupResponse));
        log.info("Changed group name");
    }

    @RepeatedTest(2)
    @Order(13)
    public void testChangeGroupDescription() throws InterruptedException, ExecutionException {
        log.info("Changing group description...");
        var changeGroupResponse = whatsappAPI.changeGroupDescription(group, BinaryArray.random(12).toHex()).get();
        Assertions.assertEquals(200, changeGroupResponse.status(), "Cannot change group description: %s".formatted(changeGroupResponse));
        log.info("Changed group description");
    }

    @Test
    @Order(14)
    public void testRemoveGroupParticipant() throws InterruptedException, ExecutionException {
        log.info("Removing %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.remove(group, contact).get();
        Assertions.assertEquals(207, changeGroupResponse.status(), "Cannot remove %s: %s".formatted(contact.bestName(), changeGroupResponse));
        log.info("Removed %s".formatted(contact.bestName()));
    }

    @Test
    @Order(15)
    public void testAddGroupParticipant() throws InterruptedException, ExecutionException {
        log.info("Adding %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.add(group, contact).get();
        Assertions.assertEquals(207, changeGroupResponse.status(), "Cannot add %s: %s".formatted(contact.bestName(), changeGroupResponse));
        log.info("Added %s".formatted(contact.bestName()));
    }

    @Test
    @Order(16)
    public void testPromotion() throws InterruptedException, ExecutionException {
        log.info("Promoting %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.promote(group, contact).get();
        Assertions.assertEquals(207, changeGroupResponse.status(), "Cannot promote %s: %s".formatted(contact.bestName(), changeGroupResponse));
        log.info("Promoted %s".formatted(contact.bestName()));
    }

    @Test
    @Order(17)
    public void testDemotion() throws InterruptedException, ExecutionException {
        log.info("Demoting %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.demote(group, contact).get();
        Assertions.assertEquals(207, changeGroupResponse.status(), "Cannot demote %s: %s".formatted(contact.bestName(), changeGroupResponse));
        log.info("Demoted %s".formatted(contact.bestName()));
    }

    @Test
    @Order(18)
    public void testChangeAllGroupSettings() throws InterruptedException, ExecutionException {
        for (var setting : WhatsappGroupSetting.values()) {
            for (var policy : WhatsappGroupPolicy.values()) {
                log.info("Changing setting %s to %s...".formatted(setting.name(), policy.name()));
                var changeGroupResponse = whatsappAPI.changeGroupSetting(group, setting, policy).get();
                Assertions.assertEquals(200, changeGroupResponse.status(), "Cannot change setting %s to %s, %s".formatted(setting.name(), policy.name(), changeGroupResponse));
                log.info("Changed setting %s to %s...".formatted(setting.name(), policy.name()));
            }
        }
    }

    @Test
    @Order(19)
    public void testChangeAndRemoveGroupPicture() {
        log.warning("Not implemented");
    }

    @Test
    @Order(20)
    public void testGroupQuery() throws InterruptedException, ExecutionException {
        log.info("Querying group %s...".formatted(group.jid()));
        whatsappAPI.queryChat(group.jid()).get().data().orElseThrow(() -> new AssertionFailedError("Cannot query missing chat"));
        log.info("Queried group");
    }

    @Test
    @Order(21)
    public void testLoadConversation() throws InterruptedException, ExecutionException {
        log.info("Loading conversation(%s)...".formatted(group.messages().size()));
        whatsappAPI.loadConversation(group).get();
        log.info("Loaded conversation(%s)!".formatted(group.messages().size()));
    }

    @Test
    @Order(22)
    public void testMute() throws ExecutionException, InterruptedException {
        log.info("Muting chat...");
        var muteResponse = whatsappAPI.mute(group, ZonedDateTime.now().plusDays(14)).get();
        Assertions.assertTrue(muteResponse, "Cannot mute chat: %s".formatted(muteResponse));
        log.info("Muted chat");
    }

    @Test
    @Order(23)
    public void testUnmute() throws ExecutionException, InterruptedException {
        log.info("Unmuting chat...");
        var unmuteResponse = whatsappAPI.unmute(group).get();
        Assertions.assertTrue(unmuteResponse, "Cannot unmute chat: %s".formatted(unmuteResponse));
        log.info("Unmuted chat");
    }

    @Test
    @Order(24)
    public void testArchive() throws ExecutionException, InterruptedException {
        log.info("Archiving chat...");
        var archiveResponse = whatsappAPI.archive(group).get();
        Assertions.assertTrue(archiveResponse, "Cannot archive chat: %s".formatted(archiveResponse));
        log.info("Archived chat");
    }

    @Test
    @Order(25)
    public void testUnarchive() throws ExecutionException, InterruptedException {
        log.info("Unarchiving chat...");
        var unarchiveResponse = whatsappAPI.unarchive(group).get();
        Assertions.assertTrue(unarchiveResponse, "Cannot unarchive chat: %s".formatted(unarchiveResponse));
        log.info("Unarchived chat");
    }

    @Test
    @Order(26)
    public void testPin() throws ExecutionException, InterruptedException {
        if(whatsappAPI.manager().chats().stream().filter(WhatsappChat::isPinned).count() >= 3){
            log.info("Skipping chat pinning as there are already three chats pinned...");
            return;
        }

        log.info("Pinning chat...");
        var pinResponse = whatsappAPI.pin(group).get();
        Assertions.assertTrue(pinResponse, "Cannot pin chat: %s".formatted(pinResponse));
        log.info("Pinned chat");
    }

    @Test
    @Order(27)
    public void testUnpin() throws ExecutionException, InterruptedException {
        if(whatsappAPI.manager().chats().stream().filter(WhatsappChat::isPinned).count() >= 3){
            log.info("Skipping chat unpinning as there are already three chats pinned...");
            return;
        }

        log.info("Unpinning chat...");
        var unpinResponse = whatsappAPI.unpin(group).get();
        Assertions.assertTrue(unpinResponse, "Cannot unpin chat: %s".formatted(unpinResponse));
        log.info("Unpinned chat");
    }

    @Test
    @Order(28)
    public void testTextMessage() throws ExecutionException, InterruptedException {
        log.info("Sending text...");
        var textResponse = whatsappAPI.sendMessage(WhatsappTextMessage.newTextMessage(group, "Testing%nData: %s%n_Godo_".formatted(Instant.now()))).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send text: %s".formatted(textResponse));
        log.info("Sent text");
    }

    @Test
    @Order(29)
    public void testMediaMessage() throws ExecutionException, InterruptedException, IOException {
        log.info("Sending media...");
        var message = WhatsappMediaMessage.newMediaMessage()
                .chat(group)
                .media(new URL("https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg").openStream().readAllBytes())
                .caption("Media test")
                .type(WhatsappMediaMessageType.IMAGE)
                .create();
        var textResponse = whatsappAPI.sendMessage(message).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send media: %s".formatted(textResponse));
        log.info("Sent media");
    }

    @Test
    @Order(30)
    public void testEnableEphemeralMessages() throws ExecutionException, InterruptedException {
        log.info("Enabling ephemeral messages...");
        var ephemeralResponse = whatsappAPI.enableEphemeralMessages(group).get();
        Assertions.assertEquals(200, ephemeralResponse.status(), "Cannot enable ephemeral messages: %s".formatted(ephemeralResponse));
        log.info("Enabled ephemeral messages");
    }

    @Test
    @Order(31)
    public void testDisableEphemeralMessages() throws ExecutionException, InterruptedException {
        log.info("Disabling ephemeral messages...");
        var ephemeralResponse = whatsappAPI.disableEphemeralMessages(group).get();
        Assertions.assertEquals(200, ephemeralResponse.status(), "Cannot disable ephemeral messages: %s".formatted(ephemeralResponse));
        log.info("Disabled ephemeral messages");
    }

    @Test
    @Order(32)
    public void testLeave() throws ExecutionException, InterruptedException {
        log.info("Leaving group...");
        var ephemeralResponse = whatsappAPI.leave(group).get();
        Assertions.assertEquals(200, ephemeralResponse.status(), "Cannot leave group: %s".formatted(ephemeralResponse));
        log.info("Left group");
    }

    @Override
    public void onLoggedIn(UserInformationResponse info) {
        log.info("Logged in!");
        latch.countDown();
    }

    @Override
    public void onChatsReceived() {
        log.info("Got chats!");
        latch.countDown();
    }

    @Override
    public void onContactsReceived() {
        log.info("Got contacts!");
        latch.countDown();
    }
}