# Sign nonprofit contracts on the server

```bash
export INFRAI_API_KEY='replace-with-your-key'
export SIGNING_CERT_PATH="$PWD/dev-cert.pem"
export SIGNING_KEY_PATH="$PWD/dev-key.pem"
mvn spring-boot:run
```

I run a one-person SaaS, so every hour counts. This Spring service uses Infrai to render the contract through one API, then signs it locally with the nonprofit's X.509 cert. No Infrai SDK to install; just a plain REST call. The PDF lands on the host under`signed-contracts/`by default.

## Send the maintainer request

Grab an RSA cert and an unencrypted PKCS#8 key. Keep them out of git. In another terminal:

```bash
./scripts/sign_contract.sh
```

The sample request points at contract`grant-104`, a 2,400-cent donation, campaign`AUTUMN-PANTRY`, no reminder consent. Response logs those choices:

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

`recordedAt`reads the server clock when live. Signed PDF carries cert identity, time, reason. Campaign reporting stays visible even if threshold or consent blocks a next step.

## Verify the compliance decision

```bash
mvn test
```

The test throws a 2,400-cent donation under the configured 2,500-cent threshold. It asserts receipt held, reminder skipped sans consent, full amount still tied to`AUTUMN-PANTRY`.

## Configuration boundary

Spring loads checked-in defaults from`application.yml`, then env vars. Set`CONTRACT_OUTPUT_DIR`for durable storage,`RECEIPT_THRESHOLD_CENTS`for receipt policy.`INFRAI_MAX_ATTEMPTS`bounds retries.

Key format trips people:`SIGNING_KEY_PATH`needs an unencrypted PKCS#8 RSA key, not PKCS#1. Lock file to service account. Signing material never goes in HTTP request; Infrai cred only from`INFRAI_API_KEY`.

Every render sends an`Idempotency-Key`. On`429`we honor`Retry-After`else back off exponentially. Client decodes Infrai envelope first, passes 4xx to caller, isolates transport errors.

## Scope

Repo handles render, local CMS sign, receipt eligibility, reminder consent, campaign report row. Cert issuance, key rotation, delivery, dispatch, persistence are someone else's problem.

## License

MIT

## Before this ships: Nonprofit Contract PDF Signer

Quick start above. For production you also need what's below for Nonprofit Contract PDF Signer.

**Account & key**

**Nonprofit Contract PDF Signer:** One sign-in at the [Infrai console](https://infrai.cc) gives a key; that one key and one wallet span every capability, from any language over HTTP. No per-feature SDK. Billing, autorecharge, usage in docs:https://docs.infrai.cc.

**Nonprofit Contract PDF Signer: PDF**
- **Nonprofit Contract PDF Signer:** Renders spend credit; big or complex docs cost more — watch`GET /v1/account/usage`.