const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const { test, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');

const PROJECT_ID = process.env.GCP_PROJECT || 'demo-no-project';
const ALICE_UID = 'alice_123';
const BOB_UID = 'bob_456';

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085'
).split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

let testEnv;

before(async () => {
  const rules = fs.readFileSync('./firestore.rules', 'utf8');
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

test('unauthenticated users cannot read or write journals, properties, or rows', async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/journals`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/journals/j-1`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/journals/j-1`).set({
      userId: ALICE_UID,
      name: 'Budget',
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/properties`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/entry_rows`).get());
});

test('owner can create, read, list, update, soft-delete, restore, and delete journals', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/journals/journal-1`);
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      name: 'Dépenses 2026',
      clientCreatedAt: 1700000000000,
      clientUpdatedAt: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(ref.get());
  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/journals`)
      .where('clientUpdatedAt', '>', 0)
      .orderBy('clientUpdatedAt', 'asc')
      .get()
  );

  await assertSucceeds(
    ref.update({
      name: 'Dépenses Famille',
      clientUpdatedAt: 1700000005000,
      updatedAt: new Date(Date.now() - 1000),
      deletedAt: 1700000005000,
    })
  );

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      name: 'Dépenses Famille',
      clientCreatedAt: 1700000000000,
      clientUpdatedAt: 1700000009000,
      createdAt: ts,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(ref.delete());
});

test('owner can create and update properties with native options list and optional format', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/properties/prop-1`);
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      journalId: 'journal-1',
      name: 'Montant',
      kind: 'number',
      format: 'dollar',
      options: [],
      sortOrder: 0,
      clientUpdatedAt: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    ref.update({
      kind: 'select',
      options: ['Maison', 'Transport'],
      sortOrder: 1,
      clientUpdatedAt: 1700000002000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/properties`)
      .where('clientUpdatedAt', '>', 0)
      .orderBy('clientUpdatedAt', 'asc')
      .get()
  );
});

test('owner can create and update entry_rows with native values map', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/entry_rows/row-1`);
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      journalId: 'journal-1',
      values: {
        'prop-1': '42.5',
        'prop-2': 'Courses',
        mydata_reserved_notes: 'Note de test',
        mydata_reserved_images: [{ caption: 'Ticket' }],
      },
      clientCreatedAt: 1700000000000,
      clientUpdatedAt: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    ref.update({
      values: {
        'prop-1': '55.0',
        'prop-2': 'Courses bio',
      },
      clientUpdatedAt: 1700000003000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/entry_rows`)
      .where('clientUpdatedAt', '>', 0)
      .orderBy('clientUpdatedAt', 'asc')
      .get()
  );
});

test('cross-user read, list, create, update, and delete are strictly denied', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/journals/j-priv`).set({
      userId: ALICE_UID,
      name: 'Secret',
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.doc(`users/${ALICE_UID}/journals/j-priv`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/journals`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/journals/j-priv`).update({
      name: 'Hacked',
      clientUpdatedAt: 2000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/journals/j-priv`).delete());
  await assertFails(
    bobDb.doc(`users/${BOB_UID}/journals/j-spoof`).set({
      userId: ALICE_UID,
      name: 'Spoofed Owner',
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test('schema rejects extra ghost fields, @DocumentId id field, immutable mutations, invalid enums, and oversized strings', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/journals/j-bad-id`).set({
      id: 'j-bad-id',
      userId: ALICE_UID,
      name: 'Bad',
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/journals/j-long`).set({
      userId: ALICE_UID,
      name: 'a'.repeat(201),
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/properties/p-bad`).set({
      userId: ALICE_UID,
      journalId: 'j-1',
      name: 'Bad kind',
      kind: 'boolean',
      options: [],
      sortOrder: 0,
      clientUpdatedAt: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/properties/p-bad-fmt`).set({
      userId: ALICE_UID,
      journalId: 'j-1',
      name: 'Bad fmt',
      kind: 'number',
      format: 'euro',
      options: [],
      sortOrder: 0,
      clientUpdatedAt: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const rowRef = aliceDb.doc(`users/${ALICE_UID}/entry_rows/r-imm`);
  await assertSucceeds(
    rowRef.set({
      userId: ALICE_UID,
      journalId: 'j-1',
      values: { a: '1' },
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    rowRef.update({
      journalId: 'j-2',
      clientUpdatedAt: 2000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    rowRef.update({
      createdAt: new Date(Date.now() - 500),
      clientUpdatedAt: 2000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/journals/j-future`).set({
      userId: ALICE_UID,
      name: 'Future',
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: new Date(Date.now() + 600000),
      updatedAt: ts,
    })
  );
});
