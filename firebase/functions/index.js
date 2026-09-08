const { onRequest } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getDatabase } = require("firebase-admin/database");
const crypto = require("crypto");

initializeApp();

function token(bytes) {
  return crypto.randomBytes(bytes).toString("hex");
}
function hash(value) {
  return crypto.createHash("sha256").update(value, "utf8").digest("hex");
}
function safeEqualHex(a, b) {
  try {
    const aa = Buffer.from(a, "hex");
    const bb = Buffer.from(b, "hex");
    return aa.length === bb.length && crypto.timingSafeEqual(aa, bb);
  } catch (_) {
    return false;
  }
}
async function requireFirebaseUser(req) {
  const value = req.header("authorization") || "";
  if (!value.startsWith("Bearer ")) throw new Error("missing_auth");
  return getAuth().verifyIdToken(value.substring(7));
}

exports.provisionDevice = onRequest(async (req, res) => {
  if (req.method !== "POST") return res.status(405).json({ error: "method_not_allowed" });
  try {
    const user = await requireFirebaseUser(req);
    const deviceId = String(req.body?.deviceId || "").trim();
    if (!/^ESP-[A-Z0-9-]{4,32}$/.test(deviceId)) return res.status(400).json({ error: "invalid_device_id" });

    const accessKey = `AK_${token(12)}`;
    const secretKey = `SK_${token(24)}`;
    const db = getDatabase();
    await db.ref(`device_api/${deviceId}`).set({
      ownerUid: user.uid,
      accessKeyHash: hash(accessKey),
      secretKeyHash: hash(secretKey),
      enabled: true,
      createdAt: Date.now()
    });
    await db.ref(`devices/${deviceId}`).update({
      ownerUid: user.uid,
      enabled: true,
      updatedAt: Date.now()
    });
    return res.json({ deviceId, accessKey, secretKey });
  } catch (e) {
    return res.status(401).json({ error: "unauthorized" });
  }
});

exports.deviceState = onRequest(async (req, res) => {
  if (req.method !== "GET") return res.status(405).json({ error: "method_not_allowed" });
  const deviceId = String(req.header("x-device-id") || "");
  const accessKey = String(req.header("x-access-key") || "");
  const secretKey = String(req.header("x-secret-key") || "");
  if (!deviceId || !accessKey || !secretKey) return res.status(401).json({ error: "missing_credentials" });

  const db = getDatabase();
  const authSnap = await db.ref(`device_api/${deviceId}`).get();
  if (!authSnap.exists()) return res.status(401).json({ error: "invalid_device" });
  const record = authSnap.val();
  if (!record.enabled || !safeEqualHex(hash(accessKey), record.accessKeyHash) || !safeEqualHex(hash(secretKey), record.secretKeyHash)) {
    return res.status(401).json({ error: "invalid_credentials" });
  }

  const stateSnap = await db.ref(`live/${record.ownerUid}/${deviceId}`).get();
  res.set("Cache-Control", "no-store");
  return res.json({ deviceId, state: stateSnap.val() || null, serverTime: Date.now() });
});
