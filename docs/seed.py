#!/usr/bin/env python3
"""Batch seed script for PLM platform. Run from repo root: python docs/seed.py"""

import json, sys
import urllib.request
import urllib.parse
import urllib.error

BASE = "http://localhost/api"
KC   = "http://localhost:8081"

def post(url, data, headers={}):
    body = json.dumps(data).encode()
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json", **headers}, method="POST")
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"  ERROR {e.code} on POST {url}: {e.read().decode()}")
        return {}

# ─── Token ───────────────────────────────────────────────────────────────────

print("==> Getting Keycloak token...")
params = urllib.parse.urlencode({
    "grant_type": "password", "client_id": "plm-frontend",
    "username": "admin", "password": "admin123"
}).encode()
req = urllib.request.Request(f"{KC}/realms/plm/protocol/openid-connect/token", data=params)
try:
    with urllib.request.urlopen(req) as r:
        token = json.loads(r.read())["access_token"]
except Exception as e:
    print(f"ERROR: Could not get token — {e}")
    sys.exit(1)

AUTH = {"Authorization": f"Bearer {token}"}
print("  Token acquired.\n")

# ─── Step 1: Items ───────────────────────────────────────────────────────────

print("==> Creating items...")

def create_item(number, name, desc, state):
    r = post(f"{BASE}/items", {"itemNumber": number, "name": name, "description": desc, "lifecycleState": state}, AUTH)
    id_ = r.get("id")
    print(f"  [{id_}] {number} — {name}")
    return id_

ID_BAT_PACK  = create_item("EV-BAT-001",     "Electric Vehicle Battery Pack",   "72 kWh lithium-ion battery pack assembly for EV platform",           "RELEASED")
ID_BAT_MOD   = create_item("EV-BAT-MOD-001", "Battery Module 18S4P",            "Individual battery module — 18 cells in series, 4 in parallel",      "RELEASED")
ID_BMS       = create_item("EV-BMS-001",     "Battery Management System",       "BMS PCB — cell balancing, SOC estimation, thermal management",        "RELEASED")
ID_COOLING   = create_item("EV-COOL-001",    "Cooling Plate Assembly",          "Aluminium liquid cooling plate for battery thermal regulation",       "IN_REVIEW")
ID_HOUSING   = create_item("EV-HSG-001",     "Battery Pack Housing",            "Die-cast aluminium enclosure — IP67, crash-rated",                   "RELEASED")
ID_MOTOR     = create_item("EV-MOT-001",     "Rear Drive Motor Assembly",       "250 kW permanent magnet synchronous motor with integrated inverter",  "RELEASED")
ID_INVERTER  = create_item("EV-INV-001",     "Power Inverter Module",           "SiC MOSFET 3-phase inverter — 800V bus, 400A peak",                  "RELEASED")
ID_STATOR    = create_item("EV-MOT-STA-001", "Motor Stator Assembly",           "Hairpin wound stator — 48 slots, 8 poles",                           "RELEASED")
ID_ROTOR     = create_item("EV-MOT-ROT-001", "Motor Rotor Assembly",            "Interior permanent magnet rotor — NdFeB magnets",                    "DRAFT")
ID_CHASSIS   = create_item("EV-CHF-001",     "Chassis Frame",                   "Skateboard platform chassis — high-strength steel + aluminium",       "RELEASED")
ID_OBC       = create_item("EV-OBC-001",     "On-Board Charger",                "11 kW AC on-board charger with CCS2 DC fast-charge support",          "IN_REVIEW")
ID_DCDC      = create_item("EV-DCDC-001",    "DC/DC Converter",                 "800V to 12V DC/DC converter — 3 kW continuous",                      "RELEASED")

print()

# ─── Step 2: Revisions ───────────────────────────────────────────────────────

print("==> Creating revisions...")

def create_rev(item_id, code, status):
    r = post(f"{BASE}/items/{item_id}/revisions", {"revisionCode": code, "status": status}, AUTH)
    id_ = r.get("id")
    print(f"  [{id_}] item={item_id} rev={code} status={status}")
    return id_

