const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid";
const BOB_UID = "bob_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read khatas", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("khatas").get());
});

test("Unauthenticated user: can read user_ids for availability check but cannot create", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  // Can read existing or non-existing username doc to check availability
  await assertSucceeds(unauthDb.collection("user_ids").doc("check_username").get());
  // Cannot create username without auth
  await assertFails(
    unauthDb.collection("user_ids").doc("check_username").set({
      customUserId: "check_username",
      uid: "fake_uid",
    })
  );
});

test("Username uniqueness: Alice registers username, Bob cannot overwrite or steal it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice successfully registers anshul47957
  await assertSucceeds(
    aliceDb.collection("user_ids").doc("anshul47957").set({
      customUserId: "anshul47957",
      uid: ALICE_UID,
    })
  );

  // Bob attempts to overwrite or claim anshul47957 -> MUST FAIL
  await assertFails(
    bobDb.collection("user_ids").doc("anshul47957").set({
      customUserId: "anshul47957",
      uid: BOB_UID,
    })
  );

  // Bob attempts to update anshul47957 -> MUST FAIL
  await assertFails(
    bobDb.collection("user_ids").doc("anshul47957").update({
      uid: BOB_UID,
    })
  );
});

test("Alice creates Khata: succeeds and Bob cannot access it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("khatas").doc("khata_1").set({
      id: "khata_1",
      name: "Khata 1",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID],
      totalPeople: 0,
      totalLena: 0,
      totalDena: 0,
      netBalance: 0,
    })
  );

  // Bob cannot read Khata 1
  await assertFails(bobDb.collection("khatas").doc("khata_1").get());
});

test("Khata sharing test: User A shares Khata 1 with User B, but User B cannot read Khata 2 or newly created Khata 4", async () => {
  // Seed Alice's khatas
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    // Khata 1 shared with Bob
    await adminDb.collection("khatas").doc("khata_1").set({
      id: "khata_1",
      name: "Khata 1",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      totalPeople: 1,
      totalLena: 100,
      totalDena: 0,
      netBalance: 100,
    });
    // Khata 1 subcollection person
    await adminDb.collection("khatas").doc("khata_1").collection("people").doc("p1").set({
      id: "p1",
      khataId: "khata_1",
      name: "Ramesh",
    });

    // Khata 2 private to Alice
    await adminDb.collection("khatas").doc("khata_2").set({
      id: "khata_2",
      name: "Khata 2",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID],
      totalPeople: 0,
      totalLena: 0,
      totalDena: 0,
      netBalance: 0,
    });
    await adminDb.collection("khatas").doc("khata_2").collection("people").doc("p2").set({
      id: "p2",
      khataId: "khata_2",
      name: "Suresh",
    });

    // Khata 3 private to Alice
    await adminDb.collection("khatas").doc("khata_3").set({
      id: "khata_3",
      name: "Khata 3",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID],
      totalPeople: 0,
      totalLena: 0,
      totalDena: 0,
      netBalance: 0,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Bob CAN read Khata 1
  await assertSucceeds(bobDb.collection("khatas").doc("khata_1").get());
  await assertSucceeds(bobDb.collection("khatas").doc("khata_1").collection("people").doc("p1").get());

  // Bob CANNOT read Khata 2 or Khata 3
  await assertFails(bobDb.collection("khatas").doc("khata_2").get());
  await assertFails(bobDb.collection("khatas").doc("khata_2").collection("people").doc("p2").get());
  await assertFails(bobDb.collection("khatas").doc("khata_3").get());

  // Alice creates Khata 4
  await assertSucceeds(
    aliceDb.collection("khatas").doc("khata_4").set({
      id: "khata_4",
      name: "Khata 4",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID],
    })
  );

  // Bob CANNOT read Khata 4
  await assertFails(bobDb.collection("khatas").doc("khata_4").get());

  // Alice stops sharing Khata 1 with Bob
  await assertSucceeds(
    aliceDb.collection("khatas").doc("khata_1").update({
      memberUids: [ALICE_UID],
    })
  );

  // Now Bob IMMEDIATELY loses access to Khata 1!
  await assertFails(bobDb.collection("khatas").doc("khata_1").get());
  await assertFails(bobDb.collection("khatas").doc("khata_1").collection("people").doc("p1").get());
});

test("Trash and Recover permissions: Only owner can soft-delete and recover", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("khatas").doc("khata_shared").set({
      id: "khata_shared",
      name: "Shared Store",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      isDeleted: false,
      totalPeople: 0,
      totalLena: 0,
      totalDena: 0,
      netBalance: 0,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Bob (member) tries to soft-delete -> MUST FAIL
  await assertFails(
    bobDb.collection("khatas").doc("khata_shared").update({
      isDeleted: true,
      deletedBy: BOB_UID,
    })
  );

  // Alice (owner) soft-deletes -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("khatas").doc("khata_shared").update({
      isDeleted: true,
      deletedBy: ALICE_UID,
    })
  );

  // Bob (member) tries to recover -> MUST FAIL
  await assertFails(
    bobDb.collection("khatas").doc("khata_shared").update({
      isDeleted: false,
      deletedBy: null,
    })
  );

  // Alice (owner) recovers -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("khatas").doc("khata_shared").update({
      isDeleted: false,
      deletedBy: null,
    })
  );

  // Bob (member) cannot permanently delete
  await assertFails(bobDb.collection("khatas").doc("khata_shared").delete());

  // Alice (owner) can permanently delete
  await assertSucceeds(aliceDb.collection("khatas").doc("khata_shared").delete());
});

test("Partner leave permission: Bob can leave shared khata, but cannot remove Alice", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("khatas").doc("khata_team").set({
      id: "khata_team",
      name: "Team Khata",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      isDeleted: false,
      totalPeople: 0,
      totalLena: 0,
      totalDena: 0,
      netBalance: 0,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Bob tries to remove Alice (owner) -> MUST FAIL
  await assertFails(
    bobDb.collection("khatas").doc("khata_team").update({
      memberUids: [BOB_UID],
    })
  );

  // Bob leaves by removing only himself -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("khatas").doc("khata_team").update({
      memberUids: [ALICE_UID],
    })
  );

  // After leaving, Bob can no longer read
  await assertFails(bobDb.collection("khatas").doc("khata_team").get());
});

