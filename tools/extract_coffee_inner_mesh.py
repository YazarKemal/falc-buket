#!/usr/bin/env python3
"""Extract the real interior overlay mesh from the Meshy coffee_cup.glb.

The extraction is fully offline / dev-time. It classifies the cup interior using
actual triangle geometry (ray casting from the fitted bowl axis), clips the top
at a safe height below the gold rim, offsets the sheet slightly into the cavity
to avoid z-fighting, generates cylindrical UVs (with seam + pole handling) and
exports a separate GLB that keeps the exact model-local coordinate system.

Usage:
    python tools/extract_coffee_inner_mesh.py
    python tools/extract_coffee_inner_mesh.py --selftest
    python tools/extract_coffee_inner_mesh.py --validate-only
"""

import argparse
import hashlib
import os
import sys
import time

import numpy as np
import trimesh

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_SRC = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "models", "coffee_cup.glb")
DEFAULT_OUT = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "models", "coffee_cup_inner.glb")

Y_CAP = 0.700
OFFSET = 0.003
SEAM_ANGLE_DEG = 0.0
POLE_RADIUS = 0.025
WALL_Y_LEVELS = 80
WALL_THETA = 288
FLOOR_RADII = 16
FLOOR_RADIUS_MAX = 0.30
FIT_Y_LEVELS = 20
FIT_THETA = 180
FIT_ITERATIONS = 3


def load_mesh(path):
    mesh = trimesh.load(path, force="mesh", process=False)
    return mesh


def triangle_arrays(mesh):
    v = np.asarray(mesh.vertices, dtype=np.float64)
    f = np.asarray(mesh.faces, dtype=np.int64)
    tri = v[f]
    return v, f, tri


def first_hit(origins, directions, v0, e1, e2, candidates=None, chunk=128, eps=1e-7):
    origins = np.asarray(origins, dtype=np.float64)
    directions = np.asarray(directions, dtype=np.float64)
    directions = directions / np.linalg.norm(directions, axis=1, keepdims=True)
    if candidates is not None:
        v0 = v0[candidates]
        e1 = e1[candidates]
        e2 = e2[candidates]
    r = len(origins)
    n = len(v0)
    best_t = np.full(r, np.inf)
    best_f = np.full(r, -1, dtype=np.int64)
    if n == 0:
        return best_f, best_t
    for s in range(0, r, chunk):
        e = min(r, s + chunk)
        o = origins[s:e][:, None, :]
        d = directions[s:e][:, None, :]
        pvec = np.cross(d, e2[None, :, :])
        det = np.sum(pvec * e1[None, :, :], axis=2)
        inv = np.zeros_like(det)
        ok = np.abs(det) > eps
        inv[ok] = 1.0 / det[ok]
        tvec = o - v0[None, :, :]
        u = np.sum(tvec * pvec, axis=2) * inv
        ok &= (u >= 0.0) & (u <= 1.0)
        qvec = np.cross(tvec, e1[None, :, :])
        v = np.sum(d * qvec, axis=2) * inv
        ok &= (v >= 0.0) & (u + v <= 1.0)
        t = np.sum(e2[None, :, :] * qvec, axis=2) * inv
        ok &= t > eps
        t_masked = np.where(ok, t, np.inf)
        idx = np.argmin(t_masked, axis=1)
        tmin = t_masked[np.arange(len(idx)), idx]
        upd = tmin < best_t[s:e]
        best_t[s:e][upd] = tmin[upd]
        if candidates is not None:
            best_f[s:e][upd] = candidates[idx[upd]]
        else:
            best_f[s:e][upd] = idx[upd]
    return best_f, best_t


def kasa_circle(x, z):
    a = np.column_stack([2.0 * x, 2.0 * z, np.ones_like(x)])
    b = x * x + z * z
    sol, *_ = np.linalg.lstsq(a, b, rcond=None)
    cx, cz, c = sol
    radius = float(np.sqrt(max(c + cx * cx + cz * cz, 0.0)))
    return float(cx), float(cz), radius


