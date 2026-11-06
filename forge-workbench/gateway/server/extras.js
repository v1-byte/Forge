// Forge Workbench — fitur tambahan gateway (GGUF, hex, git, search, run)
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execFile } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const TOOL = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../tools/forge_gguf.py');

const GGML_TYPES = { 0: 'F32', 1: 'F16', 2: 'Q4_0', 3: 'Q4_1', 6: 'Q5_0', 7: 'Q5_1', 8: 'Q8_0', 9: 'Q8_1', 10: 'Q2_K', 11: 'Q3_K', 12: 'Q4_K', 13: 'Q5_K', 14: 'Q6_K', 15: 'Q8_K', 16: 'IQ2_XXS', 17: 'IQ2_XS', 18: 'IQ3_XXS', 19: 'IQ1_S', 20: 'IQ4_NL', 21: 'IQ3_S', 22: 'IQ2_S', 23: 'IQ4_XS', 24: 'I8', 25: 'I16', 26: 'I32', 27: 'I64', 28: 'F64', 29: 'IQ1_M', 30: 'BF16' };
const SCALAR_SIZE = { 0: 1, 1: 1, 2: 2, 3: 2, 4: 4, 5: 4, 6: 4, 7: 1, 10: 8, 11: 8, 12: 8 };
const SKIP_DIRS = new Set(['node_modules', '.git', 'gateway', '__pycache__', '.venv']);
const BINARY_EXT = new Set(['.gguf', '.safetensors', '.bin', '.pt', '.pth', '.onnx', '.so', '.dll', '.dylib', '.a', '.wasm', '.apk', '.zip', '.png', '.jpg', '.jpeg', '.gif', '.ico', '.pdf']);

