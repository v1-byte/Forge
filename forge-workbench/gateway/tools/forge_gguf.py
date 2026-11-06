#!/usr/bin/env python3
"""Forge GGUF tool — konversi safetensors->GGUF (F16/F32), quantize Q8_0, dan edit ZIP.
Hanya butuh numpy. Output selalu JSON di baris terakhir stdout.

  forge_gguf.py convert  <model_dir_atau_.safetensors> <out.gguf> [--type f16|f32] [--name NAME] [--vocab]
  forge_gguf.py quantize <in.gguf> <out.gguf> [--type q8_0]
  forge_gguf.py zip-read  <archive.zip> <entry>
  forge_gguf.py zip-write <archive.zip> <entry> <out.zip>   (isi baru dari stdin)

Batasan jujur: konversi hanya arsitektur LlamaForCausalLM; metadata tokenizer (--vocab) bersifat
eksperimental dan belum diverifikasi terhadap llama.cpp.
"""
import json, struct, sys, os, glob, zipfile, io
import numpy as np

ALIGN = 32
T_F32, T_F16, T_Q8_0 = 0, 1, 8
FTYPE = {T_F32: 0, T_F16: 1, T_Q8_0: 7}
# tipe nilai metadata GGUF
U8, I8, U16, I16, U32, I32, F32, BOOL, STR, ARR, U64, I64, F64 = range(13)
SCALAR = {U8: '<B', I8: '<b', U16: '<H', I16: '<h', U32: '<I', I32: '<i', F32: '<f', BOOL: '<?', U64: '<Q', I64: '<q', F64: '<d'}

# ---------------- GGUF writer / reader ----------------
def _s(x):
    b = x.encode('utf-8'); return struct.pack('<Q', len(b)) + b

def _val(t, v):
    if t == STR: return _s(v)
    if t == ARR:
        it, items = v
        return struct.pack('<IQ', it, len(items)) + b''.join(_val(it, x) for x in items)
    return struct.pack(SCALAR[t], v)

def write_gguf(path, meta, tensors):
    """meta: list[(key, type, value)]; tensors: list[(name, ne(list, urutan GGUF), ggml_type, bytes)]"""
    head = b'GGUF' + struct.pack('<IQQ', 3, len(tensors), len(meta))
    for k, t, v in meta: head += _s(k) + struct.pack('<I', t) + _val(t, v)
    offs, off = [], 0
    for _, _, _, data in tensors:
        offs.append(off); off += (len(data) + ALIGN - 1) // ALIGN * ALIGN
    for (name, ne, ty, _), o in zip(tensors, offs):
        head += _s(name) + struct.pack('<I', len(ne)) + b''.join(struct.pack('<Q', d) for d in ne) + struct.pack('<IQ', ty, o)
    pad = (-len(head)) % ALIGN
    with open(path, 'wb') as f:
        f.write(head + b'\0' * pad)
        for (_, _, _, data) in tensors:
            f.write(data); f.write(b'\0' * ((-len(data)) % ALIGN))

class R:
    def __init__(s, b): s.b, s.p = b, 0
    def take(s, n):
        if s.p + n > len(s.b): raise ValueError('gguf terpotong')
        o = s.b[s.p:s.p + n]; s.p += n; return o
    def u32(s): return struct.unpack('<I', s.take(4))[0]
    def u64(s): return struct.unpack('<Q', s.take(8))[0]
    def str(s): return s.take(s.u64()).decode('utf-8')

def _rval(r, t):
    if t == STR: return r.str()
    if t == ARR:
        it = r.u32(); n = r.u64(); return (it, [_rval(r, it) for _ in range(n)])
    f = SCALAR[t]; return struct.unpack(f, r.take(struct.calcsize(f)))[0]