def fit_axis(mesh, v0, e1, e2, cx=0.0, cz=0.0, verbose=True):
    ys = np.linspace(-0.45, 0.60, FIT_Y_LEVELS)
    thetas = np.linspace(0.0, 2.0 * np.pi, FIT_THETA, endpoint=False)
    for iteration in range(FIT_ITERATIONS):
        origins = []
        directions = []
        for y in ys:
            for t in thetas:
                origins.append([cx, y, cz])
                directions.append([np.cos(t), 0.0, np.sin(t)])
        face, dist = first_hit(np.array(origins), np.array(directions), v0, e1, e2)
        centers = []
        for i, _y in enumerate(ys):
            m = slice(i * FIT_THETA, (i + 1) * FIT_THETA)
            d = dist[m]
            good = np.isfinite(d)
            if good.sum() < FIT_THETA // 2:
                continue
            dd = d[good]
            tt = thetas[good]
            med = np.median(dd)
            keep = (dd > 0.6 * med) & (dd < 1.4 * med)
            if keep.sum() < 20:
                continue
            hx = dd[keep] * np.cos(tt[keep]) + cx
            hz = dd[keep] * np.sin(tt[keep]) + cz
            centers.append(kasa_circle(hx, hz))
        if not centers:
            break
        arr = np.array(centers)
        new_cx = float(np.median(arr[:, 0]))
        new_cz = float(np.median(arr[:, 1]))
        if verbose:
            print(f"[axis] iter {iteration + 1}: cx={new_cx:+.4f} cz={new_cz:+.4f} "
                  f"residual_med={np.median(arr[:, 2]):.4f}")
        if abs(new_cx - cx) < 1e-4 and abs(new_cz - cz) < 1e-4:
            cx, cz = new_cx, new_cz
            break
        cx, cz = new_cx, new_cz
    return cx, cz


def profile_table(mesh, v0, e1, e2, cx, cz):
    ys = np.linspace(-0.62, Y_CAP, 13)
    thetas = np.linspace(0.0, 2.0 * np.pi, 180, endpoint=False)
    origins = []
    directions = []
    for y in ys:
        for t in thetas:
            origins.append([cx, y, cz])
            directions.append([np.cos(t), 0.0, np.sin(t)])
    face, dist = first_hit(np.array(origins), np.array(directions), v0, e1, e2)
    rows = []
    for i, y in enumerate(ys):
        m = slice(i * 180, (i + 1) * 180)
        d = dist[m]
        good = np.isfinite(d)
        if good.sum():
            rows.append((float(y), float(d[good].min()), float(np.median(d[good])), float(d[good].max())))
    return rows


def select_wall(mesh, v0, e1, e2, cx, cz):
    tri_y = mesh.triangles[:, :, 1]
    tri_ymin = tri_y.min(axis=1)
    tri_ymax = tri_y.max(axis=1)
    base = np.linspace(-0.625, Y_CAP, WALL_Y_LEVELS)
    mid = (base[:-1] + base[1:]) / 2.0
    ys = np.unique(np.concatenate([base, mid]))
    thetas = np.linspace(0.0, 2.0 * np.pi, WALL_THETA, endpoint=False)
    selected = set()
    for y in ys:
        cand = np.nonzero((tri_ymin <= y + 1e-6) & (tri_ymax >= y - 1e-6))[0]
        if len(cand) == 0:
            continue
        origins = np.array([[cx, y, cz]] * WALL_THETA)
        directions = np.column_stack([np.cos(thetas), np.zeros(WALL_THETA), np.sin(thetas)])
        face, dist = first_hit(origins, directions, v0, e1, e2, candidates=cand)
        for f in face:
            if f >= 0:
                selected.add(int(f))
    return selected


def select_floor(mesh, v0, e1, e2, cx, cz):
    selected = set()
    origins = []
    directions = []
    for r in np.linspace(0.0, FLOOR_RADIUS_MAX, FLOOR_RADII):
        count = max(1, int(2.0 * np.pi * r / 0.02))
        for a in np.linspace(0.0, 2.0 * np.pi, count, endpoint=False):
            origins.append([cx + r * np.cos(a), 0.0, cz + r * np.sin(a)])
            directions.append([0.0, -1.0, 0.0])
    face, dist = first_hit(np.array(origins), np.array(directions), v0, e1, e2)
    for f in face:
        if f >= 0:
            selected.add(int(f))
    return selected


def filter_rim_faces(mesh, selected):
    centroid = mesh.triangles.mean(axis=1)
    normals = mesh.face_normals
    kept = set()
    for f in selected:
        y = centroid[f, 1]
        ny = normals[f, 1]
        if y > 0.66 and ny > 0.45:
            continue
        kept.add(f)
    return kept