// ---------- GGUF reader (hanya membaca header, bukan seluruh file) ----------
class Reader {
  constructor(fd, size) { this.fd = fd; this.size = size; this.pos = 0; this.buf = Buffer.alloc(0); this.start = 0; }
  need(n) {
    if (this.pos + n > this.size) throw new Error('gguf_truncated');
    if (this.pos >= this.start && this.pos + n <= this.start + this.buf.length) return;
    const len = Math.min(Math.max(n, 1 << 20), this.size - this.pos);
    const b = Buffer.alloc(len); fs.readSync(this.fd, b, 0, len, this.pos);
    this.buf = b; this.start = this.pos;
  }
  take(n) { this.need(n); const o = this.pos - this.start; this.pos += n; return this.buf.subarray(o, o + n); }
  u8() { return this.take(1).readUInt8(); }
  u32() { return this.take(4).readUInt32LE(); }
  u64() { const v = this.take(8).readBigUInt64LE(); if (v > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error('gguf_value_too_large'); return Number(v); }
  str() { const n = this.u64(); if (n > 64 * 1024 * 1024) throw new Error('gguf_bad_string'); return this.take(n).toString('utf8'); }
  skip(n) { if (this.pos + n > this.size) throw new Error('gguf_truncated'); this.pos += n; }
}

function readValue(r, type, preview) {
  switch (type) {
    case 0: return r.u8();
    case 1: return r.take(1).readInt8();
    case 2: return r.take(2).readUInt16LE();
    case 3: return r.take(2).readInt16LE();
    case 4: return r.u32();
    case 5: return r.take(4).readInt32LE();
    case 6: return r.take(4).readFloatLE();
    case 7: return r.u8() !== 0;
    case 8: return r.str();
    case 10: return Number(r.take(8).readBigUInt64LE());
    case 11: return Number(r.take(8).readBigInt64LE());
    case 12: return r.take(8).readDoubleLE();
    case 9: {
      const itemType = r.u32(); const count = r.u64(); const sample = [];
      if (SCALAR_SIZE[itemType]) { const keep = Math.min(count, preview); for (let i = 0; i < keep; i++) sample.push(readValue(r, itemType, 0)); r.skip((count - keep) * SCALAR_SIZE[itemType]); }
      else for (let i = 0; i < count; i++) { const v = readValue(r, itemType, 0); if (i < preview) sample.push(v); }
      return { array: true, itemType, count, sample };
    }
    default: throw new Error(`gguf_unknown_type_${type}`);
  }
}

export function inspectGguf(file, { tensorLimit = 500, arrayPreview = 8 } = {}) {
  const fd = fs.openSync(file, 'r');
  try {
    const size = fs.fstatSync(fd).size; const r = new Reader(fd, size);
    if (size < 24 || r.take(4).toString('latin1') !== 'GGUF') throw new Error('not_gguf');
    const version = r.u32(); if (version < 2 || version > 3) throw new Error(`gguf_version_unsupported_${version}`);
    const tensorCount = r.u64(); const kvCount = r.u64();
    const metadata = {};
    for (let i = 0; i < kvCount; i++) { const key = r.str(); const type = r.u32(); metadata[key] = readValue(r, type, arrayPreview); }
    const tensors = []; const typeTotals = {};
    for (let i = 0; i < tensorCount; i++) {
      const name = r.str(); const nd = r.u32(); const dims = []; for (let d = 0; d < nd; d++) dims.push(r.u64());
      const t = r.u32(); const offset = r.u64();
      const typeName = GGML_TYPES[t] || `type_${t}`; typeTotals[typeName] = (typeTotals[typeName] || 0) + 1;
      if (tensors.length < tensorLimit) tensors.push({ name, dims, type: typeName, offset });
    }
    const arch = metadata['general.architecture'];
    return {
      version, tensorCount, kvCount, size, architecture: arch, name: metadata['general.name'],
      contextLength: arch ? metadata[`${arch}.context_length`] : undefined,
      blockCount: arch ? metadata[`${arch}.block_count`] : undefined,
      vocabSize: metadata['tokenizer.ggml.tokens']?.count,
      quantTypes: typeTotals, metadata, tensors, tensorsTruncated: tensorCount > tensors.length
    };
  } finally { fs.closeSync(fd); }
}

// ---------- Hex viewer (read-only) ----------
export function hexDump(file, offset = 0, length = 512) {
  const size = fs.statSync(file).size; offset = Math.max(0, Math.min(offset, size)); length = Math.max(0, Math.min(length, 4096, size - offset));
  const fd = fs.openSync(file, 'r'); const b = Buffer.alloc(length);
  try { fs.readSync(fd, b, 0, length, offset); } finally { fs.closeSync(fd); }
  const rows = [];
  for (let i = 0; i < b.length; i += 16) { const c = b.subarray(i, i + 16); rows.push({ offset: offset + i, hex: [...c].map(x => x.toString(16).padStart(2, '0')).join(' '), ascii: [...c].map(x => x >= 32 && x < 127 ? String.fromCharCode(x) : '.').join('') }); }
  return { size, offset, length, rows };
}

// ---------- Code search ----------
export function searchCode(base, query, { regex = false, caseSensitive = false, max = 200 } = {}) {
  let re; try { re = regex ? new RegExp(query, caseSensitive ? '' : 'i') : null; } catch { throw new Error('invalid_regex'); }
  const needle = caseSensitive ? query : query.toLowerCase(); const hits = []; let scanned = 0;
  const walk = dir => {
    if (hits.length >= max || scanned > 5000) return;
    let entries; try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch { return; }
    for (const e of entries) {
      if (hits.length >= max) return;
      if (e.isSymbolicLink()) continue;
      const abs = path.join(dir, e.name);
      if (e.isDirectory()) { if (!SKIP_DIRS.has(e.name)) walk(abs); continue; }
      if (BINARY_EXT.has(path.extname(e.name).toLowerCase())) continue;
      let st; try { st = fs.statSync(abs); } catch { continue; } if (st.size > 1_000_000) continue;
      scanned++; let text; try { text = fs.readFileSync(abs, 'utf8'); } catch { continue; } if (text.includes('\0')) continue;
      const lines = text.split(/\r?\n/);
      for (let i = 0; i < lines.length && hits.length < max; i++) {
        const l = lines[i]; const ok = re ? re.test(l) : (caseSensitive ? l : l.toLowerCase()).includes(needle);
        if (ok) hits.push({ file: path.relative(base, abs), line: i + 1, text: l.slice(0, 300) });
      }
    }
  };
  walk(base); return { hits, scanned, truncated: hits.length >= max };
}

// ---------- Git (tanpa shell, hooks dimatikan) ----------
function git(cwd, args) {
  return new Promise(resolve => execFile('git', ['-c', 'core.hooksPath=/dev/null', '-c', 'core.fsmonitor=false', ...args], { cwd, timeout: 15000, maxBuffer: 2_000_000, env: { PATH: process.env.PATH, HOME: os.tmpdir(), GIT_TERMINAL_PROMPT: '0', GIT_CONFIG_NOSYSTEM: '1' } },
    (err, stdout, stderr) => resolve({ ok: !err, code: err?.code ?? 0, stdout: String(stdout), stderr: String(stderr) })));
}

// ---------- Run (butuh FORGE_EXECUTE=1) ----------
const RUNNERS = { '.js': ['node'], '.mjs': ['node'], '.py': ['python3'], '.sh': ['sh'] };
const LANG_EXT = { javascript: '.js', js: '.js', python: '.py', py: '.py', shell: '.sh', sh: '.sh', bash: '.sh' };
function runProcess(cmd, args, cwd, timeoutMs) {
  return new Promise(resolve => {
    const started = Date.now(); let out = ''; let timedOut = false; let truncated = false;
    const child = spawn(cmd, args, { cwd, env: { PATH: process.env.PATH, HOME: cwd, LANG: 'C.UTF-8' }, stdio: ['ignore', 'pipe', 'pipe'] });
    const add = d => { if (out.length < 100_000) out += d; else truncated = true; };
    child.stdout.on('data', add); child.stderr.on('data', add);
    const timer = setTimeout(() => { timedOut = true; child.kill('SIGKILL'); }, timeoutMs);
    child.on('close', (code, signal) => { clearTimeout(timer); resolve({ code, signal, timedOut, truncated, output: out, ms: Date.now() - started }); });
    child.on('error', e => { clearTimeout(timer); resolve({ code: -1, signal: null, timedOut: false, truncated: false, output: e.message, ms: Date.now() - started }); });
  });
}


// ---------- Tokenizer dari vocab GGUF (perkiraan; belum diverifikasi terhadap llama.cpp) ----------
const vocabCache = new Map();
function loadVocab(file) {
  const st = fs.statSync(file); const key = `${file}:${st.mtimeMs}`; if (vocabCache.has(key)) return vocabCache.get(key);
  const g = inspectGguf(file, { tensorLimit: 0, arrayPreview: Infinity }); const m = g.metadata;
  const tokens = m['tokenizer.ggml.tokens']?.sample; if (!tokens) throw new Error('no_vocab');
  const v = { model: m['tokenizer.ggml.model'] || 'llama', tokens, scores: m['tokenizer.ggml.scores']?.sample || [], merges: m['tokenizer.ggml.merges']?.sample || [], unk: m['tokenizer.ggml.unknown_token_id'] ?? 0 };
  v.index = new Map(tokens.map((t, i) => [t, i])); v.rank = new Map(v.merges.map((x, i) => [x, i]));
  if (vocabCache.size > 4) vocabCache.clear(); vocabCache.set(key, v); return v;
}
function byteMap() { const bs = []; for (let i = 33; i <= 126; i++) bs.push(i); for (let i = 161; i <= 172; i++) bs.push(i); for (let i = 174; i <= 255; i++) bs.push(i); const cs = [...bs]; let n = 0; for (let b = 0; b < 256; b++) if (!bs.includes(b)) { bs.push(b); cs.push(256 + n++); } const enc = new Map(), dec = new Map(); bs.forEach((b, i) => { enc.set(b, String.fromCodePoint(cs[i])); dec.set(String.fromCodePoint(cs[i]), b); }); return { enc, dec }; }
const BYTES = byteMap();
function encodeSpm(v, text) {
  let syms = Array.from('\u2581' + text.replace(/ /g, '\u2581')); const out = [];
  for (;;) { let best = -1, bs = -Infinity; for (let i = 0; i < syms.length - 1; i++) { const id = v.index.get(syms[i] + syms[i + 1]); if (id !== undefined && (v.scores[id] ?? 0) > bs) { bs = v.scores[id] ?? 0; best = i; } } if (best < 0) break; syms.splice(best, 2, syms[best] + syms[best + 1]); }
  for (const sym of syms) { const id = v.index.get(sym); if (id !== undefined) out.push(id); else for (const b of Buffer.from(sym)) { const bid = v.index.get(`<0x${b.toString(16).toUpperCase().padStart(2, '0')}>`); out.push(bid ?? v.unk); } }
  return out;
}
function encodeBpe(v, text) {
  const out = []; const words = text.match(/'(?:s|t|re|ve|m|ll|d)| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+/gu) || [];
  for (const w of words) {
    let parts = Array.from(Buffer.from(w)).map(b => BYTES.enc.get(b));
    for (;;) { let best = -1, br = Infinity; for (let i = 0; i < parts.length - 1; i++) { const r = v.rank.get(parts[i] + ' ' + parts[i + 1]); if (r !== undefined && r < br) { br = r; best = i; } } if (best < 0) break; parts.splice(best, 2, parts[best] + parts[best + 1]); }
    for (const p of parts) out.push(v.index.get(p) ?? v.unk);
  }
  return out;
}
export function tokenizeGguf(file, text) {
  const v = loadVocab(file); if (text.length > 20000) throw new Error('text_too_long');
  const ids = v.model === 'gpt2' ? encodeBpe(v, text) : encodeSpm(v, text);
  return { model: v.model, ids, tokens: ids.map(id => v.tokens[id]), approximate: true };
}
export function detokenizeGguf(file, ids) {
  const v = loadVocab(file); const toks = ids.map(i => v.tokens[i] ?? '');
  if (v.model === 'gpt2') return Buffer.from(toks.join('').split('').map(c => BYTES.dec.get(c) ?? 63)).toString('utf8');
  const bytes = []; for (const t of toks) { const m = /^<0x([0-9A-F]{2})>$/.exec(t); if (m) bytes.push(parseInt(m[1], 16)); else bytes.push(...Buffer.from(t.replace(/\u2581/g, ' '))); }
  const s = Buffer.from(bytes).toString('utf8'); return s.startsWith(' ') ? s.slice(1) : s;
}

// ---------- Hex edit (dengan backup) & benchmark ----------
export function hexWrite(file, offset, hex) {
  const clean = String(hex).replace(/[\s,]/g, ''); if (!/^([0-9a-fA-F]{2})+$/.test(clean) || clean.length > 8192) throw new Error('invalid_hex');
  const data = Buffer.from(clean, 'hex'); const size = fs.statSync(file).size;
  if (!Number.isInteger(offset) || offset < 0 || offset + data.length > size) throw new Error('out_of_range');
  const backup = `${file}.bak-${Date.now()}`; fs.copyFileSync(file, backup);
  const fd = fs.openSync(file, 'r+'); try { fs.writeSync(fd, data, 0, data.length, offset); } finally { fs.closeSync(fd); }
  return { written: data.length, offset, backup };
}
export function benchRead(file, maxBytes = 256 * 1024 * 1024) {
  const size = fs.statSync(file).size; const total = Math.min(size, maxBytes); const fd = fs.openSync(file, 'r'); const buf = Buffer.alloc(4 * 1024 * 1024); let done = 0; const t0 = process.hrtime.bigint();
  try { while (done < total) { const n = fs.readSync(fd, buf, 0, Math.min(buf.length, total - done), done); if (!n) break; done += n; } } finally { fs.closeSync(fd); }
  const sec = Number(process.hrtime.bigint() - t0) / 1e9; return { bytes: done, seconds: sec, mbPerSec: sec > 0 ? done / 1048576 / sec : null, note: 'Hanya kecepatan baca disk (mungkin dari cache OS). Bukan benchmark inference.' };
}
function pyTool(args, { input, timeout }) {
  return new Promise(resolve => { const c = execFile('python3', [TOOL, ...args], { timeout, maxBuffer: 20_000_000, env: { PATH: process.env.PATH, HOME: os.tmpdir() } }, (err, stdout, stderr) => { const last = String(stdout).trim().split('\n').pop(); let data; try { data = JSON.parse(last); } catch { data = { ok: false, error: (String(stderr) || err?.message || 'tool_error').slice(-500) }; } resolve(data); }); if (input !== undefined) c.stdin.end(input); else c.stdin.end(); });
}

export async function handleExtras({ req, res, url, pathname, ctx }) {
  const { workspaceRoot, safeResolve, json, fail, readBody, executeEngines } = ctx;
  const get = req.method === 'GET', post = req.method === 'POST';
  const fileParam = () => safeResolve(workspaceRoot, url.searchParams.get('path') || '');

  if (get && pathname === '/api/models/gguf') {
    const f = fileParam(); if (!f || !fs.existsSync(f) || !fs.statSync(f).isFile()) return fail(res, 404, 'not_found', 'File tidak ditemukan'), true;
    try { json(res, { ok: true, gguf: inspectGguf(f, { tensorLimit: Number(url.searchParams.get('tensors') || 500) }) }); }
    catch (e) { fail(res, 422, e.message.startsWith('gguf') || e.message === 'not_gguf' ? e.message : 'gguf_parse_error', `Gagal membaca GGUF: ${e.message}`); }
    return true;
  }
  if (get && pathname === '/api/files/hex') {
    const f = fileParam(); if (!f || !fs.existsSync(f) || !fs.statSync(f).isFile()) return fail(res, 404, 'not_found', 'File tidak ditemukan'), true;
    json(res, { ok: true, ...hexDump(f, Number(url.searchParams.get('offset') || 0), Number(url.searchParams.get('length') || 512)) }); return true;
  }
  if (get && pathname === '/api/search') {
    const q = url.searchParams.get('q') || ''; if (!q) return fail(res, 400, 'empty_query', 'Parameter q wajib diisi'), true;
    const base = fileParam(); if (!base || !fs.existsSync(base)) return fail(res, 404, 'not_found', 'Folder tidak ditemukan'), true;
    try { json(res, { ok: true, query: q, ...searchCode(base, q, { regex: url.searchParams.get('regex') === '1', caseSensitive: url.searchParams.get('case') === '1' }) }); }
    catch (e) { fail(res, 400, e.message, 'Regex tidak valid'); }
    return true;
  }
  if (pathname.startsWith('/api/git/')) {
    const dir = (post ? safeResolve(workspaceRoot, (await readBodyOnce(req, readBody, res)).path || '') : fileParam());
    if (!dir || !fs.existsSync(dir)) return fail(res, 404, 'not_found', 'Folder tidak ditemukan'), true;
    const op = pathname.slice('/api/git/'.length); const body = req.__body || {};
    if (get && op === 'status') { const r = await git(dir, ['status', '--porcelain=v1', '--branch']); if (!r.ok) return fail(res, 400, 'not_a_git_repo', r.stderr.trim() || 'Bukan repo git'), true; const lines = r.stdout.split('\n').filter(Boolean); const branch = (lines.shift() || '').replace(/^## /, ''); json(res, { ok: true, branch, changes: lines.map(l => ({ status: l.slice(0, 2).trim() || '?', file: l.slice(3) })) }); return true; }
    if (get && op === 'diff') { const file = url.searchParams.get('file'); const r = await git(dir, ['diff', '--no-ext-diff', ...(file ? ['--', file] : [])]); json(res, { ok: r.ok, diff: r.stdout.slice(0, 200_000), error: r.ok ? undefined : r.stderr }); return true; }
    if (get && op === 'log') { const r = await git(dir, ['log', '-n', '20', '--pretty=format:%h%x09%an%x09%ad%x09%s', '--date=short']); json(res, { ok: r.ok, commits: r.stdout.split('\n').filter(Boolean).map(l => { const [hash, author, date, subject] = l.split('\t'); return { hash, author, date, subject }; }) }); return true; }
    if (post && op === 'init') { const r = await git(dir, ['init']); json(res, { ok: r.ok, output: r.stdout + r.stderr }); return true; }
    if (post && op === 'commit') {
      const message = String(body.message || '').trim(); if (!message) return fail(res, 400, 'empty_message', 'Pesan commit wajib diisi'), true;
      const files = Array.isArray(body.files) && body.files.length ? body.files.map(String) : ['-A'];
      if (files.some(f => f.startsWith('-') && f !== '-A' || path.isAbsolute(f) || f.split(/[\\/]/).includes('..'))) return fail(res, 400, 'invalid_files', 'Daftar file tidak valid'), true;
      const add = await git(dir, ['add', '--', ...files.map(f => f === '-A' ? '.' : f)]); if (!add.ok) return fail(res, 400, 'git_add_failed', add.stderr), true;
      const c = await git(dir, ['-c', 'user.name=' + (body.author || 'Forge Workbench'), '-c', 'user.email=' + (body.email || 'forge@localhost'), 'commit', '-m', message]);
      json(res, { ok: c.ok, output: (c.stdout + c.stderr).trim() }, c.ok ? 200 : 400); return true;
    }
    return fail(res, 404, 'not_found', 'Operasi git tidak dikenal'), true;
  }

  if (post && ['/api/models/convert', '/api/models/quantize', '/api/archive/read', '/api/archive/write', '/api/tokenizer/gguf', '/api/files/hex-write', '/api/models/benchmark'].includes(pathname)) {
    const body = await readBody(req); const toolTimeout = Number(process.env.FORGE_TOOL_TIMEOUT_MS || 600000);
    const inWs = (p, mustExist = true) => { const f = safeResolve(workspaceRoot, p || ''); return f && f !== workspaceRoot && (!mustExist || fs.existsSync(f)) ? f : null; };
    if (pathname === '/api/tokenizer/gguf') {
      const f = inWs(body.path); if (!f) return fail(res, 404, 'not_found', 'Model tidak ditemukan'), true;
      try { const out = { ok: true }; if (typeof body.text === 'string') Object.assign(out, tokenizeGguf(f, body.text), { count: undefined }), out.count = out.ids.length; if (Array.isArray(body.ids)) out.decoded = detokenizeGguf(f, body.ids.map(Number)); json(res, out); }
      catch (e) { fail(res, e.message === 'no_vocab' ? 422 : 400, e.message, e.message === 'no_vocab' ? 'GGUF ini tidak memuat vocab tokenizer' : `Tokenizer gagal: ${e.message}`); } return true;
    }
    if (pathname === '/api/files/hex-write') {
      const f = inWs(body.path); if (!f || !fs.statSync(f).isFile()) return fail(res, 404, 'not_found', 'File tidak ditemukan'), true;
      if (body.confirm !== true) return fail(res, 400, 'confirm_required', 'Kirim confirm:true untuk menulis byte (backup dibuat otomatis)'), true;
      try { const r = hexWrite(f, Number(body.offset), body.hex); json(res, { ok: true, ...r, backup: path.relative(workspaceRoot, r.backup) }); } catch (e) { fail(res, 400, e.message, e.message === 'out_of_range' ? 'Offset/ukuran di luar file (file tidak boleh membesar)' : 'Hex tidak valid'); } return true;
    }
    if (pathname === '/api/models/benchmark') {
      const f = inWs(body.path); if (!f || !fs.statSync(f).isFile()) return fail(res, 404, 'not_found', 'File tidak ditemukan'), true; json(res, { ok: true, ...benchRead(f) }); return true;
    }
    if (pathname === '/api/archive/read') {
      const a = inWs(body.archive); if (!a || path.extname(a).toLowerCase() !== '.zip') return fail(res, 400, 'invalid_archive', 'Arsip ZIP tidak valid'), true;
      const r = await pyTool(['zip-read', a, String(body.entry || '')], { timeout: 30000 }); return json(res, r, r.ok ? 200 : 400), true;
    }
    if (pathname === '/api/archive/write') {
      const a = inWs(body.archive); const out = inWs(body.output || String(body.archive).replace(/\.zip$/i, '.edited.zip'), false);
      if (!a || !out || path.extname(a).toLowerCase() !== '.zip' || path.extname(out).toLowerCase() !== '.zip' || typeof body.content !== 'string') return fail(res, 400, 'invalid_request', 'archive/output harus .zip di workspace dan content wajib string'), true;
      const r = await pyTool(['zip-write', a, String(body.entry || ''), out], { input: body.content, timeout: 60000 }); if (r.ok) r.output = path.relative(workspaceRoot, out); return json(res, r, r.ok ? 200 : 400), true;
    }
    // convert / quantize: butuh FORGE_EXECUTE=1
    if (!executeEngines) return fail(res, 403, 'execution_disabled', 'Konversi/quantize dimatikan. Jalankan gateway dengan FORGE_EXECUTE=1.'), true;
    const src = inWs(body.path || body.input); const out = inWs(body.output, false);
    if (!src || !out || path.extname(out).toLowerCase() !== '.gguf') return fail(res, 400, 'invalid_request', 'path/input ada di workspace dan output harus .gguf di workspace'), true;
    const args = pathname === '/api/models/convert' ? ['convert', src, out, '--type', body.type === 'f32' ? 'f32' : 'f16', ...(body.name ? ['--name', String(body.name)] : []), ...(body.vocab ? ['--vocab'] : [])] : ['quantize', src, out, '--type', 'q8_0'];
    const r = await pyTool(args, { timeout: toolTimeout }); if (r.ok) r.output = path.relative(workspaceRoot, out); json(res, r, r.ok ? 200 : 422); return true;
  }
  if (post && pathname === '/api/run') {
    if (!executeEngines) return fail(res, 403, 'execution_disabled', 'Run dimatikan. Jalankan gateway dengan FORGE_EXECUTE=1 untuk mengizinkan eksekusi kode.'), true;
    const body = await readBody(req); const timeout = Math.min(Number(process.env.FORGE_RUN_TIMEOUT_MS || 10000), 60000);
    let file, cleanup = null, ext;
    if (typeof body.code === 'string') {
      ext = LANG_EXT[String(body.language || '').toLowerCase()] || path.extname(String(body.name || '')).toLowerCase();
      if (!RUNNERS[ext]) return fail(res, 400, 'unsupported_language', 'Bahasa belum didukung (js, py, sh)'), true;
      const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'forge-run-')); file = path.join(dir, `main${ext}`); fs.writeFileSync(file, body.code); cleanup = dir;
    } else {
      file = safeResolve(workspaceRoot, body.path || ''); if (!file || !fs.existsSync(file) || !fs.statSync(file).isFile()) return fail(res, 404, 'not_found', 'File tidak ditemukan'), true;
      ext = path.extname(file).toLowerCase(); if (!RUNNERS[ext]) return fail(res, 400, 'unsupported_language', 'Ekstensi belum didukung (.js, .mjs, .py, .sh)'), true;
    }
    try { const r = await runProcess(RUNNERS[ext][0], [file], path.dirname(file), timeout); json(res, { ok: r.code === 0, exitCode: r.code, signal: r.signal, timedOut: r.timedOut, truncated: r.truncated, ms: r.ms, output: r.output }); }
    finally { if (cleanup) fs.rmSync(cleanup, { recursive: true, force: true }); }
    return true;
  }
  return false;
}

async function readBodyOnce(req, readBody, res) { try { req.__body = await readBody(req); } catch { req.__body = {}; } return req.__body; }
