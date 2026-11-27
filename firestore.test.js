const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, now = new Date(Date.now() - 10000)) {
  return {
    userId: uid,
    displayName: "Little Explorer",
    totalStars: 5,
    gamesPlayed: 2,
    balloonBestScore: 90,
    memoryBestScore: 120,
    mathBestScore: 80,
    xylophoneBestScore: 60,
    unlockedStickers: ["lion_king"],
    createdAt: now,
    updatedAt: now,
  };
}

function validSession(uid, now = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    gameType: "balloon_pop",
    score: 90,
    starsEarned: 3,
    difficulty: "easy",
    createdAt: now,
  };
}

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

test("Unauthenticated user cannot read or write profiles or game sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("gameSessions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gameSessions")
      .doc("sess_1")
      .set(validSession(ALICE_UID))
  );
});

test("Owner can create, read, and partially update PlayerProfile while preserving immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());

  // Valid partial update matching Kotlin repository
  await assertSucceeds(
    profileRef.update({
      totalStars: 8,
      gamesPlayed: 3,
      balloonBestScore: 110,
      unlockedStickers: ["lion_king", "space_panda"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow/ghost field injection
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId 'id' leakage on create
  await assertFails(
    aliceDb.collection("users").doc("alice_alt").set({
      id: "alice_alt",
      ...validProfile("alice_alt"),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's profile and sessions", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID));
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gameSessions")
      .doc("sess_1")
      .set(validSession(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      totalStars: 999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("gameSessions").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gameSessions")
      .doc("sess_2")
      .set(validSession(BOB_UID))
  );
});

test("Owner can list and create valid GameSessions; invalid enums, future timestamps, and updates fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("gameSessions");

  // Empty subcollection list must succeed
  await assertSucceeds(sessionsCol.orderBy("createdAt", "desc").limit(10).get());

  // Valid game session create
  await assertSucceeds(sessionsCol.doc("sess_1").set(validSession(ALICE_UID)));

  // Immutable: update must fail
  await assertFails(sessionsCol.doc("sess_1").update({ score: 999 }));

  // Invalid gameType enum must fail
  await assertFails(
    sessionsCol.doc("sess_bad_enum").set({
      ...validSession(ALICE_UID),
      gameType: "unknown_game",
    })
  );

  // Future timestamp must fail
  await assertFails(
    sessionsCol.doc("sess_future").set({
      ...validSession(ALICE_UID),
      createdAt: new Date(Date.now() + 60000),
    })
  );
});
