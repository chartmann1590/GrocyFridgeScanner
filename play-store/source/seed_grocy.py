"""
Seed the Grocy instance with realistic inventory items for Play Store screenshots,
then optionally clean them up.

Usage:
    python seed_grocy.py seed    # adds items, prints list of added IDs
    python seed_grocy.py cleanup # deletes items recorded in created_ids.json

The script only touches IDs it created itself (tracked in created_ids.json next to this file).
The pre-existing 'Chicken Corn Chowder' product is left alone.
"""
import json
import os
import sys
import time
from datetime import datetime, timedelta, timezone

import requests

URL = os.environ.get("GROCY_URL") or "https://your-grocy.example.com"
KEY = os.environ.get("GROCY_API_KEY") or ""
if not KEY or URL.startswith("https://your-grocy"):
    sys.exit("Set GROCY_URL and GROCY_API_KEY environment variables before running.")
HEADERS = {"GROCY-API-KEY": KEY, "Content-Type": "application/json"}
STATE_FILE = os.path.join(os.path.dirname(__file__), "created_ids.json")

# (name, location_label, qu_id, stock_amount)
# location_label: "Fridge" or "Cupboards" — created/looked up at runtime
# qu_id: 2=Piece, 3=Pack
ITEMS = [
    # Fridge
    ("Whole Milk", "Fridge", 3, 1),
    ("Large Eggs", "Fridge", 2, 12),
    ("Greek Yogurt", "Fridge", 3, 4),
    ("Cheddar Cheese", "Fridge", 3, 1),
    ("Orange Juice", "Fridge", 3, 1),
    ("Butter", "Fridge", 3, 2),
    ("Baby Spinach", "Fridge", 3, 1),
    ("Strawberries", "Fridge", 3, 2),
    # Cupboards
    ("Pasta", "Cupboards", 3, 3),
    ("Long Grain Rice", "Cupboards", 3, 1),
    ("Cereal", "Cupboards", 3, 2),
    ("Tomato Sauce", "Cupboards", 3, 4),
    ("Olive Oil", "Cupboards", 3, 1),
    ("Black Beans", "Cupboards", 3, 3),
    ("Peanut Butter", "Cupboards", 3, 1),
    ("Honey", "Cupboards", 3, 1),
]


def get_or_create_location(name: str) -> int:
    r = requests.get(f"{URL}/api/objects/locations", headers=HEADERS, timeout=15)
    r.raise_for_status()
    for loc in r.json():
        if loc["name"].lower() == name.lower():
            return loc["id"]
    r = requests.post(
        f"{URL}/api/objects/locations",
        headers=HEADERS,
        data=json.dumps({"name": name}),
        timeout=15,
    )
    r.raise_for_status()
    return r.json()["created_object_id"]


def create_product(name: str, location_id: int, qu_id: int) -> int:
    payload = {
        "name": name,
        "location_id": location_id,
        "qu_id_purchase": qu_id,
        "qu_id_stock": qu_id,
        "qu_id_consume": qu_id,
        "qu_id_price": qu_id,
        "min_stock_amount": 0,
        "default_best_before_days": 0,
    }
    r = requests.post(
        f"{URL}/api/objects/products",
        headers=HEADERS,
        data=json.dumps(payload),
        timeout=15,
    )
    r.raise_for_status()
    return r.json()["created_object_id"]


def add_stock(product_id: int, amount: int, location_id: int):
    payload = {
        "amount": amount,
        "transaction_type": "purchase",
        "location_id": location_id,
        "note": "Seeded for Play Store screenshots",
    }
    r = requests.post(
        f"{URL}/api/stock/products/{product_id}/add",
        headers=HEADERS,
        data=json.dumps(payload),
        timeout=15,
    )
    r.raise_for_status()


def delete_product(product_id: int):
    r = requests.delete(
        f"{URL}/api/objects/products/{product_id}",
        headers=HEADERS,
        timeout=15,
    )
    if r.status_code not in (200, 204):
        print(f"  delete product {product_id} returned {r.status_code}: {r.text}")


def seed():
    created = {"products": [], "locations": []}
    location_ids = {}
    for label in ("Fridge", "Cupboards"):
        before = requests.get(f"{URL}/api/objects/locations", headers=HEADERS, timeout=15).json()
        existed = any(loc["name"].lower() == label.lower() for loc in before)
        loc_id = get_or_create_location(label)
        location_ids[label] = loc_id
        if not existed:
            created["locations"].append(loc_id)
        print(f"Location {label}: id={loc_id}")

    for name, label, qu_id, amount in ITEMS:
        pid = create_product(name, location_ids[label], qu_id)
        add_stock(pid, amount, location_ids[label])
        created["products"].append({"id": pid, "name": name, "amount": amount, "location": label})
        print(f"  +{amount} {name} (id={pid}) -> {label}")

    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(created, f, indent=2)
    print(f"\nWrote {STATE_FILE}")


def cleanup():
    if not os.path.exists(STATE_FILE):
        print("Nothing to clean up (state file missing).")
        return
    with open(STATE_FILE, "r", encoding="utf-8") as f:
        state = json.load(f)
    for p in state.get("products", []):
        print(f"  deleting product id={p['id']} name={p['name']}")
        delete_product(p["id"])
    # leave locations in place — user may want them
    print("Cleanup complete.")
    os.remove(STATE_FILE)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "seed"
    if cmd == "seed":
        seed()
    elif cmd == "cleanup":
        cleanup()
    else:
        print("Usage: seed_grocy.py [seed|cleanup]")
        sys.exit(2)
