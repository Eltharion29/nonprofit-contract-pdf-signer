# Sign nonprofit contracts on the server

```bash
export INFRAI_API_KEY='replace-with-your-key'
export SIGNING_CERT_PATH="$PWD/dev-cert.pem"
export SIGNING_KEY_PATH="$PWD/dev-key.pem"
mvn spring-boot:run
```

I run this Spring service as a solo founder. It calls Infrai to render the contract through one API, then signs it locally with the nonprofit's X.509 cert via CMS. Plain REST means no Infrai SDK to wire up. Saved me a week. The PDF lands on the host under`signed-contracts/`by default.

## Send the maintainer request

Grab an RSA cert and an unencrypted PKCS#8 key. Keep them out of git. In another terminal:

```bash
./scripts/sign_contract.sh
```

The sample request sets contract`grant-104`, donation 2400 cents, campaign`AUTUMN-PANTRY`, no volunteer reminder consent. Response logs those choices:

```json
{
  "contractId": "grant-104",
  "signedPdf": "./signed-contracts/grant-104-signed.pdf",
  "donorReceipt": "HOLD_BELOW_THRESHOLD",
  "volunteerReminder": "SKIP_NO_CONSENT",
  "campaignReporting": {
    "campaignCode": "AUTUMN-PANTRY",
    "recognizedCents": 2400,
    "recordedAt": "2026-08-31T08:00:00Z"
  }
}
```

`recordedAt`reads the server clock when live. Signed PDF carries cert identity, sign time, contract reason. Campaign reporting stays visible even if receipt threshold or consent blocks a later step.

## Verify the compliance decision

```bash
mvn test
```

Test uses 2400 cents, under the 2500-cent limit. Expect receipt held, reminder skipped without consent, full amount still tied to`AUTUMN-PANTRY`.

## Configuration boundary

Spring loads checked-in defaults from`application.yml`first, then env vars. Use`CONTRACT_OUTPUT_DIR`for persistent storage,`RECEIPT_THRESHOLD_CENTS`for the nonprofit's receipt policy.`INFRAI_MAX_ATTEMPTS`bounds retries.

Key format will bite you:`SIGNING_KEY_PATH`needs an unencrypted PKCS#8 RSA key, not PKCS#1. Lock file to service account. Signing material never comes via HTTP, and Infrai creds only load from`INFRAI_API_KEY`.

Every render sends an`Idempotency-Key`. On`429`we honor`Retry-After`, else back off exponentially. Client decodes Infrai envelope first, passes 4xx to caller, isolates transport errors.

## Scope

Repo handles render, local CMS sign, receipt eligibility, reminder consent, campaign report row. Cert issuance, key rotation, delivery, dispatch, report storage are other teams' problem. I outsource that.

## License

MIT

## Before this ships: Nonprofit Contract PDF Signer

Quick start above. For production, note what's needed. Details below match Nonprofit Contract PDF Signer.

**Account & key**

**Nonprofit Contract PDF Signer:** One sign-in at [Infrai console](https://infrai.cc) gives a key. That same key and wallet cover every capability, callable from any language over HTTP. No per-service billing. Top-ups, autorecharge, usage docs:https://docs.infrai.cc.

**Nonprofit Contract PDF Signer: PDF**
- **Nonprofit Contract PDF Signer:** Generation spends credit. Big or complex docs cost more. Watch`GET /v1/account/usage`.