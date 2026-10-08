#!/usr/bin/env python3
"""Encode an authored, reserved-domain QR into a camera frame; never opens the URL.

Run: uv run --with qrcode==8.2 python scripts/generate_camera_fixture.py
The wide quiet area models a code fitting inside the emulator camera's field of view.
PNG pixels are generated from QR modules, not an edited photograph.
"""
from pathlib import Path
import struct
import zlib
import qrcode

URL = "https://paypal-secure.example/verify?redirect=https://example.com"
qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, border=4)
qr.add_data(URL)
qr.make(fit=True)
matrix = qr.get_matrix()
width, height, scale = 720, 720, 6
side = len(matrix) * scale
# The portrait imagefile camera samples the left side of its square source.
left, top = (width - side) // 6, (height - side) // 2
pixels = bytearray()
for y in range(height):
    row = bytearray([255] * width)
    if top <= y < top + side:
        modules = matrix[(y - top) // scale]
        for i, black in enumerate(modules):
            if black:
                start = left + i * scale
                row[start:start + scale] = b"\x00" * scale
    pixels += b"\x00" + row

def chunk(name, data):
    return struct.pack(">I", len(data)) + name + data + struct.pack(">I", zlib.crc32(name + data))

png = b"\x89PNG\r\n\x1a\n"
png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 0, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(pixels, 9))
png += chunk(b"IEND", b"")
destination = Path(__file__).resolve().parents[1] / "android-app/app/src/androidTest/assets/camera-qr.png"
destination.write_bytes(png)
print(f"Wrote {width}x{height} reserved-domain QR camera fixture")