def read_gguf(path):
    b = open(path, 'rb').read(); r = R(b)
    if r.take(4) != b'GGUF': raise ValueError('bukan file GGUF')
    ver = r.u32()
    if ver not in (2, 3): raise ValueError(f'versi GGUF {ver} tidak didukung')
    nt, nkv = r.u64(), r.u64(); meta = []
    for _ in range(nkv):
        k = r.str(); t = r.u32(); meta.append((k, t, _rval(r, t)))
    infos = []
    for _ in range(nt):
        name = r.str(); nd = r.u32(); ne = [r.u64() for _ in range(nd)]; ty = r.u32(); off = r.u64(); infos.append((name, ne, ty, off))
    align = next((v for k, t, v in meta if k == 'general.alignment'), ALIGN)
    base = (r.p + align - 1) // align * align
    return meta, infos, b, base

def tensor_nbytes(ne, ty):
    n = 1
    for d in ne: n *= d
    if ty == T_F32: return n * 4
    if ty == T_F16: return n * 2
    if ty == T_Q8_0: return n // 32 * 34
    raise ValueError(f'tipe ggml {ty} belum didukung')

def to_f32(raw, ne, ty):
    n = 1
    for d in ne: n *= d
    if ty == T_F32: return np.frombuffer(raw, '<f4', n).copy()
    if ty == T_F16: return np.frombuffer(raw, '<f2', n).astype(np.float32)
    if ty == T_Q8_0:
        blk = np.frombuffer(raw, dtype=np.dtype([('d', '<f2'), ('q', 'i1', 32)]), count=n // 32)
        return (blk['q'].astype(np.float32) * blk['d'].astype(np.float32)[:, None]).reshape(-1)
    raise ValueError(f'tipe ggml {ty} belum didukung')

def quant_q8_0(x):
    x = x.astype(np.float32).reshape(-1, 32)
    d = np.abs(x).max(axis=1) / 127.0
    inv = np.where(d != 0, 1.0 / np.where(d == 0, 1, d), 0)
    q = np.rint(x * inv[:, None]).clip(-127, 127).astype(np.int8)
    out = np.empty(len(x), dtype=np.dtype([('d', '<f2'), ('q', 'i1', 32)]))
    out['d'] = d.astype(np.float16); out['q'] = q
    return out.tobytes()

# ---------------- safetensors ----------------
def read_safetensors(path):
    with open(path, 'rb') as f:
        n = struct.unpack('<Q', f.read(8))[0]
        if n > 100_000_000: raise ValueError('header safetensors tidak wajar')
        hdr = json.loads(f.read(n)); base = 8 + n
        for name, info in hdr.items():
            if name == '__metadata__': continue
            s, e = info['data_offsets']; f.seek(base + s); raw = f.read(e - s)
            dt = info['dtype']
            if dt == 'F32': a = np.frombuffer(raw, '<f4')
            elif dt == 'F16': a = np.frombuffer(raw, '<f2').astype(np.float32)
            elif dt == 'BF16': a = (np.frombuffer(raw, '<u2').astype(np.uint32) << 16).view(np.float32)
            else: raise ValueError(f'dtype {dt} belum didukung ({name})')
            yield name, a.reshape(info['shape']).astype(np.float32)

LAYER_MAP = {'self_attn.q_proj': 'attn_q', 'self_attn.k_proj': 'attn_k', 'self_attn.v_proj': 'attn_v', 'self_attn.o_proj': 'attn_output',
             'input_layernorm': 'attn_norm', 'post_attention_layernorm': 'ffn_norm', 'mlp.gate_proj': 'ffn_gate', 'mlp.up_proj': 'ffn_up', 'mlp.down_proj': 'ffn_down'}

def map_name(n):
    if n == 'model.embed_tokens.weight': return 'token_embd.weight'
    if n == 'model.norm.weight': return 'output_norm.weight'
    if n == 'lm_head.weight': return 'output.weight'
    if n.endswith('rotary_emb.inv_freq'): return None
    p = n.split('.')
    if len(p) > 4 and p[0] == 'model' and p[1] == 'layers' and p[2].isdigit():
        mod = '.'.join(p[3:-1])
        if mod in LAYER_MAP and p[-1] == 'weight': return f'blk.{p[2]}.{LAYER_MAP[mod]}.weight'
    raise ValueError(f'nama tensor tidak dikenal: {n}')

def permute(w, n_head, n_kv):
    if n_kv is not None and n_head != n_kv: n_head = n_kv
    return w.reshape(n_head, 2, w.shape[0] // n_head // 2, *w.shape[1:]).swapaxes(1, 2).reshape(w.shape)

def convert(src, out, dtype='f16', name=None, vocab=False):
    d = src if os.path.isdir(src) else os.path.dirname(os.path.abspath(src))
    cfg = json.load(open(os.path.join(d, 'config.json')))
    arch = (cfg.get('architectures') or [''])[0]
    if arch != 'LlamaForCausalLM': raise ValueError(f'arsitektur {arch or "tidak diketahui"} belum didukung (hanya LlamaForCausalLM)')
    files = [src] if os.path.isfile(src) else sorted(glob.glob(os.path.join(src, '*.safetensors')))
    if not files: raise ValueError('tidak ada file .safetensors')
    nh = cfg['num_attention_heads']; nkv = cfg.get('num_key_value_heads', nh); hs = cfg['hidden_size']
    gty = T_F16 if dtype == 'f16' else T_F32; npd = '<f2' if dtype == 'f16' else '<f4'
    tensors = []; seen = set()
    for fp in files:
        for hf, arr in read_safetensors(fp):
            g = map_name(hf)
            if g is None: continue
            if g.endswith(('attn_q.weight',)): arr = permute(arr, nh, nh)
            if g.endswith(('attn_k.weight',)): arr = permute(arr, nh, nkv)
            is_norm = arr.ndim == 1
            ty, nd = (T_F32, '<f4') if is_norm else (gty, npd)  # norm selalu F32, seperti llama.cpp
            tensors.append((g, list(reversed(arr.shape)), ty, np.ascontiguousarray(arr.astype(nd)).tobytes())); seen.add(g)
    if 'output.weight' not in seen and cfg.get('tie_word_embeddings', True) is False:
        raise ValueError('lm_head.weight tidak ada padahal tie_word_embeddings=false')
    meta = [('general.architecture', STR, 'llama'), ('general.name', STR, name or os.path.basename(os.path.abspath(d))),
            ('general.file_type', U32, FTYPE[gty]), ('general.quantization_version', U32, 2),
            ('llama.context_length', U32, cfg.get('max_position_embeddings', 2048)), ('llama.embedding_length', U32, hs),
            ('llama.block_count', U32, cfg['num_hidden_layers']), ('llama.feed_forward_length', U32, cfg['intermediate_size']),
            ('llama.attention.head_count', U32, nh), ('llama.attention.head_count_kv', U32, nkv),
            ('llama.rope.dimension_count', U32, hs // nh), ('llama.rope.freq_base', F32, float(cfg.get('rope_theta', 10000.0))),
            ('llama.attention.layer_norm_rms_epsilon', F32, float(cfg.get('rms_norm_eps', 1e-5))),
            ('llama.vocab_size', U32, cfg.get('vocab_size', 0))]
    warn = []
    tj = os.path.join(d, 'tokenizer.json')
    if vocab and os.path.exists(tj):
        t = json.load(open(tj)); m = t.get('model', {})
        if m.get('type') == 'BPE' and m.get('vocab'):
            toks = [''] * (max(m['vocab'].values()) + 1)
            for k, v in m['vocab'].items(): toks[v] = k
            merges = [' '.join(x) if isinstance(x, list) else x for x in m.get('merges', [])]
            meta += [('tokenizer.ggml.model', STR, 'gpt2'), ('tokenizer.ggml.tokens', ARR, (STR, toks)), ('tokenizer.ggml.merges', ARR, (STR, merges))]
            warn.append('tokenizer BPE ditulis apa adanya; belum diverifikasi dengan llama.cpp')
        else: warn.append('tokenizer.json bukan BPE; metadata tokenizer dilewati')
    elif vocab: warn.append('tokenizer.json tidak ditemukan')
    write_gguf(out, meta, tensors)
    return {'ok': True, 'tensors': len(tensors), 'size': os.path.getsize(out), 'type': dtype, 'warnings': warn}

def quantize(src, out, qtype='q8_0'):
    if qtype != 'q8_0': raise ValueError('hanya q8_0 yang didukung')
    meta, infos, b, base = read_gguf(src); tensors = []; nq = 0
    for name, ne, ty, off in infos:
        raw = b[base + off: base + off + tensor_nbytes(ne, ty)]
        if len(ne) == 2 and ne[0] % 32 == 0 and ty in (T_F32, T_F16):
            tensors.append((name, ne, T_Q8_0, quant_q8_0(to_f32(raw, ne, ty)))); nq += 1
        else: tensors.append((name, ne, ty, bytes(raw)))
    meta = [(k, t, 7 if k == 'general.file_type' else v) for k, t, v in meta]
    if not any(k == 'general.file_type' for k, _, _ in meta): meta.append(('general.file_type', U32, 7))
    write_gguf(out, meta, tensors)
    return {'ok': True, 'quantized': nq, 'kept': len(tensors) - nq, 'size_in': os.path.getsize(src), 'size_out': os.path.getsize(out)}

# ---------------- ZIP ----------------
def _safe(name):
    if name.startswith('/') or '\\' in name or '..' in name.split('/'): raise ValueError('nama entry tidak aman')
    return name

def zip_read(path, entry):
    with zipfile.ZipFile(path) as z:
        info = z.getinfo(_safe(entry))
        if info.file_size > 5_000_000: raise ValueError('entry terlalu besar (>5 MB)')
        data = z.read(info)
    if b'\0' in data[:8192]: return {'ok': True, 'binary': True, 'size': len(data)}
    return {'ok': True, 'binary': False, 'size': len(data), 'content': data.decode('utf-8', 'replace')}

def zip_write(path, entry, out, data):
    _safe(entry)
    if os.path.abspath(out) == os.path.abspath(path): raise ValueError('output harus berbeda dari arsip asli')
    with zipfile.ZipFile(path) as zin, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
        found = False
        for i in zin.infolist():
            if i.filename == entry: zout.writestr(i.filename, data); found = True
            else: zout.writestr(i, zin.read(i))
        if not found: zout.writestr(entry, data)
    return {'ok': True, 'replaced': found, 'size': os.path.getsize(out)}

def main(a):
    if len(a) < 2: raise ValueError(__doc__)
    cmd, rest = a[0], a[1:]
    opt = lambda k, d=None: rest[rest.index(k) + 1] if k in rest else d
    pos = [x for i, x in enumerate(rest) if not x.startswith('--') and (i == 0 or not rest[i - 1].startswith('--') or rest[i - 1] == '--vocab')]
    if cmd == 'convert': return convert(pos[0], pos[1], opt('--type', 'f16'), opt('--name'), '--vocab' in rest)
    if cmd == 'quantize': return quantize(pos[0], pos[1], opt('--type', 'q8_0'))
    if cmd == 'zip-read': return zip_read(pos[0], pos[1])
    if cmd == 'zip-write': return zip_write(pos[0], pos[1], pos[2], sys.stdin.buffer.read())
    raise ValueError(f'perintah tidak dikenal: {cmd}')

if __name__ == '__main__':
    try: print(json.dumps(main(sys.argv[1:])))
    except Exception as e:
        print(json.dumps({'ok': False, 'error': str(e)})); sys.exit(1)
