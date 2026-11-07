import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { handleExtras } from './extras.js';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const frontendDir = path.join(root, 'frontend');
const workspaceRoot = path.resolve(process.env.FORGE_WORKSPACE_ROOT || path.join(root, 'workspaces'));
const engineRoot = path.resolve(process.env.FORGE_ENGINE_ROOT || path.join(root, '..', 'forge-engine'));
const port = Number(process.env.PORT || 8787);
const executeEngines = process.env.FORGE_EXECUTE === '1';
const strictNative = process.env.FORGE_NATIVE_STRICT !== '0';
const jobs = new Map();
const dataRoot = path.join(root, 'gateway', 'data');
const jobFile = path.join(dataRoot, 'jobs.json');
const jobStreams = new Map();
const workbenchVersion = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8')).version;
let releaseCache = { checkedAt: 0, data: null };
fs.mkdirSync(dataRoot, { recursive: true });
try { for (const job of JSON.parse(fs.readFileSync(jobFile, 'utf8'))) jobs.set(job.id, job); } catch {}
function persistJobs() { try { fs.writeFileSync(jobFile, JSON.stringify([...jobs.values()].slice(-500), null, 2)); } catch {} }
function broadcast(job) { persistJobs(); for (const stream of jobStreams.get(job.id) || []) stream.write(`data: ${JSON.stringify(job)}\n\n`); }
fs.mkdirSync(workspaceRoot, { recursive: true });

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.png': 'image/png', '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf', '.txt': 'text/plain; charset=utf-8'
};
const CODE_EXT = new Set(['.c','.cc','.cpp','.h','.hpp','.py','.js','.ts','.tsx','.jsx','.java','.kt','.rs','.go','.swift','.sh','.smali','.json','.yaml','.yml','.toml','.xml']);
const MODEL_EXT = new Set(['.gguf','.safetensors','.bin','.pt','.pth','.ckpt','.onnx']);
const ALLOWED_JOB_TYPES = new Set(['inspect-model','tokenize','inference','compile','quantize','serve','training','evaluate','convert']);

