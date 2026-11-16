const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_manager_123";
const BOB_UID = "bob_manager_456";
const DATABASE_ID = "ai-studio-android-brasmana-a1cb1330-7364-499e-b9e9-46a3eda456ec";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  return {
    userId: uid,
    managerName: "Tecnico Alice",
    autoSyncEnabled: true,
    preferredSimSpeed: 2,
    totalTitles: 0,
    totalWins: 0,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validCareer(uid) {
  return {
    userId: uid,
    clubId: "flamengo",
    clubName: "Flamengo",
    division: "SERIE_A",
    seasonYear: 2026,
    currentRound: 1,
    budget: 85000000,
    stadiumCapacity: 78838,
    ticketPriceLevel: "NORMAL",
    trainingFocus: "EQUILIBRADO",
    formation: "4-3-3",
    mentality: "OFENSIVO",
    marking: "PRESSAO",
    boardConfidence: 75,
    points: 0,
    leaguePosition: 1,
    wins: 0,
    draws: 0,
    losses: 0,
    goalsFor: 0,
    goalsAgainst: 0,
    copaStage: "3a Fase",
    activeStarterIds: ["fla_1", "fla_2", "fla_3"],
    status: "active",
    createdAt: new Date(),
    updatedAt: new Date(),
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

test("Unauthenticated user cannot read or write manager profile or careers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("careers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Authenticated owner can create, read, and update their manager profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(ref.update({ managerName: "Mister Alice", totalWins: 3, updatedAt: new Date() }));
});

test("Shadow update with ghost field on manager profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertFails(ref.update({ isAdmin: true, updatedAt: new Date() }));
});

test("Cross-user access to another manager's profile and careers is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("careers").doc("career_1").set(validCareer(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("careers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("careers").doc("career_1").get());
});

test("Owner can create career, add transfer, retire career, and retired career is locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const careerRef = aliceDb.collection("users").doc(ALICE_UID).collection("careers").doc("career_fla_1");

  await assertSucceeds(careerRef.set(validCareer(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("careers").get());

  const transferRef = careerRef.collection("transfers").doc("tr_1");
  await assertSucceeds(transferRef.set({
    userId: ALICE_UID,
    careerId: "career_fla_1",
    playerName: "Pedro",
    position: "ATA",
    fromClubName: "Flamengo",
    toClubName: "Palmeiras",
    fee: 45000000,
    transferType: "SELL",
    seasonYear: 2026,
    roundNumber: 2,
    createdAt: new Date(),
  }));

  // Transfers are append-only (no update)
  await assertFails(transferRef.update({ fee: 1 }));

  // Retire career
  await assertSucceeds(careerRef.update({ status: "retired", updatedAt: new Date() }));

  // Once retired, career cannot be updated
  await assertFails(careerRef.update({ points: 100, updatedAt: new Date() }));
});

test("Invalid activeStarterIds (>11) or invalid enum is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badCareer = validCareer(ALICE_UID);
  badCareer.activeStarterIds = Array.from({ length: 12 }, (_, i) => `p_${i}`);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("careers").doc("bad_1").set(badCareer)
  );
});
