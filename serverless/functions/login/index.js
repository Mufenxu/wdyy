'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const https = require('https');

// 登录云函数：接受 { username, password, captchaId, captchaText }，返回 { success, token, message, role? }
module.exports = async (ctx) => {
  let username = '';
  let password = '';
  let captchaId = '';
  let captchaText = '';

  try {
    // 1) 优先从 POST JSON 读取
    const rawBody = ctx && ctx.request && ctx.request.body;
    if (rawBody) {
      if (typeof rawBody === 'string') {
        try {
          const parsed = JSON.parse(rawBody);
          username = parsed.username || username;
          password = parsed.password || password;
        } catch (_) {}
      } else if (typeof rawBody === 'object') {
        username = rawBody.username || username;
        password = rawBody.password || password;
        captchaId = rawBody.captchaId || captchaId;
        captchaText = rawBody.captchaText || captchaText;
      }
    }

    // 2) 兼容 GET 查询：args.queryStringParameters 或 request.queries
    if (!username || !password) {
      const qs = (ctx && ctx.args && ctx.args.queryStringParameters) ||
                 (ctx && ctx.request && ctx.request.queries) || {};
      if (qs) {
        username = qs.username || username;
        password = qs.password || password;
        captchaId = qs.captchaId || captchaId;
        captchaText = qs.captchaText || captchaText;
      }
    }
  } catch (e) {}

  if (!username || !password) {
    return { success: false, message: '用户名或密码为空' };
  }

  // 校验验证码（存在则校验）
  if (captchaId || captchaText) {
    const crypto = require('crypto');
    const ansHash = crypto.createHash('sha256').update(String(captchaText||'')).digest('hex');
    let passed = false;
    // 优先云数据库
    try {
      const coll = ctx.mpserverless.db.collection('captchas');
      const found = await coll.findOne({ id: captchaId });
      const rec = (found && (found.data || found.result?.data || found.result)) ? (found.data || found.result?.data || found.result) : null;
      if (rec && rec.expireAt > Date.now() && rec.answerHash === ansHash) {
        passed = true;
      }
      try { await coll.deleteOne({ id: captchaId }); } catch(_) {}
    } catch (_){
      // 回退到 /tmp
      const fs = require('fs');
      const path = require('path');
      const file = path.join('/tmp', 'captchas.json');
      try {
        if (fs.existsSync(file)) {
          const raw = fs.readFileSync(file, 'utf8');
          const db = JSON.parse(raw || '{}');
          const items = Array.isArray(db.items) ? db.items : [];
          const now = Date.now();
          const idx = items.findIndex(it => it && it.id === captchaId);
          if (idx >= 0) {
            const rec = items[idx];
            if (rec && rec.expireAt > now && rec.answerHash === ansHash) {
              passed = true;
            }
            items.splice(idx, 1); // 使用一次即删除
            db.items = items.filter(it => it && it.expireAt > now);
            fs.writeFileSync(file, JSON.stringify(db, null, 2), 'utf8');
          }
        }
      } catch(_){ }
    }
    if (!passed) return { success: false, message: '验证码错误或已过期' };
  }

  // 优先从 EMAS Serverless 的 ctx.mpserverless.db 查询
  try {
    const coll = ctx.mpserverless.db.collection('users');
    const found = await coll.findOne({ username });
    const u = (found && (found.data || found.result?.data || found.result)) ? (found.data || found.result?.data || found.result) : null;
    if (u) {
      const calc = crypto.createHash('sha256').update(password + ':' + (u.salt||'')) .digest('hex');
      if (calc !== u.pwd) return { success: false, message: '密码错误' };
      const token = `t-${Date.now()}-${Buffer.from(username).toString('hex').slice(0,8)}`;
      const role = (username === 'admin') ? 'super_admin' : 'user';
      return { success: true, token, role, message: '登录成功' };
    }
  } catch (e) {
    return { success: false, message: 'db_error', detail: String(e) };
  }

  // 回退到 /tmp（仅用于演示/单实例）
  const file = path.join('/tmp', 'users.json');
  let users = [];
  try {
    if (fs.existsSync(file)) {
      const raw = fs.readFileSync(file, 'utf8');
      const db = JSON.parse(raw || '{}');
      users = Array.isArray(db.users) ? db.users : [];
    }
  } catch (_) {}

  // 查找用户并校验密码
  const found = users.find(u => (u.username||'').toLowerCase() === username.toLowerCase());
  if (!found) {
    return { success: false, message: '用户不存在' };
  }
  const calc = crypto.createHash('sha256').update(password + ':' + (found.salt||'')) .digest('hex');
  if (calc !== found.pwd) {
    return { success: false, message: '密码错误' };
  }

  const token = `t-${Date.now()}-${Buffer.from(username).toString('hex').slice(0,8)}`;
  const role = (username === 'admin') ? 'super_admin' : 'user';

  return { success: true, token, role, message: '登录成功' };
};


