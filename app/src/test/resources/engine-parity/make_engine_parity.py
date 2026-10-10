"""Regenerates fake-vs-engine-cases.json by calling the real simulations endpoint of HipoSim.

Usage (from this folder): python -I make_engine_parity.py <base-url> <output-json>
The endpoint must be the real engine (hiposim-backEnd). Every case is sent with the app's reference rate.
"""
import json
import sys
import urllib.request

BASE = sys.argv[1].rstrip("/")
OUT = sys.argv[2]
RATE = 0.0885

# (name, price, down payment, years, bonus requested)
CASES = [
    ("Landing example with bonus", 280000, 42000, 20, True),
    ("Landing example without bonus", 280000, 42000, 20, False),
    ("Shortest term, cheapest home", 120000, 12000, 5, True),
    ("Top of the first bonus range", 200000, 40000, 10, True),
    ("Top of the second bonus range, longest term", 300000, 30000, 30, True),
    ("Second bonus range", 350000, 70000, 15, True),
    ("Top of the third bonus range", 400000, 80000, 25, True),
    ("Just above the bonus ranges", 400001, 80000, 20, True),
    ("Outside the bonus ranges", 450000, 90000, 20, True),
    ("Most expensive home, longest term", 800000, 160000, 30, True),
    ("Bonus not requested", 150000, 15000, 15, False),
]


def post(body):
    request = urllib.request.Request(
        BASE + "/api/simulations",
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def row(entry):
    keys = ("period", "installment", "interest", "amortization", "remainingBalance", "totalPayment")
    return {key: entry[key] for key in keys}


cases = []
for name, price, down, years, bonus in CASES:
    request = {
        "propertyPrice": price,
        "downPayment": down,
        "annualEffectiveRate": RATE,
        "termInMonths": years * 12,
        "applyGoodPayerBonus": bonus,
    }
    response = post(request)
    schedule = response["amortizationSchedule"]
    cases.append(
        {
            "name": name,
            "request": request,
            "expected": {
                "financedAmount": response["financedAmount"],
                "monthlyInstallment": response["monthlyInstallment"],
                "totalPaid": response["totalPaid"],
                "totalInterest": response["totalInterest"],
                "additionalCosts": response["additionalCosts"],
                "tcea": response["tcea"],
                "irr": response["irr"],
                "npv": response["npv"],
                "benefit": {
                    "eligible": response["benefit"]["eligible"],
                    "appliedAmount": response["benefit"]["appliedAmount"],
                },
                "rows": len(schedule),
                "firstRow": row(schedule[0]),
                "lastRow": row(schedule[-1]),
            },
        }
    )

with open(OUT, "w", encoding="utf-8", newline="\n") as handle:
    json.dump(cases, handle, indent=2, ensure_ascii=True)
    handle.write("\n")
print(f"{len(cases)} cases written to {OUT}")