def expand_interior(mesh, selected, cx, cz):
    adjacency = mesh.face_adjacency
    neighbors = {}
    for a, b in adjacency:
        neighbors.setdefault(int(a), []).append(int(b))
        neighbors.setdefault(int(b), []).append(int(a))
    centroid = mesh.triangles.mean(axis=1)
    normals = mesh.face_normals
    radial = np.column_stack([centroid[:, 0] - cx, np.zeros(len(centroid)), centroid[:, 2] - cz])
    radial_len = np.linalg.norm(radial, axis=1)
    radial_unit = radial / np.where(radial_len < 1e-9, 1e-9, radial_len)[:, None]
    inward_dot = normals[:, 0] * radial_unit[:, 0] + normals[:, 2] * radial_unit[:, 2]
    radius = np.hypot(centroid[:, 0] - cx, centroid[:, 2] - cz)
    y = centroid[:, 1]
    ny = normals[:, 1]
    x = centroid[:, 0]

    def eligible(f):
        if y[f] > Y_CAP + 1e-6:
            return False
        if x[f] >= 0.60 or radius[f] > 0.70:
            return False
        if y[f] > 0.66 and ny[f] > 0.45:
            return False
        if inward_dot[f] < 0.25:
            return True
        if y[f] < -0.45 and ny[f] > 0.35 and radius[f] < 0.35:
            return True
        return False

    visited = set(selected)
    queue = list(selected)
    added = 0
    while queue:
        f = queue.pop()
        for n in neighbors.get(f, ()):
            if n in visited:
                continue
            if eligible(n):
                visited.add(n)
                queue.append(n)
                added += 1
    return visited, added


def clip_below(triangles, y_cap):
    out = []
    for tri in triangles:
        ys = tri[:, 1]
        inside = ys <= y_cap
        if inside.all():
            out.append(tri)
            continue
        if not inside.any():
            continue
        poly = [tri[0], tri[1], tri[2]]
        clipped = []
        for i in range(3):
            cur = poly[i]
            nxt = poly[(i + 1) % 3]
            cur_in = cur[1] <= y_cap
            nxt_in = nxt[1] <= y_cap
            if cur_in:
                clipped.append(cur)
            if cur_in != nxt_in:
                t = (y_cap - cur[1]) / (nxt[1] - cur[1])
                clipped.append(cur + t * (nxt - cur))
        for i in range(1, len(clipped) - 1):
            out.append(np.array([clipped[0], clipped[i], clipped[i + 1]], dtype=np.float64))
    return np.array(out) if out else np.zeros((0, 3, 3), dtype=np.float64)


def weld(triangles):
    vertices = triangles.reshape(-1, 3)
    faces = np.arange(len(vertices), dtype=np.int64).reshape(-1, 3)
    mesh = trimesh.Trimesh(vertices=vertices, faces=faces, process=False)
    mesh.merge_vertices()
    mesh.update_faces(mesh.nondegenerate_faces())
    mesh.remove_unreferenced_vertices()
    return mesh


def compute_uv(vertices, cx, cz, seam_rad, pole_radius, y_min, y_max):
    x = vertices[:, 0] - cx
    z = vertices[:, 2] - cz
    radius = np.hypot(x, z)
    angle = np.arctan2(z, x) - seam_rad
    u = (angle / (2.0 * np.pi)) % 1.0
    v = (vertices[:, 1] - y_min) / max(y_max - y_min, 1e-9)
    v = np.clip(v, 0.0, 1.0)
    pole = radius < pole_radius
    u[pole] = np.nan
    return u, v, pole


def unwrap_face(us):
    order = np.argsort(us)
    sorted_u = us[order]
    extended = np.concatenate([sorted_u, sorted_u[:1] + 1.0])
    gaps = np.diff(extended)
    k = int(np.argmax(gaps))
    if k == len(gaps) - 1:
        return us, False
    shifted = us.copy()
    shifted[order[:k + 1]] += 1.0
    return shifted, True


