
## Considerations

 - Since each user has only one wallet, then I decided to not have a wallet table.
If I wanted to have a wallet table I would have a `wallet` and `wallet_asset` table instead of a `user_asset` table.
 - The auth mechanism is very simple. If I wanted to get serious about it I would rely on services like Keycloak.
But it doesn't seem to be the point of this exercise.