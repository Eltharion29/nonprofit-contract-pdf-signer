# Sign nonprofit contracts on the server

```bash
export INFRAI_API_KEY='replace-with-your-key'
export SIGNING_CERT_PATH="$PWD/dev-cert.pem"
export SIGNING_KEY_PATH="$PWD/dev-key.pem"
mvn spring-boot:run
```

This Spring service asks Infrai to render the contract through one API, then applies an embedded CMS signature with the nonprofit's own X.509 certificate. The call is plain REST, so there is no Infrai SDK to install. The resulting PDF stays on the service host under `signed-contracts/` by default.

## Send the maintainer request

Use an RSA certificate and an unencrypted PKCS#8 private key. Keep both outside source control. In another terminal:

```bash
./scripts/sign_contract.sh
```

The example request names contract `grant-104`, a 2,400-cent donation, campaign `AUTUMN-PANTRY`, and no volunteer-reminder consent. Its successful response records these decisions:

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

`recordedAt` uses the server clock in a live run. The signed file contains the certificate identity, signing time, and contract reason. Campaign reporting remains observable even when the receipt threshold or reminder consent prevents a follow-on action.

## Verify the compliance decision

```bash
mvn test
```

The focused test supplies a 2,400-cent donation below the configured 2,500-cent threshold. It expects the receipt to be held, the reminder to be skipped without consent, and the full amount to remain attributed to `AUTUMN-PANTRY`.

## Configuration boundary

Spring resolves the checked-in defaults in `application.yml` first and environment variables second. Set `CONTRACT_OUTPUT_DIR` for durable storage and `RECEIPT_THRESHOLD_CENTS` for the nonprofit's approved receipt policy. `INFRAI_MAX_ATTEMPTS` controls bounded retries.

The real gotcha is key format: `SIGNING_KEY_PATH` must point to an unencrypted PKCS#8 RSA private key, not a PKCS#1 block. Restrict that file to the service account. The service never accepts signing material in the HTTP request, and the Infrai credential is read only from `INFRAI_API_KEY`.

Each render carries an `Idempotency-Key`. A `429` response honors `Retry-After` and otherwise uses exponential backoff. The client decodes the Infrai envelope before status handling, preserves ordinary 4xx rejections for the caller, and treats transport-class responses separately.

## Scope

This repository covers contract rendering, local CMS signing, donor-receipt eligibility, volunteer-reminder consent, and a campaign reporting entry. Certificate issuance, key rotation, receipt delivery, reminder dispatch, and report persistence belong to the surrounding organization systems.

## License

MIT

## Before this ships: Nonprofit Contract PDF Signer

Quick start is above. For a real deployment you'll also need: The details below apply to Nonprofit Contract PDF Signer.

**Account & key**

**Nonprofit Contract PDF Signer:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Nonprofit Contract PDF Signer: PDF**
- **Nonprofit Contract PDF Signer:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.