def build_uv_faces(u, v, faces, pole):
    final_verts = []
    final_uv = []
    final_faces = []
    base_index = {}
    for f in faces:
        tri = [int(i) for i in f]
        tri_pole = any(pole[i] for i in tri)
        us = np.array([u[i] for i in tri], dtype=np.float64)
        if tri_pole:
            known = us[~np.isnan(us)]
            fill = float(np.mean(known)) if len(known) else 0.5
            us = np.where(np.isnan(us), fill, us)
        us, crossing = unwrap_face(us)
        needs_split = tri_pole or crossing
        if needs_split:
            local = []
            for k in range(3):
                local.append(len(final_verts))
                final_verts.append(tri[k])
                final_uv.append([us[k], v[tri[k]]])
            final_faces.append(local)
        else:
            local = []
            for k in range(3):
                key = tri[k]
                if key not in base_index:
                    base_index[key] = len(final_verts)
                    final_verts.append(key)
                    final_uv.append([u[key], v[key]])
                local.append(base_index[key])
            final_faces.append(local)
    return np.array(final_verts, dtype=np.int64), np.array(final_uv, dtype=np.float64), np.array(final_faces, dtype=np.int64)


def build_overlay(mesh, selected, cx, cz):
    triangles = mesh.triangles[list(selected)]
    clipped = clip_below(triangles, Y_CAP)
    if len(clipped) == 0:
        raise RuntimeError("no interior geometry survived clipping")
    sheet = weld(clipped)
    vertices = np.asarray(sheet.vertices, dtype=np.float64)
    faces = np.asarray(sheet.faces, dtype=np.int64)

    normals = np.asarray(sheet.vertex_normals, dtype=np.float64)
    radial = vertices.copy()
    radial[:, 0] -= cx
    radial[:, 2] -= cz
    radial[:, 1] = 0.0
    radial_len = np.linalg.norm(radial, axis=1)
    radial_unit = radial / np.where(radial_len < 1e-9, 1e-9, radial_len)[:, None]
    radial_dot = np.sum(normals * radial_unit, axis=1)
    if np.median(radial_dot) > 0.0:
        raise RuntimeError("interior normals do not point toward the cavity")

    uv_u, uv_v, pole = compute_uv(
        vertices, cx, cz, np.radians(SEAM_ANGLE_DEG), POLE_RADIUS,
        float(vertices[:, 1].min()), Y_CAP)
    src_index, uv, out_faces = build_uv_faces(uv_u, uv_v, faces, pole)
    out_positions = vertices[src_index] + normals[src_index] * OFFSET
    out_positions[:, 1] = np.minimum(out_positions[:, 1], Y_CAP)

    visual = trimesh.visual.TextureVisuals(
        uv=uv,
        material=trimesh.visual.material.PBRMaterial(
            name="coffee_cup_inner",
            baseColorFactor=[255, 255, 255, 255],
            metallicFactor=0.0,
            roughnessFactor=1.0,
        ),
    )
    overlay = trimesh.Trimesh(vertices=out_positions, faces=out_faces, visual=visual, process=False)
    stats = {
        "sheet_vertices": len(vertices),
        "sheet_faces": len(faces),
        "output_vertices": len(out_positions),
        "output_faces": len(out_faces),
        "uv_min": float(uv.min()),
        "uv_max": float(uv.max()),
    }
    return overlay, stats


def validate(src, out, cx, cz, selected_count, overlay, stats):
    report = {}
    v = np.asarray(overlay.vertices, dtype=np.float64)
    f = np.asarray(overlay.faces, dtype=np.int64)
    report["output_vertices"] = int(len(v))
    report["output_triangles"] = int(len(f))
    report["y_min"] = float(v[:, 1].min())
    report["y_max"] = float(v[:, 1].max())
    radial = np.hypot(v[:, 0] - cx, v[:, 2] - cz)
    report["radius_max"] = float(radial.max())
    report["x_max"] = float(v[:, 0].max())
    report["finite"] = bool(np.isfinite(v).all() and np.isfinite(overlay.visual.uv).all())
    report["uv_min"] = float(overlay.visual.uv.min())
    report["uv_max"] = float(overlay.visual.uv.max())
    report["selected_source_faces"] = int(selected_count)
    report["y_cap_respected"] = bool(report["y_max"] <= Y_CAP + 1e-4)
    report["below_rim"] = bool(report["y_max"] < 0.71)
    report["handle_excluded"] = bool(report["x_max"] < 0.60)
    report["rim_radius_excluded"] = bool(report["radius_max"] < 0.72)
    return report


