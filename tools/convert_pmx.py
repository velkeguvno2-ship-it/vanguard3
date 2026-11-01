"""PMX -> .mesh (v2, skeletal) converter.

Usage: python convert_pmx.py <model.pmx> <out.mesh>

Output is in Minecraft space (front = +Z, 1 unit = 1 block): the PMX (front = -Z, left-handed) is mirrored on Z
and scaled by 1/8. The file stores the bone hierarchy, the idle (standing) pose of every bone as a quaternion,
and the triangles of every bone relative to its pivot. The renderer applies the walk animation on top of it.

Edit IDLE to change the standing pose. Values are degrees (rx, ry, rz); ("W", rx, ry, rz) = absolute world orientation.
If the mob faces backwards in game, set FLIP_FRONT = True and re-run.
"""
import struct, sys, math
import numpy as np
import pmx

SCALE = 0.125
FLIP_FRONT = False
IDLE = {
    "J_R_Arm":    ("L", 61.7, 0, -30.9),
    "Shield":     ("W", -81.9, 177.6, 187),
    "J_L_Arm":    ("L", -22, -3, 53),
    "J_L_Weapon": ("W", 1, 5.7, 31.2),
    "J_R_Leg":    ("L", 3, 0, -2),
    "J_L_Leg":    ("L", -11, 0, 2),
    "J_Neck":     ("L", -4, 0, 0),
}

def rot3(rx, ry, rz):
    a, b, c = map(math.radians, (rx, ry, rz))
    Rx = np.array([[1,0,0],[0,math.cos(a),-math.sin(a)],[0,math.sin(a),math.cos(a)]])
    Ry = np.array([[math.cos(b),0,math.sin(b)],[0,1,0],[-math.sin(b),0,math.cos(b)]])
    Rz = np.array([[math.cos(c),-math.sin(c),0],[math.sin(c),math.cos(c),0],[0,0,1]])
    return Rz @ Ry @ Rx

def quat(R):
    t = R[0,0] + R[1,1] + R[2,2]
    if t > 0:
        s = math.sqrt(t + 1.0) * 2
        return ((R[2,1]-R[1,2])/s, (R[0,2]-R[2,0])/s, (R[1,0]-R[0,1])/s, 0.25*s)
    i = int(np.argmax([R[0,0], R[1,1], R[2,2]]))
    j, k = (i+1) % 3, (i+2) % 3
    s = math.sqrt(1.0 + R[i,i] - R[j,j] - R[k,k]) * 2
    q = [0, 0, 0, 0]
    q[i] = 0.25*s
    q[j] = (R[j,i]+R[i,j])/s
    q[k] = (R[k,i]+R[i,k])/s
    q[3] = (R[k,j]-R[j,k])/s
    return tuple(q)

def utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b

m = pmx.load(sys.argv[1])
B = m["B"]
n = len(B)
Rw = [None]*n          # world rotation (PMX space)
Rl = [None]*n          # local rotation (PMX space)
for i, (name, p, par) in enumerate(B):
    v = IDLE.get(name)
    if v is None: loc = np.eye(3)
    elif v[0] == "W":
        t = rot3(*v[1:])
        loc = t if par < 0 else Rw[par].T @ t
    else: loc = rot3(*v[1:])
    Rl[i] = loc
    Rw[i] = loc if par < 0 else Rw[par] @ loc

MIR = np.diag([1.0, 1.0, -1.0])
FLIP = np.diag([-1.0, 1.0, -1.0]) if FLIP_FRONT else np.eye(3)   # 180 deg about Y, applied to the root bone

def mc_point(p):
    return MIR @ np.array(p) * SCALE

out = bytearray(struct.pack(">i", n))
for i, (name, p, par) in enumerate(B):
    Rmc = MIR @ Rl[i] @ MIR
    if par < 0: Rmc = FLIP @ Rmc
    px, py, pz = mc_point(p)
    out += utf(name) + struct.pack(">i3f4f", par, px, py, pz, *quat(Rmc))

# triangles grouped by (bone, material)
parts = {}
o = 0
for mi, (mname, tex, cnt, _) in enumerate(m["M"]):
    for t in range(cnt // 3):
        i0, i1, i2 = m["F"][o+t*3:o+t*3+3]
        bone = m["V"][i0][3][0]
        parts.setdefault((bone, tex), []).append((i0, i2, i1))   # reversed winding (mirrored axis)
    o += cnt
out += struct.pack(">i", len(parts))
for (bone, tex), tris in sorted(parts.items()):
    out += struct.pack(">iii", bone, tex, len(tris))
    pv = np.array(B[bone][1])
    for tri in tris:
        for i in tri:
            pos, nor, uv, _, _ = m["V"][i]
            x, y, z = (MIR @ (np.array(pos) - pv)) * SCALE
            nx, ny, nz = MIR @ np.array(nor)
            out += struct.pack(">8f", x, y, z, nx, ny, nz, uv[0], uv[1])
open(sys.argv[2], "wb").write(out)
print("wrote", len(out), "bytes,", n, "bones,", len(parts), "parts")
