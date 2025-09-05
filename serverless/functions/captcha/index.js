'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function readJson(file) {
  try {
    if (fs.existsSync(file)) {
      const raw = fs.readFileSync(file, 'utf8');
      return JSON.parse(raw || '{}');
    }
  } catch (_) {}
  return {};
}

function writeJson(file, data) {
  try {
    fs.writeFileSync(file, JSON.stringify(data, null, 2), 'utf8');
    return true;
  } catch (_) {
    return false;
  }
}

module.exports = async (ctx) => {
  // 生成一个简单算术题（支持 + 或 -，结果为非负整数）
  const a = randomInt(1, 9);
  const b = randomInt(1, 9);
  const usePlus = Math.random() < 0.6 || a < b; // 大多数时候使用加法，确保结果非负
  const question = usePlus ? `${a} + ${b} = ?` : `${a} - ${b} = ?`;
  const answer = String(usePlus ? (a + b) : (a - b));

  const captchaId = crypto.randomBytes(12).toString('hex');
  const expireAt = Date.now() + 2 * 60 * 1000; // 2 分钟有效
  const answerHash = crypto.createHash('sha256').update(answer).digest('hex');

  // 优先尝试 EMAS Serverless 云数据库
  try {
    const coll = ctx.mpserverless.db.collection('captchas');
    // 清理过期
    try { await coll.deleteMany({ expireAt: { $lt: Date.now() } }); } catch(_) {}
    await coll.insertOne({ id: captchaId, answerHash, expireAt });
    return { success: true, captchaId, question, ttlMs: 120000 };
  } catch (_) {}

  // 回退到 /tmp 文件
  const file = path.join('/tmp', 'captchas.json');
  const db = readJson(file);
  db.items = Array.isArray(db.items) ? db.items : [];
  // 清理过期
  const now = Date.now();
  db.items = db.items.filter(it => (it && typeof it.expireAt === 'number' && it.expireAt > now));
  db.items.push({ id: captchaId, answerHash, expireAt });
  writeJson(file, db);

  return { success: true, captchaId, question, ttlMs: 120000 };
};


