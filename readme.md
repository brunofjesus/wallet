
## Considerations

 - Since each user has only one wallet, then I decided to not have a wallet table.
If I wanted to have a wallet table I would have a `wallet` and `wallet_asset` table instead of a `user_asset` table.
 - The auth mechanism is very simple. If I wanted to get serious about it I would rely on services like Keycloak.
But it doesn't seem to be the point of this exercise.
 - The exercise request was a bit vague, it wasn't clear if the price and values shown on the wallet info
was the one from the customer input or the most recent one from our database. For that reason I decided to deviate a bit
from the example response payload and implemented a more complete alternative.