function json(res, value, status = 200) { res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' }); res.end(JSON.stringify(value)); }
function fail(res, status, code, message, details = undefined) { return json(res, { ok: false, error: { code, message, details } }, status); }
function sendFile(res, filePath) { try { const data = fs.readFileSync(filePath); const type = MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream'; res.writeHead(200, { 'content-type': type, 'cache-control': 'no-cache' }); res.end(data); } catch { fail(res, 404, 'not_found', 'File tidak ditemukan'); } }
function safeResolve(base, requested = '') { const target = path.resolve(base, requested); return target === base || target.startsWith(`${base}${path.sep}`) ? target : null; }
function readBody(req) { return new Promise((resolve, reject) => { let raw = ''; req.on('data', chunk => { raw += chunk; if (raw.length > 2_000_000) req.destroy(new Error('body_too_large')); }); req.on('end', () => { try { resolve(raw ? JSON.parse(raw) : {}); } catch { reject(new Error('invalid_json')); } }); req.on('error', reject); }); }
function classify(file) { const lower = file.toLowerCase(); const ext = path.extname(lower); const token = /tokenizer|vocab|sentencepiece|token/.test(lower); const binary = MODEL_EXT.has(ext) || ['.so','.dll','.dylib','.a','.wasm','.apk','.zip'].includes(ext); const system = /(^|[/_.-])(config|manifest|settings|schema|package|cmake|docker|lock|env)([/_.-]|$)/.test(lower) || ['.json','.yaml','.yml','.toml','.xml'].includes(ext); const engine = /forge-(compiler|editor|inference|trainer|litgpt)/.exec(lower)?.[0] || null; return { group: engine ? 'engine' : binary || token ? 'machine' : system ? 'system' : 'file', kind: token ? 'token' : binary ? 'binary' : CODE_EXT.has(ext) ? 'code' : system ? 'config' : 'file', engine }; }
function engineStatus(id) { const p = path.join(engineRoot, id); return { id, path: p, available: fs.existsSync(p), executable: executeEngines }; }
function hardware() { const cpus = os.cpus(); return { platform: process.platform, arch: process.arch, cpu: cpus[0]?.model || 'unknown', cpuCount: cpus.length, memoryBytes: os.totalmem(), freeMemoryBytes: os.freemem(), gpu: process.env.FORGE_GPU || 'not-detected', backends: (process.env.FORGE_BACKENDS || 'cpu').split(',').map(s => s.trim()).filter(Boolean) }; }
function createJob(type, payload = {}) { const id = crypto.randomUUID(); const job = { id, type, payload, status: 'queued', progress: 0, logs: [], createdAt: new Date().toISOString(), result: null }; jobs.set(id, job); persistJobs(); runJob(job); return job; }
function log(job, line) { job.logs.push(`[${new Date().toISOString()}] ${line}`); job.logs = job.logs.slice(-200); broadcast(job); }
function nativeCommand(type) {
  const key = `FORGE_${type.toUpperCase().replaceAll('-', '_')}_CMD`;
  return process.env[key] || '';
}
function runNative(job, command) { return new Promise(resolve => {
  const child = spawn('/bin/sh', ['-c', command], { cwd: workspaceRoot, env: { ...process.env, BASH_ENV: undefined, ENV: undefined, FORGE_JOB_ID: job.id } });
  let output = ''; child.stdout.on('data', d => { output += d; log(job, String(d).trim()); }); child.stderr.on('data', d => { output += d; log(job, String(d).trim()); });
  child.on('close', code => resolve({ code, output })); child.on('error', error => resolve({ code: -1, output: error.message }));
}); }
function walkModels(dir, rel = '', out = []) { if (out.length >= 200 || !fs.existsSync(dir)) return out; for (const entry of fs.readdirSync(dir, { withFileTypes: true })) { const absolute = path.join(dir, entry.name); const relative = path.join(rel, entry.name); if (entry.isDirectory()) walkModels(absolute, relative, out); else if (MODEL_EXT.has(path.extname(entry.name).toLowerCase())) { const stat = fs.statSync(absolute); out.push({ path: relative.replaceAll(path.sep, '/'), size: stat.size, format: path.extname(entry.name).slice(1).toUpperCase(), classification: classify(relative) }); } } return out; }
function validateDataset(file) { const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/).filter(Boolean); const errors = []; let valid = 0; const keys = new Set(); for (let i = 0; i < lines.length; i++) { try { const row = JSON.parse(lines[i]); Object.keys(row).forEach(k => keys.add(k)); if (!row.prompt && !row.instruction && !row.messages && !row.text) errors.push({ line: i + 1, message: 'Tidak ada field prompt/instruction/messages/text' }); else valid++; } catch { errors.push({ line: i + 1, message: 'JSON tidak valid' }); } } return { rows: lines.length, valid, invalid: errors.length, keys: [...keys], errors: errors.slice(0, 50) }; }
async function runJob(job) { job.status = 'running'; log(job, `${job.type} started`); const steps = job.type === 'training' ? 10 : 5; for (let i = 1; i <= steps; i++) { if (job.status === 'cancelled') return; await new Promise(r => setTimeout(r, 120)); job.progress = Math.round(i / steps * 100); log(job, `progress ${job.progress}%`); }
  const engine = job.type === 'training' ? (engineStatus('forge-trainer').available ? 'forge-trainer' : 'forge-litgpt') : job.type === 'compile' || job.type === 'convert' ? 'forge-compiler' : 'forge-inference';
  const command = nativeCommand(job.type);
  if (strictNative && (!executeEngines || !command || !engineStatus(engine).available)) {
    job.result = { native: false, errorCode: 'native_engine_unavailable', engine, message: `Native engine unavailable: configure FORGE_EXECUTE=1, FORGE_${job.type.toUpperCase().replaceAll('-', '_')}_CMD, and a valid ${engine} source/build.`, payload: job.payload };
    job.status = 'failed'; log(job, job.result.message);
  } else if (executeEngines && command) {
    const native = await runNative(job, command);
    job.result = { engine, native: true, exitCode: native.code, output: native.output.slice(-10000), payload: job.payload };
    job.status = native.code === 0 ? 'completed' : 'failed'; log(job, `native exit=${native.code}`);
  } else {
    job.result = { engine, fallback: true, message: 'Non-strict preview mode.', payload: job.payload };
    job.status = 'completed'; log(job, job.result.message);
  }
  broadcast(job);
}
function listFiles(base, rel = '') { const dir = safeResolve(base, rel); if (!dir || !fs.existsSync(dir)) return null; return fs.readdirSync(dir, { withFileTypes: true }).map(entry => { const name = path.join(rel, entry.name); const item = { name: entry.name, path: name.replaceAll(path.sep, '/'), type: entry.isDirectory() ? 'folder' : 'file' }; if (!entry.isDirectory()) { const stat = fs.statSync(path.join(dir, entry.name)); item.size = stat.size; item.classification = classify(entry.name); } return item; }); }
function inspectModel(filePath) { const stat = fs.statSync(filePath); const ext = path.extname(filePath).toLowerCase(); return { path: path.relative(workspaceRoot, filePath), size: stat.size, format: ext.slice(1).toUpperCase(), classification: classify(filePath), readable: !MODEL_EXT.has(ext) || stat.size < 1_000_000, note: MODEL_EXT.has(ext) ? 'Binary model; metadata parser native engine diperlukan untuk tensor detail.' : 'File dapat dibaca sebagai teks.' }; }

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`); const pathname = decodeURIComponent(url.pathname);
  try {
    if (req.method === 'GET' && pathname === '/health') return json(res, { ok: true, service: 'forge-workbench-gateway', time: new Date().toISOString() });
    if (req.method === 'GET' && pathname === '/api/update') {
      const now = Date.now();
      if (!releaseCache.data || now - releaseCache.checkedAt > 15 * 60 * 1000) {
        try {
          const response = await fetch('https://api.github.com/repos/v1-byte/Forge/releases/latest', { headers: { 'Accept': 'application/vnd.github+json', 'User-Agent': 'Forge-Workbench-Updater/2.1.2' }, signal: AbortSignal.timeout(5000) });
          if (!response.ok) throw new Error(`GitHub API returned ${response.status}`);
          const release = await response.json();
          const latestVersion = String(release.tag_name || '').replace(/^v/i, '');
          const numeric = value => String(value).replace(/^v/i, '').split(/[.+-]/).slice(0, 3).map(part => Number.parseInt(part, 10) || 0);
          const current = numeric(workbenchVersion), latest = numeric(latestVersion);
          let comparison = 0; for (let i = 0; i < 3; i++) { if ((latest[i] || 0) !== (current[i] || 0)) { comparison = (latest[i] || 0) > (current[i] || 0) ? 1 : -1; break; } }
          releaseCache = { checkedAt: now, data: { ok: true, currentVersion: workbenchVersion, latestVersion, updateAvailable: comparison > 0, releaseUrl: release.html_url || 'https://github.com/v1-byte/Forge/releases/latest', checkedAt: new Date(now).toISOString() } };
        } catch (error) {
          releaseCache = { checkedAt: now, data: { ok: false, currentVersion: workbenchVersion, error: 'Pengecekan versi belum tersedia', checkedAt: new Date(now).toISOString() } };
        }
      }
      return json(res, releaseCache.data);
    }
    if (req.method === 'GET' && pathname === '/api/engine-map') return sendFile(res, path.join(root, 'config/engine-map.json'));
    if (req.method === 'GET' && pathname === '/api/workbench-manifest') return sendFile(res, path.join(root, 'config/workbench-manifest.json'));
    if (req.method === 'GET' && pathname === '/api/status') return json(res, { ok: true, workspaceRoot, engineRoot, executeEngines, strictNative, hardware: hardware(), engines: ['forge-compiler','forge-editor','forge-inference','forge-trainer','forge-litgpt'].map(engineStatus) });
    if (req.method === 'GET' && pathname === '/api/models') return json(res, { ok: true, models: walkModels(workspaceRoot) });
    if (req.method === 'POST' && pathname === '/api/datasets/validate') { const body = await readBody(req); const file = safeResolve(workspaceRoot, body.path || ''); if (!file || !fs.existsSync(file)) return fail(res, 404, 'not_found', 'Dataset tidak ditemukan'); return json(res, { ok: true, path: body.path, dataset: validateDataset(file) }); }
    if (req.method === 'GET' && pathname === '/api/archive/list') { const file = safeResolve(workspaceRoot, url.searchParams.get('path') || ''); if (!file || !fs.existsSync(file) || path.extname(file).toLowerCase() !== '.zip') return fail(res, 400, 'invalid_archive', 'ZIP tidak ditemukan'); const listing = await runNative({ id: 'archive-list', logs: [] }, `unzip -Z1 -- ${JSON.stringify(file)}`); if (listing.code !== 0) return fail(res, 422, 'archive_error', listing.output); const entries = listing.output.split(/\r?\n/).filter(Boolean).map(name => ({ name, classification: classify(name) })); return json(res, { ok: true, entries }); }
    if (req.method === 'POST' && pathname === '/api/archive/extract') { const body = await readBody(req); const archive = safeResolve(workspaceRoot, body.archive || ''); const destination = safeResolve(workspaceRoot, body.destination || 'extracted'); if (!archive || !destination || !fs.existsSync(archive) || path.extname(archive).toLowerCase() !== '.zip') return fail(res, 400, 'invalid_archive', 'ZIP tidak valid'); const check = await runNative({ id: 'archive-check', logs: [] }, `unzip -Z1 -- ${JSON.stringify(archive)}`); const bad = check.output.split(/\r?\n/).filter(Boolean).some(name => !safeResolve(destination, name)); if (bad) return fail(res, 422, 'zip_path_traversal', 'Entry ZIP berbahaya'); fs.mkdirSync(destination, { recursive: true }); const result = await runNative({ id: 'archive-extract', logs: [] }, `unzip -oq -- ${JSON.stringify(archive)} -d ${JSON.stringify(destination)}`); return result.code === 0 ? json(res, { ok: true, destination: body.destination }) : fail(res, 422, 'archive_error', result.output); }
    if (req.method === 'GET' && pathname === '/api/files') { const files = listFiles(workspaceRoot, url.searchParams.get('path') || ''); return files ? json(res, { ok: true, files }) : fail(res, 404, 'not_found', 'Folder tidak ditemukan'); }
    if (req.method === 'GET' && pathname === '/api/files/read') { const file = safeResolve(workspaceRoot, url.searchParams.get('path') || ''); if (!file || !fs.existsSync(file) || !fs.statSync(file).isFile()) return fail(res, 404, 'not_found', 'File tidak ditemukan'); const stat = fs.statSync(file); if (stat.size > 5_000_000) return fail(res, 413, 'too_large', 'File terlalu besar untuk dibuka di editor'); return json(res, { ok: true, path: url.searchParams.get('path'), content: fs.readFileSync(file, 'utf8'), classification: classify(file) }); }
    if (req.method === 'POST' && pathname === '/api/files/write') { const body = await readBody(req); const file = safeResolve(workspaceRoot, body.path || ''); if (!file || typeof body.content !== 'string') return fail(res, 400, 'invalid_path', 'Path atau content tidak valid'); if (!CODE_EXT.has(path.extname(file).toLowerCase()) && body.allowNonCode !== true) return fail(res, 415, 'binary_write_blocked', 'Penulisan binary diblokir; gunakan adapter khusus'); fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, body.content, 'utf8'); return json(res, { ok: true, path: body.path, bytes: Buffer.byteLength(body.content) }); }
    if (req.method === 'GET' && pathname === '/api/models/inspect') { const file = safeResolve(workspaceRoot, url.searchParams.get('path') || ''); if (!file || !fs.existsSync(file)) return fail(res, 404, 'not_found', 'Model tidak ditemukan'); return json(res, { ok: true, model: inspectModel(file) }); }
    if (req.method === 'POST' && pathname === '/api/tokenizer') { const body = await readBody(req); const text = String(body.text || ''); const tokens = Array.from(text).map((char, index) => ({ id: char.codePointAt(0), text: char, index })); return json(res, { ok: true, mode: body.mode || 'encode', tokenCount: tokens.length, tokens, text }); }
    if (req.method === 'POST' && pathname === '/api/jobs') { const body = await readBody(req); if (!ALLOWED_JOB_TYPES.has(body.type)) return fail(res, 400, 'unsupported_job', 'Tipe job tidak didukung'); return json(res, { ok: true, job: createJob(body.type, body.payload || {}) }, 202); }
    if (req.method === 'GET' && pathname.startsWith('/api/jobs/') && pathname.endsWith('/events')) { const id = pathname.split('/')[3]; const job = jobs.get(id); if (!job) return fail(res, 404, 'not_found', 'Job tidak ditemukan'); res.writeHead(200, { 'content-type': 'text/event-stream', 'cache-control': 'no-cache', connection: 'keep-alive' }); res.write(`data: ${JSON.stringify(job)}\n\n`); const streams = jobStreams.get(id) || []; streams.push(res); jobStreams.set(id, streams); req.on('close', () => jobStreams.set(id, (jobStreams.get(id) || []).filter(x => x !== res))); return; }
    if (req.method === 'GET' && pathname.startsWith('/api/jobs/')) { const id = pathname.split('/').pop(); const job = jobs.get(id); return job ? json(res, { ok: true, job }) : fail(res, 404, 'not_found', 'Job tidak ditemukan'); }
    if (req.method === 'POST' && pathname.startsWith('/api/jobs/') && pathname.endsWith('/cancel')) { const id = pathname.split('/')[3]; const job = jobs.get(id); if (!job) return fail(res, 404, 'not_found', 'Job tidak ditemukan'); job.status = 'cancelled'; log(job, 'cancelled by user'); broadcast(job); return json(res, { ok: true, job }); }
    if (await handleExtras({ req, res, url, pathname, ctx: { workspaceRoot, safeResolve, json, fail, readBody, executeEngines } })) return;
    if (pathname === '/' || pathname === '/index.html') return sendFile(res, path.join(frontendDir, 'index.html'));
    const safePath = path.normalize(pathname).replace(/^([.][.][/\\])+/, ''); const candidate = path.join(frontendDir, safePath); if (candidate.startsWith(frontendDir) && fs.existsSync(candidate) && fs.statSync(candidate).isFile()) return sendFile(res, candidate);
    return fail(res, 404, 'not_found', 'Route tidak ditemukan');
  } catch (error) { return fail(res, 500, error.message === 'invalid_json' ? 'invalid_json' : 'server_error', error.message); }
});
server.listen(port, '0.0.0.0', () => { console.log(`Forge Workbench gateway listening on http://0.0.0.0:${port}`); console.log(`workspace=${workspaceRoot} engineRoot=${engineRoot} executeEngines=${executeEngines}`); });