def coverage_check(mesh, v0, e1, e2, cx, cz, n_y, n_theta):
    tri_y = mesh.triangles[:, :, 1]
    tri_ymin = tri_y.min(axis=1)
    tri_ymax = tri_y.max(axis=1)
    ys = np.linspace(-0.615, Y_CAP - 0.005, n_y)
    thetas = np.linspace(0.0, 2.0 * np.pi, n_theta, endpoint=False)
    wall_hits = set()
    for y in ys:
        cand = np.nonzero((tri_ymin <= y + 1e-6) & (tri_ymax >= y - 1e-6))[0]
        if len(cand) == 0:
            continue
        origins = np.array([[cx, y, cz]] * len(thetas))
        directions = np.column_stack([np.cos(thetas), np.zeros(len(thetas)), np.sin(thetas)])
        face, _ = first_hit(origins, directions, v0, e1, e2, candidates=cand)
        wall_hits.update(int(f) for f in face if f >= 0)
    origins = []
    directions = []
    for r in np.linspace(0.0, FLOOR_RADIUS_MAX, 20):
        count = max(1, int(2.0 * np.pi * r / 0.015))
        for a in np.linspace(0.0, 2.0 * np.pi, count, endpoint=False):
            origins.append([cx + r * np.cos(a), 0.0, cz + r * np.sin(a)])
            directions.append([0.0, -1.0, 0.0])
    face, _ = first_hit(np.array(origins), np.array(directions), v0, e1, e2)
    floor_hits = {int(f) for f in face if f >= 0}
    centroid = mesh.triangles.mean(axis=1)
    normals = mesh.face_normals
    rim_like = (centroid[:, 1] > 0.66) & (normals[:, 1] > 0.45)
    wall_hits = {f for f in wall_hits if not rim_like[f]}
    floor_hits = {f for f in floor_hits if not rim_like[f]}
    return wall_hits, floor_hits


def run_extraction(src, out):
    print(f"[load] {src}")
    mesh = load_mesh(src)
    v0, e1, e2 = _face_arrays(mesh)
    print(f"[mesh] vertices={len(mesh.vertices)} faces={len(mesh.faces)}")
    t0 = time.time()
    cx, cz = fit_axis(mesh, v0, e1, e2)
    print(f"[axis] fitted center x={cx:+.4f} z={cz:+.4f}")
    for row in profile_table(mesh, v0, e1, e2, cx, cz):
        print(f"[profile] y={row[0]:+.3f} r={row[1]:.3f}/{row[2]:.3f}/{row[3]:.3f}")
    wall = select_wall(mesh, v0, e1, e2, cx, cz)
    floor = select_floor(mesh, v0, e1, e2, cx, cz)
    combined = filter_rim_faces(mesh, wall | floor)
    combined, expanded = expand_interior(mesh, combined, cx, cz)
    combined = filter_rim_faces(mesh, combined)
    print(f"[select] wall={len(wall)} floor={len(floor)} expanded={expanded} "
          f"selected={len(combined)} ({time.time() - t0:.1f}s)")

    fill_wall, fill_floor = coverage_check(mesh, v0, e1, e2, cx, cz, 53, 200)
    added = (fill_wall | fill_floor) - combined
    combined = filter_rim_faces(mesh, combined | added)
    combined, extra = expand_interior(mesh, combined, cx, cz)
    combined = filter_rim_faces(mesh, combined)
    print(f"[fill] added={len(added)} expanded={extra} selected={len(combined)}")

    ver_wall, ver_floor = coverage_check(mesh, v0, e1, e2, cx, cz, 71, 257)
    wm = ver_wall - combined
    fm = ver_floor - combined
    wcov = 100.0 * (len(ver_wall) - len(wm)) / max(len(ver_wall), 1)
    fcov = 100.0 * (len(ver_floor) - len(fm)) / max(len(ver_floor), 1)
    print(f"[verify] wall={wcov:.2f}% ({len(wm)}/{len(ver_wall)}) "
          f"floor={fcov:.2f}% ({len(fm)}/{len(ver_floor)})")
    if wm:
        print(f"[verify] wall missed sample={sorted(wm)[:20]}")
    if fm:
        print(f"[verify] floor missed sample={sorted(fm)[:20]}")
    overlay, stats = build_overlay(mesh, combined, cx, cz)
    overlay.export(out)
    print(f"[export] {out} verts={stats['output_vertices']} tris={stats['output_faces']} "
          f"uv=[{stats['uv_min']:.3f},{stats['uv_max']:.3f}]")
    report = validate(src, out, cx, cz, len(combined), overlay, stats)
    print("[report]")
    for k in sorted(report):
        print(f"  {k} = {report[k]}")
    print(f"[done] axis=({cx:+.4f},{cz:+.4f}) source_sha256="
          f"{hashlib.sha256(open(src, 'rb').read()).hexdigest()[:16]}")
    return report


