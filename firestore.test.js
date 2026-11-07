const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  await testEnv.cleanup();
});

test("Unauthenticated user cannot read or write hubs", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("hubs").doc("hub1").get());
  await assertFails(
    unauthedDb.collection("hubs").doc("hub1").set({
      name: "Family Hub",
      ownerId: "user1",
      adminUids: ["user1"],
      memberUids: ["user1"],
      whitelistedEmails: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated owner can create a hub", async () => {
  const aliceDb = testEnv.authenticatedContext("alice", { email: "alice@example.com" }).firestore();
  await assertSucceeds(
    aliceDb.collection("hubs").doc("hub_alice").set({
      name: "Alice Galaxy Hub",
      ownerId: "alice",
      adminUids: ["alice"],
      memberUids: ["alice"],
      whitelistedEmails: ["bob@example.com"],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Member can create grocery items", async () => {
  // First setup hub with admin context
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("hubs").doc("hub_alice").set({
      name: "Alice Galaxy Hub",
      ownerId: "alice",
      adminUids: ["alice"],
      memberUids: ["alice", "bob"],
      whitelistedEmails: ["bob@example.com"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext("bob", { email: "bob@example.com" }).firestore();
  await assertSucceeds(
    bobDb.collection("hubs").doc("hub_alice").collection("groceries").doc("milk").set({
      hubId: "hub_alice",
      memberUids: ["alice", "bob"],
      title: "Oat Milk",
      category: "Dairy",
      quantity: 2,
      isCompleted: false,
      addedByUid: "bob",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Stranger cannot write grocery
  const eveDb = testEnv.authenticatedContext("eve", { email: "eve@example.com" }).firestore();
  await assertFails(
    eveDb.collection("hubs").doc("hub_alice").collection("groceries").doc("chips").set({
      hubId: "hub_alice",
      memberUids: ["alice", "bob"],
      title: "Chips",
      category: "Snacks",
      quantity: 1,
      isCompleted: false,
      addedByUid: "eve",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