REV_BAT_A  = create_rev(ID_BAT_PACK,  "A", "RELEASED")
REV_BAT_B  = create_rev(ID_BAT_PACK,  "B", "RELEASED")
REV_MOD_A  = create_rev(ID_BAT_MOD,   "A", "RELEASED")
REV_BMS_A  = create_rev(ID_BMS,       "A", "RELEASED")
REV_BMS_B  = create_rev(ID_BMS,       "B", "IN_REVIEW")
REV_COOL_A = create_rev(ID_COOLING,   "A", "IN_WORK")
REV_HSG_A  = create_rev(ID_HOUSING,   "A", "RELEASED")
REV_MOT_A  = create_rev(ID_MOTOR,     "A", "RELEASED")
REV_INV_A  = create_rev(ID_INVERTER,  "A", "RELEASED")
REV_STA_A  = create_rev(ID_STATOR,    "A", "RELEASED")
REV_ROT_A  = create_rev(ID_ROTOR,     "A", "IN_WORK")
REV_CHA_A  = create_rev(ID_CHASSIS,   "A", "RELEASED")
REV_OBC_A  = create_rev(ID_OBC,       "A", "IN_REVIEW")
REV_DCD_A  = create_rev(ID_DCDC,      "A", "RELEASED")

print()

# ─── Step 3: BOM ─────────────────────────────────────────────────────────────

print("==> Building BOM...")

def add_bom(parent_id, child_id, qty):
    post(f"{BASE}/revisions/{parent_id}/bom/children", {"childRevisionId": child_id, "quantity": qty}, AUTH)
    print(f"  parent={parent_id} -> child={child_id} qty={qty}")

# Battery Pack Rev B
add_bom(REV_BAT_B, REV_MOD_A,  12)
add_bom(REV_BAT_B, REV_BMS_A,   1)
add_bom(REV_BAT_B, REV_COOL_A,  1)
add_bom(REV_BAT_B, REV_HSG_A,   1)

# Motor Assembly Rev A
add_bom(REV_MOT_A, REV_STA_A,  1)
add_bom(REV_MOT_A, REV_ROT_A,  1)
add_bom(REV_MOT_A, REV_INV_A,  1)

print()

# ─── Step 4: Change Requests ─────────────────────────────────────────────────

print("==> Creating change requests...")

def create_cr(title, desc, status, item_id):
    r = post(f"{BASE}/change-requests", {"title": title, "description": desc, "status": status, "linkedItemId": item_id}, AUTH)
    id_ = r.get("id")
    print(f"  [{id_}] {title[:60]}")
    return id_

create_cr(
    "BMS firmware update — SOC algorithm improvement",
    "Update SOC estimation to ML-based model. Reduces error from 3% to 1%. Requires new PCB layout for additional MCU flash.",
    "IN_REVIEW", ID_BMS)

create_cr(
    "Cooling plate channel redesign for improved flow rate",
    "CFD analysis shows hotspots on cells 14-17 under 3C discharge. Redesign serpentine channels to equalise flow. Target: max delta-T < 3C.",
    "OPEN", ID_COOLING)

create_cr(
    "Rotor magnet supplier qualification — secondary source",
    "Qualify second NdFeB magnet supplier to mitigate supply chain risk. Samples show equivalent magnetic properties. Requires motor dyno validation.",
    "OPEN", ID_ROTOR)

create_cr(
    "OBC Rev A design freeze and release",
    "On-board charger completed EMC pre-compliance testing (CISPR 25 Class 5). Final DV test report ready. Approved for production release.",
    "IN_REVIEW", ID_OBC)

create_cr(
    "Chassis frame mass reduction — topology optimisation",
    "FEA-guided topology optimisation identifies 8 kg saving in front subframe. Proposal to switch 4 brackets from stamped steel to HPDC aluminium.",
    "OPEN", ID_CHASSIS)

print()
print("==> Seed complete!")
print("    12 items | 14 revisions | 7 BOM links | 5 change requests")
print("    Open http://localhost to explore the data.")