def _face_arrays(mesh):
    v = np.asarray(mesh.vertices, dtype=np.float64)
    f = np.asarray(mesh.faces, dtype=np.int64)
    tri = v[f]
    v0 = tri[:, 0]
    v1 = tri[:, 1]
    v2 = tri[:, 2]
    e1 = v1 - v0
    e2 = v2 - v0
    return v0, e1, e2


def validate_only(out):
    mesh = trimesh.load(out, force="mesh", process=False)
    v = np.asarray(mesh.vertices, dtype=np.float64)
    f = np.asarray(mesh.faces, dtype=np.int64)
    uv = mesh.visual.uv if hasattr(mesh.visual, "uv") else None
    print(f"[validate] vertices={len(v)} triangles={len(f)}")
    print(f"[validate] y=[{v[:,1].min():.4f},{v[:,1].max():.4f}] "
          f"x=[{v[:,0].min():.4f},{v[:,0].max():.4f}] z=[{v[:,2].min():.4f},{v[:,2].max():.4f}]")
    if uv is not None:
        print(f"[validate] uv=[{uv.min():.4f},{uv.max():.4f}] finite={np.isfinite(uv).all()}")
    else:
        print("[validate] UV MISSING")


def selftest():
    ok = True

    def check(name, cond):
        nonlocal ok
        print(f"[selftest] {name}: {'PASS' if cond else 'FAIL'}")
        ok = ok and cond

    rng = np.random.default_rng(7)
    theta = rng.uniform(0.0, 2.0 * np.pi, 400)
    true_cx, true_cz, true_r = 0.31, -0.12, 0.55
    x = true_cx + true_r * np.cos(theta)
    z = true_cz + true_r * np.sin(theta)
    cx, cz, r = kasa_circle(x, z)
    check("axis fit recovers center", abs(cx - true_cx) < 1e-6 and abs(cz - true_cz) < 1e-6)
    check("axis fit recovers radius", abs(r - true_r) < 1e-6)

    tri = np.array([[0.0, 0.0, 0.0], [1.0, 0.0, 0.0], [0.0, 0.0, 1.0]])
    clipped = clip_below(np.array([tri]), 0.5)
    check("clip keeps fully-inside triangle", len(clipped) == 1)
    clipped = clip_below(np.array([tri]), -0.5)
    check("clip drops fully-outside triangle", len(clipped) == 0)
    tri2 = np.array([[0.0, 0.0, 0.0], [1.0, 0.0, 0.0], [0.0, 2.0, 0.0]])
    clipped = clip_below(np.array([tri2]), 1.0)
    check("clip splits crossing triangle", len(clipped) >= 1 and clipped[:, :, 1].max() <= 1.0 + 1e-9)

    verts = np.array([[1.0, 0.0, 0.0], [0.0, 0.0, 1.0], [1.0, 0.0, 1.0]])
    u, v, pole = compute_uv(verts, 0.0, 0.0, 0.0, 0.01, 0.0, 1.0)
    check("uv finite away from pole", np.isfinite(u).all())
    check("uv in range", u.min() >= 0.0 and u.max() <= 1.0)

    faces = np.array([[0, 1, 2]])
    src, uv, out_faces = build_uv_faces(np.array([0.99, 0.01, 0.5]), np.array([0.0, 0.5, 1.0]),
                                        faces, np.array([False, False, False]))
    check("seam face duplicated", len(src) == 3)
    check("seam u unwrapped", uv[:, 0].max() - uv[:, 0].min() < 0.6)

    us, crossing = unwrap_face(np.array([0.90, 0.95, 0.05]))
    check("largest-gap unwrap crossing", crossing and (us.max() - us.min()) < 0.5)
    us2, crossing2 = unwrap_face(np.array([0.10, 0.20, 0.30]))
    check("largest-gap unwrap non-crossing", (not crossing2) and abs(us2.max() - us2.min() - 0.20) < 1e-9)

    if not ok:
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--src", default=DEFAULT_SRC)
    parser.add_argument("--out", default=DEFAULT_OUT)
    parser.add_argument("--selftest", action="store_true")
    parser.add_argument("--validate-only", action="store_true")
    args = parser.parse_args()
    if args.selftest:
        selftest()
        return
    if args.validate_only:
        validate_only(args.out)
        return
    run_extraction(args.src, args.out)


if __name__ == "__main__":
    main()
