#!/usr/bin/env python3
"""Re-map ONLY the V channel of coffee_cup_inner.glb's UVs.

Geometry (positions, faces, winding, node transforms, material name) is left
byte-for-byte equivalent as a triangle multiset. The cylindrical V channel is
remapped monotonically so the cup FLOOR occupies a usable fraction of the
texture height instead of ~5%.

Why: the runtime atlas indexes the mesh by (u=angle, v=height). The floor is a
shallow bowl whose height range is only ~5% of the interior, which starves the
floor of radial texture resolution. A monotonic V remap expands the floor band
without changing any triangle's shape or position.

Usage:
    python tools/retag_coffee_inner_uv.py --check
    python tools/retag_coffee_inner_uv.py
"""

import argparse
import hashlib
import os

import numpy as np
import trimesh

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_IN = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "models", "coffee_cup_inner.glb")
DEFAULT_OUT = DEFAULT_IN

FLOOR_V = 0.28
FLOOR_LIKE_NY = 0.70
FLOOR_MAX_Y = -0.30
FLOOR_SPLIT_MARGIN = 0.012


def load(path):
    return trimesh.load(path, force="mesh", process=False)


def geometry_signature(mesh):
    v = np.asarray(mesh.vertices, dtype=np.float64)
    f = np.asarray(mesh.faces, dtype=np.int64)
    tri = v[f]
    # order-independent per-triangle signature
    tri_sorted = np.sort(tri, axis=1)
    return {
        "verts": len(v),
        "faces": len(f),
        "bbox_min": v.min(axis=0).round(6).tolist(),
        "bbox_max": v.max(axis=0).round(6).tolist(),
        "tri_sum": float(tri.sum()),
        "tri_abs_sum": float(np.abs(tri).sum()),
        "face_sig": hashlib.sha256(np.ascontiguousarray(tri_sorted).tobytes()).hexdigest()[:16],
    }


def compute_v_split(mesh):
    v = np.asarray(mesh.vertices, dtype=np.float64)
    f = np.asarray(mesh.faces, dtype=np.int64)
    uv = np.asarray(mesh.visual.uv, dtype=np.float64)
    n = np.asarray(mesh.face_normals, dtype=np.float64)
    y_min = float(v[:, 1].min())
    y_cap = float(v[:, 1].max())
    v_old = (v[:, 1] - y_min) / max(y_cap - y_min, 1e-9)

    # face -> floor-like. The rim is also near-horizontal, so require a low centroid.
    centroid = mesh.triangles.mean(axis=1)
    floor_faces = (np.abs(n[:, 1]) > FLOOR_LIKE_NY) & (centroid[:, 1] < FLOOR_MAX_Y)
    floor_verts = np.zeros(len(v), dtype=bool)
    for fi in np.nonzero(floor_faces)[0]:
        floor_verts[f[fi]] = True

    if not floor_verts.any():
        raise RuntimeError("no floor-like vertices found")
    v_split = float(v_old[floor_verts].max()) + FLOOR_SPLIT_MARGIN
    v_split = float(np.clip(v_split, 0.02, 0.5))
    return v_old, v_split, y_min, y_cap, int(floor_verts.sum())


def remap_v(v_old, v_split, floor_v):
    v_new = np.empty_like(v_old)
    lo = v_old <= v_split
    v_new[lo] = floor_v * (v_old[lo] / max(v_split, 1e-9))
    v_new[~lo] = floor_v + (1.0 - floor_v) * ((v_old[~lo] - v_split) / max(1.0 - v_split, 1e-9))
    return np.clip(v_new, 0.0, 1.0)


def run(in_path, out_path, check_only):
    print(f"[load] {in_path}")
    mesh = load(in_path)
    before = geometry_signature(mesh)
    print(f"[before] {before}")

    uv = np.asarray(mesh.visual.uv, dtype=np.float64)
    v_old_from_uv = uv[:, 1].copy()
    v_old, v_split, y_min, y_cap, floor_verts = compute_v_split(mesh)
    print(f"[split] y_min={y_min:.4f} y_cap={y_cap:.4f} v_split={v_split:.4f} "
          f"floor_verts={floor_verts}/{len(v_old)}")

    max_err = float(np.abs(v_old - v_old_from_uv).max())
    print(f"[check] uv v matches (y-y_min)/(y_cap-y_min) max_err={max_err:.6f}")

    v_new = remap_v(v_old, v_split, FLOOR_V)
    print(f"[remap] floor band v=[0.0,{FLOOR_V:.2f}] wall band v=[{FLOOR_V:.2f},1.0]")
    print(f"[remap] v_new range=[{v_new.min():.4f},{v_new.max():.4f}] monotonic={bool(np.all(np.diff(v_new[np.argsort(v_old)]) >= -1e-9))}")

    if check_only:
        print("[check-only] no export")
        return

    uv[:, 1] = v_new
    mesh.visual.uv = uv
    mesh.export(out_path)
    print(f"[export] {out_path}")

    after_mesh = load(out_path)
    after = geometry_signature(after_mesh)
    print(f"[after] {after}")
    same = (before["verts"] == after["verts"] and before["faces"] == after["faces"]
            and before["face_sig"] == after["face_sig"]
            and abs(before["tri_sum"] - after["tri_sum"]) < 1e-6
            and abs(before["tri_abs_sum"] - after["tri_abs_sum"]) < 1e-6)
    print(f"[validate] geometry_invariant={same}")
    uv2 = np.asarray(after_mesh.visual.uv, dtype=np.float64)
    print(f"[validate] uv finite={bool(np.isfinite(uv2).all())} "
          f"u=[{uv2[:,0].min():.4f},{uv2[:,0].max():.4f}] "
          f"v=[{uv2[:,1].min():.4f},{uv2[:,1].max():.4f}]")
    if not same:
        raise SystemExit("GEOMETRY CHANGED")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--in", dest="inp", default=DEFAULT_IN)
    p.add_argument("--out", dest="out", default=DEFAULT_OUT)
    p.add_argument("--check", action="store_true")
    args = p.parse_args()
    run(args.inp, args.out, args.check)


if __name__ == "__main__":
    main()
