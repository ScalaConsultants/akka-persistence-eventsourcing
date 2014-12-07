Even if it will appear that one AR needs some resources from another (so in actors language it will mean calling actor)
it can be (or even should be) expressed in some DDD service trait (interface).

What if someone wants to return something even if event is published?

What if someone is publishing few events? We will not know which should be used to change state.