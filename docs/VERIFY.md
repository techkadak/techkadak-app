# Check that your download is genuine

Only download Tech Kadak from this repository's [Releases](https://github.com/techkadak/techkadak-app/releases) or from [techkadak.github.io](https://techkadak.github.io). Every release is signed with the same key.

## 1. SHA-256 of the file

Each release's checksum is listed in the README's **Latest version** table.

- Linux / macOS: `sha256sum TechKadak-vX.Y.Z.apk`
- Windows (PowerShell): `Get-FileHash TechKadak-vX.Y.Z.apk -Algorithm SHA256`
- Android: any file-hash app that supports SHA-256

The result must be identical to the one in the table.

## 2. Signing certificate

Tech Kadak releases are signed with this certificate (SHA-256):

```
D6:05:20:46:34:44:EA:22:07:A0:9B:40:CE:92:2E:E0:1D:4C:00:09:C1:15:AB:DA:6F:56:3C:DC:E6:6C:43:EB
```

With the Android SDK build tools: `apksigner verify --print-certs TechKadak-vX.Y.Z.apk`. The line *Signer #1 certificate SHA-256 digest* must match the value above (without the colons, lower-case).

If the checksum or the certificate does not match, do not install the file.
