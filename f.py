#!/usr/bin/env python3
"""Downgrade API37-requiring dependencies to API36-compatible versions."""
import re, os

p = "V2rayNG/gradle/libs.versions.toml"
s = open(p).read()

fixes = [
    ('coreKtx = "1.19.1"', 'coreKtx = "1.16.0"'),
    ('okhttp = "5.5.0"', 'okhttp = "4.12.0"'),
    ('composeBom = "2026.09.00"', 'composeBom = "2025.04.00"'),
]

for old, new in fixes:
    if old in s:
        s = s.replace(old, new)
        print(f"Fixed: {old} -> {new}")

open(p, "w").write(s)
print("Done")
