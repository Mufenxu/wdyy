'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// 简易“云数据库”示例：将数据写入函数运行环境的 /tmp/users.json。
// 注意：/tmp 为临时存储，适合 Demo；生产请替换为正式云数据库。

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
  } catch (e) {
    return false;
  }
}

module.exports = async (ctx) => {
  let username = '';
  let password = '';
  let captchaId = '';
  let captchaText = '';

  try {
    const rawBody = ctx && ctx.request && ctx.request.body;
    if (rawBody) {
      if (typeof rawBody === 'string') {
        try {
          const parsed = JSON.parse(rawBody);
          username = parsed.username||''; password = parsed.password||'';
          captchaId = parsed.captchaId||''; captchaText = parsed.captchaText||'';
        } catch(_){ }
      } else if (typeof rawBody === 'object') {
        username = rawBody.username || '';
        password = rawBody.password || '';
        captchaId = rawBody.captchaId || '';
        captchaText = rawBody.captchaText || '';
      }
    }
    if (!username || !password) {
      const qs = (ctx && ctx.args && ctx.args.queryStringParameters) || (ctx && ctx.request && ctx.request.queries) || {};
      username = qs.username || username;
      password = qs.password || password;
      captchaId = qs.captchaId || captchaId;
      captchaText = qs.captchaText || captchaText;
    }
  } catch (_) {}

  if (!username || !password) return { success: false, message: '用户名或密码为空' };

  // 校验验证码
  try {
    const crypto = require('crypto');
    const ansHash = crypto.createHash('sha256').update(String(captchaText||'')).digest('hex');
    let passed = false;
    try {
      const coll = ctx.mpserverless.db.collection('captchas');
      const found = await coll.findOne({ id: captchaId });
      const rec = (found && (found.data || found.result?.data || found.result)) ? (found.data || found.result?.data || found.result) : null;
      if (rec && rec.expireAt > Date.now() && rec.answerHash === ansHash) {
        passed = true;
      }
      try { await coll.deleteOne({ id: captchaId }); } catch(_) {}
    } catch (_){
      const fs = require('fs');
      const path = require('path');
      const file = path.join('/tmp', 'captchas.json');
      if (fs.existsSync(file)) {
        try {
          const raw = fs.readFileSync(file, 'utf8');
          const db = JSON.parse(raw || '{}');
          const items = Array.isArray(db.items) ? db.items : [];
          const now = Date.now();
          const idx = items.findIndex(it => it && it.id === captchaId);
          if (idx >= 0) {
            const rec = items[idx];
            if (rec && rec.expireAt > now && rec.answerHash === ansHash) passed = true;
            items.splice(idx, 1);
            db.items = items.filter(it => it && it.expireAt > now);
            fs.writeFileSync(file, JSON.stringify(db, null, 2), 'utf8');
          }
        } catch(_){ }
      }
    }
    if (!passed) return { success: false, message: '验证码错误或已过期' };
  } catch (_){ return { success: false, message: '验证码校验失败' }; }

  // 使用 EMAS Serverless 提供的 ctx.mpserverless.db 直接访问云数据库
  try {
    const coll = ctx.mpserverless.db.collection('users');
    // 某些环境下 findOne 返回结构不一致，改用 find + limit 并做鲁棒判断
    const q = await coll.find({ username }).limit(1);
    let exists = false;
    try {
      const d = (q && (q.data ?? q.result?.data ?? q.result ?? q[0]));
      exists = Array.isArray(d) ? d.length > 0 : !!d;
    } catch (_) {}
    if (exists) {
      return { success: false, message: '用户名已存在' };
    }
    const salt = crypto.randomBytes(8).toString('hex');
    const hash = crypto.createHash('sha256').update(password + ':' + salt).digest('hex');
    const ins = await coll.insertOne({ username, pwd: hash, salt, createdAt: Date.now() });
    if (ins && (ins.success || ins.insertedId)) {
      return { success: true, message: '注册成功' };
    }
  } catch (e) {
    // 继续走回退方案
  }

  // 回退方案（仅用于演示/单实例）
  const file = path.join('/tmp', 'users.json');
  const db = readJson(file);
  db.users = Array.isArray(db.users) ? db.users : [];
  if (db.users.find(u => (u.username||'').toLowerCase() === username.toLowerCase())) return { success: false, message: '用户名已存在' };
  const salt = crypto.randomBytes(8).toString('hex');
  const hash = crypto.createHash('sha256').update(password + ':' + salt).digest('hex');
  db.users.push({ username, pwd: hash, salt, createdAt: Date.now() });
  if (!writeJson(file, db)) return { success: false, message: '写入失败' };
  return { success: true, message: '注册成功(临时存储)' };
};


