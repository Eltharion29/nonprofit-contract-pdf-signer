#!/usr/bin/env sh
set -eu

curl --fail-with-body --request POST http://localhost:8080/contracts/sign \
  --header 'Content-Type: application/json' \
  --header 'Idempotency-Key: 11c10ad3-0ff6-4870-8f25-33ac75a26b18' \
  --data '{
    "contractId": "grant-104",
    "nonprofitName": "River Food Fund",
    "donorName": "A. Donor",
    "donationCents": 2400,
    "campaignCode": "AUTUMN-PANTRY",
    "volunteerReminderConsent": false,
    "contractHtml": "<html><body><h1>River Food Fund</h1><p>Donation agreement for A. Donor.</p></body></html>"
  }'
